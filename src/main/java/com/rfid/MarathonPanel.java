package com.rfid;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarathonPanel {

    private JPanel mainPanel;
    private JTextField marathonNameField;
    private JSpinner lapNumberSpinner;
    private JButton syncButton;
    private JLabel syncStatusLabel;
    private SyncHandler syncHandler;
    private JPanel ipTagsPanel;

    public MarathonPanel(SyncHandler syncHandler){
        this.syncHandler = syncHandler;
    }
    public MarathonPanel() {
        this(null);
    }

    public void setSyncHandler(SyncHandler syncHandler) {
        this.syncHandler = syncHandler;
    }

    public JPanel createMarathonPanel() {
        mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(new Color(248, 250, 252));

        // Create header panel
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Create form panel
        JPanel formPanel = createFormPanel();
        mainPanel.add(formPanel, BorderLayout.CENTER);

        if (syncHandler != null) {
            loadUnsyncedIpTags();
        }

        // Create instructions panel
        JPanel instructionsPanel = createInstructionsPanel();
        mainPanel.add(instructionsPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    public void loadUnsyncedIpTags() {
        System.out.println("Fetching unsynced IP tags");
        List<TagDetail> tags = syncHandler.fetchUnsyncedIpTags();
        ipTagsPanel.removeAll();

        if (tags == null || tags.isEmpty()) {
            JLabel emptyLabel = new JLabel("No tags found in database", JLabel.CENTER);
            emptyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            emptyLabel.setForeground(new Color(107, 114, 128));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            ipTagsPanel.add(Box.createVerticalGlue());
            ipTagsPanel.add(emptyLabel);
            ipTagsPanel.add(Box.createVerticalGlue());
        } else {
            ipTagsPanel.add(Box.createVerticalStrut(5));

            for (TagDetail tag : tags) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
                row.setBackground(Color.WHITE);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

                JCheckBox ipCheck = new JCheckBox(tag.getReader());
                ipCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                ipCheck.setSelected(true);

                // Spinner setup
                JSpinner lapSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
                lapSpinner.setPreferredSize(new Dimension(70, 24));

                JFormattedTextField spinnerTextField =
                        ((JSpinner.DefaultEditor) lapSpinner.getEditor()).getTextField();
                spinnerTextField.setHorizontalAlignment(JTextField.CENTER);
                spinnerTextField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                spinnerTextField.setText(""); // visually empty

                // Track spinner changes
                spinnerTextField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    public void changedUpdate(javax.swing.event.DocumentEvent e) {
                        updateSyncButton();
                    }

                    public void removeUpdate(javax.swing.event.DocumentEvent e) {
                        updateSyncButton();
                    }

                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        updateSyncButton();
                    }
                });

                row.add(ipCheck);
                row.add(Box.createHorizontalStrut(15));
                row.add(new JLabel("Lap:"));
                row.add(lapSpinner);

                ipTagsPanel.add(row);
            }

            ipTagsPanel.add(Box.createVerticalStrut(15));
        }

