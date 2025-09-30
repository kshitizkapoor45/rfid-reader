package com.rfid;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ReaderConnectionManager {
    private final RfidTagProcessor tagProcessor;
    private final Util util;
    private final Map<String, RfidReaderConnection> connections = new ConcurrentHashMap<>();

    public ReaderConnectionManager(RfidTagProcessor tagProcessor, Util util) {
        this.tagProcessor = tagProcessor;
        this.util = util;
    }

    public void connectReader(String ip,
                              Consumer<Integer> tagCountCallback,
                              Runnable onConnected,
                              Runnable onFailed) {

        if (connections.containsKey(ip)) {
            util.addLog("Already connected: " + ip);
            return;
        }

        RfidReaderConnection connection = new RfidReaderConnection(tagProcessor, util, tagCountCallback);
        connections.put(ip, connection);

        connection.connect(ip,
                () -> {
                    util.addLog("✅ Connected: " + ip);
                    onConnected.run();
                },
                () -> {
                    util.addLog("❌ Failed to connect: " + ip);
                    connections.remove(ip);
                    onFailed.run();
                }
        );
    }

    public void disconnectReader(String ip) {
        RfidReaderConnection connection = connections.remove(ip);
        if (connection != null) {
            connection.disconnect();
            util.addLog("🔌 Disconnected: " + ip);
        }
    }

    public void startReader(String ip) {
        RfidReaderConnection connection = connections.get(ip);
        if (connection != null) connection.startReader();
    }

    public void stopReader(String ip) {
        RfidReaderConnection connection = connections.get(ip);
        if (connection != null) connection.stopReader();
    }

    public void startAll() {
        connections.values().forEach(RfidReaderConnection::startReader);
    }

    public void stopAll() {
        connections.values().forEach(RfidReaderConnection::stopReader);
    }

    public int getTotalTagCount() {
        return connections.values().stream().mapToInt(RfidReaderConnection::getTagCount).sum();
    }
}
