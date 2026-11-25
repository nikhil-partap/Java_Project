# Campus Marketplace — Java Viva Preparation Guide

**Course:** Programming in Java (`25CSE0202`)  
**Project:** Campus Marketplace (Console Edition)  
**Language & Environment:** Plain Java 21 (JDK only, no external libraries)

---

## 1. Quick Project Elevator Pitch (30-second Viva Answer)

> *"Campus Marketplace is a console-based peer-to-peer buy and sell application designed for college campuses. It allows students to create accounts, browse/search items, post used goods for sale, and send purchase requests with meetup messages. Sellers review incoming purchase requests on their listings and decide which buyer to accept. Under the hood, it uses **Object-Oriented Programming**, **Collections Framework**, **File Handling with buffered streams**, and **Multithreading** with an `ExecutorService` thread pool and `synchronized` locking to ensure thread-safe sales transactions."*

---

## 2. Project File Structure & What Each File Does

The entire application is organized cleanly into **4 core Java source files** inside `backend/src/com/campusmarketplace/`:

```
backend/
├── run.bat / run.sh              One-click compilation and execution script
├── data/                         Directory where text files are saved automatically
│   ├── users.txt
│   ├── listings.txt
│   ├── requests.txt
│   └── purchases.txt
└── src/com/campusmarketplace/
    ├── Models.java               Data models (User, Listing, PurchaseRequest, Purchase)
    ├── FileStorage.java          File I/O (BufferedReader / BufferedWriter persistence)
    ├── MarketplaceService.java   Core business logic, collections, requests & thread-safe sales
    └── Main.java                 Program entry point, thread pool setup & console menus
```

---

### Detailed Breakdown of Each File

### 1. `Models.java`
* **What it does:** Defines the entity classes used across the system:
  1. `User`: Holds student credentials (`id`, `name`, `email`, `password`, `department`). Uses encapsulation so passwords can only be verified via `checkPassword()`.
  2. `Listing`: Represents an item for sale (`id`, `ownerId`, `title`, `category`, `price`, `location`, `status`). Implements `Comparable<Listing>` to sort listings by newest first. Uses a `volatile String status` to ensure visibility across multiple threads.
  3. `PurchaseRequest`: Represents a buyer's request to purchase an item (`id`, `listingId`, `buyerId`, `note`, `createdAt`).
  4. `Purchase`: An immutable receipt created once a seller accepts a request (`id`, `listingId`, `title`, `price`, `sellerId`, `buyerId`, `purchasedAt`).
* **Key Java features:** Encapsulation (private fields + getters), `Comparable<T>` interface implementation, serialization helpers (`toLine()` and `fromLine()`).

---

### 2. `FileStorage.java`
* **What it does:** Provides persistent file storage so data survives application restarts.
* **How it works:**
  - Reads and writes text files inside the `backend/data/` folder: `users.txt`, `listings.txt`, `requests.txt`, and `purchases.txt`.
  - Serializes fields using a pipe separator (`|`).
  - Includes a `clean()` method to sanitize user input so commas and pipes don't corrupt the file format.
* **Key Java features:** 
  - `BufferedReader` and `BufferedWriter` for efficient I/O.
  - `FileReader` and `FileWriter`.
  - **Try-with-resources** to guarantee file streams are automatically closed without resource leaks.

---

### 3. `MarketplaceService.java`
* **What it does:** The engine of the project containing all business rules, in-memory collections, and thread concurrency.
* **Responsibilities:**
  - **Account Operations:** `login()` with case-insensitive email check and `register()` with duplicate email prevention.
  - **Listing Operations:** `addListing()`, `deleteListing()` (ensuring a student can only delete their own listing and cannot delete sold items), and `getListings()` with category/keyword filters and sorting.
  - **Purchase Request Workflow:**
    - `sendPurchaseRequest()`: Validates that a buyer cannot request their own item, prevents duplicate requests on the same item, and stores the buyer's meetup note.
    - `getIncomingRequests()`: Filters and displays pending purchase requests for items owned by the logged-in seller.
    - `acceptRequest()`: Allows the seller to pick which buyer to sell to. It executes `completeSale()` on the background `orderPool` thread pool with a `synchronized (target)` lock on the item, sets status to `Sold`, records the `Purchase` receipt, and automatically purges pending requests for that item.
  - **Auto-save:** Automatically triggers `FileStorage.saveAll()` whenever an item is added, requested, accepted, or deleted.
* **Key Java features:**
  - `ArrayList` and `HashSet` (`O(1)` email uniqueness check).
  - `AtomicInteger` (`userSeq`, `listingSeq`, `requestSeq`, `purchaseSeq`) for thread-safe sequential ID generation without locking.
  - `ExecutorService` and `Future<Purchase>` for multithreaded order execution.
  - `synchronized` methods and object-level synchronized blocks.

---

