package com.rfid;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface SyncHandler {
    void normalSyncFromDatabase(Map<Integer,List<String>> lapInMap);
    void uploadCsv(File csvFile,Map<Integer,List<String>> lapInMap);
    void downloadReport();
    List<TagDetail> fetchUnsyncedIpTags();
    void mergeSyncFromDatabase(Map<Integer, List<String>> lapIpMap);
    void mergeUploadCsv(File selectedFile, Map<Integer, List<String>> tagIpMap);

//    void mergeSyncFromDatabase(Map<Integer, List<String>> lapIpMap);
//    void syncSelectedTags(SyncTagDto tagDto, boolean flush);
}