package com.rfid;

import com.impinj.octane.*;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class RfidReaderConnection {

    private ImpinjReader reader;
    private final RfidTagProcessor rfidTagProcessor;
    private final Consumer<String> logConsumer;
    private final Consumer<Integer> tagCountConsumer;

    private final AtomicInteger tagCount = new AtomicInteger(0);
    private boolean isConnected = false;
    private boolean isStarted = false;

    public RfidReaderConnection(RfidTagProcessor rfidTagProcessor,
                                Consumer<String> logConsumer,
                                Consumer<Integer> tagCountConsumer) {
        this.rfidTagProcessor = rfidTagProcessor;
        this.logConsumer = logConsumer;
        this.tagCountConsumer = tagCountConsumer;
    }

    public void connect(String hostname, Runnable onConnected, Runnable onFailed) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    reader = new ImpinjReader();
                    log("Connecting to reader: " + hostname);
                    reader.connect(hostname);

                    if (!reader.isConnected()) {
                        throw new RuntimeException("Could not connect to reader: " + hostname);
                    }

                    Settings settings = reader.queryDefaultSettings();
                    configureReader(settings);
                    reader.applySettings(settings);

                    configureTagListener();

                    isConnected = true;
                    SwingUtilities.invokeLater(onConnected);

                } catch (OctaneSdkException | RuntimeException e) {
                    log("❌ Error connecting to reader: " + e.getMessage());
                    SwingUtilities.invokeLater(onFailed);
                }
                return null;
            }
        };
        worker.execute();
    }

    private void configureReader(Settings settings) throws OctaneSdkException {
        ReportConfig report = settings.getReport();
        report.setIncludeAntennaPortNumber(true);
        report.setIncludeFirstSeenTime(true);
        report.setIncludeLastSeenTime(true);
        report.setIncludePeakRssi(true);
        report.setMode(ReportMode.Individual);

        AntennaConfigGroup antennas = settings.getAntennas();
        antennas.disableAll();

        for (int i = 1; i <= 4; i++) {
            antennas.getAntenna(i).setEnabled(true);
            antennas.getAntenna(i).setTxPowerinDbm(30.0);
            antennas.getAntenna(i).setRxSensitivityinDbm(-70);
            log("Enabled antenna " + i);
        }
    }

    private void configureTagListener() {
        reader.setTagReportListener((r, report) -> {
            for (Tag tag : report.getTags()) {
                TagDetail tagDetail = new TagDetail();
                tagDetail.setTagId(tag.getEpc().toString());
                tagDetail.setAntenna(tag.getAntennaPortNumber());
                tagDetail.setFirstSeen(tag.getFirstSeenTime().getLocalDateTime().toInstant());
                tagDetail.setLastSeen(tag.getLastSeenTime().getLocalDateTime().toInstant());

                rfidTagProcessor.enqueue(tagDetail);

                log(String.format("📡 Tag detected: %s, Antenna: %d, RSSI: %.2f",
                        tag.getEpc(), tag.getAntennaPortNumber(), tag.getPeakRssiInDbm()));

                int currentCount = tagCount.incrementAndGet();
                SwingUtilities.invokeLater(() -> tagCountConsumer.accept(currentCount));
            }
        });
    }

    public void startReader() {
        if (reader != null && !isStarted) {
            try {
                reader.start();
                isStarted = true;
                log("✅ Reader started.");
            } catch (OctaneSdkException e) {
                log("❌ Error starting reader: " + e.getMessage());
            }
        }
    }

    public void stopReader() {
        if (reader != null && isStarted) {
            try {
                reader.stop();
                isStarted = false;
                log("⏹ Reader stopped.");
            } catch (OctaneSdkException e) {
                log("❌ Error stopping reader: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        if (reader != null && isConnected) {
            try {
                stopReader();
                reader.disconnect();
                isConnected = false;
                log("🔌 Disconnected from reader.");
            } catch (Exception e) {
                log("❌ Error disconnecting reader: " + e.getMessage());
            }
        }
    }

    public void resetTagCount() {
        tagCount.set(0);
        SwingUtilities.invokeLater(() -> tagCountConsumer.accept(0));
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logConsumer.accept(msg));
    }
}