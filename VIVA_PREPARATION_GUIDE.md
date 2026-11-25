# Campus Marketplace — Java Viva Preparation Guide

**Course:** Programming in Java (`25CSE0202`)  
**Project:** Campus Marketplace (Console Edition)  
**Language & Environment:** Plain Java 21 (JDK only, no external libraries)

---

## 1. Quick Project Elevator Pitch (30-second Viva Answer)

> *"Campus Marketplace is a console-based buy and sell application designed for college campuses. It allows students to create accounts, browse/search items, post used goods for sale, and buy items safely. Under the hood, it uses **Object-Oriented Programming**, **Collections Framework**, **File Handling with buffered streams**, and **Multithreading** with an `ExecutorService` thread pool and `synchronized` locking to ensure two students can never buy the same item at the same time."*

---

## 2. Project File Structure & What Each File Does

The entire application is organized cleanly into **4 core Java source files** inside `backend/src/com/campusmarketplace/`:

```
backend/
├── run.bat / run.sh              One-click compilation and execution script
├── data/                         Directory where text files are saved automatically
│   ├── users.txt
│   ├── listings.txt
│   └── purchases.txt
└── src/com/campusmarketplace/
    ├── Models.java               Data models (User, Listing, Purchase)
    ├── FileStorage.java          File I/O (BufferedReader / BufferedWriter persistence)
    ├── MarketplaceService.java   Core business logic, collections & thread-safe purchases
    └── Main.java                 Program entry point, thread pool setup & console menus
```

---

### Detailed Breakdown of Each File

### 1. `Models.java`
* **What it does:** Defines the three essential entity classes used across the system:
  1. `User`: Holds student credentials (`id`, `name`, `email`, `password`, `department`). Uses encapsulation so passwords can only be verified via `checkPassword()`.
  2. `Listing`: Represents an item for sale (`id`, `ownerId`, `title`, `category`, `price`, `location`, `status`). Implements `Comparable<Listing>` to sort listings by newest first. Uses a `volatile String status` to ensure visibility across multiple threads.
  3. `Purchase`: An immutable receipt created once a sale completes (`id`, `listingId`, `title`, `price`, `sellerId`, `buyerId`, `purchasedAt`).
* **Key Java features:** Encapsulation (private fields + getters), `Comparable<T>` interface implementation, serialization helpers (`toLine()` and `fromLine()`).

---

### 2. `FileStorage.java`
* **What it does:** Provides persistent file storage so data survives application restarts.
* **How it works:**
  - Reads and writes text files inside the `backend/data/` folder: `users.txt`, `listings.txt`, and `purchases.txt`.
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
  - **Listing Operations:** `addListing()`, `deleteListing()` (ensuring a student can only delete their own listing), and `getListings()` with category/keyword filters and sorting.
  - **Thread-Safe Purchases:** `buyItem()` submits an asynchronous purchase task to the `orderPool`. `completePurchase()` applies a `synchronized (target)` block on the specific listing to prevent race conditions.
  - **Auto-save:** Automatically triggers `FileStorage.saveAll()` whenever an item is added, bought, or deleted.
* **Key Java features:**
  - `ArrayList` and `HashSet` (`O(1)` email uniqueness check).
  - `AtomicInteger` (`userSeq`, `listingSeq`, `purchaseSeq`) for thread-safe sequential ID generation without locking.
  - `ExecutorService` and `Future<Purchase>` for multithreaded order execution.
  - `synchronized` methods and blocks.

---

### 4. `Main.java`
* **What it does:** The main entry point and user interface.
* **Responsibilities:**
  - `main()` creates an `ExecutorService` thread pool (4 worker threads), initializes `FileStorage` and `MarketplaceService`, and sets up clean console menus via `Scanner`.
  - **Guest Menu:** Login, Register, Browse Listings, Exit.
  - **User Menu:** Browse/Search Listings, Buy Item, Sell Item, Delete Listing, My Purchases, Logout, Exit.
  - Implements a `finally` block that shuts down the thread pool gracefully (`orderPool.shutdown()` and `awaitTermination()`).
