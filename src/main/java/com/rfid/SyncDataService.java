package com.rfid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.impinj.octane.Tag;
import okhttp3.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SyncDataService implements SyncHandler {
    private MarathonPanel marathonPanel;
    private TagStorage storage;
    private Util util;
    private final OkHttpClient client = new OkHttpClient();
    private final GenerateReport report = new GenerateReport();

    public SyncDataService(MarathonPanel marathonPanel, TagStorage storage,Util util){
        this.marathonPanel = marathonPanel;
        this.storage = storage;
        this.util = util;
    }

    @Override
    public void normalSyncFromDatabase(Map<Integer,List<String>> lapInMap) {
        marathonPanel.getSyncStatusLabel().setText("Syncing data from database...");
        marathonPanel.getSyncStatusLabel().setForeground(new Color(59, 130, 246));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    String marathonName = marathonPanel.getMarathonNameField().getText().trim();
                    Map<Integer, List<TagDetail>> lapTags = buildLapTagMap(lapInMap);

                    if (lapTags.isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            marathonPanel.getSyncStatusLabel().setText("No tags found to be synced");
                            marathonPanel.getSyncStatusLabel().setForeground(Color.ORANGE);
                            util.addLog("No tags found in local storage, skipping sync.");
                        });
                        return null;
                    }
                    String jsonBody = buildSyncRequestJson(marathonName, lapTags);
                    // 3. Send POST request
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8083/api/rfid/sync"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        try {
                            List<String> allSyncedIps = lapInMap.values().stream()
                                    .flatMap(List::stream)
                                    .distinct()
                                    .collect(Collectors.toList());
                            storage.deleteByReaderIps(allSyncedIps);
                        } catch (Exception ex) {
                            util.addLog("Failed to clear local tags: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                        SwingUtilities.invokeLater(() -> {
                            marathonPanel.getMarathonNameField().setText("");
                            marathonPanel.getSyncStatusLabel().setText("Database sync completed successfully!");
                            marathonPanel.getSyncStatusLabel().setForeground(new Color(34, 197, 94));
                            util.addLog("Database sync successful: " + response.body());
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                // parse error JSON cleanly
                                ObjectMapper mapper = new ObjectMapper();
                                JsonNode node = mapper.readTree(response.body());
                                String errorMessage = node.has("errorDescription")
                                        ? node.get("errorDescription").asText()
                                        : "Sync failed";

                                marathonPanel.getSyncStatusLabel().setText(errorMessage);
                                marathonPanel.getSyncStatusLabel().setForeground(Color.RED);
                                util.addLog("Sync failed: " + errorMessage);
                            } catch (Exception parseEx) {
                                // fallback if response is not JSON
                                marathonPanel.getSyncStatusLabel().setText("Sync failed");
                                marathonPanel.getSyncStatusLabel().setForeground(Color.RED);
                                util.addLog("Sync failed (unparseable body): " + response.body());
                            }
                        });
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        marathonPanel.getSyncStatusLabel().setText("Something went wrong during sync!");
                        marathonPanel.getSyncStatusLabel().setForeground(Color.RED);
                        util.addLog("Unexpected error during sync: " + ex.getMessage());
                    });
                }
                return null;
            }
        };
        worker.execute();
    }

    @Override
    public void mergeSyncFromDatabase(Map<Integer, List<String>> lapIpMap) {
        marathonPanel.getSyncStatusLabel().setText("🔄 Merging and syncing data...");
        marathonPanel.getSyncStatusLabel().setForeground(new Color(59, 130, 246));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String marathonName = marathonPanel.getMarathonNameField().getText().trim();
                    Map<Integer, List<TagDetail>> mergedLapTagMap = new HashMap<>();

                    for (Map.Entry<Integer, List<String>> entry : lapIpMap.entrySet()) {
                        int lapNumber = entry.getKey();
                        List<String> ips = entry.getValue();

                        List<TagDetail> allTags = new ArrayList<>();
                        for (String ip : ips) {
                            List<TagDetail> tags = storage.findTagDetailsByIp(ip);
                            if (tags != null) allTags.addAll(tags);
                        }

                        Map<String, TagDetail> uniqueTags = allTags.stream()
                                .collect(Collectors.toMap(
                                        TagDetail::getTagId,
                                        t -> t,
                                        (t1, t2) -> t1.getLastSeen().isAfter(t2.getLastSeen()) ? t1 : t2
                                ));

                        mergedLapTagMap.put(lapNumber, new ArrayList<>(uniqueTags.values()));
                    }

                    if (mergedLapTagMap.isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            marathonPanel.getSyncStatusLabel().setText("No tags found to merge or sync");
                            marathonPanel.getSyncStatusLabel().setForeground(Color.ORANGE);
                            util.addLog("No tags found to merge, skipping sync.");
                        });
                        return null;
                    }

                    String jsonBody = buildSyncRequestJson(marathonName, mergedLapTagMap);

                    // 4️⃣ Send to server
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8083/api/rfid/sync"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // 5️⃣ On success → delete merged IP records
                    if (response.statusCode() == 200) {
                        try {
                            List<String> allSyncedIps = lapIpMap.values().stream()
                                    .flatMap(List::stream)
                                    .distinct()
                                    .collect(Collectors.toList());
                            storage.deleteByReaderIps(allSyncedIps);
                        } catch (Exception ex) {
                            util.addLog("Failed to clear merged IP tags: " + ex.getMessage());
                        }

                        SwingUtilities.invokeLater(() -> {
                            marathonPanel.getSyncStatusLabel().setText("Merge sync completed successfully!");
                            marathonPanel.getSyncStatusLabel().setForeground(new Color(34, 197, 94));
                            util.addLog("Merge sync successful: " + response.body());
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            marathonPanel.getSyncStatusLabel().setText("Merge sync failed!");
                            marathonPanel.getSyncStatusLabel().setForeground(Color.RED);
                            util.addLog("Merge sync failed: " + response.body());
                        });
                    }

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        marathonPanel.getSyncStatusLabel().setText("Something went wrong during merge sync!");
                        marathonPanel.getSyncStatusLabel().setForeground(Color.RED);
                        util.addLog("Error in merge sync: " + ex.getMessage());
                    });
                }
                return null;
            }
        };
        worker.execute();
    }

    private Map<Integer, List<TagDetail>> buildLapTagMap(Map<Integer, List<String>> lapIpMap) {
        Map<Integer, List<TagDetail>> lapTagMap = new HashMap<>();

        for (Map.Entry<Integer, List<String>> entry : lapIpMap.entrySet()) {
            int lapNumber = entry.getKey();
            List<String> ips = entry.getValue();

            List<TagDetail> tagsForLap = ips.stream()
                    .flatMap(ip -> storage.findTagDetailsByIp(ip).stream()) // ✅ flatten List<TagDetail>
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!tagsForLap.isEmpty()) {
                lapTagMap.put(lapNumber, tagsForLap);
            }
        }
        return lapTagMap;
    }

    @Override
    public void uploadCsv(File csvFile,Map<Integer,List<String>> lapInMap) {
        marathonPanel.getSyncStatusLabel().setText("📤 Uploading CSV...");
        marathonPanel.getSyncStatusLabel().setForeground(new Color(59, 130, 246));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String marathonName = marathonPanel.getMarathonNameField().getText().trim();

                    List<CsvRequest> requests = new ArrayList<>();
                    lapInMap.forEach((lap, readers) ->
                            readers.forEach(reader ->
                                    requests.add(new CsvRequest(lap, reader))
                            )
                    );
                    ObjectMapper mapper = new ObjectMapper();
                    String requestsJson = mapper.writeValueAsString(requests);

                    RequestBody fileBody = RequestBody.create(csvFile, MediaType.parse("text/csv"));
                    MultipartBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", csvFile.getName(),
                                    RequestBody.create(csvFile, MediaType.parse("text/csv"))
                            )
                            .addFormDataPart("marathon", marathonName)
                            .addFormDataPart("requests", null,
                                    RequestBody.create(requestsJson, MediaType.parse("application/json"))
                            )
                            .build();

                    String uploadUrl = "http://localhost:8083/api/rfid/file-upload?marathon="
                            + marathonName;

                    Request uploadRequest = new Request.Builder()
                            .url(uploadUrl)
                            .post(requestBody)
                            .build();

                    // ✅ 2. Call /file-upload
                    try (Response uploadResponse = client.newCall(uploadRequest).execute()) {
                        if (!uploadResponse.isSuccessful() || uploadResponse.body() == null) {
                            String body = uploadResponse.body() != null ? uploadResponse.body().string() : "";
                            String errMsg = parseErrorMessage(body, "Upload failed");
                            updateErrorUI(errMsg);
                            return null;
                        }

                        String parsedRequestBody = uploadResponse.body().string();
                        util.addLog("Upload success, got parsed body: " + parsedRequestBody);

                        // ✅ 3. Now send parsedRequestBody to /sync
                        Request syncRequest = new Request.Builder()
                                .url("http://localhost:8083/api/rfid/sync")
                                .header("Content-Type", "application/json")
                                .post(RequestBody.create(parsedRequestBody, MediaType.parse("application/json")))
                                .build();

                        try (Response syncResponse = client.newCall(syncRequest).execute()) {
                            if (syncResponse.isSuccessful()) {
                                String syncBody = syncResponse.body() != null ? syncResponse.body().string() : "";
                                List<String> allSyncedIps = lapInMap.values().stream()
                                        .flatMap(List::stream)
                                        .distinct()
                                        .collect(Collectors.toList());
                                storage.deleteByReaderIps(allSyncedIps);
                                SwingUtilities.invokeLater(() -> {
                                    marathonPanel.getSyncStatusLabel().setText("✅ CSV uploaded & synced!");
                                    marathonPanel.getSyncStatusLabel().setForeground(new Color(34, 197, 94));
                                    util.addLog("Sync success: " + syncBody);
                                });
                            } else {
                                String syncBody = syncResponse.body() != null ? syncResponse.body().string() : "";
                                String errorMessage = parseErrorMessage(syncBody, "Sync failed");
                                updateErrorUI(errorMessage);
                            }
                        }
                    }
                } catch (Exception ex) {
                    // 🟢 Only handle truly unexpected errors here
                    updateErrorUI("Unexpected error: " + ex.getMessage());
                    ex.printStackTrace();
                }
                return null;
            }
        };
        worker.execute();
    }

    @Override
    public void mergeUploadCsv(File csvFile, Map<Integer, List<String>> lapInMap) {
        marathonPanel.getSyncStatusLabel().setText("📤 Uploading CSV...");
        marathonPanel.getSyncStatusLabel().setForeground(new Color(59, 130, 246));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String marathonName = marathonPanel.getMarathonNameField().getText().trim();

                    List<CsvRequest> requests = new ArrayList<>();
                    lapInMap.forEach((lap, readers) ->
                            readers.forEach(reader ->
                                    requests.add(new CsvRequest(lap, reader))
                            )
                    );
                    ObjectMapper mapper = new ObjectMapper();
                    String requestsJson = mapper.writeValueAsString(requests);

                    RequestBody fileBody = RequestBody.create(csvFile, MediaType.parse("text/csv"));
                    MultipartBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", csvFile.getName(),
                                    RequestBody.create(csvFile, MediaType.parse("text/csv"))
                            )
                            .addFormDataPart("marathon", marathonName)
                            .addFormDataPart("requests", null,
                                    RequestBody.create(requestsJson, MediaType.parse("application/json"))
                            )
                            .build();

                    String uploadUrl = "http://localhost:8083/api/rfid/file-upload?marathon="
                            + marathonName;

                    Request uploadRequest = new Request.Builder()
                            .url(uploadUrl)
                            .post(requestBody)
                            .build();

                    // ✅ 2. Call /file-upload
                    try (Response uploadResponse = client.newCall(uploadRequest).execute()) {
                        if (!uploadResponse.isSuccessful() || uploadResponse.body() == null) {
                            String body = uploadResponse.body() != null ? uploadResponse.body().string() : "";
                            String errMsg = parseErrorMessage(body, "Upload failed");
                            updateErrorUI(errMsg);
                            return null;
                        }

                        String parsedRequestBody = uploadResponse.body().string();
                        util.addLog("Upload success, got parsed body: " + parsedRequestBody);

                        SyncDataRequest fullRequest = mapper.readValue(parsedRequestBody, SyncDataRequest.class);

                        // ✅ 3. Now send parsedRequestBody to /sync
                        Request syncRequest = new Request.Builder()
                                .url("http://localhost:8083/api/rfid/sync")
                                .header("Content-Type", "application/json")
                                .post(RequestBody.create(parsedRequestBody, MediaType.parse("application/json")))
                                .build();

                        try (Response syncResponse = client.newCall(syncRequest).execute()) {
                            if (syncResponse.isSuccessful()) {
                                String syncBody = syncResponse.body() != null ? syncResponse.body().string() : "";
                                List<String> allSyncedIps = lapInMap.values().stream()
                                        .flatMap(List::stream)
                                        .distinct()
                                        .collect(Collectors.toList());
                                storage.deleteByReaderIps(allSyncedIps);

                                SwingUtilities.invokeLater(() -> {
                                    marathonPanel.getSyncStatusLabel().setText("✅ CSV uploaded & synced!");
                                    marathonPanel.getSyncStatusLabel().setForeground(new Color(34, 197, 94));
                                    util.addLog("Sync success: " + syncBody);
                                });
                            } else {
                                String syncBody = syncResponse.body() != null ? syncResponse.body().string() : "";
                                String errorMessage = parseErrorMessage(syncBody, "Sync failed");
                                updateErrorUI(errorMessage);
                            }
                        }
                    }
                } catch (Exception ex) {
                    // 🟢 Only handle truly unexpected errors here
                    updateErrorUI("Unexpected error: " + ex.getMessage());
                    ex.printStackTrace();
                }
                return null;
            }
        };
        worker.execute();
    }

    @Override
    public void downloadReport() {
        try {
            // 1. Fetch all tag details
            List<TagDetail> tags = storage.findAll();
            if (tags.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    marathonPanel.getSyncStatusLabel().setText("No tags found to be synced");
                    marathonPanel.getSyncStatusLabel().setForeground(Color.ORANGE);
                    util.addLog("No tags found in database.");
                });
                return;
            }

            // 2. Open file chooser dialog for CSV
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save RFID Report");
            fileChooser.setSelectedFile(new File("rfid-report.csv"));

            int userSelection = fileChooser.showSaveDialog(null);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();

                try (PrintWriter writer = new PrintWriter(new FileWriter(fileToSave))) {
                    writer.println("tagId,antenna,firstSeen,lastSeen");
                    // Data rows
                    for (TagDetail tag : tags) {
                        writer.printf("%s,%d,%s,%s%n",
                                tag.getTagId(),
                                tag.getAntenna(),
                                tag.getFirstSeen() != null ? tag.getFirstSeen().toString() : "",
                                tag.getLastSeen() != null ? tag.getLastSeen().toString() : "");
                    }
                }
                JOptionPane.showMessageDialog(null,
                        "Report saved to:\n" + fileToSave.getAbsolutePath(),
                        "Download Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error while downloading report: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public List<TagDetail> fetchUnsyncedIpTags() {
        List<TagDetail> tags = storage.fetchUnsyncedIpTags();
        return tags;
    }

    private void updateErrorUI(String message) {
        SwingUtilities.invokeLater(() -> {
            marathonPanel.getSyncStatusLabel().setText(message);
            marathonPanel.getSyncStatusLabel().setForeground(Color.RED);
            util.addLog("Error: " + message);
        });
    }

    private String parseErrorMessage(String responseBody, String defaultMsg) {
        if (responseBody == null || responseBody.isEmpty()) {
            return defaultMsg;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseBody);
            if (node.has("errorDescription")) {
                return node.get("errorDescription").asText();
            }
            return responseBody; // raw body if no errorDescription
        } catch (Exception e) {
            return responseBody; // fallback: raw body
        }
    }

    private String buildSyncRequestJson(String marathonName, Map<Integer, List<TagDetail>> lapToTagsMap) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.put("marathon", marathonName);

        ArrayNode lapsArray = mapper.createArrayNode();

        for (Map.Entry<Integer, List<TagDetail>> entry : lapToTagsMap.entrySet()) {
            int lapNumber = entry.getKey();
            List<TagDetail> tags = entry.getValue();

            ObjectNode lapNode = mapper.createObjectNode();
            lapNode.put("lapNumber", lapNumber);

            ArrayNode tagsArray = mapper.createArrayNode();
            for (TagDetail tag : tags) {
                ObjectNode tagNode = mapper.createObjectNode();
                tagNode.put("tagId", tag.getTagId());
                tagNode.put("antenna", tag.getAntenna());
                tagNode.put("firstSeen", tag.getFirstSeen().toString());
                tagNode.put("lastSeen", tag.getLastSeen().toString());
                tagsArray.add(tagNode);
            }

            lapNode.set("tags", tagsArray);
            lapsArray.add(lapNode);
        }
        root.set("laps", lapsArray);
        return mapper.writeValueAsString(root);
    }

}