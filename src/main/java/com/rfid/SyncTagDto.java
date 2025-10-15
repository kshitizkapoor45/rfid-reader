package com.rfid;

public class SyncTagDto {
    private String reader;
    private Integer lapNumber;

    public String getReader() {
        return reader;
    }

    public SyncTagDto() {
    }

    public SyncTagDto(String reader, Integer lapNumber) {
        this.reader = reader;
        this.lapNumber = lapNumber;
    }

    public void setReader(String reader) {
        this.reader = reader;
    }

    public Integer getLapNumber() {
        return lapNumber;
    }

    public void setLapNumber(Integer lapNumber) {
        this.lapNumber = lapNumber;
    }
}
