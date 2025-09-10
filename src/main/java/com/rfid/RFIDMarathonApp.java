package com.rfid;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RFIDMarathonApp extends JFrame {

    private MarathonPanel marathonPanel = new MarathonPanel();
    private RfidPanel rfidPanel = new RfidPanel();

    private boolean isConnected = false;
    private boolean isReaderStarted = false;
    private RfidReaderConnection rfidReaderConnection;
    private RfidTagProcessor rfidTagProcessor;

    public RFIDMarathonApp()  {
        initializeUI();
        setupEventHandlers();

        String jdbcUrl = "jdbc:postgresql://localhost:5432/speedway";
        String username = "perfectkode";
        String password = "perfectkode";

        TagStorage storage = new TagStorageService(jdbcUrl, username, password);
        RfidTagProcessor rfidTagProcessor = new RfidTagProcessor(
                storage,
                this::addLog
        );

        rfidReaderConnection = new RfidReaderConnection(
                rfidTagProcessor,
                this::addLog,
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
        marathonPanel.getSyncButton().addActionListener(e -> handleSync());

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
            addLog("Disconnected from RFID reader");
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
                    addLog("Connected to RFID reader at " + ipAddress);
                },
                () -> {
                    rfidPanel.getConnectionStatusLabel().setText("Connection failed");
                    rfidPanel.getConnectionStatusLabel().setForeground(Color.RED);
                    rfidPanel.getConnectButton().setText("Connect");
                    addLog("Failed to connect to RFID reader");
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

    private void handleSync() {
        String marathonName = marathonPanel.getMarathonNameField().getText().trim();
        int lapNumber = (Integer) marathonPanel.getLapNumberSpinner().getValue();

        if (marathonName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter marathon name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        marathonPanel.getSyncButton().setEnabled(false);
        marathonPanel.getSyncButton().setText("Syncing...");
        marathonPanel.getSyncStatusLabel().setText("Syncing data to server...");
        marathonPanel.getSyncStatusLabel().setForeground(Color.BLUE);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Thread.sleep(3000);
                return Math.random() > 0.2;
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        marathonPanel.getSyncStatusLabel().setText(
                                "Data synced successfully! (" + rfidPanel.getTagCount() + " tags uploaded)");
                        marathonPanel.getSyncStatusLabel().setForeground(Color.GREEN);
                        addLog("Marathon '" + marathonName + "' synced successfully with " + rfidPanel.getTagCount() + " tags");

                        // ✅ Clear table after successful sync
                        if (rfidTagProcessor != null) {
                            ((TagStorageService) rfidTagProcessor.getStorage()).clearAll();
                            addLog("🗑 Cleared tag_details table after sync");
                        }
                    }

                } catch (Exception ex) {
                    marathonPanel.getSyncStatusLabel().setText("Sync error occurred");
                    marathonPanel.getSyncStatusLabel().setForeground(Color.RED);
                    addLog("Sync error: " + ex.getMessage());
                }
                marathonPanel.getSyncButton().setEnabled(true);
                marathonPanel.getSyncButton().setText("Sync Data to Server");
            }
        };
        worker.execute();
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

    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
            rfidPanel.getLogsArea().append("[" + ts + "] " + message + "\n");
            rfidPanel.getLogsArea().setCaretPosition(rfidPanel.getLogsArea().getDocument().getLength());
        });
    }
}