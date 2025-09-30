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
        mainPanel = new JPanel(new BorderLayout(15,15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Top controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
        addReaderButton = new JButton("+ Add Reader");
        startAllButton = new JButton("Start All");
        stopAllButton = new JButton("Stop All");

        startAllButton.setEnabled(false);
        stopAllButton.setEnabled(false);

        topPanel.add(addReaderButton);
        topPanel.add(startAllButton);
        topPanel.add(stopAllButton);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Readers container
        readersPanel = new JPanel();
        readersPanel.setLayout(new BoxLayout(readersPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(readersPanel);
        scroll.setPreferredSize(new Dimension(0,200));
        mainPanel.add(scroll, BorderLayout.CENTER);

        // Logs area
        logsArea = new JTextArea(10,50);
        logsArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logsArea);
        mainPanel.add(logScroll, BorderLayout.SOUTH);

        // Button actions
        addReaderButton.addActionListener(e -> addNewReader());
        startAllButton.addActionListener(e -> startAllReaders());
        stopAllButton.addActionListener(e -> stopAllReaders());

        return mainPanel;
    }

    private void addNewReader() {
        // Reset reader ID if no readers exist
        if (readerCards.isEmpty()) {
            nextReaderId = 1;
        }

        // Create the reader card
        ReaderCard card = new ReaderCard(
                "Reader " + nextReaderId++,
                connectionManager,
                this::updateTotalTagCount,
                this::appendLog
        );

        // Set the delete callback
        card.setDeleteCallback(() -> {
            readersPanel.remove(card.getPanel());
            readerCards.remove(card);
            readersPanel.revalidate();
            readersPanel.repaint();
            updateTotalTagCount();
            updateGlobalButtons();

            // Reset nextReaderId if no readers left
            if (readerCards.isEmpty()) {
                nextReaderId = 1;
            }
        });

        // Add card to panel and list
        readerCards.add(card);
        readersPanel.add(card.getPanel());
        readersPanel.add(Box.createVerticalStrut(10));
        readersPanel.revalidate();
        readersPanel.repaint();

        updateGlobalButtons();
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