package io.github.vinsdsniv.ludo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GameTest {
    @Test
    void aRollWithoutMovesPassesTheTurn() {
        Game game = new Game(2);
        roll(game, 5);
        assertThat(game.state().turn()).isEqualTo(1);
        assertThat(game.state().die()).isNull();
        assertThat(game.state().lastRoll()).isEqualTo(5);
    }

    @Test
    void sixUnlocksTheYardAndKeepsTheTurnAfterMoving() {
        Game game = new Game(2);
        roll(game, 6);
        assertThat(game.state().legalMoves()).containsExactly(0, 1, 2, 3);
        assertThatThrownBy(() -> roll(game, 6)).isInstanceOf(IllegalStateException.class);
        game.move(0, 1);
        assertThat(game.state().players().get(0)).containsExactly(0, -1, -1, -1);
        assertThat(game.state().turn()).isZero();
        assertThat(game.state().die()).isNull();
        assertThatThrownBy(() -> game.roll(6, 1)).isInstanceOf(IllegalStateException.class);
        assertThat(game.state().revision()).isEqualTo(2);
    }

    @Test
    void anOpponentIsCapturedOnAnOrdinaryCell() {
        Game game = new Game(2);
        play(game, 6, 0);
        play(game, 6, 0);
        play(game, 6, 0);
        play(game, 2, 0);
        play(game, 6, 0);
        play(game, 1, 0);
        assertThat(game.state().players().get(0).get(0)).isEqualTo(-1);
        assertThat(game.state().players().get(1).get(0)).isEqualTo(1);
    }

    @Test
    void tokensShareSafeCells() {
        Game game = new Game(2);
        play(game, 6, 0);
        play(game, 6, 0);
        play(game, 6, 0);
        play(game, 1, 0);
        play(game, 6, 0);
        assertThat(game.state().players().get(0).get(0)).isEqualTo(13);
        assertThat(game.state().players().get(1).get(0)).isZero();
    }

    @Test
    void reachingTheFinishRequiresAnExactRollAndFourTokensWin() {
        Game game = new Game(2);
        for (int token = 0; token < 4; token++) {
            play(game, 6, token);
            for (int move = 0; move < 9; move++) {
                play(game, 6, token);
            }
            roll(game, 3);
            assertThat(game.state().players().get(0).get(token)).isEqualTo(54);
            assertThat(game.state().turn()).isEqualTo(1);
            roll(game, 1);
            play(game, 2, token);
            if (token < 3) {
                assertThat(game.state().winner()).isNull();
                roll(game, 1);
            }
        }
        assertThat(game.state().winner()).isZero();
        assertThat(game.state().players().get(0)).containsExactly(56, 56, 56, 56);
        assertThatThrownBy(() -> roll(game, 6)).isInstanceOf(IllegalStateException.class);
    }

    private static void play(Game game, int value, int token) {
        roll(game, value);
        game.move(token, game.state().revision());
    }

    private static void roll(Game game, int value) {
        game.roll(value, game.state().revision());
    }
}
