package com.rfid;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RfidPanel {

    private JPanel mainPanel;
    private JPanel readersPanel;
    private JButton addReaderButton, startAllButton, stopAllButton;
    private JTextArea logsArea;
    private SyncHandler syncHandler;

    private List<ReaderCard> readerCards = new ArrayList<>();
    private ReaderConnectionManager connectionManager;

    private int nextReaderId = 1;

    public RfidPanel() {}

    public RfidPanel(SyncHandler syncHandler, ReaderConnectionManager manager) {
        this.syncHandler = syncHandler;
        this.connectionManager = manager;
    }

    public void setSyncHandler(SyncHandler syncHandler) {
        this.syncHandler = syncHandler;
    }

    public void setConnectionManager(ReaderConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public JPanel createRFIDPanel() {
        mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 🔹 Top panel with left and right sections
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));

        // Left buttons: Add, Start All, Stop All
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        addReaderButton = new JButton("+ Add Reader");
        startAllButton = new JButton("Start All");
        stopAllButton = new JButton("Stop All");

        startAllButton.setEnabled(false);
        stopAllButton.setEnabled(false);

        leftButtons.add(addReaderButton);
        leftButtons.add(startAllButton);
        leftButtons.add(stopAllButton);

        // Right button: Report
        JButton reportButton = new JButton("Report");
        reportButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reportButton.setBackground(new Color(59, 96, 246));
        reportButton.setForeground(Color.WHITE);
        reportButton.setFocusPainted(false);
        reportButton.addActionListener(e -> downloadTagsReport());

        topPanel.add(leftButtons, BorderLayout.WEST);
        topPanel.add(reportButton, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 🔹 Readers container with NO vertical gaps
        readersPanel = new JPanel();
        readersPanel.setLayout(new BoxLayout(readersPanel, BoxLayout.Y_AXIS));
        readersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scroll = new JScrollPane(readersPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setPreferredSize(new Dimension(0, 200));
        mainPanel.add(scroll, BorderLayout.CENTER);

        // 🔹 Logs area
        logsArea = new JTextArea(10, 50);
        logsArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logsArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Logs"));
        mainPanel.add(logScroll, BorderLayout.SOUTH);

        // 🔹 Button actions
        addReaderButton.addActionListener(e -> addNewReader());
        startAllButton.addActionListener(e -> startAllReaders());
        stopAllButton.addActionListener(e -> stopAllReaders());

        return mainPanel;
    }

    private void downloadTagsReport(){
        syncHandler.downloadReport();
    }

    private void addNewReader() {
        int newId = getNextAvailableId();

        ReaderCard card = new ReaderCard(
                "Reader " + newId,
                connectionManager,
                this::updateTotalTagCount,
                this::appendLog
        );

        // Delete callback
        card.setDeleteCallback(() -> {
            readersPanel.remove(card.getPanel().getParent());
            readerCards.remove(card);
            readersPanel.revalidate();
            readersPanel.repaint();
            updateTotalTagCount();
            updateGlobalButtons();
        });

        JPanel cardPanel = card.getPanel();
        cardPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // ✅ Create wrapper with ZERO spacing
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));
        wrapper.add(cardPanel, BorderLayout.CENTER);

        // ✅ Set maximum height to prevent BoxLayout from adding vertical space
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapper.getPreferredSize().height));

        readerCards.add(card);
        readersPanel.add(wrapper);

        readersPanel.revalidate();
        readersPanel.repaint();

        updateGlobalButtons();
    }

    private int getNextAvailableId() {
        List<Integer> usedIds = readerCards.stream()
                .map(card -> Integer.parseInt(card.getReaderName().replace("Reader ", "")))
                .sorted()
                .toList();

        int id = 1;
        for (int usedId : usedIds) {
            if (usedId == id) id++;
            else break;
        }
        return id;
    }

    private void startAllReaders() {
        connectionManager.startAll();
    }

    private void stopAllReaders() {
        connectionManager.stopAll();
    }

    private void updateTotalTagCount() {
        int total = readerCards.stream().mapToInt(ReaderCard::getTagCount).sum();
        appendLog("Total tags: " + total);
    }

    private void updateGlobalButtons() {
        boolean hasReaders = !readerCards.isEmpty();
        startAllButton.setEnabled(hasReaders);
        stopAllButton.setEnabled(hasReaders);
    }

    public void appendLog(String text) {
        SwingUtilities.invokeLater(() -> logsArea.append(text + "\n"));
    }
}