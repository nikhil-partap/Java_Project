package com.campusmarketplace;

import com.campusmarketplace.model.Item;
import com.campusmarketplace.service.MarketplaceService;
import com.campusmarketplace.thread.MarketplaceMonitorThread;
import com.campusmarketplace.thread.OrderProcessorTask;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        MarketplaceService service = new MarketplaceService();

        // Thread pool for processing buyer orders asynchronously without blocking the menu
        ExecutorService orderExecutor = Executors.newFixedThreadPool(3);

        // Background daemon thread updating live marketplace summary
        MarketplaceMonitorThread monitorThread = new MarketplaceMonitorThread(service);
        monitorThread.start();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=== CAMPUS MARKETPLACE CONSOLE (JAVA MULTITHREADING DEMO) ===");

        while (running) {
            printMenu();
            System.out.print("Enter choice: ");
            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);

                switch (choice) {
                    case 1 -> addListingFlow(scanner, service);
                    case 2 -> viewListingsFlow(service);
                    case 3 -> buyItemAsyncFlow(scanner, service, orderExecutor);
                    case 4 -> simulateConcurrentBuyersDemo(service, orderExecutor);
                    case 5 -> System.out.println("\n[Live Background Monitor Snapshot]\n" + monitorThread.getLatestSnapshot());
                    case 6 -> {
                        running = false;
                        System.out.println("\nShutting down threads and exiting...");
                    }
                    default -> System.out.println("Invalid option. Please choose between 1 and 6.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number (1-6).");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        monitorThread.shutdownMonitor();
        orderExecutor.shutdown();
        try {
            if (!orderExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                orderExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            orderExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        scanner.close();
        System.out.println("Goodbye! Campus Marketplace closed cleanly.");
    }

    private static void printMenu() {
        System.out.println("\n--- Menu ---");
        System.out.println("1. Add Campus Item (Seller)");
        System.out.println("2. View All Listings");
        System.out.println("3. Buy Item (Async Worker Thread)");
        System.out.println("4. Simulate Concurrent Buyers Race-Condition Demo");
        System.out.println("5. View Background Monitor Thread Snapshot");
        System.out.println("6. Exit");
    }

    private static void addListingFlow(Scanner scanner, MarketplaceService service) {
        System.out.print("Item Title: ");
        String title = scanner.nextLine();
        System.out.print("Category (Books / Lab Gear / Electronics / Hostel): ");
        String category = scanner.nextLine();
        System.out.print("Price (INR): ");
        double price = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Seller Name: ");
        String seller = scanner.nextLine();

        Item created = service.addItem(title, category, price, seller);
        System.out.println("Listed successfully: " + created);
    }

    
    private static void viewListingsFlow(MarketplaceService service) {
        List<Item> all = service.getAllItems();
        System.out.println("\n--- Campus Listings ---");
        if (all.isEmpty()) {
            System.out.println("No items available.");
            return;
        }
        for (Item item : all) {
            System.out.println(item);
        }
    }

    private static void buyItemAsyncFlow(Scanner scanner, MarketplaceService service, ExecutorService orderExecutor) {
        viewListingsFlow(service);
        System.out.print("Enter Item ID to buy: ");
        int itemId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Your Name (Buyer): ");
        String buyer = scanner.nextLine().trim();
        System.out.print("Preferred Campus Meetup (e.g., Library Gate / Hostel): ");
        String meetup = scanner.nextLine().trim();

        orderExecutor.submit(new OrderProcessorTask(service, itemId, buyer, meetup));
    }

    private static void simulateConcurrentBuyersDemo(MarketplaceService service, ExecutorService orderExecutor) {
        viewListingsFlow(service);
        System.out.println("\nLaunching 3 buyer threads simultaneously for Item #101...");
        orderExecutor.submit(new OrderProcessorTask(service, 101, "Student_A", "Central Library"));
        orderExecutor.submit(new OrderProcessorTask(service, 101, "Student_B", "Main Canteen"));
        orderExecutor.submit(new OrderProcessorTask(service, 101, "Student_C", "Boys Hostel Gate"));
    }
}