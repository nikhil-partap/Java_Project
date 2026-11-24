package com.campusmarketplace;

import com.campusmarketplace.http.Router;
import com.campusmarketplace.service.MarketplaceService;
import com.campusmarketplace.storage.FileStorage;
import com.campusmarketplace.thread.MarketplaceMonitorThread;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Program entry point: wires everything together and starts the HTTP server.
 * MULTITHREADING: two thread pools, one daemon monitor thread and a shutdown hook.
 */
public class Main {

    private static final int DEFAULT_PORT = 8080;          // STATIC constant
    private static final long MONITOR_INTERVAL_MS = 3000;

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0].trim()); // WRAPPER CLASS parsing
            } catch (NumberFormatException e) {
                System.out.println("Invalid port '" + args[0] + "', using " + DEFAULT_PORT);
            }
        }

        // Thread pool that serves HTTP requests (8 requests can run at the same time).
        ExecutorService requestPool = Executors.newFixedThreadPool(8);
        // Separate pool where OrderProcessor (Callable) purchase jobs run.
        ExecutorService orderPool = Executors.newFixedThreadPool(4);

        // FILE HANDLING: data is saved in the "data" folder next to run.bat.
        MarketplaceService service = new MarketplaceService(orderPool, new FileStorage("data"));

        MarketplaceMonitorThread monitor = new MarketplaceMonitorThread(service, MONITOR_INTERVAL_MS);
        System.out.println("Monitor thread state before start(): " + monitor.getState()); // NEW
        monitor.start();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new Router(service, monitor));
        server.setExecutor(requestPool);
        server.start();

        // SHUTDOWN HOOK: a thread the JVM runs on Ctrl+C - clean up everything gracefully.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            server.stop(1);
            monitor.interrupt();
            requestPool.shutdown();
            orderPool.shutdown();
            try {
                if (!orderPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    orderPool.shutdownNow();
                }
                if (!requestPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    requestPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Bye.");
        }, "ShutdownHook"));

        System.out.println(String.format("Campus Marketplace backend running on http://localhost:%d/api/health", port));
    }
}
