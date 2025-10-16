package com.rfid;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RFIDMarathonApp extends JFrame {
    private final Util util;
    private final MarathonPanel marathonPanel;
    private final RfidPanel rfidPanel;
    private final ReaderConnectionManager connectionManager;
    private final RfidTagProcessor rfidTagProcessor;

    public RFIDMarathonApp() {

        TagStorage storage = new TagStorageService(
                "jdbc:postgresql://localhost:5432/speedway",
                "perfectkode",
                "perfectkode"
        );

        this.rfidPanel = new RfidPanel();
        this.marathonPanel = new MarathonPanel();
        this.util = new Util(rfidPanel);

        this.rfidTagProcessor = new RfidTagProcessor(storage, util);

        this.connectionManager = new ReaderConnectionManager(rfidTagProcessor, util);

        SyncDataService syncService = new SyncDataService(marathonPanel, storage, util);
        rfidPanel.setSyncHandler(syncService);
        rfidPanel.setConnectionManager(connectionManager);
        marathonPanel.setSyncHandler(syncService);

        initializeUI();
    }

    private void initializeUI() {
        setTitle("RFID Marathon Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel rfidPanelContent = rfidPanel.createRFIDPanel();
        JPanel marathonPanelContent = marathonPanel.createMarathonPanel();

        tabbedPane.addTab("RFID Reader", rfidPanelContent);
        tabbedPane.addTab("Marathon Setup", marathonPanelContent);

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == marathonPanelContent) {
                SwingUtilities.invokeLater(() -> marathonPanel.loadUnsyncedIpTags());
            }
        });

        add(tabbedPane, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        JLabel statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);

        JLabel timeLabel = new JLabel();
        Timer timer = new Timer(1000, e ->
                timeLabel.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
        );
        timer.start();
        statusBar.add(timeLabel, BorderLayout.EAST);

        return statusBar;
    }
}