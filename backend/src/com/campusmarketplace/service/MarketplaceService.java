package com.campusmarketplace.service;

import com.campusmarketplace.exception.ApiException;
import com.campusmarketplace.exception.ConflictException;
import com.campusmarketplace.exception.ForbiddenException;
import com.campusmarketplace.exception.NotFoundException;
import com.campusmarketplace.exception.UnauthorizedException;
import com.campusmarketplace.exception.ValidationException;
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

/**
 * SERVICE LAYER: all business rules live here (the HTTP layer only routes).
 * Methods declare "throws ApiException" (THROW/THROWS) and let the exception
 * PROPAGATE up to Router, which is the single place that turns it into JSON.
 */
public class MarketplaceService {

    // INTERFACE type on the left, implementation on the right (POLYMORPHISM).
    private final Repository<User> users = new InMemoryRepository<>();
    private final Repository<Listing> listings = new InMemoryRepository<>();
    private final Repository<Purchase> purchases = new InMemoryRepository<>();

    // COLLECTIONS - HashSet: guarantees every e-mail is registered only once.
    private final Set<String> emails = new HashSet<>();

    // MULTITHREADING - AtomicInteger: thread-safe id counters without locks.
    private final AtomicInteger userCounter = new AtomicInteger();
    private final AtomicInteger listingCounter = new AtomicInteger();
    private final AtomicInteger requestCounter = new AtomicInteger();
    private final AtomicInteger purchaseCounter = new AtomicInteger();

    private final ExecutorService orderExecutor;
    private final FileStorage storage;

