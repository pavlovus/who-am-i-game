package com.whoami.server.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameLogicTest {

    @Test
    public void questionConsumesCounterAndNeedsAnswerBeforeNext() {
        GameLogic logic = new GameLogic("Batman", 20, 3);

        assertTrue(logic.submitQuestion("Are you real?"));
        assertEquals(19, logic.getRemainingQuestions());
        assertFalse(logic.submitQuestion("Second?"), "cannot ask while a question is unanswered");

        assertTrue(logic.submitAnswer(GameLogic.Answer.NO));
        assertEquals("Are you real? => NO", logic.getTranscript().get(0));
        assertTrue(logic.submitQuestion("Second?"));
    }

    @Test
    public void answerWithoutPendingIsRejected() {
        GameLogic logic = new GameLogic("Batman");
        assertFalse(logic.submitAnswer(GameLogic.Answer.YES));
    }

    @Test
    public void correctGuessWinsForGuesser() {
        GameLogic logic = new GameLogic("Sherlock Holmes");
        assertEquals(GameLogic.GuessResult.CORRECT, logic.submitGuess("  sherlock holmes "));
        assertTrue(logic.isFinished());
        assertEquals(GameLogic.Winner.GUESSER, logic.getWinner());
    }

    @Test
    public void wrongGuessesExhaustAttemptsThenRiddlerWins() {
        GameLogic logic = new GameLogic("Mario", 20, 3);
        assertEquals(GameLogic.GuessResult.WRONG_RETRY, logic.submitGuess("Luigi"));
        assertEquals(GameLogic.GuessResult.WRONG_RETRY, logic.submitGuess("Peach"));
        assertEquals(1, logic.getRemainingGuesses(), "two of three attempts used");
        assertEquals(GameLogic.GuessResult.WRONG_GAME_OVER, logic.submitGuess("Bowser"));
        assertTrue(logic.isFinished());
        assertEquals(GameLogic.Winner.RIDDLER, logic.getWinner());
        assertEquals(GameLogic.GuessResult.REJECTED, logic.submitGuess("Mario"), "no guesses after game over");
    }

    @Test
    public void runningOutOfQuestionsGivesRiddlerTheWin() {
        GameLogic logic = new GameLogic("Yoda", 2, 3);
        assertTrue(logic.submitQuestion("q1"));
        assertTrue(logic.submitAnswer(GameLogic.Answer.PARTIALLY));
        assertTrue(logic.submitQuestion("q2"));
        assertFalse(logic.isFinished());
        assertTrue(logic.submitAnswer(GameLogic.Answer.NO));

        assertTrue(logic.isFinished());
        assertEquals(GameLogic.Winner.RIDDLER, logic.getWinner());
        assertFalse(logic.submitQuestion("q3"), "no questions after game over");
    }

    @Test
    public void cannotGuessAfterRunningOutOfQuestions() {
        GameLogic logic = new GameLogic("Yoda", 1, 3);
        logic.submitQuestion("q1");
        logic.submitAnswer(GameLogic.Answer.YES);
        assertEquals(GameLogic.GuessResult.REJECTED, logic.submitGuess("Yoda"));
    }
}
