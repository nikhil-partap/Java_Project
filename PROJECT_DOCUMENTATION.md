# Campus Marketplace — Project Documentation

| | |
|---|---|
| **Course** | Programming in Java (25CSE0202) |
| **Institute** | Chitkara University Institute of Engineering and Technology |
| **Programme / Semester** | B.E. CSE (AI & ML), 3rd Semester, Batch 2025 |
| **Session** | July – Dec 2026 |
| **Team Members** | _(fill in names and roll numbers)_ |
| **Repository** | https://github.com/nikhil-partap/Java_Project |

---

## 1. Introduction

**Campus Marketplace** is a web application where college students can **buy and sell used items within their campus** — textbooks, cycles, lab kits, electronics, furniture and hostel essentials.

Students who are leaving hostels or moving to the next year often have useful items they no longer need, while juniors are looking for the same items at low prices. Today this happens through scattered WhatsApp groups and notice boards. Campus Marketplace gives one organised place to list items, browse them, send purchase requests and complete sales safely.

## 2. Problem Statement

- No single, organised platform for second-hand trading inside a campus.
- Buyers can't easily search or filter what is available.
- When two students want the same item at the same time, it is unclear who gets it (the **same item can be "sold twice"**).
- No record of past purchases and no way for an administrator to block misuse.

## 3. Objectives

1. Build a working full-stack application: a React frontend and a **Java backend**.
2. Apply the Java concepts covered in the course — **OOP, Strings, Exception Handling, Collections Framework and Multithreading**.
3. Handle **many users at the same time** safely using threads and synchronization, so an item can never be sold twice.
4. Keep the design simple, layered and easy to extend (e.g. a database can be added later using JDBC).

---

## 4. Features

### Buyer
- Register and log in with a campus e-mail
- Browse all listings; **search** by title/description; **filter** by category and maximum price; **sort** by newest or price
- View item details (photos, condition, seller, location)
- Add items to a cart and send **purchase requests** to sellers
- View **purchase history**

### Seller
- Switch between Buyer and Seller mode
- **Create listings** with photos, price, category, condition and location
- **Edit / delete** own listings
- See incoming buyer requests and **mark an item as sold**

