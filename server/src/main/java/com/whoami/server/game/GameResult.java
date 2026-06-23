package com.whoami.server.game;

/**
 * Snapshot of a finished round to persist. User ids are nullable because guests
 * may not have authenticated. {@code winnerUserId} is the id of whichever player
 * actually won (guesser or riddler), or null if that winner was not authenticated.
 */
public record GameResult(String roomCode,
                         Integer riddlerUserId,
                         Integer guesserUserId,
                         Integer winnerUserId,
                         String characterName,
                         int questionsAsked) {
}
