package com.campusmarketplace;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Business logic and concurrency
class MarketplaceService {

    private final List<User> users = new ArrayList<>();
    private final List<Listing> listings = new ArrayList<>();
    private final List<PurchaseRequest> requests = new ArrayList<>();
    private final List<Purchase> purchases = new ArrayList<>();
    private final Set<String> emails = new HashSet<>();

    private final AtomicInteger userSeq = new AtomicInteger();
    private final AtomicInteger listingSeq = new AtomicInteger();
    private final AtomicInteger requestSeq = new AtomicInteger();
    private final AtomicInteger purchaseSeq = new AtomicInteger();

    private final ExecutorService orderPool;
    private final FileStorage storage;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MarketplaceService(ExecutorService orderPool, FileStorage storage) {
        this.orderPool = orderPool;
        this.storage = storage;
        initData();
    }

    private void initData() {
        if (storage.hasData()) {
            users.addAll(storage.loadUsers());
            listings.addAll(storage.loadListings());
            requests.addAll(storage.loadRequests());
            purchases.addAll(storage.loadPurchases());
        } else {
            // Initial seed data
            users.add(new User("u1", "Aarav Mehta", "aarav@campus.edu", "password123", "CSE"));
            users.add(new User("u2", "Ananya Sharma", "ananya@campus.edu", "password123", "Design"));
            users.add(new User("u3", "Riya Kapoor", "riya@campus.edu", "password123", "Commerce"));

            listings.add(new Listing("l1", "u1", "Study Table", "Furniture", 2400.0, "Hostel A", "Available"));
            listings.add(new Listing("l2", "u2", "HP Laptop Bag", "Accessories", 850.0, "North Gate", "Available"));
            listings.add(new Listing("l3", "u3", "Engineering Textbooks", "Books", 1200.0, "Library Block", "Available"));
            listings.add(new Listing("l4", "u1", "Campus Bicycle", "Cycles", 7500.0, "Sports Complex", "Available"));
            save();
        }

        for (User u : users) {
            emails.add(u.getEmail().toLowerCase());
            userSeq.accumulateAndGet(parseNum(u.getId(), "u"), Math::max);
        }
        for (Listing l : listings) {
            listingSeq.accumulateAndGet(parseNum(l.getId(), "l"), Math::max);
        }
        for (PurchaseRequest r : requests) {
            requestSeq.accumulateAndGet(parseNum(r.getId(), "r"), Math::max);
        }
        for (Purchase p : purchases) {
            purchaseSeq.accumulateAndGet(parseNum(p.getId(), "p"), Math::max);
        }
    }

    private static int parseNum(String id, String prefix) {
        try {
            if (id != null && id.startsWith(prefix)) {
                return Integer.parseInt(id.substring(prefix.length()));
            }
        } catch (Exception ignored) { }
        return 0;
    }

    private synchronized void save() {
        storage.saveAll(new ArrayList<>(users), new ArrayList<>(listings), new ArrayList<>(requests), new ArrayList<>(purchases));
    }

