package com.rfid;

import java.util.List;

public class SyncDataRequest {
    private String marathon;
    private List<Lap> laps;

    public SyncDataRequest() {
    }

    public SyncDataRequest(String marathon, List<Lap> laps) {
        this.marathon = marathon;
        this.laps = laps;
    }

    public String getMarathon() {
        return marathon;
    }

    public void setMarathon(String marathon) {
        this.marathon = marathon;
    }

    public List<Lap> getLaps() {
        return laps;
    }

    public void setLaps(List<Lap> laps) {
        this.laps = laps;
    }

    public static class Lap {
        private int lapNumber;

        public Lap() {
        }

        public Lap(int lapNumber, List<TagDetail> tags) {
            this.lapNumber = lapNumber;
            this.tags = tags;
        }

        private List<TagDetail> tags;

        public int getLapNumber() {
            return lapNumber;
        }

        public void setLapNumber(int lapNumber) {
            this.lapNumber = lapNumber;
        }

        public List<TagDetail> getTags() {
            return tags;
        }

        public void setTags(List<TagDetail> tags) {
            this.tags = tags;
        }
    }
}