package com.rfid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class SyncDataService implements SyncHandler {
    private MarathonPanel marathonPanel;
    private TagStorage storage;
    private Util util;

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
                            .uri(URI.create("http://localhost:8080/api/rfid/sync"))
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
    public void uploadCsv() {

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