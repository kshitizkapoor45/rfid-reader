package com.rfid;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
public class ReaderCard {
    private JPanel panel;
    private JTextField ipField;
    private JButton connectButton, startButton, deleteButton;
    private JLabel tagCountLabel;
    private boolean connected = false;
    private boolean reading = false;
    private int tagCount = 0;

    private String ipAddress = ""; // store IP
    private Runnable deleteCallback;

    public ReaderCard(String name,
                      ReaderConnectionManager manager,
                      Runnable updateTotalTagCount,
                      Consumer<String> logCallback) {

        panel = new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
        ipField = new JTextField(12);
        connectButton = new JButton("Connect");
        startButton = new JButton("Start");
        deleteButton = new JButton("Delete");
        tagCountLabel = new JLabel("Tags: 0");

        startButton.setEnabled(false);

        panel.add(new JLabel(name));
        panel.add(ipField);
        panel.add(connectButton);
        panel.add(startButton);
        panel.add(deleteButton);
        panel.add(tagCountLabel);

        // Connect / Disconnect
        connectButton.addActionListener(e -> {
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter IP address");
                return;
            }

            if (!connected) {
                connectButton.setEnabled(false);
                manager.connectReader(ip,
                        count -> {
                            tagCount = count;
                            tagCountLabel.setText("Tags: " + tagCount);
                            updateTotalTagCount.run();
                        },
                        () -> {
                            connected = true;
                            ipAddress = ip; // store the IP
                            startButton.setEnabled(true);
                            connectButton.setText("Disconnect");
                            connectButton.setEnabled(true);
                            logCallback.accept(name + " connected to " + ip);
                        },
                        () -> {
                            connected = false;
                            connectButton.setEnabled(true);
                            logCallback.accept(name + " connection failed");
                        }
                );
            } else {
                if (reading) stopReading(manager, ipAddress, logCallback);
                manager.disconnectReader(ipAddress);
                connected = false;
                startButton.setEnabled(false);
                connectButton.setText("Connect");
                logCallback.accept(name + " disconnected");
            }
        });

        // Start / Stop reading
        startButton.addActionListener(e -> {
            if (!reading) {
                manager.startReader(ipAddress);
                reading = true;
                startButton.setText("Stop");
                logCallback.accept(name + " started reading");
            } else {
                stopReading(manager, ipAddress, logCallback);
            }
        });

        // Delete button
        deleteButton.addActionListener(e -> {
            if (deleteCallback != null) {
                deleteCallback.run();
            }
        });
    }

    private void stopReading(ReaderConnectionManager manager, String ip, Consumer<String> logCallback) {
        manager.stopReader(ip);
        reading = false;
        startButton.setText("Start");
        logCallback.accept("Stopped reading on " + ip);
    }

    public JPanel getPanel() { return panel; }
    public int getTagCount() { return tagCount; }
    public void setDeleteCallback(Runnable deleteCallback) { this.deleteCallback = deleteCallback; }
    public String getIpAddress() { return ipAddress; }
}
