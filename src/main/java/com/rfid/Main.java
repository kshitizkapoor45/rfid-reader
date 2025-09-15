package com.rfid;

import javax.swing.*;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                RFIDMarathonApp app = new RFIDMarathonApp();
                ImageIcon icon = new ImageIcon(Main.class.getResource("/ICON.png"));
                app.setIconImage(icon.getImage());
                app.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}