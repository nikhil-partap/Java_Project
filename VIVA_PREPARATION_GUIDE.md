# Campus Marketplace — Complete Java Viva Preparation Guide

**Course:** Programming in Java (`25CSE0202`)  
**Project:** Campus Marketplace (Console Edition)  
**Language & Runtime:** Plain Java 21 (JDK Standard Library, No External Frameworks)  
**Architecture:** 4 Core Source Files + File Persistence + Multithreaded Concurrency  

---

## 1. 30-Second Elevator Pitch (Memorize This for Viva Opening)

> *"Campus Marketplace is a console-based peer-to-peer marketplace designed for college campuses. It allows students to create accounts, browse and search items across categories, list their own goods for sale, and send purchase requests with meetup messages. Sellers review incoming requests on their listings and decide which buyer to accept. Under the hood, it is built with **Object-Oriented Programming (encapsulation and interfaces)**, the **Collections Framework (ArrayList, HashSet)**, **File I/O (BufferedReader/BufferedWriter with try-with-resources)**, and **Multithreading** with an `ExecutorService` thread pool and `synchronized` locking to guarantee that an item can never be double-sold."*

---

## 2. Project File Structure

The entire application consists of only **4 clean, self-contained Java source files** inside `backend/src/com/campusmarketplace/`:

```
Java_Project/
├── VIVA_PREPARATION_GUIDE.md         This complete preparation guide
└── backend/
    ├── run.bat / run.sh              One-click compile and execution script
    ├── data/                         Auto-generated text database files
    │   ├── users.txt                 Registered user accounts
    │   ├── listings.txt              Active and sold items
    │   ├── requests.txt              Pending buyer purchase requests
    │   └── purchases.txt             Completed sale receipts
    └── src/com/campusmarketplace/
        ├── Models.java               Data entities (User, Listing, PurchaseRequest, Purchase)
        ├── FileStorage.java          File I/O persistence engine
        ├── MarketplaceService.java   Business rules, collections & thread-safe transactions
        └── Main.java                 Console UI, menus & thread pool lifecycle
```

---

## 3. Deep Dive: What Each File Does & Key Java Concepts

---

### File 1: `Models.java`
**Role:** Defines the data structure of the application.

It contains 4 classes:
1. **`User`**:
   - Fields: `id`, `name`, `email`, `password`, `department`.
   - **Encapsulation:** Password is stored as `private`. There is no getter for password (`getPassword()` does not exist). Instead, verification is done through `checkPassword(pass)` to protect credentials.
2. **`Listing`**:
   - Fields: `id`, `ownerId`, `title`, `category`, `price`, `location`, `volatile String status`.
   - **`Comparable<Listing>` Interface:** Implements `compareTo(Listing o)` to sort listings naturally by ID (newest first).
   - **`volatile` Keyword:** `status` is marked `volatile` so that when a worker thread marks an item as `"Sold"`, all other threads immediately read the updated value from main memory.
3. **`PurchaseRequest`**:
   - Fields: `id`, `listingId`, `buyerId`, `note`, `createdAt`.
   - Represents an inquiry from a buyer to a seller with meetup notes.
4. **`Purchase`**:
   - Fields: `id`, `listingId`, `title`, `price`, `sellerId`, `buyerId`, `purchasedAt`.
   - An immutable transaction record generated after a seller accepts a request.

> **Serialization Format:** Each model class has `toLine()` (converts the object into a pipe-delimited string `field1|field2|...`) and a static `fromLine(String[] f)` (reconstructs the object from split text).

---

### File 2: `FileStorage.java`
**Role:** Handles data persistence so student data is never lost when the program closes.

* **Key Methods:**
  - `saveAll(users, listings, requests, purchases)`: Writes four separate text files inside `backend/data/` (`users.txt`, `listings.txt`, `requests.txt`, `purchases.txt`).
  - `loadUsers()`, `loadListings()`, `loadRequests()`, `loadPurchases()`: Reads line-by-line and parses them back into Java collections.
  - `clean(String s)`: Sanitizes user input by replacing any `|` with `/` to prevent file formatting corruption.
* **Java Concepts Used:**
  - **`BufferedReader` and `BufferedWriter`:** Buffered streams wrap lower-level `FileReader` and `FileWriter` to minimize disk read/write system calls and improve performance.
  - **Try-with-resources:** Uses `try (BufferedReader br = new BufferedReader(...))` so the streams automatically close, preventing file leaks.

---

### File 3: `MarketplaceService.java`
**Role:** The "Brain" / Controller containing all business rules, in-memory collections, and thread concurrency.