* **Key Java features:** CLI menu loops, `Scanner` input parsing, exception handling (`try-catch`), formatted printing (`System.out.printf`).

---

## 3. How the Code Works (Execution Lifecycle)

1. **Startup**: When you run `run.bat` or `java com.campusmarketplace.Main`:
   - `Main.main()` creates an `orderPool` thread pool.
   - `MarketplaceService` initializes. It checks if `backend/data/users.txt` exists:
     - If yes, it loads saved data from disk.
     - If no, it seeds initial demo data (`Aarav`, `Ananya`, `Riya`, and sample items) and saves them to `data/`.
2. **Guest Flow**:
   - The user sees the guest menu and can browse items without an account or choose to log in / register.
3. **Login / User Flow**:
   - Once logged in, the user menu opens. Any user can both **buy** items and **sell** items directly.
4. **Buying an Item (The Multithreaded Race-Condition Protection)**:
   - When a user inputs an item ID to buy, `service.buyItem(id, userId)` wraps the purchase in a `Callable` and submits it to the background thread pool (`orderPool`).
   - The worker thread enters `completePurchase(target, buyerId)` which executes inside `synchronized (target)`:
     ```java
     synchronized (target) {
         if ("Sold".equalsIgnoreCase(target.getStatus())) {
             throw new Exception("This item has already been SOLD.");
         }
         target.setStatus("Sold");
         // Record purchase and save
     }
     ```
   - If two users attempt to purchase the same item at the same instant, Java's intrinsic monitor lock forces the second thread to wait. Once it enters, `target.getStatus()` is already `"Sold"`, immediately throwing an error and preventing double-selling.
5. **Persistence**:
   - Any state mutation automatically writes back to `backend/data/*.txt`.
6. **Graceful Exit**:
   - Exiting shuts down the thread pool cleanly with a timeout, closing open resources.

---

## 4. Top Viva Questions & Answers

### Q1: What Java concepts did you implement in this project?
**Answer:**
- **OOP:** Encapsulation with private fields in `User`/`Listing`, interface implementation with `Comparable`.
- **Collections:** `ArrayList` for dynamic lists, `HashSet` for unique email lookups, `Comparator` for custom price sorting.
- **File I/O:** `BufferedReader`/`BufferedWriter` with try-with-resources to save/load persistent text records.
- **Multithreading:** `ExecutorService` thread pool, `Future`, `AtomicInteger`, and `synchronized` blocks.

### Q2: Why did you use `synchronized` on the listing object instead of synchronizing the whole method?
**Answer:**
> *"If we synchronized the entire method, only one user could buy anything in the whole marketplace at a time, causing a bottleneck. By synchronizing only on the specific `Listing` object (`synchronized (target)`), multiple users can buy different items simultaneously, but two users can never buy the same item at the same time."*

### Q3: What is the purpose of `AtomicInteger` in your service?
**Answer:**
> *"We use `AtomicInteger` for `userSeq`, `listingSeq`, and `purchaseSeq`. Unlike regular integers (`counter++` is not atomic), `AtomicInteger.incrementAndGet()` guarantees thread-safe, lock-free ID generation even if multiple threads create records at once."*

### Q4: Why use Try-with-Resources in FileStorage?
**Answer:**
> *"Try-with-resources automatically closes readers and writers (`AutoCloseable`) when the block exits, even if an IOException or runtime error occurs, preventing file descriptor and memory leaks."*

### Q5: How is data saved, and how do you reset to demo data?
**Answer:**
> *"Data is saved in `backend/data/` as pipe-delimited text (`users.txt`, `listings.txt`, `purchases.txt`). To reset back to original demo data, you simply delete the `backend/data` folder and restart the program."*