### 4. `Main.java`
* **What it does:** The main entry point and user interface.
* **Responsibilities:**
  - `main()` creates an `ExecutorService` thread pool (4 worker threads), initializes `FileStorage` and `MarketplaceService`, and sets up clean console menus via `Scanner`.
  - **Guest Menu:** Login, Register, Browse Listings, Exit.
  - **User Menu:**
    1. Browse / Search Listings
    2. Send Purchase Request (Buy)
    3. Sell Item
    4. View Incoming Requests & Accept
    5. Delete Listing
    6. My Purchases & Sales
    7. Logout
    0. Exit
  - Implements a `finally` block that shuts down the thread pool gracefully (`orderPool.shutdown()` and `awaitTermination()`).
* **Key Java features:** CLI menu loops, `Scanner` input parsing, exception handling (`try-catch`), formatted printing (`System.out.printf`).

---

## 3. How the Code Works (Execution Lifecycle)

1. **Startup**: When you run `run.bat` or `java com.campusmarketplace.Main`:
   - `Main.main()` creates an `orderPool` thread pool.
   - `MarketplaceService` initializes. It checks if `backend/data/users.txt` exists:
     - If yes, it loads saved data from disk (`users.txt`, `listings.txt`, `requests.txt`, `purchases.txt`).
     - If no, it seeds initial demo data (`Aarav`, `Ananya`, `Riya`, and sample items) and saves them to `data/`.
2. **Guest Flow**:
   - The user sees the guest menu and can browse items without an account or choose to log in / register.
3. **Login / User Flow**:
   - Once logged in, the user menu opens. Any user can both send buy requests and list items for sale.
4. **Buyer Request & Seller Approval (Multithreaded Protection)**:
   - When a buyer wants an item, they select Option 2, enter the Listing ID, and include an optional message (e.g. meetup place/time).
   - This records a `PurchaseRequest` in `requests.txt`.
   - The seller logs in, goes to Option 4 ("View Incoming Requests & Accept"), and reviews all incoming requests for their items.
   - When the seller accepts a request ID, `acceptRequest()` submits a task to `orderPool`:
     ```java
     synchronized (target) {
         if ("Sold".equalsIgnoreCase(target.getStatus())) {
             throw new Exception("This item has already been SOLD.");
         }
         target.setStatus("Sold");
         // Record purchase and remove other pending requests for this item
     }
     ```
   - If multiple actions or threads interact at the same moment, `synchronized (target)` guarantees only one sale can execute on the listing, preventing race conditions or double-selling.
5. **Persistence**:
   - Any state mutation automatically writes back to `backend/data/*.txt`.
6. **Graceful Exit**:
   - Exiting shuts down the thread pool cleanly with a timeout, closing open resources.

---

## 4. Top Viva Questions & Answers

### Q1: What Java concepts did you implement in this project?
**Answer:**
- **OOP:** Encapsulation with private fields in `User`/`Listing`/`PurchaseRequest`, interface implementation with `Comparable`.
- **Collections:** `ArrayList` for dynamic lists, `HashSet` for unique email lookups, `Comparator` for custom price sorting.
- **File I/O:** `BufferedReader`/`BufferedWriter` with try-with-resources to save/load persistent text records.
- **Multithreading:** `ExecutorService` thread pool, `Future`, `AtomicInteger`, and `synchronized` blocks.

### Q2: Why does the seller have to approve requests instead of instant purchase?
**Answer:**
> *"In a campus peer-to-peer setting, physical handover and meeting locations need mutual consent. Rather than auto-marking items as sold immediately, buyers send purchase requests with meetup notes. The seller can review multiple interested buyers and pick whom to accept, matching realistic marketplace dynamics."*

### Q3: Why did you use `synchronized` on the listing object instead of synchronizing the whole method?
**Answer:**
> *"If we synchronized the entire method, only one user could perform an action in the whole marketplace at a time, creating a system bottleneck. By synchronizing only on the specific `Listing` object (`synchronized (target)`), multiple sellers and buyers can complete transactions on different items simultaneously without blocking each other."*

### Q4: What is the purpose of `AtomicInteger` in your service?
**Answer:**
> *"We use `AtomicInteger` for `userSeq`, `listingSeq`, `requestSeq`, and `purchaseSeq`. Unlike regular integers (`counter++` is not atomic and can suffer race conditions), `AtomicInteger.incrementAndGet()` guarantees thread-safe, lock-free ID generation."*

### Q5: Why use Try-with-Resources in FileStorage?
**Answer:**
> *"Try-with-resources automatically closes readers and writers (`AutoCloseable`) when the block exits, even if an IOException or runtime error occurs, preventing file descriptor and memory leaks."*

### Q6: How is data saved, and how do you reset to demo data?
**Answer:**
> *"Data is saved in `backend/data/` as pipe-delimited text (`users.txt`, `listings.txt`, `requests.txt`, `purchases.txt`). To reset back to original demo data, you simply delete the `backend/data` folder and restart the program."*
