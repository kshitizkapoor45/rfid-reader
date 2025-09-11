package com.rfid;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {
    private final RfidPanel rfidPanel;

    public Util(RfidPanel rfidPanel) {
        this.rfidPanel = rfidPanel;
    }
    public void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
            rfidPanel.getLogsArea().append("[" + ts + "] " + message + "\n");
            rfidPanel.getLogsArea().setCaretPosition(rfidPanel.getLogsArea().getDocument().getLength());
        });
    }
}