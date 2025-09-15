package com.rfid;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RFIDMarathonApp extends JFrame {
    private boolean isConnected = false;
    private boolean isReaderStarted = false;

    private final Util util;
    private final MarathonPanel marathonPanel;
    private final RfidPanel rfidPanel;

    private final RfidReaderConnection rfidReaderConnection;
    private final RfidTagProcessor rfidTagProcessor;

    public RFIDMarathonApp() {

        TagStorage storage = new TagStorageService(
                "jdbc:postgresql://localhost:5432/speedway",
                "perfectkode",
                "perfectkode"
        );
        this.rfidPanel = new RfidPanel();
        this.util = new Util(rfidPanel);
        // Step 1: create marathon panel
        this.marathonPanel = new MarathonPanel();

        // Step 2: create service with panel
        SyncDataService syncService = new SyncDataService(marathonPanel, storage, util);

        // Step 3: inject handler back into panel
        marathonPanel.setSyncHandler(syncService);
        rfidPanel.setSyncHandler(syncService);

        initializeUI();
        setupEventHandlers();

        this.rfidTagProcessor = new RfidTagProcessor(storage, util);
        this.rfidReaderConnection = new RfidReaderConnection(
                rfidTagProcessor,
                util,
                count -> rfidPanel.setTagCount(count)
        );
    }

    private void initializeUI() {
        setTitle("RFID Marathon Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("RFID Reader", rfidPanel.createRFIDPanel());
        tabbedPane.addTab("Marathon Setup", marathonPanel.createMarathonPanel());

        add(tabbedPane, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        JLabel statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);

        JLabel timeLabel = new JLabel();
        Timer timer = new Timer(1000, e ->
                timeLabel.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
        timer.start();
        statusBar.add(timeLabel, BorderLayout.EAST);

        return statusBar;
    }

    private void setupEventHandlers() {
        rfidPanel.getConnectButton().addActionListener(e -> handleConnection());
        rfidPanel.getStartReaderButton().addActionListener(e -> handleStartReader());
        rfidPanel.getDownloadReportsButton().addActionListener(e -> rfidPanel.handleDownloadReport());

        marathonPanel.getMarathonNameField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSyncButton(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSyncButton(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSyncButton(); }
        });
    }

    private void handleConnection() {
        if (isConnected) {
            rfidReaderConnection.disconnect();
            isConnected = false;
            rfidPanel.getConnectionStatusLabel().setText("Not connected");
            rfidPanel.getConnectionStatusLabel().setForeground(Color.RED);
            rfidPanel.getIpAddressField().setEnabled(true);
            rfidPanel.getConnectButton().setText("Connect");
            rfidPanel.getStartReaderButton().setEnabled(false);
            util.addLog("Disconnected from RFID reader");
            return;
        }

        String ipAddress = rfidPanel.getIpAddressField().getText().trim();
        if (ipAddress.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an IP address", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        rfidPanel.getConnectButton().setEnabled(false);
        rfidPanel.getConnectButton().setText("Connecting...");

        rfidReaderConnection.connect(ipAddress,
                () -> {
                    isConnected = true;
                    rfidPanel.getConnectionStatusLabel().setText("Connected to " + ipAddress);
                    rfidPanel.getConnectionStatusLabel().setForeground(Color.GREEN);
                    rfidPanel.getStartReaderButton().setEnabled(true);
                    rfidPanel.getIpAddressField().setEnabled(false);
                    rfidPanel.getConnectButton().setText("Disconnect");
                    util.addLog("Connected to RFID reader at " + ipAddress);
                },
                () -> {
                    rfidPanel.getConnectionStatusLabel().setText("Connection failed");
                    rfidPanel.getConnectionStatusLabel().setForeground(Color.RED);
                    rfidPanel.getConnectButton().setText("Connect");
                    util.addLog("Failed to connect to RFID reader");
                });

        rfidPanel.getConnectButton().setEnabled(true);
    }

    private void handleStartReader() {
        if (!isReaderStarted) {
            rfidReaderConnection.resetTagCount();
            rfidReaderConnection.startReader();
            rfidPanel.getStartReaderButton().setText("Stop Reader");
            isReaderStarted = true;
        } else {
            rfidReaderConnection.stopReader();
            rfidPanel.getStartReaderButton().setText("Start RFID Reader");
            isReaderStarted = false;
        }
        updateSyncButton();
    }

    public void updateSyncButton() {
        boolean enableSync =
                !marathonPanel.getMarathonNameField().getText().trim().isEmpty()
                        && rfidPanel.getTagCount() > 0;
        marathonPanel.getSyncButton().setEnabled(enableSync);

        if (enableSync) {
            marathonPanel.getSyncStatusLabel().setText("🎯 Ready to sync " + rfidPanel.getTagCount() + " RFID tags");
            marathonPanel.getSyncStatusLabel().setForeground(new Color(33, 150, 243));
        }
    }
}