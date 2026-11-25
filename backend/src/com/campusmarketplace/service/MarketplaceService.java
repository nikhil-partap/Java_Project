package com.campusmarketplace.service;

import com.campusmarketplace.exception.ItemSoldException;
import com.campusmarketplace.exception.MarketplaceException;
import com.campusmarketplace.model.BaseEntity;
import com.campusmarketplace.model.BuyerRequest;
import com.campusmarketplace.model.Listing;
import com.campusmarketplace.model.Purchase;
import com.campusmarketplace.model.User;
import com.campusmarketplace.repository.InMemoryRepository;
import com.campusmarketplace.repository.Repository;
import com.campusmarketplace.storage.FileStorage;
import com.campusmarketplace.thread.OrderProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** Business logic for users, listings, requests, purchases, and stats. */
public class MarketplaceService {

    private final Repository<User> users = new InMemoryRepository<>();
    private final Repository<Listing> listings = new InMemoryRepository<>();
    private final Repository<Purchase> purchases = new InMemoryRepository<>();
    private final Set<String> emails = new HashSet<>();

    private final AtomicInteger userCounter = new AtomicInteger();
    private final AtomicInteger listingCounter = new AtomicInteger();
    private final AtomicInteger requestCounter = new AtomicInteger();
    private final AtomicInteger purchaseCounter = new AtomicInteger();

    private final ExecutorService orderExecutor;
    private final FileStorage storage;

    public MarketplaceService(ExecutorService orderExecutor, FileStorage storage) {
        this.orderExecutor = orderExecutor;
        this.storage = storage;
        loadInitialData();
    }

    private void loadInitialData() {
        List<User> startUsers = SeedData.users();
        List<Listing> startListings = SeedData.listings();
        List<Purchase> startPurchases = new ArrayList<>();
        boolean loaded = false;

        if (storage.hasSavedData()) {
            try {
                startUsers = storage.loadUsers();
                startListings = storage.loadListings();
                startPurchases = storage.loadPurchases();
                loaded = true;
            } catch (IOException e) {
                System.out.println("Could not read saved data, using demo data.");
            }
        }

        for (User u : startUsers) {
            users.add(u);
            emails.add(u.getEmail().toLowerCase());
        }
        for (Listing l : startListings) {
            listings.add(l);
        }
        for (Purchase p : startPurchases) {
            purchases.add(p);
        }

        if (loaded) {
            System.out.printf("Loaded %d users, %d listings, %d purchases from %s%n",
                    users.count(), listings.count(), purchases.count(), storage.getFolderPath());
        } else {
            System.out.println("Starting with demo data.");
            save();
        }
    }

    private void save() {
        try {
            synchronized (storage) {
                storage.saveAll(users.findAll(), listings.findAll(), purchases.findAll());
            }
        } catch (IOException e) {
            System.out.println("Warning: could not save data: " + e.getMessage());
        }
    }

    // ---- Auth & Users ----

    public User login(String email, String password) throws MarketplaceException {
        String cleanEmail = email == null ? "" : email.trim();
        for (User u : users.findAll()) {
            if (u.getEmail().equalsIgnoreCase(cleanEmail) && u.checkPassword(password)) {
                if (u.isBlocked()) {
                    throw new MarketplaceException("This account is blocked by an administrator.");
                }
                return u;
            }
        }
        throw new MarketplaceException("Invalid email or password.");
    }

    public synchronized User register(String name, String email, String password, String department)
            throws MarketplaceException {
        String cleanEmail = FileStorage.clean(email).toLowerCase();
        if (cleanEmail.isEmpty() || password == null || password.isEmpty()) {
            throw new MarketplaceException("Email and password are required.");
        }
        if (!emails.add(cleanEmail)) {
            throw new MarketplaceException("An account with this email already exists.");
        }
        String cleanName = FileStorage.clean(name);
        if (cleanName.isEmpty()) {
            cleanName = cleanEmail;
        }
        User user = new User(nextId("u-", userCounter, users), cleanName, cleanEmail,
                FileStorage.clean(password), FileStorage.clean(department), "buyer", "user", false);
        users.add(user);
        save();
        return user;
    }

    public User switchMode(User user) {
        String next = "buyer".equalsIgnoreCase(user.getMode()) ? "seller" : "buyer";
        user.setMode(next);
        save();
        return user;
    }

    public User toggleBlock(String userId) throws MarketplaceException {
        User user = findUser(userId);
        user.toggleBlocked();
        save();
        return user;
    }

    public List<User> getUsers() { return users.findAll(); }

    public User getUserById(String id) throws MarketplaceException { return findUser(id); }

    // ---- Listings & Requests ----

    public List<Listing> getListings(String category, String search, String sortBy) {
        List<Listing> result = new ArrayList<>(listings.findAll());
        String cat = category == null ? "" : category.trim();
        String term = search == null ? "" : search.trim().toLowerCase();

        Iterator<Listing> it = result.iterator();
        while (it.hasNext()) {
            Listing l = it.next();
            boolean catOk = cat.isEmpty() || cat.equalsIgnoreCase("all") || cat.equalsIgnoreCase(l.getCategory());
            boolean searchOk = term.isEmpty() || l.getTitle().toLowerCase().contains(term);
            if (!catOk || !searchOk) {
                it.remove();
            }
        }

        if ("price_asc".equalsIgnoreCase(sortBy)) {
            result.sort(Comparator.comparingDouble(Listing::getPrice));
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            result.sort(Comparator.comparingDouble(Listing::getPrice).reversed());
        } else if ("newest".equalsIgnoreCase(sortBy)) {
            Collections.sort(result);
        }
        return result;
    }

