package io.github.vinsdsniv.ludo.application;

import io.github.vinsdsniv.ludo.domain.Game;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class Games {
    private final Map<UUID, Game> games = new ConcurrentHashMap<>();

    public Session create(int players) {
        Game game = new Game(players);
        UUID id = UUID.randomUUID();
        games.put(id, game);
        return read(id);
    }

    public Session read(UUID id) {
        return access(id, game -> {});
    }

    public Session roll(UUID id, long revision) {
        return access(id, game -> game.roll(ThreadLocalRandom.current().nextInt(1, 7), revision));
    }

    public Session move(UUID id, int token, long revision) {
        return access(id, game -> game.move(token, revision));
    }

    private Session access(UUID id, Consumer<Game> action) {
        Game game = games.get(id);
        if (game == null) {
            throw new NoSuchElementException("Partie introuvable.");
        }
        synchronized (game) {
            action.accept(game);
            return new Session(id, game.state());
        }
    }

    public record Session(UUID id, Game.State state) {}
}
