package com.github.gcolin.event;

/**
 * A selectable open competition for Sharly Chess import: either a collection
 * with at least one ACTIVE tournament, or a standalone ACTIVE tournament.
 */
public class SharlySelectItem {

    public enum Kind {
        COLLECTION,
        EVENT
    }

    private final Kind kind;
    private final Integer collectionId;
    private final Integer eventId;
    private final String chessEventId;
    private final String name;
    private final int tournamentCount;

    public SharlySelectItem(
            Kind kind,
            Integer collectionId,
            Integer eventId,
            String chessEventId,
            String name,
            int tournamentCount) {
        this.kind = kind;
        this.collectionId = collectionId;
        this.eventId = eventId;
        this.chessEventId = chessEventId;
        this.name = name;
        this.tournamentCount = tournamentCount;
    }

    public Kind getKind() {
        return kind;
    }

    public Integer getCollectionId() {
        return collectionId;
    }

    public Integer getEventId() {
        return eventId;
    }

    public String getChessEventId() {
        return chessEventId;
    }

    public String getName() {
        return name;
    }

    public int getTournamentCount() {
        return tournamentCount;
    }

    public boolean isCollection() {
        return kind == Kind.COLLECTION;
    }
}