    public Listing getListingById(String id) throws MarketplaceException { return findListing(id); }

    public synchronized Listing createListing(String ownerId, String title, String category,
                                              double price, String condition, String location)
            throws MarketplaceException {
        User owner = findUser(ownerId);
        if (owner.isBlocked()) {
            throw new MarketplaceException("Blocked users cannot post listings.");
        }
        String cleanTitle = FileStorage.clean(title);
        if (cleanTitle.isEmpty()) {
            throw new MarketplaceException("Title cannot be empty.");
        }
        if (price < 0) {
            throw new MarketplaceException("Price must be >= 0.");
        }
        Listing listing = new Listing(nextId("l-", listingCounter, listings), ownerId, cleanTitle,
                FileStorage.clean(category), price, FileStorage.clean(condition),
                FileStorage.clean(location), "Available", BaseEntity.nowText());
        listings.add(listing);
        save();
        return listing;
    }

    public void deleteListing(String listingId, User actor) throws MarketplaceException {
        Listing listing = findListing(listingId);
        boolean isAdmin = "admin".equalsIgnoreCase(actor.getRole());
        if (!isAdmin && !listing.getOwnerId().equals(actor.getId())) {
            throw new MarketplaceException("You can only delete your own listings.");
        }
        listings.deleteById(listingId);
        save();
    }

    public Listing sendRequest(String listingId, String buyerId, String message, double offerPrice)
            throws MarketplaceException {
        Listing listing = findListing(listingId);
        User buyer = findUser(buyerId);
        if (buyer.isBlocked()) {
            throw new MarketplaceException("Blocked users cannot send requests.");
        }
        if (offerPrice < 0) {
            throw new MarketplaceException("Offer price must be >= 0.");
        }
        synchronized (listing) {
            if ("Sold".equals(listing.getStatus())) {
                throw new ItemSoldException("This item has already been sold.");
            }
            BuyerRequest req = new BuyerRequest(nextId("r-", requestCounter, null),
                    buyerId, FileStorage.clean(message), offerPrice, BaseEntity.nowText());
            listing.addRequest(req);
            if ("Available".equals(listing.getStatus())) {
                listing.setStatus("Pending");
            }
        }
        save();
        return listing;
    }

    // ---- Thread-safe Purchases ----

    public Purchase buyListing(String listingId, String buyerId) throws MarketplaceException {
        findListing(listingId);
        Future<Purchase> future = orderExecutor.submit(new OrderProcessor(this, listingId, buyerId));
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof MarketplaceException) {
                throw (MarketplaceException) e.getCause();
            }
            throw new MarketplaceException("Order failed: " + e.getCause().getMessage());
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new MarketplaceException("Order timed out, please retry.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MarketplaceException("Order interrupted.");
        }
    }

    /** Synchronized on listing so two concurrent threads can never sell the same item twice. */
    public Purchase completePurchase(String listingId, String buyerId)
            throws MarketplaceException, InterruptedException {
        Listing listing = findListing(listingId);
        User buyer = users.findById(buyerId);
        if (buyer != null && buyer.isBlocked()) {
            throw new MarketplaceException("Blocked users cannot buy items.");
        }

        Purchase purchase;
        synchronized (listing) {
            if ("Sold".equals(listing.getStatus())) {
                throw new ItemSoldException("This item has already been sold.");
            }
            Thread.sleep(100); // simulate order processing
            listing.setStatus("Sold");
            purchase = new Purchase(nextId("p-", purchaseCounter, purchases), listing.getId(),
                    listing.getTitle(), listing.getPrice(), listing.getOwnerId(),
                    buyerId, BaseEntity.nowText());
            purchases.add(purchase);
        }
        save();
        return purchase;
    }

    public List<Purchase> getPurchases() { return purchases.findAll(); }

    public Map<String, Object> computeStats() {
        int available = 0, pending = 0, sold = 0;
        Map<String, Integer> byCategory = new TreeMap<>();
        List<Listing> all = listings.findAll();
        for (Listing l : all) {
            if ("Available".equals(l.getStatus())) available++;
            else if ("Pending".equals(l.getStatus())) pending++;
            else if ("Sold".equals(l.getStatus())) sold++;
            byCategory.merge(l.getCategory(), 1, Integer::sum);
        }
        double revenue = 0;
        for (Purchase p : purchases.findAll()) {
            revenue += p.getPrice();
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalListings", all.size());
        stats.put("available", available);
        stats.put("pending", pending);
        stats.put("sold", sold);
        stats.put("totalUsers", users.count());
        stats.put("ordersProcessed", OrderProcessor.getOrdersProcessed());
        stats.put("revenue", revenue);
        stats.put("byCategory", byCategory);
        stats.put("lastUpdated", BaseEntity.nowText());
        return stats;
    }

    private User findUser(String id) throws MarketplaceException {
        User u = users.findById(id);
        if (u == null) throw new MarketplaceException("User not found: " + id);
        return u;
    }

    private Listing findListing(String id) throws MarketplaceException {
        Listing l = listings.findById(id);
        if (l == null) throw new MarketplaceException("Listing not found: " + id);
        return l;
    }

    private static String nextId(String prefix, AtomicInteger counter, Repository<?> repo) {
        String id;
        do {
            id = prefix + counter.incrementAndGet();
        } while (repo != null && repo.findById(id) != null);
        return id;
    }
}
