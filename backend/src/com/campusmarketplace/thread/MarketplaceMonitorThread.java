package com.campusmarketplace.thread;

import com.campusmarketplace.service.MarketplaceService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MULTITHREADING - EXTENDING THREAD: a background "daemon" thread that recomputes the
 * marketplace statistics every few seconds (sleep loop). GET /api/stats just returns
 * the latest snapshot, so the request thread does not have to compute anything.
 * Stopped by interrupt() from the shutdown hook.
 */
public class MarketplaceMonitorThread extends Thread {

    private final MarketplaceService service;
    private final long intervalMillis;

    // volatile: the HTTP threads always read the newest snapshot written by this thread.
    private volatile Map<String, Object> snapshot;

    public MarketplaceMonitorThread(MarketplaceService service, long intervalMillis) {
        super("MarketplaceMonitor"); // thread name
        this.service = service;
        this.intervalMillis = intervalMillis;
        this.snapshot = service.computeStats(); // first snapshot right away
        setDaemon(true); // daemon: the JVM will not wait for this thread when exiting
    }

    @Override
    public void run() {
        System.out.println("[" + getName() + "] started, state = " + getState());
        while (!isInterrupted()) {
            snapshot = service.computeStats();
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                // interrupt() wakes us from sleep -> leave the loop and finish.
                System.out.println("[" + getName() + "] interrupted, stopping.");
                break;
            }
        }
    }

    /** Copy of the latest stats plus live info about this thread. */
    public Map<String, Object> getSnapshot() {
        Map<String, Object> copy = new LinkedHashMap<>(snapshot);
        copy.put("monitorThread", String.format("%s (%s, daemon=%b)", getName(), getState(), isDaemon()));
        return copy;
    }
}
