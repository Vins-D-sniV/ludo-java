package io.github.vinsdsniv.ludo.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class Game {
    private static final int YARD = -1;
    private static final int FINISH = 56;
    private static final int TRACK_LENGTH = 52;
    private static final int LAST_SHARED_STEP = 50;
    private static final Set<Integer> SAFE = Set.of(0, 8, 13, 21, 26, 34, 39, 47);

    private final int[][] positions;
    private int turn;
    private Integer die;
    private Integer lastRoll;
    private Integer winner;
    private long revision;

    public Game(int players) {
        if (players < 2 || players > 4) {
            throw new IllegalArgumentException("Choisis entre deux et quatre joueurs.");
        }
        positions = new int[players][4];
        for (int[] tokens : positions) {
            Arrays.fill(tokens, YARD);
        }
    }

    public void roll(int value, long expectedRevision) {
        checkAction(expectedRevision);
        if (die != null) {
            throw new IllegalStateException("Déplace d’abord un pion.");
        }
        if (value < 1 || value > 6) {
            throw new IllegalArgumentException("Le dé doit donner une valeur de 1 à 6.");
        }
        die = value;
        lastRoll = value;
        if (legalMoves().isEmpty()) {
            endTurn();
        }
        revision++;
    }

    public void move(int token, long expectedRevision) {
        checkAction(expectedRevision);
        if (!legalMoves().contains(token)) {
            throw new IllegalStateException("Ce pion ne peut pas être déplacé.");
        }
        int step = positions[turn][token];
        int destination = step == YARD ? 0 : step + die;
        positions[turn][token] = destination;
        if (destination <= LAST_SHARED_STEP) {
            capture(trackIndex(turn, destination));
        }
        if (Arrays.stream(positions[turn]).allMatch(position -> position == FINISH)) {
            winner = turn;
            die = null;
        } else {
            endTurn();
        }
        revision++;
    }

    public State state() {
        List<List<Integer>> players = Arrays.stream(positions)
                .map(tokens -> Arrays.stream(tokens).boxed().toList())
                .toList();
        return new State(players, turn, die, lastRoll, winner, legalMoves(), revision);
    }

    private void checkAction(long expectedRevision) {
        if (expectedRevision != revision) {
            throw new IllegalStateException("La partie a changé. Réessaie.");
        }
        if (winner != null) {
            throw new IllegalStateException("La partie est terminée.");
        }
    }

    private List<Integer> legalMoves() {
        if (die == null) {
            return List.of();
        }
        List<Integer> moves = new ArrayList<>();
        for (int token = 0; token < positions[turn].length; token++) {
            int position = positions[turn][token];
            if (position == YARD ? die == 6 : position + die <= FINISH) {
                moves.add(token);
            }
        }
        return List.copyOf(moves);
    }

    private void capture(int cell) {
        if (SAFE.contains(cell)) {
            return;
        }
        for (int player = 0; player < positions.length; player++) {
            if (player == turn) {
                continue;
            }
            for (int token = 0; token < positions[player].length; token++) {
                int step = positions[player][token];
                if (step >= 0 && step <= LAST_SHARED_STEP && trackIndex(player, step) == cell) {
                    positions[player][token] = YARD;
                }
            }
        }
    }

    private static int trackIndex(int player, int step) {
        return (13 * player + step) % TRACK_LENGTH;
    }

    private void endTurn() {
        if (die != 6) {
            turn = (turn + 1) % positions.length;
        }
        die = null;
    }

    public record State(List<List<Integer>> players, int turn, Integer die, Integer lastRoll,
                        Integer winner, List<Integer> legalMoves, long revision) {}
}