    // Auth
    public synchronized User login(String email, String pass) throws Exception {
        String em = email.trim();
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(em) && u.checkPassword(pass)) return u;
        }
        throw new Exception("Invalid email or password.");
    }

    public synchronized User register(String name, String email, String pass, String dept) throws Exception {
        String cleanEmail = FileStorage.clean(email).toLowerCase();
        if (cleanEmail.isEmpty() || pass == null || pass.isEmpty()) {
            throw new Exception("Email and password cannot be empty.");
        }
        if (!emails.add(cleanEmail)) {
            throw new Exception("Email already registered.");
        }
        User u = new User("u" + userSeq.incrementAndGet(), FileStorage.clean(name), cleanEmail, FileStorage.clean(pass), FileStorage.clean(dept));
        users.add(u);
        save();
        return u;
    }

    // Listings
    public synchronized List<Listing> getListings(String cat, String keyword, String sort) {
        List<Listing> list = new ArrayList<>();
        String c = cat == null ? "" : cat.trim();
        String k = keyword == null ? "" : keyword.trim().toLowerCase();

        for (Listing l : listings) {
            boolean catOk = c.isEmpty() || c.equalsIgnoreCase("all") || c.equalsIgnoreCase(l.getCategory());
            boolean keyOk = k.isEmpty() || l.getTitle().toLowerCase().contains(k);
            if (catOk && keyOk) list.add(l);
        }

        if ("price_asc".equalsIgnoreCase(sort)) list.sort(Comparator.comparingDouble(Listing::getPrice));
        else if ("price_desc".equalsIgnoreCase(sort)) list.sort(Comparator.comparingDouble(Listing::getPrice).reversed());
        else Collections.sort(list); // natural newest by id
        return list;
    }

    public synchronized Listing addListing(String ownerId, String title, String cat, double price, String location) throws Exception {
        String t = FileStorage.clean(title);
        if (t.isEmpty()) throw new Exception("Title cannot be empty.");
        if (price < 0) throw new Exception("Price must be >= 0.");

        Listing l = new Listing("l" + listingSeq.incrementAndGet(), ownerId, t, FileStorage.clean(cat), price, FileStorage.clean(location), "Available");
        listings.add(0, l);
        save();
        return l;
    }

    public synchronized void deleteListing(String id, String userId) throws Exception {
        Listing target = null;
        for (Listing l : listings) {
            if (l.getId().equalsIgnoreCase(id)) {
                target = l;
                break;
            }
        }
        if (target == null) throw new Exception("Listing not found.");
        if (!target.getOwnerId().equals(userId)) throw new Exception("You can only delete your own listings.");
        if ("Sold".equalsIgnoreCase(target.getStatus())) {
            throw new Exception("Cannot delete an item that has already been SOLD.");
        }

        String targetId = target.getId();
        listings.remove(target);
        requests.removeIf(r -> r.getListingId().equalsIgnoreCase(targetId));
        save();
    }

    // Purchase Requests (Buyer sends request)
    public synchronized PurchaseRequest sendPurchaseRequest(String listingId, String buyerId, String note) throws Exception {
        Listing target = findListing(listingId);
        if (target.getOwnerId().equals(buyerId)) {
            throw new Exception("You cannot request to buy your own listing!");
        }
        if ("Sold".equalsIgnoreCase(target.getStatus())) {
            throw new Exception("This item has already been SOLD.");
        }
        for (PurchaseRequest r : requests) {
            if (r.getListingId().equalsIgnoreCase(target.getId()) && r.getBuyerId().equals(buyerId)) {
                throw new Exception("You have already sent a purchase request for this item!");
            }
        }
        String cleanNote = FileStorage.clean(note);
        if (cleanNote.isEmpty()) cleanNote = "Interested in buying";
        PurchaseRequest req = new PurchaseRequest("r" + requestSeq.incrementAndGet(), target.getId(), buyerId, cleanNote, LocalDateTime.now().format(TIME_FMT));
        requests.add(0, req);
        save();
        return req;
    }

    // View incoming requests for a seller
    public synchronized List<PurchaseRequest> getIncomingRequests(String sellerId) {
        Set<String> myListingIds = new HashSet<>();
        for (Listing l : listings) {
            if (l.getOwnerId().equals(sellerId) && "Available".equalsIgnoreCase(l.getStatus())) {
                myListingIds.add(l.getId());
            }
        }
        List<PurchaseRequest> list = new ArrayList<>();
        for (PurchaseRequest r : requests) {
            if (myListingIds.contains(r.getListingId())) {
                list.add(r);
            }
        }
        return list;
    }

    // Seller accepts a request -> triggers multithreaded sale transaction
    public Purchase acceptRequest(String requestId, String sellerId) throws Exception {
        PurchaseRequest req = findRequest(requestId);
        Listing target = findListing(req.getListingId());
        if (!target.getOwnerId().equals(sellerId)) {
            throw new Exception("You can only accept requests for your own listings!");
        }
        Future<Purchase> future = orderPool.submit(() -> completeSale(target, req));
        return future.get(5, TimeUnit.SECONDS);
    }

    // Synchronized lock on the item so multiple concurrent threads cannot sell it twice
    private Purchase completeSale(Listing target, PurchaseRequest acceptedReq) throws Exception {
        Purchase p;
        synchronized (target) {
            if ("Sold".equalsIgnoreCase(target.getStatus())) {
                throw new Exception("This item has already been SOLD.");
            }
            Thread.sleep(100); // simulate quick transaction
            target.setStatus("Sold");
            p = new Purchase("p" + purchaseSeq.incrementAndGet(), target.getId(), target.getTitle(), target.getPrice(),
                    target.getOwnerId(), acceptedReq.getBuyerId(), LocalDateTime.now().format(TIME_FMT));
            synchronized (this) {
                // Remove all pending requests for this listing now that it's sold
                requests.removeIf(r -> r.getListingId().equalsIgnoreCase(target.getId()));
                purchases.add(0, p);
                save();
            }
        }
        return p;
    }

    public synchronized List<Purchase> getPurchasesForUser(String userId) {
        List<Purchase> res = new ArrayList<>();
        for (Purchase p : purchases) {
            if (userId.equals(p.getBuyerId()) || userId.equals(p.getSellerId())) res.add(p);
        }
        return res;
    }

    public synchronized Listing findListing(String id) throws Exception {
        for (Listing l : listings) {
            if (l.getId().equalsIgnoreCase(id.trim())) return l;
        }
        throw new Exception("Listing not found with ID: " + id);
    }

    private synchronized PurchaseRequest findRequest(String id) throws Exception {
        for (PurchaseRequest r : requests) {
            if (r.getId().equalsIgnoreCase(id.trim())) return r;
        }
        throw new Exception("Purchase request not found with ID: " + id);
    }

    public synchronized String getUserName(String userId) {
        for (User u : users) {
            if (u.getId().equals(userId)) return u.getName();
        }
        return userId;
    }
}
