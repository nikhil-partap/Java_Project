package com.campusmarketplace.console;

import com.campusmarketplace.exception.MarketplaceException;
import com.campusmarketplace.model.BuyerRequest;
import com.campusmarketplace.model.Listing;
import com.campusmarketplace.model.Purchase;
import com.campusmarketplace.model.User;
import com.campusmarketplace.service.MarketplaceService;
import com.campusmarketplace.thread.MarketplaceMonitorThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Simple menu-driven console interface for Campus Marketplace. */
public class ConsoleApp {

    private final MarketplaceService service;
    private final MarketplaceMonitorThread monitor;
    private final Scanner scanner;
    private User currentUser;

    public ConsoleApp(MarketplaceService service, MarketplaceMonitorThread monitor, Scanner scanner) {
        this.service = service;
        this.monitor = monitor;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("=== CAMPUS MARKETPLACE (CONSOLE EDITION) ===");
        System.out.println("Demo password: password123 | Buyer: ananya@campus.edu | Seller: aarav@campus.edu | Admin: nikhilpartapsingh@gmail.com");

        boolean running = true;
        while (running && scanner.hasNextLine()) {
            try {
                running = (currentUser == null) ? guestMenu() : userMenu();
            } catch (MarketplaceException e) {
                System.out.println("[Error] " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("[Error] Please enter a valid number.");
            }
        }
    }

    private boolean guestMenu() throws MarketplaceException {
        System.out.println("\n--- Guest Menu ---");
        System.out.println("1. Login   2. Register   3. Browse Listings   4. Concurrency Demo   5. Live Stats   0. Exit");
        String ch = prompt("Choice: ");
        if (ch == null) return false;

        switch (ch) {
            case "1" -> login();
            case "2" -> register();
            case "3" -> browseListings();
            case "4" -> concurrencyDemo();
            case "5" -> showStats();
            case "0" -> { return false; }
            default -> System.out.println("Invalid option.");
        }
        return true;
    }

    private boolean userMenu() throws MarketplaceException {
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());
        System.out.printf("%n--- Menu (%s | %s mode) ---%n", currentUser.getName(), currentUser.getMode());
        System.out.println("1. Browse/Search   2. Item Details   3. Send Request   4. Buy Item   5. My Purchases");
        System.out.println("6. Switch Mode     7. Post Listing   8. Delete Listing 9. Race Demo  10. Live Stats");
        if (isAdmin) System.out.println("11. Admin: Block/Unblock User");
        System.out.println("12. Logout         0. Exit");

        String ch = prompt("Choice: ");
        if (ch == null) return false;

        switch (ch) {
            case "1" -> browseListings();
            case "2" -> itemDetails();
            case "3" -> sendRequest();
            case "4" -> buyItem();
            case "5" -> myPurchases();
            case "6" -> {
                currentUser = service.switchMode(currentUser);
                System.out.println("Mode switched to: " + currentUser.getMode());
            }
            case "7" -> postListing();
            case "8" -> deleteListing();
            case "9" -> concurrencyDemo();
            case "10" -> showStats();
            case "11" -> { if (isAdmin) adminBlockUser(); else System.out.println("Admin only."); }
            case "12" -> { currentUser = null; System.out.println("Logged out."); }
            case "0" -> { return false; }
            default -> System.out.println("Invalid option.");
        }
        return true;
    }

    private void login() throws MarketplaceException {
        String email = prompt("Email: ");
        String pass = prompt("Password: ");
        currentUser = service.login(email, pass);
        System.out.println("Welcome, " + currentUser.getName() + "!");
    }

    private void register() throws MarketplaceException {
        String name = prompt("Name: ");
        String email = prompt("Email: ");
        String pass = prompt("Password: ");
        String dept = prompt("Department: ");
        currentUser = service.register(name, email, pass, dept);
        System.out.println("Registered and logged in as " + currentUser.getName());
    }

    private void browseListings() {
        String cat = prompt("Category (blank for all): ");
        String search = prompt("Keyword (blank for all): ");
        String sort = prompt("Sort (newest / price_asc / price_desc, blank for default): ");
        printListings(service.getListings(cat, search, sort));
    }

    private void itemDetails() throws MarketplaceException {
        Listing l = service.getListingById(prompt("Listing ID: "));
        System.out.printf("%n[%s] %s | %s | INR %.2f | %s | %s | Seller: %s%n",
                l.getId(), l.getTitle(), l.getCategory(), l.getPrice(),
                l.getCondition(), l.getLocation(), l.getStatus());
        for (BuyerRequest r : l.getBuyerRequests()) {
            System.out.printf("  Request %s from %s: INR %.2f - \"%s\"%n",
                    r.getId(), r.getBuyerId(), r.getOfferPrice(), r.getMessage());
        }
    }

