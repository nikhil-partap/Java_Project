# Campus Marketplace — Exhaustive Code Walkthrough & Viva Guide

This document is an exhaustive, line-by-line and section-by-section breakdown of the entire codebase for **Campus Marketplace**. It explains every class, every method, how the components interact, how data is fetched and stored, and the exact Java mechanics used.

---

## Table of Contents
1. [System Architecture & Component Interactions](#1-system-architecture--component-interactions)
2. [Data Flow (Fetch, Process, Persist)](#2-data-flow-fetch-process-persist)
3. [Line-by-Line Explanation: Models.java](#3-line-by-line-explanation-modelsjava)
4. [Line-by-Line Explanation: FileStorage.java](#4-line-by-line-explanation-filestoragejava)
5. [Line-by-Line Explanation: MarketplaceService.java](#5-line-by-line-explanation-marketplaceservicejava)
6. [Line-by-Line Explanation: Main.java](#6-line-by-line-explanation-mainjava)
7. [Examiner Gotcha Questions & Deep Technical Defense](#7-examiner-gotcha-questions--deep-technical-defense)

---

## 1. System Architecture & Component Interactions

The project follows a clean **Layered Architecture (MVC / Service-Oriented pattern)** separated into 4 primary source files:

```
+-------------------------------------------------------------+
|                          MAIN.JAVA                          |
|  - Console UI (Menus, Scanner input, formatted printing)     |
|  - Thread Pool Lifecycle (Executors.newFixedThreadPool)     |
+------------------------------+------------------------------+
                               |
                               v calls business methods
+-------------------------------------------------------------+
|                   MARKETPLACESERVICE.JAVA                   |
|  - Business Rules & Validation (Login, Register, Dedupe)     |
|  - Concurrency Control (synchronized, AtomicInteger)        |
|  - In-Memory Collections (List<User>, List<Listing>, etc.)   |
+--------------+-------------------------------+--------------+
               |                               |
    uses Model |                               | saves/loads via
    objects    v                               v
+-------------------------------+ +---------------------------+
|          MODELS.JAVA          | |      FILESTORAGE.JAVA     |
|  - User                       | |  - BufferedReader / Writer|
|  - Listing (Comparable)       | |  - Pipe-delimited parsing |
|  - PurchaseRequest            | |  - Writes to data/*.txt   |
|  - Purchase                   | |                           |
+-------------------------------+ +---------------------------+
```

### How the Classes Work Together:
1. **`Main` $\to$ `MarketplaceService`**:
   - `Main` does not know how files work or how threads synchronize. It only calls high-level methods on `MarketplaceService` (e.g., `service.login()`, `service.sendPurchaseRequest()`, `service.acceptRequest()`).
2. **`MarketplaceService` $\to$ `Models`**:
   - When operations run, the service creates, queries, updates, and deletes instances of `User`, `Listing`, `PurchaseRequest`, and `Purchase`.
3. **`MarketplaceService` $\to$ `FileStorage`**:
   - At startup, the service asks `FileStorage` to load data from disk.
   - Any time state changes (e.g. an item is added, requested, sold, or deleted), the service invokes `storage.saveAll(...)`.
4. **`FileStorage` $\to$ `Models`**:
   - `FileStorage` reads text lines and passes the tokenized strings into each model's static `fromLine()` factory method.
   - When saving, `FileStorage` calls `toLine()` on each model object to get a pipe-separated string.

---

## 2. Data Flow (Fetch, Process, Persist)

### Phase 1: Startup & Data Fetching
1. `Main.main()` runs. It instantiates `FileStorage("data")`.
2. It passes the `storage` object into the constructor of `MarketplaceService`.
3. Inside `MarketplaceService.initData()`:
   - `storage.hasData()` checks if `data/users.txt` exists.
   - **If YES:** It calls `storage.loadUsers()`, `loadListings()`, `loadRequests()`, `loadPurchases()`.
     - `FileStorage.readLines(fileName)` opens a `BufferedReader` with try-with-resources.
     - It reads line-by-line, splitting on the pipe character (`line.split("\\|", -1)`).
     - Each string array is passed to `Model.fromLine(f)` to instantiate Java objects.
     - The objects are added into the service's `ArrayList`s.
     - Sequence numbers (`userSeq`, `listingSeq`, etc.) are computed by finding the highest existing ID number using `parseNum()` and `AtomicInteger.accumulateAndGet(..., Math::max)`.
   - **If NO:** It generates default initial seed data (6 students, 10 items, 2 requests, 1 purchase) and immediately calls `storage.saveAll(...)` to create the disk files.

### Phase 2: Runtime User Actions
- **Viewing Items:** `service.getListings()` filters and sorts the in-memory `List<Listing>` using Java Streams / loops and `Comparator`. Fast, zero disk I/O.
- **Buyer Request:** `service.sendPurchaseRequest()` checks for duplicate requests and ownership, creates a `PurchaseRequest`, prepends it to `requests`, and triggers `save()`.
- **Seller Approval (Multithreaded):**
  1. The seller selects a `requestId`.
  2. `service.acceptRequest()` submits a task to `orderPool.submit(...)`.
  3. A background worker thread acquires an intrinsic lock on that specific item: `synchronized (target)`.
  4. It sets `target.setStatus("Sold")`.
  5. It generates a new `Purchase` record, removes pending requests for that item, and calls `save()`.
  6. The main thread awaits the result via `future.get(5, TimeUnit.SECONDS)`.

---

## 3. Line-by-Line Explanation: `Models.java`

`Models.java` contains all entity classes in one file under package-private visibility.

### 3.1 `class User`
```java
class User {
    private final String id;
    private final String name;
    private final String email;
    private final String password;
    private final String department;
```
- **Lines 4–9:** Declares private, immutable fields. `final` guarantees that once a `User` is constructed, their ID, name, email, and password cannot be mutated accidentally.
```java
    public User(String id, String name, String email, String password, String department) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.department = department;
    }
```
- **Lines 11–17:** Standard parameterized constructor. Assigns incoming parameters to fields using `this`.
```java
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public boolean checkPassword(String pass) { return password.equals(pass); }
```
- **Lines 19–23:** Getters for non-sensitive attributes. Notice there is **NO** `getPassword()`. Instead, `checkPassword(String pass)` performs verification. This is a classic OOP **Encapsulation** pattern to prevent credentials from being exposed in memory logs.
```java
    public String toLine() {
        return String.join("|", id, name, email, password, department);
    }
    public static User fromLine(String[] f) {
        return new User(f[0], f[1], f[2], f[3], f[4]);
    }
```
- **Lines 25–31:** Serialization methods. `toLine()` joins the fields with a pipe (`|`) for file writing. `fromLine()` is a static factory method that accepts a split string array and returns a reconstructed `User` object.

---

### 3.2 `class Listing implements Comparable<Listing>`
```java
class Listing implements Comparable<Listing> {
    private final String id;
    private final String ownerId;
    private final String title;
    private final String category;
    private final double price;
    private final String location;
    private volatile String status; // "Available" or "Sold"
```
- **Lines 35–42:**
  - Implements `Comparable<Listing>`: Allows default sorting without external utility classes.
  - `private volatile String status`: **Crucial Viva Concept!** `volatile` prevents CPU thread caching. When one worker thread marks an item as `"Sold"`, all other threads immediately see the new status in main memory.
```java
    @Override
    public int compareTo(Listing o) {
        return o.id.compareTo(this.id);
    }
```
- **Lines 63–66:** Implements the `Comparable` contract. Compares `o.id` to `this.id` (reversed), which sorts listings newest first by default (e.g. `l10` before `l1`).
```java
    public String toLine() {
        return String.join("|", id, ownerId, title, category, String.valueOf(price), location, status);
    }
    public static Listing fromLine(String[] f) {
        return new Listing(f[0], f[1], f[2], f[3], Double.parseDouble(f[4]), f[5], f[6]);
    }
```
- **Lines 68–74:** `Double.parseDouble(f[4])` converts the string representation of price back into a primitive `double`.

---

### 3.3 `class PurchaseRequest`
```java
class PurchaseRequest {
    private final String id;
    private final String listingId;
    private final String buyerId;
    private final String note;
    private final String createdAt;
```
- **Lines 78–84:** Represents an expression of interest by a student. Stores `id` (e.g., `r1`), `listingId`, `buyerId`, meetup/inquiry `note`, and timestamp `createdAt`.
- **Lines 99–105:** Provides `toLine()` and `fromLine()` storing records as `r1|l1|u2|Can we meet at 5pm?|2026-11-25 10:15`.

---

### 3.4 `class Purchase`
```java
class Purchase {
    private final String id;
    private final String listingId;
    private final String title;
    private final double price;
    private final String sellerId;
    private final String buyerId;
    private final String purchasedAt;
```
- **Lines 109–117:** Represents an immutable transaction receipt. It snapshot-stores the item's `title` and `price` at the moment of the sale, so even if the original listing is modified or deleted later, the historical financial receipt remains untouched and accurate.

---

## 4. Line-by-Line Explanation: `FileStorage.java`

Handles reading and writing to disk files.

```java
package com.campusmarketplace;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

class FileStorage {
    private final File folder;

    public FileStorage(String folderName) {
        this.folder = new File(folderName);
    }
```
- **Lines 8–13:** `folder` points to the `data/` directory.

```java
    public boolean hasData() {
        return new File(folder, "users.txt").exists();
    }
```
- **Lines 15–17:** Checks if `users.txt` exists. Used during startup to decide whether to load existing records or bootstrap default demo data.

```java
    public synchronized void saveAll(List<User> users, List<Listing> listings, List<PurchaseRequest> requests, List<Purchase> purchases) {
        if (!folder.exists()) folder.mkdirs();
        writeFile("users.txt", users.stream().map(User::toLine).toList());
        writeFile("listings.txt", listings.stream().map(Listing::toLine).toList());
        writeFile("requests.txt", requests.stream().map(PurchaseRequest::toLine).toList());
        writeFile("purchases.txt", purchases.stream().map(Purchase::toLine).toList());
    }
```
- **Lines 19–24:**
  - `synchronized`: Prevents two threads from writing to the disk files simultaneously.
  - `folder.mkdirs()`: Creates the `data/` folder if it doesn't already exist.
  - Java Streams: `users.stream().map(User::toLine).toList()` maps each model object to its formatted text string.

```java
    private void writeFile(String fileName, List<String> lines) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(folder, fileName)))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("File write error: " + e.getMessage());
        }
    }
```
- **Lines 26–35:**
  - **Try-with-resources:** `try (BufferedWriter bw = ...)` guarantees that `bw.close()` is invoked automatically when exiting the block, even if an exception occurs.
  - `BufferedWriter` wraps `FileWriter` to buffer characters in memory and write them in chunks, drastically reducing slow operating system disk write calls.
  - `bw.newLine()` writes platform-independent newline characters (`\r\n` on Windows, `\n` on Linux).

```java
    private List<String[]> readLines(String fileName) {
        List<String[]> list = new ArrayList<>();
        File file = new File(folder, fileName);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank()) list.add(line.split("\\|", -1));
            }
        } catch (IOException e) {
            System.out.println("File read error: " + e.getMessage());
        }
        return list;
    }
```
- **Lines 55–69:**
  - `while ((line = br.readLine()) != null)`: Reads the file line by line until End-Of-File (`null`).
  - `line.split("\\|", -1)`:
    - In regex, `|` is a special operator (logical OR). We must escape it with `\\|` to split on the literal pipe character.
    - `-1` limit argument: Ensures that empty trailing fields (e.g. if note is blank) are not discarded.

```java
    public static String clean(String s) {
        return s == null ? "" : s.replace("|", "/").trim();
    }
```
- **Lines 71–73:** Sanitizes user inputs. If a student types a pipe `|` in their name or listing title, `clean()` replaces it with `/` so it does not break the file format.

---

## 5. Line-by-Line Explanation: `MarketplaceService.java`

Contains all business logic, sequence generation, collections, and synchronization.

```java
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
```
- **Lines 12–23:**
  - `emails = new HashSet<>()`: Stores lowercase emails for $O(1)$ duplicate checking.
  - `AtomicInteger`: Thread-safe integer sequences for IDs (`u1`, `l1`, `r1`, `p1`).
  - `ExecutorService orderPool`: Thread pool passed in from `Main`.

```java
    private static int parseNum(String id, String prefix) {
        try {
            if (id != null && id.startsWith(prefix)) {
                return Integer.parseInt(id.substring(prefix.length()));
            }
        } catch (Exception ignored) { }
        return 0;
    }
```
- **Lines 61–68:** Strips the prefix from an ID (e.g. extracts `14` from `"l14"`) so sequence numbers can resume accurately after a program restart.

```java
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
```
- **Lines 83–95:**
  - `!emails.add(cleanEmail)`: `Set.add()` returns `false` if the item is already present in the set. This gives an instantaneous duplicate registration check in $O(1)$ time without searching through the entire user list.
  - `userSeq.incrementAndGet()`: Safely increments the sequence and returns the new number.

```java
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
```
- **Lines 98–113:**
  - Demonstrates both **`Comparable`** and **`Comparator`**:
    - `Collections.sort(list)` uses the class's natural `Comparable<Listing>` ordering (newest ID first).
    - `Comparator.comparingDouble(...)` applies dynamic custom price ordering on demand.

```java
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
```
- **Lines 149–168:**
  - Enforces three key marketplace business rules:
    1. A seller cannot request their own item.
    2. Sold items cannot receive requests.
    3. Prevents spamming duplicate requests on the same item.

```java
    public Purchase acceptRequest(String requestId, String sellerId) throws Exception {
        PurchaseRequest req = findRequest(requestId);
        Listing target = findListing(req.getListingId());
        if (!target.getOwnerId().equals(sellerId)) {
            throw new Exception("You can only accept requests for your own listings!");
        }
        Future<Purchase> future = orderPool.submit(() -> completeSale(target, req));
        return future.get(5, TimeUnit.SECONDS);
    }
```
- **Lines 185–194:**
  - Verifies that only the actual owner of the listing can accept requests on it.
  - Submits `completeSale(target, req)` to `orderPool` via a `Callable` lambda expression.
  - `future.get(5, TimeUnit.SECONDS)` waits up to 5 seconds for the background worker thread to finish.

```java
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
```
- **Lines 197–215 (Core Viva Topic):**
  - `synchronized (target)`: Obtains the monitor lock of the specific `Listing` object. If two threads run this for the same item at the same millisecond, thread 2 is forced to wait until thread 1 completes.
  - When thread 2 enters, `target.getStatus()` is `"Sold"`, triggering an exception.
  - `requests.removeIf(...)` uses a lambda predicate to clean up all other pending requests for that item now that it has been sold to someone.

---

## 6. Line-by-Line Explanation: `Main.java`

Handles input, output, console navigation, and thread shutdown.

```java
public class Main {
    private final MarketplaceService service;
    private final Scanner sc;
    private User currentUser;
```
- **Lines 11–13:** Holds the running dependencies. `currentUser` is `null` when a user is logged out, and holds a `User` reference when logged in.

```java
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
```
- **Lines 20–36 (Thread Lifecycle Management):**
  - `Executors.newFixedThreadPool(4)`: Creates a pool of 4 reusable background worker threads.
  - `finally` block: Runs regardless of how the program exits.
  - `orderPool.shutdown()` stops accepting new tasks.
  - `orderPool.awaitTermination(3, TimeUnit.SECONDS)` allows ongoing tasks up to 3 seconds to finish cleanly. If they stall, `shutdownNow()` interrupts them immediately.

```java
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
```
- **Lines 38–50:**
  - Main event loop.
  - Conditional menu dispatch: If `currentUser == null`, shows `guestMenu()`. Once authenticated, flips to `userMenu()`.
  - Global `try-catch`: Catches all validation exceptions thrown by `MarketplaceService` (e.g., wrong password, item already sold) and prints `[Error] message` without crashing the application.

```java
    private void viewIncomingRequests() throws Exception {
        List<PurchaseRequest> incoming = service.getIncomingRequests(currentUser.getId());
        ...
        String reqId = prompt("\nEnter Request ID to accept (or press Enter to cancel): ");
        if (reqId != null && !reqId.isEmpty()) {
            Purchase p = service.acceptRequest(reqId, currentUser.getId());
            System.out.printf("Request accepted! Item '%s' marked as SOLD to %s for INR %.2f! (Order ID: %s)%n",
                    p.getTitle(), service.getUserName(p.getBuyerId()), p.getPrice(), p.getId());
        }
    }
```
- **Lines 142–165:** Formats and prints incoming requests in an ASCII table. If the seller enters a request ID, `service.acceptRequest()` completes the sale.

---

## 7. Examiner Gotcha Questions & Deep Technical Defense

### Gotcha 1: "Why did you implement your own text file storage instead of using JDBC and MySQL?"
**Your Defense:**
> *"The project requirement was to build a clean, self-contained application using standard Java 21 without external dependencies. By using a Flat-File system with `BufferedReader`, `BufferedWriter`, and try-with-resources, we directly demonstrate core Java I/O syllabus concepts. Furthermore, the architecture cleanly isolates storage in `FileStorage.java`, meaning if we ever want to switch to MySQL, we only need to swap `FileStorage` for a JDBC DAO without altering the business logic in `MarketplaceService`."*

### Gotcha 2: "What is a race condition, and where could it happen in this application?"
**Your Defense:**
> *"A race condition occurs when multiple concurrent threads attempt to read and modify shared mutable state at the same time, producing unpredictable results. In our application, if two buyers send purchase requests and two sellers or threads try to accept and mark the same item as sold at the same millisecond, a naive implementation would allow the item to be sold twice. We prevented this with `synchronized (target)`, which guarantees that only one thread can execute the sale block on an item at any given moment."*

### Gotcha 3: "Why is `line.split(\"\\\\|\", -1)` used instead of `line.split(\"|\")`?"
**Your Defense:**
> *"In Java's `String.split()`, the argument is treated as a regular expression. In regex, `|` is the OR operator, so splitting on `\"|\"` would split between every single character! We must escape it as `\"\\\\|\"` to match the literal pipe character. The `-1` limit ensures that empty trailing tokens (such as an empty meetup note) are preserved in the returned array instead of being discarded."*

### Gotcha 4: "Why did you use `orderPool.submit()` instead of `orderPool.execute()`?"
**Your Defense:**
> *"`execute()` takes a `Runnable` and is 'fire-and-forget'—it cannot return a value or throw checked exceptions back to the caller. `submit()` accepts a `Callable<T>`, runs asynchronously on the thread pool, and returns a `Future<T>`. This allows `acceptRequest()` to block on `future.get(5, TimeUnit.SECONDS)`, retrieve the generated `Purchase` receipt, and catch any runtime exceptions thrown inside the worker thread."*

### Gotcha 5: "How does Java guarantee that password verification is secure here?"
**Your Defense:**
> *"In `Models.java`, the `password` field in `User` is `private` and `final`. We deliberately did not write a `getPassword()` method. The only way to test a password is through `checkPassword(String pass)`, which returns a boolean. This adheres to the OOP principle of Encapsulation: internal state is hidden, and outside classes are only provided the minimum interface required."*