    /**
     * FILE HANDLING: if the data folder has saved files, load them; otherwise start
     * from the demo data and write it to the files straight away.
     */
    public MarketplaceService(ExecutorService orderExecutor, FileStorage storage) {
        this.orderExecutor = orderExecutor;
        this.storage = storage;

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
                System.out.println("Could not read saved data (" + e.getMessage() + "), using demo data.");
                startUsers = SeedData.users();
                startListings = SeedData.listings();
                startPurchases = new ArrayList<>();
            }
        }

        for (User u : startUsers) {
            users.add(u);
            emails.add(u.getEmail().toLowerCase());
        }
        for (Listing l : startListings) {
            listings.add(l);
            for (BuyerRequest r : l.getBuyerRequests()) {
                bumpCounter(requestCounter, r.getId(), "r-"); // new request ids never repeat old ones
            }
        }
        for (Purchase p : startPurchases) {
            purchases.add(p);
        }

        if (loaded) {
            System.out.printf("Loaded %d users, %d listings, %d purchases from %s%n",
                    users.count(), listings.count(), purchases.count(), storage.getFolderPath());
        } else {
            System.out.println("No saved data found, starting with demo data.");
            save();
        }
    }

    /**
     * Writes the current data to the files. Called after every change.
     * Errors are only logged: the change already succeeded in memory.
     * Must NOT be called while holding a listing lock (avoids a deadlock with saveAll).
     */
    private void save() {
        try {
            synchronized (storage) { // snapshot + write together, so the files hold the latest state
                storage.saveAll(users.findAll(), listings.findAll(), purchases.findAll());
            }
        } catch (IOException e) {
            System.out.println("WARNING: could not save data to files: " + e.getMessage());
        }
    }

    /** If id looks like "r-12", makes sure the counter is at least 12. */
    private static void bumpCounter(AtomicInteger counter, String id, String prefix) {
        if (id != null && id.startsWith(prefix)) {
            try {
                int n = Integer.parseInt(id.substring(prefix.length()));
                counter.accumulateAndGet(n, Math::max);
            } catch (NumberFormatException ignored) {
                // not a numbered id (e.g. "r-abc"), nothing to do
            }
        }
    }

    // ------------------------------------------------------------------ state

    public Map<String, Object> getState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("users", users.findAll());
        state.put("listings", listings.findAll());
        state.put("purchaseHistory", purchases.findAll());
        return state;
    }

    // ------------------------------------------------------------------ auth

    public User login(Map<String, Object> body) throws ApiException {
        String email = text(body, "email").trim(); // STRINGS: trim
        String password = text(body, "password");
        for (User u : users.findAll()) {
            // STRINGS: equalsIgnoreCase -> "Aarav@Campus.edu" also works
            if (u.getEmail().equalsIgnoreCase(email) && u.checkPassword(password)) {
                if (u.isBlocked()) {
                    throw new ForbiddenException("This account has been suspended by an administrator.");
                }
                return u;
            }
        }
        throw new UnauthorizedException("Invalid email or password.");
    }

    /** synchronized: check-then-add on the e-mail set must happen as one step. */
    public synchronized User register(Map<String, Object> body) throws ApiException {
        String email = text(body, "email").trim().toLowerCase(); // STRINGS: trim + toLowerCase
        String password = text(body, "password");
        if (email.isEmpty() || password.isEmpty()) {
            throw new ValidationException("Email and password are required.");
        }
        if (!emails.add(email)) { // HashSet.add returns false for a duplicate
            throw new ConflictException("An account with this email already exists.");
        }
        String name = text(body, "name").trim();
        if (name.isEmpty()) {
            int at = email.indexOf('@');
            name = at > 0 ? email.substring(0, at) : email;
        }
        User user = new User(nextId("u-", userCounter, users), name, email, password,
                text(body, "phone"), text(body, "department"), text(body, "year"),
                "buyer", text(body, "bio"), "user", false);
        users.add(user);
        save();
        return user;
    }

    // ------------------------------------------------------------------ users

    public User updateUser(String id, Map<String, Object> patch) throws ApiException {
        User user = findUser(id);
        // id, role, password, isBlocked are deliberately ignored.
        if (patch.containsKey("mode")) {
            String mode = text(patch, "mode").trim().toLowerCase();
            if (!mode.equals("buyer") && !mode.equals("seller")) {
                throw new ValidationException("mode must be 'buyer' or 'seller'.");
            }
            user.setMode(mode);
        }
        if (patch.containsKey("name") && !text(patch, "name").isBlank()) {
            user.setName(text(patch, "name").trim());
        }
        if (patch.containsKey("phone")) user.setPhone(text(patch, "phone"));
        if (patch.containsKey("department")) user.setDepartment(text(patch, "department"));
        if (patch.containsKey("year")) user.setYear(text(patch, "year"));
        if (patch.containsKey("bio")) user.setBio(text(patch, "bio"));
        save();
        return user;
    }

    public User toggleBlock(String id) throws ApiException {
        User user = findUser(id);
        user.toggleBlocked();
        save();
        return user;
    }

    // ------------------------------------------------------------------ listings

    public List<Listing> getListings(String category, String search, String sort) {
        List<Listing> result = new ArrayList<>(listings.findAll());
        String cat = category == null ? "" : category.trim();
        String term = search == null ? "" : search.trim().toLowerCase();

        // COLLECTIONS - ITERATOR REMOVAL: drop listings that do not match the filters.
        Iterator<Listing> it = result.iterator();
        while (it.hasNext()) {
            Listing l = it.next();
            boolean categoryOk = cat.isEmpty() || cat.equalsIgnoreCase("all")
                    || cat.equalsIgnoreCase(l.getCategory());
            boolean searchOk = term.isEmpty()
                    || lower(l.getTitle()).contains(term)
                    || lower(l.getDescription()).contains(term);
            if (!categoryOk || !searchOk) {
                it.remove();
            }
        }

        // COMPARATOR / COMPARABLE sorting
        String s = sort == null ? "" : sort.trim().toLowerCase();
        switch (s) {
            case "price_asc" -> result.sort(Comparator.comparingDouble(Listing::getPrice));
            case "price_desc" -> result.sort(Comparator.comparingDouble(Listing::getPrice).reversed());
            case "newest" -> Collections.sort(result); // uses Listing.compareTo (Comparable)
            default -> { } // keep stored order
        }
        return result;
    }

    /** synchronized: "is this id free?" + "add it" must not interleave with another create. */
    public synchronized Listing createListing(Map<String, Object> body) throws ApiException {
        String ownerId = text(body, "ownerId").trim();
        User owner = users.findById(ownerId);
        if (owner == null) {
            throw new ValidationException("ownerId does not match any user.");
        }
        if (owner.isBlocked()) {
            throw new ForbiddenException("Blocked users cannot create listings.");
        }
        String title = text(body, "title").trim();
        if (title.isEmpty()) {
            throw new ValidationException("Title is required.");
        }
        if (body.get("price") == null) {
            throw new ValidationException("Price is required.");
        }
        double price = checkPrice(number(body, "price"));

        String id = text(body, "id").trim();
        if (id.isEmpty() || listings.findById(id) != null) {
            id = nextId("l-", listingCounter, listings);
        }
        String status = body.get("status") == null ? "Available" : normalizeStatus(text(body, "status"));
        String createdAt = text(body, "createdAt").isBlank() ? BaseEntity.nowIso() : text(body, "createdAt");

        Listing listing = new Listing(id, ownerId, title, text(body, "category"), price,
                text(body, "condition"), text(body, "description"), text(body, "location"),
                status, createdAt, text(body, "imageUrl"));
        applyImages(listing, body);
        if (body.get("buyerRequests") != null) {
            listing.setBuyerRequests(parseRequests(body.get("buyerRequests"), price));
        }
        listings.addFirst(listing); // newest listing shown first, like the frontend
        save();
        return listing;
    }

    public Listing updateListing(String id, Map<String, Object> patch) throws ApiException {
        Listing listing = findListing(id);
        synchronized (listing) { // SYNCHRONIZED BLOCK: whole patch applied atomically
            if (patch.containsKey("title")) {
                String title = text(patch, "title").trim();
                if (title.isEmpty()) {
                    throw new ValidationException("Title is required.");
                }
                listing.setTitle(title);
            }
            if (patch.containsKey("price")) listing.setPrice(checkPrice(number(patch, "price")));
            if (patch.containsKey("status")) listing.setStatus(normalizeStatus(text(patch, "status")));
            if (patch.containsKey("category")) listing.setCategory(text(patch, "category"));
            if (patch.containsKey("condition")) listing.setCondition(text(patch, "condition"));
            if (patch.containsKey("description")) listing.setDescription(text(patch, "description"));
            if (patch.containsKey("location")) listing.setLocation(text(patch, "location"));
            if (patch.containsKey("createdAt")) listing.setCreatedAt(text(patch, "createdAt"));
            if (patch.containsKey("imageUrl") || patch.containsKey("images")) {
                if (patch.containsKey("imageUrl")) listing.setImageUrl(text(patch, "imageUrl"));
                applyImages(listing, patch);
            }
            if (patch.get("buyerRequests") != null) {
                listing.setBuyerRequests(parseRequests(patch.get("buyerRequests"), listing.getPrice()));
            }
        }
        save(); // outside the synchronized block
        return listing;
    }

    public void deleteListing(String id) throws ApiException {
        if (!listings.deleteById(id)) {
            throw new NotFoundException("Listing not found: " + id);
        }
        save();
    }

    public Listing addRequest(String listingId, Map<String, Object> body) throws ApiException {
        Listing listing = findListing(listingId);
        checkBuyer(text(body, "buyerId"));
        double offer = body.get("offerPrice") == null ? listing.getPrice() : checkPrice(number(body, "offerPrice"));
        BuyerRequest request = parseRequest(body, offer); // keeps the client's id if it sent one
        synchronized (listing) {
            if ("Sold".equals(listing.getStatus())) {
                throw new ConflictException("This item has already been sold.");
            }
            listing.addRequestFirst(request);
            if ("Available".equals(listing.getStatus())) {
                listing.setStatus("Pending");
            }
        }
        save();
        return listing;
    }

    // ------------------------------------------------------------------ purchases

    /**
     * MULTITHREADING: the order is handed to a worker thread (Callable submitted to an
     * ExecutorService) and we wait for its result with Future.get(timeout).
     */
    public Purchase purchase(String listingId, Map<String, Object> body) throws ApiException {
        findListing(listingId); // fast 404 before queueing
        Future<Purchase> future = orderExecutor.submit(
                new OrderProcessor(this, listingId, text(body, "buyerId").trim()));
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // The worker's exception is wrapped; unwrap it so the real status (409, 404...) reaches the client.
            if (e.getCause() instanceof ApiException) {
                throw (ApiException) e.getCause();
            }
            throw new ApiException(500, "Order failed: " + e.getCause().getMessage());
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ApiException(503, "Order processing timed out, please retry.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the interrupt flag
            throw new ApiException(500, "Order processing was interrupted.");
        }
    }

    /**
     * Runs on an order-pool thread (called from OrderProcessor.call()).
     * RACE-CONDITION PROTECTION: the synchronized block locks this one listing, so when
     * several buyers try at the same moment only the first sees "not Sold"; the others
     * wait, then see "Sold" and get a 409 Conflict.
     */
    public Purchase completePurchase(String listingId, String buyerId) throws ApiException, InterruptedException {
        Listing listing = findListing(listingId);
        // The buyer does not have to exist (a seller may mark an item sold to "unknown"),
        // but a known, blocked user may not buy.
        User buyer = users.findById(buyerId);
        if (buyer != null && buyer.isBlocked()) {
            throw new ForbiddenException("This account has been suspended by an administrator.");
        }
        Purchase purchase;
        synchronized (listing) {
            if ("Sold".equals(listing.getStatus())) {
                throw new ConflictException("This item has already been sold.");
            }
            Thread.sleep(100); // pretend we are talking to a payment gateway
            if (buyerId.isEmpty()) { // same fallback the frontend uses
                List<BuyerRequest> requests = listing.getBuyerRequests();
                buyerId = requests.isEmpty() ? "unknown" : requests.get(0).getBuyerId();
            }
            listing.setStatus("Sold");
            purchase = new Purchase(nextId("p-", purchaseCounter, purchases), listing.getId(),
                    listing.getTitle(), listing.getPrice(), listing.getOwnerId(), buyerId,
                    BaseEntity.nowIso(), listing.getImageUrl());
            purchases.addFirst(purchase);
        }
        save(); // after releasing the listing lock
        return purchase;
    }

    public List<Purchase> getPurchases() {
        return purchases.findAll();
    }

    // ------------------------------------------------------------------ stats

    /** Called every few seconds by MarketplaceMonitorThread. */
    public Map<String, Object> computeStats() {
        int available = 0, pending = 0, sold = 0;
        Map<String, Integer> byCategory = new TreeMap<>(); // TREEMAP: keys sorted alphabetically
        List<Listing> all = listings.findAll();
        for (Listing l : all) {
            switch (l.getStatus()) {
                case "Available" -> available++;
                case "Pending" -> pending++;
                case "Sold" -> sold++;
                default -> { }
            }
            String cat = l.getCategory() == null || l.getCategory().isBlank() ? "Other" : l.getCategory();
            byCategory.merge(cat, 1, Integer::sum); // AUTOBOXING: int -> Integer
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
        stats.put("lastUpdated", BaseEntity.nowIso());
        return stats;
    }

    // ------------------------------------------------------------------ helpers

    private User findUser(String id) throws NotFoundException {
        User user = users.findById(id);
        if (user == null) {
            throw new NotFoundException("User not found: " + id);
        }
        return user;
    }

    private Listing findListing(String id) throws NotFoundException {
        Listing listing = listings.findById(id);
        if (listing == null) {
            throw new NotFoundException("Listing not found: " + id);
        }
        return listing;
    }

    private User checkBuyer(String buyerId) throws ApiException {
        User buyer = users.findById(buyerId.trim());
        if (buyer == null) {
            throw new ValidationException("buyerId does not match any user.");
        }
        if (buyer.isBlocked()) {
            throw new ForbiddenException("This account has been suspended by an administrator.");
        }
        return buyer;
    }

    /** Generates the next free id such as "l-3" (skips ids already taken). */
    private static String nextId(String prefix, AtomicInteger counter, Repository<?> repo) {
        String id;
        do {
            id = prefix + counter.incrementAndGet();
        } while (repo.findById(id) != null);
        return id;
    }

    private static String normalizeStatus(String status) throws ValidationException {
        for (String allowed : new String[] {"Available", "Pending", "Sold"}) {
            if (allowed.equalsIgnoreCase(status.trim())) {
                return allowed;
            }
        }
        throw new ValidationException("status must be Available, Pending or Sold.");
    }

    private static double checkPrice(double price) throws ValidationException {
        if (Double.isNaN(price) || price < 0) {
            throw new ValidationException("Price must be a number >= 0.");
        }
        return price;
    }

    /** images defaults to [imageUrl]; imageUrl defaults to the first image. */
    private static void applyImages(Listing listing, Map<String, Object> body) {
        List<String> images = new ArrayList<>();
        if (body.get("images") instanceof List<?> raw) {
            for (Object o : raw) {
                if (o != null && !o.toString().isBlank()) {
                    images.add(o.toString());
                }
            }
        }
        String imageUrl = listing.getImageUrl() == null ? "" : listing.getImageUrl();
        if (images.isEmpty() && !imageUrl.isBlank()) {
            images.add(imageUrl);
        }
        if (imageUrl.isBlank() && !images.isEmpty()) {
            listing.setImageUrl(images.get(0));
        }
        listing.setImages(images);
    }

    private List<BuyerRequest> parseRequests(Object raw, double defaultOffer) throws ApiException {
        List<BuyerRequest> list = new ArrayList<>();
        if (raw instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) m;
                    double offer = map.get("offerPrice") == null ? defaultOffer : number(map, "offerPrice");
                    list.add(parseRequest(map, offer));
                }
            }
        }
        return list;
    }

    private BuyerRequest parseRequest(Map<String, Object> map, double offer) {
        String id = text(map, "id").trim();
        if (id.isEmpty()) {
            id = "r-" + requestCounter.incrementAndGet();
        }
        return new BuyerRequest(id, text(map, "buyerId").trim(), text(map, "message"), offer,
                defaultText(map, "status", "Pending"), defaultText(map, "createdAt", BaseEntity.nowIso()));
    }

    /** Reads a field as text ("" when missing). */
    private static String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    private static String defaultText(Map<String, Object> map, String key, String fallback) {
        String value = text(map, key);
        return value.isBlank() ? fallback : value;
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    /**
     * WRAPPER CLASSES + PARSING: the JSON may contain a number (Double) or a string
     * like "2400" from a form field. Double.parseDouble may throw NumberFormatException,
     * which we convert into our own ValidationException (400).
     */
    private static double number(Map<String, Object> map, String key) throws ValidationException {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.doubleValue(); // UNBOXING
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(String.format("'%s' must be a number but was '%s'.", key, value));
        }
    }
}
