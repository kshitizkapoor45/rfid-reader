package com.rfid;

public interface SyncHandler {
    void syncFromDatabase();
    void uploadCsv();
}