* **Collections Used:**
  - `List<User>`, `List<Listing>`, `List<PurchaseRequest>`, `List<Purchase>`: Dynamic lists holding active marketplace data.
  - `Set<String> emails`: A `HashSet` providing $O(1)$ constant-time lookup to prevent duplicate email registrations.
* **Thread-Safe ID Generation (`AtomicInteger`):**
  - `userSeq`, `listingSeq`, `requestSeq`, `purchaseSeq`.
  - Regular integers (`count++`) are **not thread-safe** (they involve read, increment, write). `AtomicInteger.incrementAndGet()` guarantees unique IDs across parallel threads without needing heavy locks.
* **Purchase Request & Seller Approval Logic:**
  - `sendPurchaseRequest()`: Validates that users cannot buy their own items, prevents duplicate requests for the same item from the same user, and records the request.
  - `getIncomingRequests()`: Filters requests so a seller only sees requests for items *they* listed.
  - `acceptRequest()`: Allows the seller to accept a buyer's request.
* **Thread Concurrency & Race-Condition Protection:**
  - Submits the purchase to the `orderPool` thread pool using `Future<Purchase> future = orderPool.submit(() -> completeSale(target, req));`.
  - Inside `completeSale()`:
    ```java
    synchronized (target) {
        if ("Sold".equalsIgnoreCase(target.getStatus())) {
            throw new Exception("This item has already been SOLD.");
        }
        target.setStatus("Sold");
        // Create purchase receipt & remove other pending requests for this listing
    }
    ```
  - **Why synchronize on `target`?** If we synchronized the whole method, only one user could buy anything in the entire marketplace at once. By locking only the specific `Listing` object, multiple students can trade different items concurrently without blocking each other, but two threads can never sell the same item twice.

---

### File 4: `Main.java`
**Role:** Program entry point and User Interface (Console CLI).

* **Lifecycle:**
  - `main()` starts an `ExecutorService` thread pool with 4 worker threads (`Executors.newFixedThreadPool(4)`).
  - Initializes `FileStorage` and `MarketplaceService`.
  - Runs the interactive CLI loop with `Scanner`.
  - Uses a `finally` block to guarantee graceful shutdown:
    ```java
    finally {
        orderPool.shutdown();
        if (!orderPool.awaitTermination(3, TimeUnit.SECONDS)) orderPool.shutdownNow();
    }
    ```
* **Menus:**
  - **Guest Menu:** Login, Register, Browse Listings, Exit.
  - **User Menu:** Browse/Search Listings, Send Purchase Request (Buy), Sell Item, View Incoming Requests & Accept, Delete Listing, My Purchases & Sales, Logout, Exit.

---

## 4. How the Code Works Step-by-Step (Execution Flow)

```
[Main.main()]
   │
   ├── 1. Creates ExecutorService (4 Threads)
   ├── 2. Initializes FileStorage("data")
   ├── 3. Initializes MarketplaceService
   │         │
   │         ├── Checks if data/users.txt exists
   │         ├── YES: Loads users, listings, requests, purchases from disk
   │         └── NO:  Auto-seeds default demo data and saves to disk
   │
   └── 4. Starts Console Loop
             │
             ├── Guest Menu ──> (Login / Register / Browse)
             │
             └── User Menu (Once Logged In)
                   ├── 1. Browse / Search Listings
                   ├── 2. Send Purchase Request ──> Buyer sends request + note
                   ├── 3. Sell Item ─────────────> Adds new item to listings
                   ├── 4. Incoming Requests ─────> Seller reviews & accepts
                   │                                     │
                   │                                     └──> Runs thread-safe sale via orderPool
                   │                                          with synchronized(target) lock
                   ├── 5. Delete Listing ────────> Owner deletes item (if not Sold)
                   ├── 6. Purchases & Sales ─────> View transaction receipts
                   └── 7. Logout / Exit
```

---

## 5. Top 10 Viva Questions & Exact Answers

### Q1: What Java topics/features are implemented in this project?
**Answer:**
1. **Object-Oriented Programming (OOP):** Encapsulation with private fields and controlled getters; abstraction of business rules; polymorphism via `Comparable<Listing>`.
2. **Collections Framework:** `ArrayList` for dynamic lists, `HashSet` for unique email lookups, and `Comparator` for sorting by price.
3. **File I/O:** `BufferedReader`, `BufferedWriter`, `FileReader`, `FileWriter`, and `try-with-resources`.
4. **Multithreading & Concurrency:** `ExecutorService`, `ThreadPool`, `Future`, `AtomicInteger`, `volatile`, and `synchronized` blocks.
5. **Exception Handling:** Custom validation throwing and catching `Exception`.
6. **Date & Time API:** `LocalDateTime` and `DateTimeFormatter`.