//        JPanel flushPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//        flushPanel.setBackground(Color.WHITE);
//        flushPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
//
//        JCheckBox flushCheckBox = new JCheckBox("Flush data after sync");
//        flushCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
//        flushCheckBox.setForeground(new Color(51, 65, 85));
//
//        flushPanel.add(flushCheckBox);
//        ipTagsPanel.add(flushPanel);

        ipTagsPanel.revalidate();
        ipTagsPanel.repaint();

        syncStatusLabel.setText("Please assign lap numbers to selected IPs");
        syncStatusLabel.setForeground(new Color(107, 114, 128));
    }

    private boolean checkDuplicateLaps() {
        Map<Integer, List<String>> lapToIPs = new HashMap<>();
        boolean allAssigned = true;
        boolean hasDuplicates = false;

        // Collect lap numbers for selected IPs
        for (Component comp : ipTagsPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel row = (JPanel) comp;
                JCheckBox ipCheck = null;
                JSpinner lapSpinner = null;

                for (Component rowComp : row.getComponents()) {
                    if (rowComp instanceof JCheckBox) {
                        ipCheck = (JCheckBox) rowComp;
                    } else if (rowComp instanceof JSpinner) {
                        lapSpinner = (JSpinner) rowComp;
                    }
                }

                if (ipCheck != null && lapSpinner != null && ipCheck.isSelected()) {
                    int lapNumber = (Integer) lapSpinner.getValue();
                    if (lapNumber > 0) {
                        lapToIPs.computeIfAbsent(lapNumber, k -> new ArrayList<>())
                                .add(ipCheck.getText());
                    } else {
                        allAssigned = false;
                    }
                }
            }
        }

        // ✅ Check for duplicates first
        for (Map.Entry<Integer, List<String>> entry : lapToIPs.entrySet()) {
            if (entry.getValue().size() > 1) {
                hasDuplicates = true;
                showMergeConfirmationDialog(entry.getKey(), entry.getValue());
                break; // stop at first duplicate found
            }
        }
        if (hasDuplicates) {
            System.out.println("Contatins duplicate lap numbers");
        } else if (!allAssigned) {
            syncStatusLabel.setText("Please assign lap numbers to all selected IPs");
            syncStatusLabel.setForeground(new Color(107, 114, 128)); // gray
        } else {
            syncStatusLabel.setText("All lap numbers look good!");
            syncStatusLabel.setForeground(new Color(34, 197, 94)); // green
        }
        return hasDuplicates;
    }

    private Map<Integer, List<String>> getSelectedLapIpMap() {
        Map<Integer, List<String>> lapToIPs = new HashMap<>();

        for (Component comp : ipTagsPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel row = (JPanel) comp;
                JCheckBox ipCheck = null;
                JSpinner lapSpinner = null;

                for (Component rowComp : row.getComponents()) {
                    if (rowComp instanceof JCheckBox) {
                        ipCheck = (JCheckBox) rowComp;
                    } else if (rowComp instanceof JSpinner) {
                        lapSpinner = (JSpinner) rowComp;
                    }
                }

                if (ipCheck != null && lapSpinner != null && ipCheck.isSelected()) {
                    int lapNumber = (Integer) lapSpinner.getValue();
                    if (lapNumber > 0) {
                        lapToIPs.computeIfAbsent(lapNumber, k -> new ArrayList<>())
                                .add(ipCheck.getText());
                    }
                }
            }
        }
        return lapToIPs;
    }


    private void showMergeConfirmationDialog(int lapNumber, List<String> ipAddresses) {
        Map<Integer, List<String>> lapIpMap = getSelectedLapIpMap();

        StringBuilder message = new StringBuilder();
        message.append("The following IP addresses have the same lap number (")
                .append(lapNumber).append("):\n\n");

        for (String ip : ipAddresses) {
            message.append("• ").append(ip).append("\n");
        }

        message.append("\nDo you want to merge the data from these IP addresses?");

        int result = JOptionPane.showConfirmDialog(
                mainPanel,
                message.toString(),
                "Duplicate Lap Number Detected",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            showSyncModal(true, lapIpMap);
            syncStatusLabel.setText("Data will be merged for lap " + lapNumber);
            syncStatusLabel.setForeground(new Color(34, 197, 94));
        } else {
            syncStatusLabel.setText("Please assign different lap numbers or deselect IPs");
            syncStatusLabel.setForeground(new Color(239, 68, 68));
        }
    }


    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(248, 250, 252));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Marathon Configuration", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(30, 58, 138));

        JLabel subtitleLabel = new JLabel("Configure your marathon details and sync data", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(71, 85, 105));

        JPanel titlePanel = new JPanel(new BorderLayout(5, 5));
        titlePanel.setBackground(new Color(248, 250, 252));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);

        headerPanel.add(titlePanel, BorderLayout.CENTER);
        return headerPanel;
    }

    private JPanel createFormPanel() {
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBackground(new Color(248, 250, 252));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        // 1️⃣ Marathon Name Field
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(createInputPanel("Marathon Name", createMarathonNameField()), gbc);

        // 2️⃣ Unsynced IP Tags Panel inside ScrollPane
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Create wrapper panel for proper centering
        JPanel tagsWrapperPanel = new JPanel(new BorderLayout());
        tagsWrapperPanel.setBackground(Color.WHITE);
        tagsWrapperPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        // Title label
        JLabel tagsTitle = new JLabel("Unsynced IP Tags", JLabel.CENTER);
        tagsTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tagsTitle.setForeground(new Color(51, 65, 85));
        tagsTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        tagsWrapperPanel.add(tagsTitle, BorderLayout.NORTH);

        ipTagsPanel = new JPanel();
        ipTagsPanel.setLayout(new BoxLayout(ipTagsPanel, BoxLayout.Y_AXIS));
        ipTagsPanel.setBackground(Color.WHITE);
        ipTagsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(ipTagsPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        tagsWrapperPanel.add(scrollPane, BorderLayout.CENTER);

        formPanel.add(tagsWrapperPanel, gbc);

        // 3️⃣ Sync Button Panel (moved after tags)
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        formPanel.add(createSyncButtonPanel(), gbc);

        // 4️⃣ Status Label
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        syncStatusLabel = new JLabel("Ready to configure marathon details", JLabel.CENTER);
        syncStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        syncStatusLabel.setForeground(new Color(100, 116, 139));
        formPanel.add(syncStatusLabel, gbc);

        containerPanel.add(formPanel, BorderLayout.CENTER);
        return containerPanel;
    }

    private JTextField createMarathonNameField() {
        marathonNameField = new JTextField();
        marathonNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        marathonNameField.setPreferredSize(new Dimension(400, 40));
        marathonNameField.setMinimumSize(new Dimension(300, 40));
        marathonNameField.setMaximumSize(new Dimension(500, 40));
        marathonNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Focus effect
        marathonNameField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                marathonNameField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(59, 130, 246), 2),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                marathonNameField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });

        // Keep the document listener
        marathonNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSyncButton(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSyncButton(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSyncButton(); }
        });

        return marathonNameField;
    }

    private JPanel createInputPanel(String labelText, JComponent inputComponent) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(new Color(51, 65, 85));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 0));

        inputComponent.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(label);
        panel.add(inputComponent);

        return panel;
    }

    private JPanel createSyncButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);

        syncButton = new JButton("Sync Marathon Data");
        syncButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        syncButton.setPreferredSize(new Dimension(250, 40));
        syncButton.setMinimumSize(new Dimension(150, 30));
        syncButton.setMaximumSize(new Dimension(250, 40));
        syncButton.setBackground(new Color(215, 221, 228));
        syncButton.setForeground(Color.WHITE);
        syncButton.setHorizontalAlignment(SwingConstants.CENTER);
        syncButton.setVerticalAlignment(SwingConstants.CENTER);
        syncButton.setFocusPainted(false);
        syncButton.setBorder(BorderFactory.createEmptyBorder());
        syncButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        syncButton.setEnabled(false);

        // Hover effect
        syncButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (syncButton.isEnabled()) {
                    syncButton.setBackground(new Color(22, 163, 74));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (syncButton.isEnabled()) {
                    syncButton.setBackground(new Color(34, 197, 94));
                }
            }
        });
        // Add action listener
        syncButton.addActionListener(e -> {
            Map<Integer, List<String>> lapIpMap = getSelectedLapIpMap();
            boolean hasDuplicates = checkDuplicateLaps();

            if (!hasDuplicates) {
                showSyncModal(false, lapIpMap);
            }
        });

        buttonPanel.add(syncButton);
        return buttonPanel;
    }

    public void refreshIpTagsPanel() {
        SwingUtilities.invokeLater(() -> loadUnsyncedIpTags());
    }

    private void showSyncModal(boolean merge, Map<Integer, List<String>> lapIpMap) {
        JDialog syncDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(mainPanel),
                merge ? "Merge Options" : "Sync Options", true);
        syncDialog.setSize(450, 300);
        syncDialog.setLocationRelativeTo(mainPanel);
        syncDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel modalPanel = new JPanel(new BorderLayout(20, 20));
        modalPanel.setBackground(Color.WHITE);
        modalPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titleLabel = new JLabel(
                merge ? "Merge Marathon Data" : "Choose Sync Method",
                JLabel.CENTER
        );
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(30, 58, 138));

        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        buttonsPanel.setBackground(Color.WHITE);

        JButton dbSyncButton = createModalButton(
                merge ? "Merge from Database" : "Sync from Database",
                merge ? "Combine overlapping lap data for selected IPs"
                        : "Load existing marathon data from the database",
                new Color(59, 130, 246)
        );

        dbSyncButton.addActionListener(e -> {
            if (merge) {
                syncHandler.mergeSyncFromDatabase(lapIpMap);
            } else {
                syncHandler.normalSyncFromDatabase(lapIpMap);
            }
            refreshIpTagsPanel();
            syncDialog.dispose();
        });

        JButton fileUploadButton = createModalButton(
                merge ? "Merge via CSV Upload" : "Upload CSV File",
                merge ? "Merge overlapping data from uploaded file"
                        : "Import marathon data from CSV file",
                new Color(16, 185, 129)
        );

        fileUploadButton.addActionListener(e -> {
            uploadCsv(lapIpMap,merge);
            refreshIpTagsPanel();
            syncDialog.dispose();
        });

        buttonsPanel.add(dbSyncButton);
        buttonsPanel.add(fileUploadButton);

        JPanel cancelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        cancelPanel.setBackground(Color.WHITE);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cancelButton.setForeground(new Color(107, 114, 128));
        cancelButton.setBackground(new Color(243, 244, 246));
        cancelButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> syncDialog.dispose());
        cancelPanel.add(cancelButton);

        modalPanel.add(titleLabel, BorderLayout.NORTH);
        modalPanel.add(buttonsPanel, BorderLayout.CENTER);
        modalPanel.add(cancelPanel, BorderLayout.SOUTH);

        syncDialog.add(modalPanel);
        syncDialog.setVisible(true);
    }

    private JButton createModalButton(String text, String description, Color bgColor) {
        JPanel buttonContent = new JPanel();
        buttonContent.setLayout(new BoxLayout(buttonContent, BoxLayout.Y_AXIS));
        buttonContent.setBackground(bgColor);
        buttonContent.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel mainText = new JLabel(text);
        mainText.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mainText.setForeground(Color.WHITE);
        mainText.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel descText = new JLabel(description);
        descText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descText.setForeground(new Color(255, 255, 255, 200));
        descText.setAlignmentY(Component.CENTER_ALIGNMENT);

        buttonContent.add(mainText);
        buttonContent.add(Box.createVerticalStrut(6)); // spacing
        buttonContent.add(descText);

        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        button.add(buttonContent, BorderLayout.CENTER);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createLineBorder(bgColor.darker(), 1));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(380, 80));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                Color hoverColor = bgColor.darker();
                button.setBackground(hoverColor);
                buttonContent.setBackground(hoverColor);
                button.setBorder(BorderFactory.createLineBorder(hoverColor.darker(), 1));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
                buttonContent.setBackground(bgColor);
                button.setBorder(BorderFactory.createLineBorder(bgColor.darker(), 1));
            }
        });
        return button;
    }

    private void uploadCsv(Map<Integer,List<String>> tagIpMap,boolean merge) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CSV File");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "CSV File", "csv");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(mainPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if(merge){
                syncHandler.mergeUploadCsv(selectedFile,tagIpMap,true);
            }else{
                syncHandler.uploadCsv(selectedFile,tagIpMap,false);
            }
        }
    }

    private JPanel createInstructionsPanel() {
        JPanel instructionsPanel = new JPanel(new BorderLayout());
        instructionsPanel.setBackground(new Color(248, 250, 252));
        instructionsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(new Color(239, 246, 255));
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254), 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JLabel instructionTitle = new JLabel("💡 Quick Instructions");
        instructionTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        instructionTitle.setForeground(new Color(30, 64, 175));

        JTextArea instructionsText = new JTextArea(
                "• Enter marathon name\n" +
                        "• Review and select IP tags to sync\n" +
                        "• Click 'Sync Data' to upload data from database or import CSV files"
        );
        instructionsText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        instructionsText.setForeground(new Color(30, 58, 138));
        instructionsText.setBackground(new Color(239, 246, 255));
        instructionsText.setEditable(false);
        instructionsText.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        cardPanel.add(instructionTitle, BorderLayout.NORTH);
        cardPanel.add(instructionsText, BorderLayout.CENTER);

        instructionsPanel.add(cardPanel, BorderLayout.CENTER);
        return instructionsPanel;
    }

    private void updateSyncButton() {
        boolean marathonFilled = marathonNameField.getText() != null && !marathonNameField.getText().trim().isEmpty();

        boolean allLapsFilled = true;
        for (Component comp : ipTagsPanel.getComponents()) {
            if (comp instanceof JPanel row) {
                for (Component inner : row.getComponents()) {
                    if (inner instanceof JSpinner spinner) {
                        JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
                        String text = textField.getText().trim();
                        if (text.isEmpty()) {
                            allLapsFilled = false;
                            break;
                        }
                        try {
                            int val = Integer.parseInt(text);
                            if (val < 1) allLapsFilled = false;
                        } catch (NumberFormatException e) {
                            allLapsFilled = false;
                        }
                    }
                }
            }
        }
        boolean enableButton = marathonFilled && allLapsFilled;
        syncButton.setEnabled(enableButton);

        if (enableButton) {
            syncButton.setBackground(new Color(34, 197, 94));
            syncStatusLabel.setText("Ready to sync marathon data");
            syncStatusLabel.setForeground(new Color(61, 189, 64));
        } else {
            syncButton.setBackground(new Color(215, 221, 228));
            syncStatusLabel.setText("Please enter marathon name and assign laps to enable sync");
            syncStatusLabel.setForeground(new Color(218, 59, 59));
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void setMainPanel(JPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    public JSpinner getLapNumberSpinner() {
        return lapNumberSpinner;
    }

    public void setLapNumberSpinner(JSpinner lapNumberSpinner) {
        this.lapNumberSpinner = lapNumberSpinner;
    }

    public JTextField getMarathonNameField() {
        return marathonNameField;
    }

    public void setMarathonNameField(JTextField marathonNameField) {
        this.marathonNameField = marathonNameField;
    }

    public JButton getSyncButton() {
        return syncButton;
    }

    public void setSyncButton(JButton syncButton) {
        this.syncButton = syncButton;
    }

    public JLabel getSyncStatusLabel() {
        return syncStatusLabel;
    }

    public void setSyncStatusLabel(JLabel syncStatusLabel) {
        this.syncStatusLabel = syncStatusLabel;
    }
}