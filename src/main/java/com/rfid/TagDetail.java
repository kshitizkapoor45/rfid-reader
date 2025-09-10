package com.rfid;

import java.time.Instant;

public class TagDetail {
    private String tagId;
    private int antenna;
    private Instant firstSeen;
    private Instant lastSeen;

    public TagDetail() {
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public int getAntenna() {
        return antenna;
    }

    public void setAntenna(int antenna) {
        this.antenna = antenna;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Instant firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public TagDetail(String tagId, int antenna, Instant firstSeen, Instant lastSeen) {
        this.tagId = tagId;
        this.antenna = antenna;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
    }
}