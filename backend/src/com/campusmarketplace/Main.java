package com.campusmarketplace;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private final MarketplaceService service;
    private final Scanner sc;
    private User currentUser;

    public Main(MarketplaceService service, Scanner sc) {
        this.service = service;
        this.sc = sc;
    }

    public static void main(String[] args) {
        ExecutorService orderPool = Executors.newFixedThreadPool(4);
        FileStorage storage = new FileStorage("data");
        MarketplaceService service = new MarketplaceService(orderPool, storage);

        try (Scanner sc = new Scanner(System.in)) {
            new Main(service, sc).run();
        } finally {
            orderPool.shutdown();
            try {
                if (!orderPool.awaitTermination(3, TimeUnit.SECONDS)) orderPool.shutdownNow();
            } catch (InterruptedException e) {
                orderPool.shutdownNow();
            }
            System.out.println("Goodbye!");
        }
    }

    public void run() {
        System.out.println("=== CAMPUS MARKETPLACE ===");
        System.out.println("Demo password: password123 | Accounts: ananya@campus.edu, aarav@campus.edu");

        boolean running = true;
        while (running && sc.hasNextLine()) {
            try {
                running = (currentUser == null) ? guestMenu() : userMenu();
            } catch (Exception e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }
    }

    // Guest Menu: Login, Register, Browse Listings, Exit
    private boolean guestMenu() throws Exception {
        System.out.println("\n--- Guest Menu ---");
        System.out.println("1. Login   2. Register   3. Browse Listings   0. Exit");
        String ch = prompt("Choice: ");
        if (ch == null) return false;

        switch (ch) {
            case "1" -> login();
            case "2" -> register();
            case "3" -> browseListings();
            case "0" -> { return false; }
            default -> System.out.println("Invalid option.");
        }
        return true;
    }

    // User Menu: Browse/Search, Send Purchase Request, Sell Item, View Requests & Accept, Delete Listing, My Purchases, Logout, Exit
    private boolean userMenu() throws Exception {
        System.out.printf("%n--- Menu (%s) ---%n", currentUser.getName());
        System.out.println("1. Browse / Search Listings");
        System.out.println("2. Send Purchase Request (Buy)");
        System.out.println("3. Sell Item");
        System.out.println("4. View Incoming Requests & Accept");
        System.out.println("5. Delete Listing");
        System.out.println("6. My Purchases & Sales");
        System.out.println("7. Logout");
        System.out.println("0. Exit");

        String ch = prompt("Choice: ");
        if (ch == null) return false;

        switch (ch) {
            case "1" -> browseListings();
            case "2" -> sendPurchaseRequest();
            case "3" -> sellItem();
            case "4" -> viewIncomingRequests();
            case "5" -> deleteListing();
            case "6" -> myPurchases();
            case "7" -> { currentUser = null; System.out.println("Logged out."); }
            case "0" -> { return false; }
            default -> System.out.println("Invalid option.");
        }
        return true;
    }

    private void login() throws Exception {
        String email = prompt("Email: ");
        String pass = prompt("Password: ");
        currentUser = service.login(email, pass);
        System.out.println("Welcome, " + currentUser.getName() + "!");
    }

    private void register() throws Exception {
        String name = prompt("Name: ");
        String email = prompt("Email: ");
        String pass = prompt("Password: ");
        String dept = prompt("Department: ");
        currentUser = service.register(name, email, pass, dept);
        System.out.println("Registered successfully! Logged in as " + currentUser.getName());
    }

    private void browseListings() {
        printItems(service.getListings("", "", "newest"));
    }

    private void printItems(List<Listing> list) {
        System.out.printf("%n%-5s %-24s %-12s %-9s %-10s %-14s%n", "ID", "Title", "Category", "Price", "Status", "Location");
        System.out.println("-------------------------------------------------------------------------------");
        for (Listing l : list) {
            System.out.printf("%-5s %-24s %-12s %-9.2f %-10s %-14s%n",
                    l.getId(), l.getTitle(), l.getCategory(), l.getPrice(), l.getStatus(), l.getLocation());
        }
    }

    private void sendPurchaseRequest() throws Exception {
        printItems(service.getListings("", "", "newest"));
        String id = prompt("\nEnter Listing ID to buy: ");
        String note = prompt("Meetup Note / Message to Seller (optional): ");
        PurchaseRequest req = service.sendPurchaseRequest(id, currentUser.getId(), note);
        System.out.printf("Purchase request [%s] sent to seller! The seller will review and approve.%n", req.getId());
    }

    private void sellItem() throws Exception {
        String title = prompt("Item Title: ");
        String cat = prompt("Category (Books / Lab Gear / Cycles / Electronics / Furniture): ");
        double price = Double.parseDouble(prompt("Price (INR): "));
        String loc = prompt("Campus Location: ");

        Listing l = service.addListing(currentUser.getId(), title, cat, price, loc);
        System.out.printf("Posted listing [%s] '%s' for INR %.2f.%n", l.getId(), l.getTitle(), l.getPrice());
    }

    private void viewIncomingRequests() throws Exception {
        List<PurchaseRequest> incoming = service.getIncomingRequests(currentUser.getId());
        System.out.println("\n--- Incoming Purchase Requests ---");
        if (incoming.isEmpty()) {
            System.out.println("No pending purchase requests for your listings.");
            return;
        }

        System.out.printf("%-6s %-10s %-22s %-14s %-20s %-16s%n", "Req ID", "Item ID", "Item Title", "Buyer", "Note", "Date");
        System.out.println("---------------------------------------------------------------------------------------------");
        for (PurchaseRequest r : incoming) {
            Listing l = service.findListing(r.getListingId());
            System.out.printf("%-6s %-10s %-22s %-14s %-20s %-16s%n",
                    r.getId(), r.getListingId(), l.getTitle(),
                    service.getUserName(r.getBuyerId()),
                    r.getNote(), r.getCreatedAt());
        }

        String reqId = prompt("\nEnter Request ID to accept (or press Enter to cancel): ");
        if (reqId != null && !reqId.isEmpty()) {
            Purchase p = service.acceptRequest(reqId, currentUser.getId());
            System.out.printf("Request accepted! Item '%s' marked as SOLD to %s for INR %.2f! (Order ID: %s)%n",
                    p.getTitle(), service.getUserName(p.getBuyerId()), p.getPrice(), p.getId());
        }
    }

    private void deleteListing() throws Exception {
        printItems(service.getListings("", "", "newest"));
        String id = prompt("\nEnter Listing ID to delete: ");
        service.deleteListing(id, currentUser.getId());
        System.out.println("Listing " + id + " deleted successfully.");
    }

    private void myPurchases() {
        List<Purchase> list = service.getPurchasesForUser(currentUser.getId());
        System.out.println("\n--- My Purchase & Sales History ---");
        if (list.isEmpty()) {
            System.out.println("No purchases or sales found.");
            return;
        }
        System.out.printf("%-5s %-22s %-9s %-14s %-14s %-18s%n", "ID", "Title", "Price", "Buyer", "Seller", "Date");
        System.out.println("-----------------------------------------------------------------------------------");
        for (Purchase p : list) {
            System.out.printf("%-5s %-22s %-9.2f %-14s %-14s %-18s%n",
                    p.getId(), p.getTitle(), p.getPrice(),
                    service.getUserName(p.getBuyerId()),
                    service.getUserName(p.getSellerId()),
                    p.getPurchasedAt());
        }
    }

    private String prompt(String text) {
        System.out.print(text);
        return sc.hasNextLine() ? sc.nextLine().trim() : null;
    }
}
