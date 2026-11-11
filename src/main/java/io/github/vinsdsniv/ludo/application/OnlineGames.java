package io.github.vinsdsniv.ludo.application;

import io.github.vinsdsniv.ludo.domain.Game;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class OnlineGames {
    private final Map<UUID, Room> rooms = new ConcurrentHashMap<>();

    public Ticket create(String nickname) {
        UUID id = UUID.randomUUID();
        Room room = new Room(nickname(nickname, "Rouge"));
        Ticket ticket = new Ticket(room.host, room.view(id, 0));
        rooms.put(id, room);
        return ticket;
    }

    public Ticket join(UUID id, UUID token, String nickname) {
        Room room = find(id);
        synchronized (room) {
            if (room.host.equals(token)) {
                return new Ticket(token, room.view(id, 0));
            }
            if (room.guest == null) {
                room.guestName = nickname(nickname, "Vert");
                room.guest = token;
            }
            if (!room.guest.equals(token)) {
                throw new IllegalStateException("Les deux places de cette partie sont prises.");
            }
            return new Ticket(token, room.view(id, 1));
        }
    }

    public View read(UUID id, UUID token) {
        Room room = find(id);
        synchronized (room) {
            return room.view(id, room.player(token));
        }
    }

    public View roll(UUID id, UUID token, long revision) {
        return play(id, token, game -> game.roll(ThreadLocalRandom.current().nextInt(1, 7), revision));
    }

    public View move(UUID id, UUID token, int piece, long revision) {
        return play(id, token, game -> game.move(piece, revision));
    }

    public View resign(UUID id, UUID token) {
        Room room = find(id);
        synchronized (room) {
            int player = room.player(token);
            room.requireGuest();
            if (!Integer.valueOf(player).equals(room.resignedBy)) {
                room.game.forfeit(player);
                room.resignedBy = player;
            }
            return room.view(id, player);
        }
    }

    private static String nickname(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String name = value.strip();
        if (name.length() > 24 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Le pseudo doit contenir 24 caractères maximum, sur une seule ligne.");
        }
        return name;
    }

    private View play(UUID id, UUID token, Consumer<Game> action) {
        Room room = find(id);
        synchronized (room) {
            int player = room.player(token);
            room.requireGuest();
            if (room.game.state().turn() != player) {
                throw new IllegalStateException("Ce n’est pas ton tour.");
            }
            action.accept(room.game);
            return room.view(id, player);
        }
    }

    private Room find(UUID id) {
        Room room = rooms.get(id);
        if (room == null) {
            throw new NoSuchElementException("Partie introuvable. Elle a peut-être été perdue au redémarrage du serveur.");
        }
        return room;
    }

    private static final class Room {
        private final Game game = new Game(2);
        private final UUID host = UUID.randomUUID();
        private final String hostName;
        private UUID guest;
        private String guestName;
        private Integer resignedBy;

        private Room(String hostName) {
            this.hostName = hostName;
        }

        private void requireGuest() {
            if (guest == null) {
                throw new IllegalStateException("Attends que le deuxième joueur rejoigne la partie.");
            }
        }

        private int player(UUID token) {
            if (host.equals(token)) {
                return 0;
            }
            if (token != null && token.equals(guest)) {
                return 1;
            }
            throw new AccessDenied();
        }

        private View view(UUID id, int player) {
            List<String> names = guest == null ? List.of(hostName) : List.of(hostName, guestName);
            return new View(id, player, guest != null, names, resignedBy, game.state());
        }
    }

    public static final class AccessDenied extends RuntimeException {
        public AccessDenied() {
            super("Cet écran n’a pas accès à cette partie.");
        }
    }

    public record View(UUID id, int player, boolean ready, List<String> nicknames, Integer resignedBy, Game.State state) {}
    public record Ticket(UUID token, View game) {}
}
