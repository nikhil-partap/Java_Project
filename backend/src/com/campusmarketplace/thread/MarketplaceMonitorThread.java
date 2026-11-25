package com.campusmarketplace.thread;

import com.campusmarketplace.service.MarketplaceService;

import java.util.LinkedHashMap;
import java.util.Map;

/** Background daemon thread that refreshes marketplace statistics periodically. */
public class MarketplaceMonitorThread extends Thread {

    private final MarketplaceService service;
    private final long intervalMillis;
    private volatile Map<String, Object> snapshot;

    public MarketplaceMonitorThread(MarketplaceService service, long intervalMillis) {
        super("MarketplaceMonitor");
        this.service = service;
        this.intervalMillis = intervalMillis;
        this.snapshot = service.computeStats();
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            snapshot = service.computeStats();
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public Map<String, Object> getSnapshot() {
        Map<String, Object> copy = new LinkedHashMap<>(snapshot);
        copy.put("monitorThread", getName() + " (" + getState() + ")");
        return copy;
    }
}
