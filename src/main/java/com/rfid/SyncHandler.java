package com.rfid;

import java.io.File;

public interface SyncHandler {
    void syncFromDatabase();
    void uploadCsv(File csvFile);
    void downloadReport();
}
