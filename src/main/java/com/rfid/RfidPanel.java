package com.rfid;

import javax.swing.*;
import javax.swing.text.html.HTML;
import java.awt.*;
import java.util.function.Consumer;

public class RfidPanel {

    private JPanel mainPanel;
    private JTextField ipAddressField;
    private JButton connectButton;
    private JButton startReaderButton;
    private JButton downloadReportsButton;
    private JLabel connectionStatusLabel;
    private JTextArea logsArea;
    private JLabel tagCountLabel;
    private SyncHandler syncHandler;

    public RfidPanel(SyncHandler syncHandler) {
        this.syncHandler = syncHandler;
    }
    public RfidPanel() {
    }

    public void setSyncHandler(SyncHandler syncHandler) {
        this.syncHandler = syncHandler;
    }

    private int tagCount = 0;
    private Consumer<Void> onLogsCleared;
    public JPanel createRFIDPanel() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top controls
        JPanel topPanel = new JPanel(new BorderLayout());

        // Left side controls
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ipAddressField = new JTextField(15);
        connectButton = new JButton("Connect");
        startReaderButton = new JButton("Start RFID Reader");
        startReaderButton.setEnabled(false);
        connectionStatusLabel = new JLabel("Not connected");
        connectionStatusLabel.setForeground(Color.RED);

        leftControls.add(new JLabel("Reader IP:"));
        leftControls.add(ipAddressField);
        leftControls.add(connectButton);
        leftControls.add(startReaderButton);
        leftControls.add(connectionStatusLabel);

        // Tag count
        tagCountLabel = new JLabel("Tags detected: 0");
        leftControls.add(tagCountLabel);

        // Right side controls
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        downloadReportsButton = new JButton("Report");

        rightControls.add(downloadReportsButton);

        topPanel.add(leftControls, BorderLayout.WEST);
        topPanel.add(rightControls, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Logs
        logsArea = new JTextArea(15, 50);
        logsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logsArea);

        JButton clearLogsButton = new JButton("Clear Logs");
        clearLogsButton.addActionListener(e -> {
            logsArea.setText("");
        });

        JPanel logsPanel = new JPanel(new BorderLayout());
        logsPanel.setBorder(BorderFactory.createTitledBorder("Logs"));
        logsPanel.add(scrollPane, BorderLayout.CENTER);
        logsPanel.add(clearLogsButton, BorderLayout.SOUTH);

        mainPanel.add(logsPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    public void handleDownloadReport() {
        syncHandler.downloadReport();
    }

    // === Public getters & setters for RFIDMarathonApp ===

    public JButton getConnectButton() {
        return connectButton;
    }

    public JButton getStartReaderButton() {
        return startReaderButton;
    }

    public JTextField getIpAddressField() {
        return ipAddressField;
    }

    public JLabel getConnectionStatusLabel() {
        return connectionStatusLabel;
    }

    public JTextArea getLogsArea() {
        return logsArea;
    }

    public void setTagCount(int count) {
        this.tagCount = count;
        tagCountLabel.setText("Tags detected: " + tagCount);
    }

    public JButton getDownloadReportsButton() {
        return downloadReportsButton;
    }

    public void setDownloadReportsButton(JButton downloadReportsButton) {
        this.downloadReportsButton = downloadReportsButton;
    }

    public int getTagCount() {
        return tagCount;
    }

    public void incrementTagCount() {
        setTagCount(tagCount + 1);
    }
}