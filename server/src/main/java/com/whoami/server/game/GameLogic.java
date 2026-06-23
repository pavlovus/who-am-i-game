package com.whoami.server.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, self-contained rules of a single "Who am I?" round. No sockets, no
 * database, so the whole gameplay flow is unit-testable. The guesser asks up to
 * {@link #maxQuestions} yes/no questions and has up to {@link #maxGuessAttempts}
 * final guesses; running out of either hands the win to the riddler.
 */
public class GameLogic {

    public enum Answer { YES, NO, PARTIALLY }

    public enum Winner { NONE, GUESSER, RIDDLER }

    public enum GuessResult { CORRECT, WRONG_RETRY, WRONG_GAME_OVER, REJECTED }

    private final String characterName;
    private final int maxQuestions;
    private final int maxGuessAttempts;

    private int questionsAsked;
    private int guessAttemptsUsed;
    private String pendingQuestion;
    private String lastQuestion;
    private Answer lastAnswer;
    private final List<String> transcript = new ArrayList<>();

    private boolean finished;
    private Winner winner = Winner.NONE;

    public GameLogic(String characterName) {
        this(characterName, 20, 3);
    }

    public GameLogic(String characterName, int maxQuestions, int maxGuessAttempts) {
        this.characterName = characterName;
        this.maxQuestions = maxQuestions;
        this.maxGuessAttempts = maxGuessAttempts;
    }

    /** Guesser asks a question. Rejected if finished, none left, or one is unanswered. */
    public synchronized boolean submitQuestion(String question) {
        if (finished || pendingQuestion != null || question == null || question.isBlank()
                || getRemainingQuestions() <= 0) {
            return false;
        }
        pendingQuestion = question.trim();
        questionsAsked++;
        return true;
    }

    /** Riddler answers the pending question; the round ends if questions run out. */
    public synchronized boolean submitAnswer(Answer answer) {
        if (finished || pendingQuestion == null || answer == null) {
            return false;
        }
        lastQuestion = pendingQuestion;
        lastAnswer = answer;
        transcript.add(lastQuestion + " => " + answer);
        pendingQuestion = null;
        if (getRemainingQuestions() <= 0) {
            finish(Winner.RIDDLER);
        }
        return true;
    }

    /** Guesser's final attempt to name the character (case-insensitive). */
    public synchronized GuessResult submitGuess(String guess) {
        if (finished || guess == null || guess.isBlank() || getRemainingGuesses() <= 0) {
            return GuessResult.REJECTED;
        }
        if (guess.trim().equalsIgnoreCase(characterName.trim())) {
            finish(Winner.GUESSER);
            return GuessResult.CORRECT;
        }
        guessAttemptsUsed++;
        if (getRemainingGuesses() <= 0) {
            finish(Winner.RIDDLER);
            return GuessResult.WRONG_GAME_OVER;
        }
        return GuessResult.WRONG_RETRY;
    }

    private void finish(Winner result) {
        this.finished = true;
        this.winner = result;
        this.pendingQuestion = null;
    }

    public synchronized int getRemainingQuestions() {
        return maxQuestions - questionsAsked;
    }

    public synchronized int getRemainingGuesses() {
        return maxGuessAttempts - guessAttemptsUsed;
    }

    public synchronized int getQuestionsAsked() {
        return questionsAsked;
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized Winner getWinner() {
        return winner;
    }

    public synchronized String getPendingQuestion() {
        return pendingQuestion;
    }

    public synchronized String getLastQuestion() {
        return lastQuestion;
    }

    public synchronized Answer getLastAnswer() {
        return lastAnswer;
    }

    public synchronized List<String> getTranscript() {
        return List.copyOf(transcript);
    }

    public String getCharacterName() {
        return characterName;
    }
}
