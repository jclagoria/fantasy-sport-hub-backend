package com.fantasysporthub.eventsourcing.core;

/**
 * Type-safe stream name builder.
 * Enforces naming conventions: {category}-{id}
 */
public record StreamName(String value) {

    public StreamName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Stream name cannot be null or blank");
        }
    }

    public static StreamName of(String category, String id) {
        return new StreamName("%s-%s".formatted(category, id));
    }

    public static StreamName match(String matchId) {
        return of("match", matchId);
    }

    public static StreamName playerId(String playerId, String date) {
        return of("player", "%s-week-%s".formatted(playerId, date));
    }

    public static StreamName league(String leagueId, String weekId) {
        return of("league", "%s-week-%s".formatted(leagueId, weekId));
    }

    @Override
    public String toString() {
        return value;
    }
}