package com.campusmarketplace;

import com.campusmarketplace.console.ConsoleApp;
import com.campusmarketplace.service.MarketplaceService;
import com.campusmarketplace.storage.FileStorage;
import com.campusmarketplace.thread.MarketplaceMonitorThread;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Program entry point for the Campus Marketplace console application. */
public class Main {

    public static void main(String[] args) {
        ExecutorService orderPool = Executors.newFixedThreadPool(4);
        MarketplaceService service = new MarketplaceService(orderPool, new FileStorage("data"));
        MarketplaceMonitorThread monitor = new MarketplaceMonitorThread(service, 3000);
        monitor.start();

        try (Scanner scanner = new Scanner(System.in)) {
            new ConsoleApp(service, monitor, scanner).run();
        } finally {
            monitor.interrupt();
            orderPool.shutdown();
            try {
                if (!orderPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    orderPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                orderPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Goodbye!");
        }
    }
}
