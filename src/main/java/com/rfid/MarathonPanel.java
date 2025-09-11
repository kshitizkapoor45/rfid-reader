package com.rfid;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class MarathonPanel {

    private JPanel mainPanel;
    private JTextField marathonNameField;
    private JSpinner lapNumberSpinner;
    private JButton syncButton;
    private JLabel syncStatusLabel;
    private SyncHandler syncHandler;

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

        // Create instructions panel
        JPanel instructionsPanel = createInstructionsPanel();
        mainPanel.add(instructionsPanel, BorderLayout.SOUTH);

        return mainPanel;
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

        // Marathon Name Field
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(createInputPanel("Marathon Name", createMarathonNameField()), gbc);

        // Lap Number Field
        gbc.gridy = 1;
        formPanel.add(createInputPanel("Number of Laps", createLapNumberSpinner()), gbc);

        // Sync Button
        gbc.gridy = 2;
        formPanel.add(createSyncButtonPanel(), gbc);

        // Status Label (spans horizontally)
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        syncStatusLabel = new JLabel("Ready to configure marathon details", JLabel.CENTER);
        syncStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        syncStatusLabel.setForeground(new Color(100, 116, 139));
        formPanel.add(syncStatusLabel, gbc);

        containerPanel.add(formPanel, BorderLayout.CENTER); // ✅ stick to left side
        return containerPanel;
    }

    private JTextField createMarathonNameField() {
        marathonNameField = new JTextField();
        marathonNameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        marathonNameField.setPreferredSize(new Dimension(250, 35));
        marathonNameField.setMinimumSize(new Dimension(200, 30));
        marathonNameField.setMaximumSize(new Dimension(400, 40));
        marathonNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        // ✅ Keep the document listener
        marathonNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSyncButton(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSyncButton(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSyncButton(); }
        });

        return marathonNameField;
    }

    private JSpinner createLapNumberSpinner() {
        // Spinner model: minimum 1, step 1
        lapNumberSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        lapNumberSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lapNumberSpinner.setPreferredSize(new Dimension(250, 35));
        lapNumberSpinner.setMinimumSize(new Dimension(200, 30));
        lapNumberSpinner.setMaximumSize(new Dimension(400, 40));
        lapNumberSpinner.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225), 1));
        lapNumberSpinner.setBackground(new Color(249, 250, 251));

        JFormattedTextField spinnerTextField = ((JSpinner.DefaultEditor) lapNumberSpinner.getEditor()).getTextField();

        // Restrict typing to digits only and minimum 1
        spinnerTextField.setDocument(new javax.swing.text.PlainDocument() {
            @Override
            public void insertString(int offs, String str, javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                if (str == null) return;
                if (!str.matches("\\d+")) return;
                String currentText = getText(0, getLength());
                String newText = currentText.substring(0, offs) + str + currentText.substring(offs);
                try {
                    int value = Integer.parseInt(newText);
                    if (value < 1) return;
                } catch (NumberFormatException e) {
                    return;
                }
                super.insertString(offs, str, a);
            }
        });

        // Style the text field
        spinnerTextField.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        spinnerTextField.setBackground(new Color(249, 250, 251));
        spinnerTextField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        spinnerTextField.setHorizontalAlignment(JTextField.LEFT);

        // Focus effect
        spinnerTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                lapNumberSpinner.setBorder(BorderFactory.createLineBorder(new Color(59, 130, 246), 2));
                spinnerTextField.setBackground(Color.WHITE);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                lapNumberSpinner.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225), 1));
                spinnerTextField.setBackground(new Color(249, 250, 251));
            }
        });

        // -------------------
        // Customize arrow buttons
        // -------------------
        Component[] comps = lapNumberSpinner.getComponents();
        for (Component comp : comps) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setBackground(new Color(249, 250, 251));
                button.setForeground(new Color(59, 130, 246));
                button.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));
                button.setFocusPainted(false);
                button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            }
        }
        return lapNumberSpinner;
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
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);

        syncButton = new JButton("Sync Marathon Data");
        syncButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        syncButton.setPreferredSize(new Dimension(250, 40));
        syncButton.setMinimumSize(new Dimension(150, 30));
        syncButton.setMaximumSize(new Dimension(250, 40));
        syncButton.setBackground(new Color(215, 221, 228));
        syncButton.setForeground(Color.WHITE);
        syncButton.setFocusPainted(false);
        syncButton.setBorder(BorderFactory.createEmptyBorder());
        syncButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        syncButton.setEnabled(false);

        // Add hover effect
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

        // Add action listener to show modal
        syncButton.addActionListener(e -> showSyncModal());

        buttonPanel.add(syncButton);
        return buttonPanel;
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
                "• Enter marathon name and set number of laps\n" +
                        "• Use sync button to upload data from database or import CSV/Excel files"
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

    private void showSyncModal() {
        JDialog syncDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(mainPanel), "Sync Options", true);
        syncDialog.setSize(450, 300);
        syncDialog.setLocationRelativeTo(mainPanel);
        syncDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel modalPanel = new JPanel(new BorderLayout(20, 20));
        modalPanel.setBackground(Color.WHITE);
        modalPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Title
        JLabel titleLabel = new JLabel("Choose Sync Method", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(30, 58, 138));

        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        buttonsPanel.setBackground(Color.WHITE);

        // Database Sync Button
        JButton dbSyncButton = createModalButton("Sync from Database",
                "Load existing marathon data from the database", new Color(59, 130, 246));

        dbSyncButton.addActionListener(e -> {
            syncFromDatabase();
            syncDialog.dispose();
        });

        // File Upload Button
        JButton fileUploadButton = createModalButton("Upload CSV/Excel File",
                "Import marathon data from CSV or Excel file", new Color(16, 185, 129));
        fileUploadButton.addActionListener(e -> {
            uploadFile();
            syncDialog.dispose();
        });

        buttonsPanel.add(dbSyncButton);
        buttonsPanel.add(fileUploadButton);

        // Cancel Button
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

    private void syncFromDatabase() {
       syncHandler.syncFromDatabase();
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CSV or Excel File");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "CSV & Excel Files", "csv", "xlsx", "xls");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(mainPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            processUploadedFile(selectedFile);
        }
    }

    private void processUploadedFile(File file) {
        syncStatusLabel.setText("Processing file: " + file.getName());
        syncStatusLabel.setForeground(new Color(16, 185, 129));

        // Simulate file processing
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.sleep(3000); // Simulate processing time
                return null;
            }

            @Override
            protected void done() {
                syncStatusLabel.setText("✅ File uploaded and synced successfully!");
                syncStatusLabel.setForeground(new Color(34, 197, 94));
            }
        };
        worker.execute();
    }

    private void updateSyncButton() {
        boolean enableSync = !marathonNameField.getText().trim().isEmpty();
        syncButton.setEnabled(enableSync);

        if (enableSync) {
            syncButton.setBackground(new Color(34, 197, 94));
            syncStatusLabel.setText("Ready to sync marathon data");
            syncStatusLabel.setForeground(new Color(61, 189, 64));
        } else {
            syncButton.setBackground(new Color(215, 221, 228));
            syncStatusLabel.setText("Please enter marathon name to enable sync");
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