    private void sendRequest() throws MarketplaceException {
        printListings(service.getListings("", "", "newest"));
        String id = prompt("Listing ID: ");
        String msg = prompt("Message: ");
        double offer = Double.parseDouble(prompt("Offer price (INR): "));
        Listing updated = service.sendRequest(id, currentUser.getId(), msg, offer);
        System.out.println("Request sent! Status is now: " + updated.getStatus());
    }

    private void buyItem() throws MarketplaceException {
        printListings(service.getListings("", "", "newest"));
        String id = prompt("Listing ID to buy: ");
        Purchase p = service.buyListing(id, currentUser.getId());
        System.out.printf("Bought '%s' for INR %.2f (Order %s).%n", p.getTitle(), p.getPrice(), p.getId());
    }

    private void myPurchases() {
        System.out.println("\n--- Purchase History ---");
        for (Purchase p : service.getPurchases()) {
            if (currentUser.getId().equals(p.getBuyerId()) || currentUser.getId().equals(p.getSellerId())) {
                System.out.printf("%s | %s | INR %.2f | buyer=%s | seller=%s | %s%n",
                        p.getId(), p.getTitle(), p.getPrice(), p.getBuyerId(), p.getSellerId(), p.getPurchasedAt());
            }
        }
    }

    private void postListing() throws MarketplaceException {
        if (!"seller".equalsIgnoreCase(currentUser.getMode())) {
            currentUser = service.switchMode(currentUser);
            System.out.println("Switched to seller mode.");
        }
        String title = prompt("Title: ");
        String cat = prompt("Category: ");
        double price = Double.parseDouble(prompt("Price (INR): "));
        String cond = prompt("Condition (Like New/Good): ");
        String loc = prompt("Location: ");
        Listing l = service.createListing(currentUser.getId(), title, cat, price, cond, loc);
        System.out.println("Posted listing: " + l.getId() + " - " + l.getTitle());
    }

    private void deleteListing() throws MarketplaceException {
        printListings(service.getListings("", "", "newest"));
        String id = prompt("Listing ID to delete: ");
        service.deleteListing(id, currentUser);
        System.out.println("Deleted listing " + id);
    }

    private void adminBlockUser() throws MarketplaceException {
        for (User u : service.getUsers()) {
            System.out.printf("%s | %-18s | %-26s | %s%n",
                    u.getId(), u.getName(), u.getEmail(), u.isBlocked() ? "BLOCKED" : "ACTIVE");
        }
        User updated = service.toggleBlock(prompt("User ID to block/unblock: "));
        System.out.println(updated.getName() + " is now " + (updated.isBlocked() ? "BLOCKED" : "ACTIVE"));
    }

    private void concurrencyDemo() {
        List<Listing> available = new ArrayList<>();
        for (Listing l : service.getListings("", "", "")) {
            if (!"Sold".equalsIgnoreCase(l.getStatus())) available.add(l);
        }
        if (available.isEmpty()) {
            System.out.println("No unsold listings left for demo.");
            return;
        }
        printListings(available);
        String target = prompt("Listing ID for 3-buyer test (blank for " + available.get(0).getId() + "): ");
        if (target == null) return;
        if (target.isEmpty()) target = available.get(0).getId();

        String[] buyers = {"u2", "u3", "u4"};
        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        final String listingId = target;

        for (String b : buyers) {
            futures.add(pool.submit(() -> {
                gate.await();
                try {
                    Purchase p = service.buyListing(listingId, b);
                    return b + " -> SUCCESS (Order " + p.getId() + ")";
                } catch (MarketplaceException e) {
                    return b + " -> FAILED (" + e.getMessage() + ")";
                }
            }));
        }
        gate.countDown();
        for (Future<String> f : futures) {
            try { System.out.println("  " + f.get()); } catch (Exception ignored) { }
        }
        pool.shutdownNow();
    }

    private void showStats() {
        System.out.println("\n--- Live Stats ---");
        for (Map.Entry<String, Object> e : monitor.getSnapshot().entrySet()) {
            System.out.printf("  %-16s: %s%n", e.getKey(), e.getValue());
        }
    }

    private void printListings(List<Listing> items) {
        System.out.printf("%n%-5s %-22s %-12s %-9s %-10s %-14s%n",
                "ID", "Title", "Category", "Price", "Status", "Location");
        for (Listing l : items) {
            System.out.printf("%-5s %-22s %-12s %-9.2f %-10s %-14s%n",
                    l.getId(), l.getTitle(), l.getCategory(), l.getPrice(), l.getStatus(), l.getLocation());
        }
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }
}
