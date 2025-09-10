package com.rfid;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class RfidPanel {

    private JPanel mainPanel;
    private JTextField ipAddressField;
    private JButton connectButton;
    private JButton startReaderButton;
    private JLabel connectionStatusLabel;
    private JTextArea logsArea;
    private JLabel tagCountLabel;

    private int tagCount = 0;
    private Consumer<Void> onLogsCleared;

    public JPanel createRFIDPanel() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ipAddressField = new JTextField(15);
        connectButton = new JButton("Connect");
        startReaderButton = new JButton("Start RFID Reader");
        startReaderButton.setEnabled(false);
        connectionStatusLabel = new JLabel("Not connected");
        connectionStatusLabel.setForeground(Color.RED);

        topPanel.add(new JLabel("Reader IP:"));
        topPanel.add(ipAddressField);
        topPanel.add(connectButton);
        topPanel.add(startReaderButton);
        topPanel.add(connectionStatusLabel);

        // Tag count
        tagCountLabel = new JLabel("Tags detected: 0");
        topPanel.add(tagCountLabel);

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

    public int getTagCount() {
        return tagCount;
    }

    public void incrementTagCount() {
        setTagCount(tagCount + 1);
    }
}