---

### Q2: Why did you use `synchronized (target)` instead of `synchronized` on the whole method?
**Answer:**
> *"If we declare `public synchronized void completeSale(...)`, the entire method is locked on `this` (the service). That means only one purchase in the entire marketplace could happen at a time. By synchronizing only on the specific `Listing` object (`synchronized (target)`), multiple buyers can purchase different items at the same time, but if two threads try to buy the exact same item, one must wait and the second will see the item is already sold."*

---

### Q3: What is the purpose of `volatile` on the listing's `status`?
**Answer:**
> *"In Java, each thread can cache variable values in its CPU cache. Marking `status` as `volatile` guarantees memory visibility: any write to `status` by a background worker thread is immediately flushed to main memory so other threads always read the freshest value."*

---

### Q4: Why use `AtomicInteger` instead of a simple `int` counter?
**Answer:**
> *"The operation `count++` looks like one step, but in bytecode it consists of three steps: read, update, and write back. If two threads increment a regular integer at the same time, they can read the same value and produce duplicate IDs (a race condition). `AtomicInteger.incrementAndGet()` uses low-level hardware-level Compare-And-Swap (CAS) instructions to guarantee thread-safe increment without locking."*

---

### Q5: Why is `HashSet` used for emails?
**Answer:**
> *"An `ArrayList` requires $O(n)$ linear time to check if an email already exists because it has to inspect every element. A `HashSet` uses hashing and provides $O(1)$ constant time lookup, making duplicate email checks instantaneous even as the user base grows."*

---

### Q6: How does Try-with-Resources work in your file storage?
**Answer:**
> *"Any class implementing `AutoCloseable` (like `BufferedReader` or `BufferedWriter`) inside `try ( ... )` is guaranteed to be closed when exiting the block, even if an IOException or runtime error occurs. This completely prevents file leaks and dangling handles."*

---

### Q7: Why use `Comparable<Listing>` and `Comparator`?
**Answer:**
> *"`Comparable` defines the natural ordering of a class through `compareTo()`. We use it in `Listing` to sort items newest first by default. `Comparator` is used when we want alternative sorting options, such as sorting listings by Price Ascending or Price Descending."*

---

### Q8: How does the Purchase Request flow work compared to an instant buy?
**Answer:**
> *"In a campus marketplace, physical handovers require mutual agreement on location and time. Instead of instantly marking an item sold, a buyer sends a `PurchaseRequest` with an optional meetup note. The seller goes to 'View Incoming Requests', reviews the requests, and accepts one. Accepting the request triggers the thread-safe sale transaction, marks the item as 'Sold', and cleans up other pending requests for that item."*

---

### Q9: What happens if two threads try to buy the same item simultaneously?
**Answer:**
> *"Both calls submit a task to the `orderPool`. Both threads attempt to enter `synchronized (target)`. Java grants the lock to the first thread, which checks `status == Available`, changes it to `Sold`, records the purchase, and releases the lock. When the second thread enters the synchronized block, it checks `status` and sees it is already `Sold`, immediately throwing an exception: 'This item has already been SOLD'."*

---

### Q10: How do you reset the project to default data if files get messed up?
**Answer:**
> *"You simply delete the `backend/data/` folder. When `MarketplaceService` starts, `storage.hasData()` returns false, triggering `initData()` which auto-seeds 6 demo users, 10 items, 2 sample requests, and saves them to fresh disk files."*

---

## 6. Live Viva Demo Cheat-Sheet

| Action | Input to Type | What Evaluator Sees |
| :--- | :--- | :--- |
| **1. Browse as Guest** | `3` | Displays all 10 active items with price and location. |
| **2. Login as Aarav** | `1` $\to$ `aarav@campus.edu` $\to$ `password123` | Welcomes Aarav Mehta. |
| **3. View Incoming Request** | `4` | Shows incoming request `r1` from **Ananya Sharma** for *Study Table* with note: *"Can I inspect the table this evening after 6pm?"*. |
| **4. Accept Request** | `r1` | Item is marked **Sold**, order receipt `p...` is created. |
| **5. Post a New Item** | `3` $\to$ Title, Category, Price, Location | Automatically generates new ID `l11` and saves to disk. |
| **6. Check Purchases/Sales** | `6` | Displays completed order receipts with buyer, seller, and timestamp. |
