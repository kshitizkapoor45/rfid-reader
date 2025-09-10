package com.rfid;

import javax.swing.*;
import java.awt.*;

public class MarathonPanel {

    private JPanel mainPanel;
    private JTextField marathonNameField;
    private JSpinner lapNumberSpinner;
    private JButton syncButton;
    private JLabel syncStatusLabel;

    public JPanel createMarathonPanel() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Marathon Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Marathon Name:"), gbc);

        gbc.gridx = 1;
        marathonNameField = new JTextField(20);
        formPanel.add(marathonNameField, gbc);

        // Lap Number
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Lap Number:"), gbc);

        gbc.gridx = 1;
        lapNumberSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        formPanel.add(lapNumberSpinner, gbc);

        // Sync Button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        syncButton = new JButton("Sync Data to Server");
        syncButton.setEnabled(false);
        formPanel.add(syncButton, gbc);

        // Sync Status
        gbc.gridy = 3;
        syncStatusLabel = new JLabel("No sync performed yet.");
        syncStatusLabel.setForeground(Color.GRAY);
        formPanel.add(syncStatusLabel, gbc);

        mainPanel.add(formPanel, BorderLayout.NORTH);

        return mainPanel;
    }

    // === Public getters for RFIDMarathonApp ===

    public JTextField getMarathonNameField() {
        return marathonNameField;
    }

    public JSpinner getLapNumberSpinner() {
        return lapNumberSpinner;
    }

    public JButton getSyncButton() {
        return syncButton;
    }

    public JLabel getSyncStatusLabel() {
        return syncStatusLabel;
    }
}
