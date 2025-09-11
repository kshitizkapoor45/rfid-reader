package com.rfid;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RfidTagProcessor {
    private final BlockingQueue<TagDetail> queue = new LinkedBlockingQueue<>();
    private final TagStorage storage;
    private final Util util;
    private volatile boolean running = true;

    public RfidTagProcessor(TagStorage storage,Util util) {
        this.storage = storage;
        this.util = util;
        startAsyncConsumer();
    }

    public void enqueue(TagDetail tag) {
        queue.offer(tag);
        util.addLog("🟢 Tag queued: " + tag.getTagId());
    }

    private void startAsyncConsumer() {
        Thread consumerThread = new Thread(() -> {
            while (running) {
                try {
                    TagDetail tag = queue.take();
                    processTag(tag);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    util.addLog("❌ Error processing tag: " + e.getMessage());
                }
            }
        }, "TagProcessor-Thread");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private void processTag(TagDetail tag) {
        try {
            Optional<TagDetail> existing = storage.findByTagIdAndAntenna(tag.getTagId(), tag.getAntenna());

            if (existing.isPresent()) {
                TagDetail t = existing.get();
                t.setLastSeen(tag.getLastSeen());
                storage.save(t);
                util.addLog("🔄 Updated existing tag " + t.getTagId());
            } else {
                storage.save(tag);
                util.addLog("✅ Inserted new tag " + tag.getTagId());
            }
        } catch (Exception ex) {
            util.addLog("⚠ Error saving tag " + tag.getTagId() + ": " + ex.getMessage());
        }
    }

    public void stop() {
        running = false;
    }

    public TagStorage getStorage() {
        return storage;
    }
}