### Admin
- View all users and listings
- **Block / unblock** users (blocked users can't log in, list or buy)
- Remove inappropriate listings
- Live **statistics** from the backend (total listings, available, sold, users, orders processed, revenue)

### System
- **Race-condition protection**: if several buyers try to buy the same item at the same moment, exactly one succeeds; the others are told it is already sold
- **Offline fallback**: if the backend is not running, the frontend keeps working using the browser's localStorage
- Clear error messages for invalid input (empty title, negative price, duplicate e-mail, etc.)

---

## 5. Technology Stack — What Is Used and Where

### 5.1 Backend (Java)

| Technology | Where it is used | Why |
|---|---|---|
| **Java 21 (JDK only)** | Entire `backend/` folder | Course language; no external libraries needed |
| **`com.sun.net.httpserver.HttpServer`** | `Main.java`, `http/Router.java` | Built-in JDK web server to expose the REST API on port 8080 |
| **REST + JSON** | `http/Router.java` (routes), `http/Json.java` | Standard way for the frontend and backend to talk |
| **Hand-written JSON parser/writer** | `http/Json.java` | Converts JSON text ↔ Java Maps/Lists without any library (uses recursion and `StringBuilder`) |
| **Collections Framework** | `repository/InMemoryRepository.java`, `service/MarketplaceService.java` | In-memory storage of users, listings, purchases |
| **`java.util.concurrent`** (ExecutorService, Callable, Future, AtomicInteger, ConcurrentHashMap) | `Main.java`, `service/MarketplaceService.java`, `thread/OrderProcessor.java` | Handle many requests and orders in parallel, safely |
| **Threads** (extends Thread, daemon, sleep, interrupt) | `thread/MarketplaceMonitorThread.java` | Background thread that refreshes live statistics every 3 seconds |
| **`java.time`** | `model/BaseEntity.java`, `model/Listing.java` | Timestamps (ISO-8601) and "newest first" sorting |
| **Batch / shell scripts** | `backend/run.bat`, `backend/run.sh` | One-command compile (`javac`) and run (`java`) |

### 5.2 Frontend (Web)

| Technology | Where it is used | Why |
|---|---|---|
| **React 19** | `frontend/src/` (components and views) | Component-based user interface |
| **Vite** | `frontend/vite.config.js` | Fast development server and build tool; **proxies `/api` calls to the Java backend** |
| **Redux Toolkit** | `frontend/src/store/` (`marketplaceSlice.js`, `cartSlice.js`) | Global state for listings, purchases and the cart |
| **React Context** | `frontend/src/context/AuthContext.jsx` | Logged-in user, login/register/logout, notifications |
| **React Router v7** | `frontend/src/App.jsx`, `components/routing/` | Page navigation and protected/admin-only routes |
| **Fetch API** | `frontend/src/lib/api.js` | Calls the Java backend's REST endpoints |
| **localStorage** | `frontend/src/lib/storage.js`, slices | Cache and offline fallback |
| **HTML5 Canvas / FileReader** | `frontend/src/lib/imageCompressor.js` | Compresses uploaded photos before sending |
| **CSS (custom properties, Flexbox, Grid)** | `frontend/src/index.css`, `App.css`, `BuyerMarketplace.css` | Responsive styling |

### 5.3 Tools

| Tool | Use |
|---|---|
| Git & GitHub | Version control and team collaboration |
| VS Code / IntelliJ IDEA | Code editing |
| npm | Installing frontend packages |
| curl / browser | Testing API endpoints |

---

## 6. System Architecture

```
┌──────────────────────────────────────────────┐
│     Browser — React Frontend (port 5173)     │
│  Views → Redux / AuthContext → lib/api.js    │
└──────────────────────┬───────────────────────┘
                       │  HTTP requests with JSON  (/api/...)
                       ▼
┌──────────────────────────────────────────────┐
│        Java Backend (port 8080)              │
│                                              │
│  Main ── HttpServer + thread pool (8)        │
│           │                                  │
│  Router (HTTP layer)                         │
│   • routing, CORS                            │
│   • one central exception handler            │
│           │                                  │
│  MarketplaceService (business logic)         │
│   • validation, rules                        │
│   • purchases run on an order thread pool (4)│
│   • synchronized → no double selling         │
│           │                                  │
│  Repository<T>  →  InMemoryRepository<T>     │
│   (ArrayList + ConcurrentHashMap)            │
│                                              │
│  MarketplaceMonitorThread (daemon)           │
│   → recomputes statistics every 3 s          │
└──────────────────────────────────────────────┘
```

**Layered design** — each layer has one job:
- **HTTP layer** (`http/`) — understands URLs, methods and JSON only.
- **Service layer** (`service/`) — all business rules (who can buy, valid prices, duplicate e-mails).
- **Repository layer** (`repository/`) — stores and finds objects. Because the service only uses the `Repository<T>` interface, the in-memory storage can later be replaced by a **database (JDBC)** without changing the service.
- **Model layer** (`model/`) — the data classes.

---

## 7. Project Structure

```
Java_Project/
├── backend/
│   ├── run.bat / run.sh                 compile + run
│   └── src/com/campusmarketplace/
│       ├── Main.java                    starts server, thread pools, monitor, shutdown hook
│       ├── http/
│       │   ├── Router.java              routes + central error handling
│       │   └── Json.java                JSON parser and writer
│       ├── service/
│       │   ├── MarketplaceService.java  business logic
│       │   └── SeedData.java            demo users and listings
│       ├── repository/
│       │   ├── Repository.java          generic interface
│       │   └── InMemoryRepository.java  collection-based implementation
│       ├── model/
│       │   ├── BaseEntity.java          abstract parent class
│       │   ├── User.java
│       │   ├── Listing.java
│       │   ├── BuyerRequest.java
│       │   └── Purchase.java
│       ├── exception/
│       │   ├── ApiException.java        base (carries HTTP status)
│       │   ├── ValidationException.java     400
│       │   ├── UnauthorizedException.java   401
│       │   ├── ForbiddenException.java      403
│       │   ├── NotFoundException.java       404
│       │   └── ConflictException.java       409
│       └── thread/
│           ├── OrderProcessor.java      Callable purchase job
│           └── MarketplaceMonitorThread.java  daemon statistics thread
├── frontend/
│   ├── vite.config.js                   dev server + /api proxy
│   └── src/
│       ├── App.jsx                      routes
│       ├── views/                       pages (Marketplace, Sell, Cart, Admin, ...)
│       ├── components/                  reusable UI pieces
│       ├── store/                       Redux slices
│       ├── context/AuthContext.jsx      login state
│       └── lib/api.js                   backend API client
├── src/, lib/                           earlier console version
├── README.md                            how to run
├── PROJECT_DOCUMENTATION.md             this file
└── BACKEND_VIVA_PREP_GUIDE.md           viva preparation
```

---

## 8. Data Model

| Class | Main fields | Notes |
|---|---|---|
| `BaseEntity` (abstract) | `id` | Parent of all entities; abstract `toMap()` |
| `User` | name, email, password, phone, department, year, mode (buyer/seller), bio, role (user/admin), blocked | Password is never sent to the frontend |
| `Listing` | ownerId, title, category, price, condition, description, location, status (Available / Pending / Sold), createdAt, images, buyerRequests | Implements `Comparable` (newest first) |
| `BuyerRequest` | buyerId, message, offerPrice, status, createdAt | Belongs to a listing |
| `Purchase` | listingId, title, price, sellerId, buyerId, purchasedAt | Immutable record of a completed sale |

**Listing status flow:**
```
Available ──(buyer sends request)──► Pending ──(purchase)──► Sold
```

---

## 9. REST API Summary

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Check that the server is running |
| GET | `/api/state` | All users, listings and purchases (initial load) |
| POST | `/api/auth/login` | Log in |
| POST | `/api/auth/register` | Create an account |
| PUT | `/api/users/{id}` | Update profile / switch buyer–seller mode |
| POST | `/api/users/{id}/toggle-block` | Admin: block or unblock a user |
| GET | `/api/listings` | List items (`?category=&search=&sort=`) |
| POST | `/api/listings` | Create a listing |
| PUT | `/api/listings/{id}` | Edit a listing |
| DELETE | `/api/listings/{id}` | Delete a listing |
| POST | `/api/listings/{id}/requests` | Send a purchase request |
| POST | `/api/listings/{id}/purchase` | Complete a purchase (thread-safe) |
| GET | `/api/purchases` | Purchase history |
| GET | `/api/stats` | Live statistics from the monitor thread |

Errors are returned as `{"error": "message"}` with the proper HTTP status (400, 401, 403, 404, 405, 409, 413, 500).

---

## 10. Java Concepts Applied (Course Mapping)

| Unit (Lectures) | Concept | Where in the project |
|---|---|---|
| **OOP (1–5)** | Classes, objects, constructors, `this`, `static` | All model classes, `Main.java`, `BaseEntity.nowIso()` |
| | Encapsulation | Private fields + getters; `User` password has no getter |
| | Inheritance | `BaseEntity` → `User`, `Listing`, `BuyerRequest`, `Purchase`; `ApiException` → 5 exceptions; `MarketplaceMonitorThread extends Thread` |
| | Abstraction | `abstract class BaseEntity`; `interface Repository<T>` |
| | Polymorphism | Overridden `toMap()`, `toString()`, `run()`, `call()`, `compareTo()`; `Repository<User> users = new InMemoryRepository<>()` |
| **Strings & Wrappers (6–9)** | `StringBuilder` | `Json.java` writer and parser |
| | String methods (`trim`, `toLowerCase`, `equalsIgnoreCase`, `split`, `substring`, `format`) | Login, register, search, routing, log messages |
| | Wrapper classes, parsing, autoboxing | `Integer.parseInt`, `Double.parseDouble`, `Map.put("isBlocked", blocked)` |
| **Exception Handling (10–15)** | Custom checked exceptions | `exception/` package |
| | `throw` / `throws` / propagation | Service methods throw; `Router.handle()` catches centrally |
| | try / catch / finally, multi-catch | `Router.handle()`, `Listing.parseTime()` |
| | try-with-resources | `Router.readBody()`, `Router.send()` |
| **Collections (16–25)** | `ArrayList`, `HashSet`, `HashMap`, `LinkedHashMap`, `TreeMap`, `ConcurrentHashMap` | Repository, service, JSON, statistics |
| | `Iterator` and safe removal | `getListings()` filter, `deleteById()` |
| | `Comparable` / `Comparator` / `Collections.sort` | Sort listings by newest or price |
| **Multithreading (26–31+)** | Extending `Thread`, `start()`, `sleep()`, `interrupt()`, daemon thread, thread states | `MarketplaceMonitorThread`, `Main.java` |
| | `ExecutorService` thread pools | Request pool (8), order pool (4) in `Main.java` |
| | `Callable` and `Future` | `OrderProcessor`, `MarketplaceService.purchase()` |
| | `synchronized` methods and blocks (race-condition fix) | `completePurchase()`, `register()`, `createListing()`, repository |
| | `volatile`, `AtomicInteger` | `Listing.status`, monitor snapshot, id counters |
| | Graceful shutdown | Shutdown hook: `shutdown()`, `awaitTermination()` |

---

## 11. Testing

| Test | Method | Result |
|---|---|---|
| Every API endpoint (success and error cases) | curl | Correct responses and status codes |
| Invalid input (blank title, negative price, wrong JSON, duplicate e-mail) | curl | 400 / 409 with clear messages |
| Blocked user tries to log in or buy | curl | 403 Forbidden |
| **Race condition**: 3 buyers buy the same item at the same moment | Parallel requests | Exactly **1 success, 2 × 409 "already sold"**; server log shows different worker threads |
| Large image upload (6 MB) | curl | Stored and returned correctly |
| Full flow in the browser (login → browse → cart → purchase request) | Manual | Requests reach the Java backend; data updates |
| Backend switched off | Manual | Frontend shows "Backend: offline" and keeps working from localStorage |
| Frontend build and lint | `npm run build`, `npm run lint` | Pass |

---

## 12. Limitations and Future Scope

| Current limitation | Planned improvement |
|---|---|
| Data is stored in memory and resets when the server restarts | Store data in **MySQL using JDBC** (`PreparedStatement`, transactions) — only a new `Repository` implementation is needed |
| Passwords are stored as plain text | Password hashing (e.g. BCrypt) |
| No session token — the frontend sends the user id | Token-based authentication |
| Filtering and statistics use loops | Rewrite with **Streams and lambdas** (`filter`, `sorted`, `Collectors.groupingBy`) |
| No data export | Export purchase history to **CSV** using file handling |
| No notifications | Producer–consumer queue with `wait()` / `notifyAll()` for order notifications |

---

## 13. Conclusion

Campus Marketplace solves a real campus problem with a simple, layered full-stack application. The Java backend applies every major unit covered so far — OOP, Strings, Exceptions, Collections and Multithreading — in a practical way. In particular, the thread pool, `Callable`/`Future` order processing and `synchronized` locking make sure the system stays correct when many students use it at the same time. The design is ready to grow with the remaining syllabus (JDBC, Streams, File Handling).

**How to run:** see `README.md`.
