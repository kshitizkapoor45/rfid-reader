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
import java.util.List;

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
    public void syncFromDatabase() {
        marathonPanel.getSyncStatusLabel().setText("🔄 Syncing data from database...");
        marathonPanel.getSyncStatusLabel().setForeground(new Color(59, 130, 246));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // 1. Fetch marathon info from DB
                    String marathonName = marathonPanel.getMarathonNameField().getText().trim();
                    int lapNumber = (Integer) marathonPanel.getLapNumberSpinner().getValue();

                    // Example: fetch tags from DB (replace with real DAO/service)
                    List<TagDetail> tags = storage.findAll();
                    if (tags.isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            marathonPanel.getSyncStatusLabel().setText("No tags found to be synced");
                            marathonPanel.getSyncStatusLabel().setForeground(Color.ORANGE);
                            util.addLog("No tags found in local storage, skipping sync.");
                        });
                        return null;
                    }
                    // 2. Build JSON
                    String jsonBody = buildSyncRequestJson(marathonName, lapNumber, tags);

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
                            storage.deleteAll();
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
    public void uploadCsv(File csvFile) {
        marathonPanel.getSyncStatusLabel().setText("📤 Uploading CSV...");
        marathonPanel.getSyncStatusLabel().setForeground(new Color(59, 130, 246));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String marathonName = marathonPanel.getMarathonNameField().getText().trim();
                    int lapNumber = (Integer) marathonPanel.getLapNumberSpinner().getValue();

                    // ✅ 1. Build multipart request
                    RequestBody fileBody = RequestBody.create(csvFile, MediaType.parse("text/csv"));
                    MultipartBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", csvFile.getName(), fileBody)
                            .build();

                    String uploadUrl = "http://localhost:8083/api/rfid/file-upload?marathon="
                            + marathonName + "&lap=" + lapNumber;

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

                // 3. Write CSV file to disk
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
    public void fetchUnsyncedIpTags() {
        List<TagDetail> tags = storage.findReaderIp();

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

    private String buildSyncRequestJson(String marathonName, int lapNumber, List<TagDetail> tags) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        // Marathon info
        ObjectNode marathonNode = mapper.createObjectNode();
        marathonNode.put("name", marathonName);
        marathonNode.put("lap", lapNumber);
        // Tags array
        ArrayNode tagsArray = mapper.createArrayNode();
        for (TagDetail t : tags) {
            ObjectNode tagNode = mapper.createObjectNode();
            tagNode.put("tagId", t.getTagId());
            tagNode.put("antenna", t.getAntenna());
            tagNode.put("lastSeen", t.getLastSeen().toString());
            tagNode.put("firstSeen", t.getFirstSeen().toString());
            tagsArray.add(tagNode);
        }

        // Root JSON
        ObjectNode root = mapper.createObjectNode();
        root.set("marathon", marathonNode);
        root.set("tags", tagsArray);

        return mapper.writeValueAsString(root);
    }
}