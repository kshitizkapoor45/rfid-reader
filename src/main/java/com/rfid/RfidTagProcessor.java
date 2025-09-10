package com.rfid;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RfidTagProcessor {
    private final BlockingQueue<TagDetail> queue = new LinkedBlockingQueue<>();
    private final TagStorage storage;
    private final java.util.function.Consumer<String> logConsumer;
    private volatile boolean running = true;

    public RfidTagProcessor(TagStorage storage, java.util.function.Consumer<String> logConsumer) {
        this.storage = storage;
        this.logConsumer = logConsumer;
        startAsyncConsumer();
    }

    public void enqueue(TagDetail tag) {
        queue.offer(tag);
        logConsumer.accept("🟢 Tag queued: " + tag.getTagId());
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
                    logConsumer.accept("❌ Error processing tag: " + e.getMessage());
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
                logConsumer.accept("🔄 Updated existing tag " + t.getTagId());
            } else {
                storage.save(tag);
                logConsumer.accept("✅ Inserted new tag " + tag.getTagId());
            }
        } catch (Exception ex) {
            logConsumer.accept("⚠ Error saving tag " + tag.getTagId() + ": " + ex.getMessage());
        }
    }

    public void stop() {
        running = false;
    }

    public TagStorage getStorage() {
        return storage;
    }
}