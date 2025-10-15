package com.rfid;

public class CsvRequest {
    private Integer lapNumber;
    private String reader;

    public Integer getLapNumber() {
        return lapNumber;
    }

    public void setLapNumber(Integer lapNumber) {
        this.lapNumber = lapNumber;
    }

    public CsvRequest(Integer lapNumber, String reader) {
        this.lapNumber = lapNumber;
        this.reader = reader;
    }

    public String getReader() {
        return reader;
    }

    public void setReader(String reader) {
        this.reader = reader;
    }
}
