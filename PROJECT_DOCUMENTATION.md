# Campus Marketplace project documentation

| | |
|---|---|
| Course | Programming in Java (25CSE0202) |
| Institute | Chitkara University Institute of Engineering and Technology |
| Programme / Semester | B.E. CSE (AI & ML), 3rd Semester, Batch 2025 |
| Session | July - Dec 2026 |
| Team Members | _(fill in names and roll numbers)_ |
| Repository | https://github.com/nikhil-partap/Java_Project |

## 1. Introduction

Campus Marketplace is a web app where students of one college can buy and sell used items like textbooks, cycles, lab kits, electronics and hostel stuff.

Seniors leaving the hostel usually have things they don't need anymore, and juniors want the same things at a low price. Right now this happens on WhatsApp groups and notice boards, which is messy. We made one place where students can list items, browse them, send purchase requests and complete the sale.

## 2. Problem statement

- There is no proper platform for second-hand buying and selling inside the campus.
- Buyers cannot search or filter what is available.
- If two students try to buy the same item at the same time, it is not clear who gets it, and the item can end up "sold twice".
- There is no purchase record, and nobody can block a user who misuses the platform.

## 3. Objectives and scope

1. Make a working full-stack app with a React frontend and a Java backend.
2. Use the Java topics from our course in the backend: OOP, Strings, exception handling, collections, file handling and multithreading.
3. Handle many users at once using threads and synchronization so that one item can never be sold twice.
4. Keep the code in layers so that a database (JDBC) can be added later.

Scope: the app is meant for a single campus and runs on a local machine. Payments are not real. The backend only simulates a payment step with a short `Thread.sleep()`.

## 4. Features

Buyer side
- Register and log in with a campus e-mail
- Browse listings, search by title or description, filter by category and max price, sort by newest or price
- See item details (photos, condition, seller, location)
- Add items to cart, send purchase requests and see purchase history

Seller side
- Switch between buyer and seller mode
- Create listings with photos, price, category, condition and location
- Edit or delete own listings, see buyer requests and mark an item as sold

Admin
- See all users and listings
- Block or unblock users (a blocked user cannot log in, list or buy)
- Remove bad listings
- See live statistics from the backend (listings, available, sold, users, orders processed, revenue)

Other things we added:
- If several buyers try to buy the same item at the same moment, only one purchase goes through. The others get an "already sold" error.
- If the backend is not running, the frontend still works using the browser's localStorage.
- Proper error messages for bad input like an empty title, negative price or duplicate e-mail.

## 5. Technologies used

### 5.1 Backend

The backend is plain Java 21 with only the JDK, no Spring or any other library.

- `com.sun.net.httpserver.HttpServer` (built into the JDK) runs the REST API on port 8080. It is set up in `Main.java` and the routes are in `http/Router.java`.
- We wrote our own JSON parser and writer in `http/Json.java`. It uses recursion and `StringBuilder` to convert JSON text to Java maps and lists and back.
- Collections (`ArrayList`, `HashMap`, `ConcurrentHashMap` etc.) hold users, listings and purchases while the server runs. See `repository/InMemoryRepository.java` and `service/MarketplaceService.java`.
- File handling with `FileReader`/`BufferedReader` and `FileWriter`/`BufferedWriter` in `storage/FileStorage.java`. Data is saved to text files after every change and loaded again on startup.
- `java.util.concurrent` (ExecutorService, Callable, Future, AtomicInteger, ConcurrentHashMap) in `Main.java`, `MarketplaceService.java` and `thread/OrderProcessor.java`.
- A normal `Thread` subclass, `thread/MarketplaceMonitorThread.java`, which updates statistics every 3 seconds.
- `java.time` for timestamps and newest-first sorting (`model/BaseEntity.java`, `model/Listing.java`).
- `backend/run.bat` and `backend/run.sh` compile with `javac` and run with `java` in one step.

### 5.2 Frontend

- React 19 for the UI (`frontend/src/`).
- Vite as the dev server. In `vite.config.js` we set a proxy so `/api` calls go to the Java backend.
- Redux Toolkit for listings, purchases and cart (`store/marketplaceSlice.js`, `store/cartSlice.js`).
- React Context (`context/AuthContext.jsx`) for the logged-in user, login/logout and notifications.
- React Router v7 for pages and admin-only routes (`App.jsx`, `components/routing/`).
- Fetch API in `lib/api.js` to call the backend, and localStorage in `lib/storage.js` as cache and offline fallback.
- Canvas and FileReader in `lib/imageCompressor.js` to compress photos before upload.
- Plain CSS with Flexbox and Grid (`index.css`, `App.css`, `BuyerMarketplace.css`).

Tools we used: Git and GitHub, VS Code / IntelliJ IDEA, npm, and curl or the browser for testing the API.

## 6. Design and architecture

```
+-------------------------------------------+
|  Browser: React frontend (port 5173)      |
|  views -> Redux / AuthContext -> api.js   |
+---------------------+---------------------+
                      |  HTTP + JSON (/api/...)
                      v
+-------------------------------------------+
|  Java backend (port 8080)                 |
|                                           |
|  Main: HttpServer + request pool (8)      |
|  Router: routes, CORS, error handling     |
|  MarketplaceService: rules, validation,   |
|     order pool (4), synchronized purchase |
|  Repository<T> / InMemoryRepository<T>    |
|  FileStorage: backend/data/*.txt          |
|  MarketplaceMonitorThread: stats every 3s |
+-------------------------------------------+
```

We divided the backend into layers, and each package does one job:
- `http/` deals only with URLs, HTTP methods and JSON.
- `service/` has the business rules, for example who can buy, valid prices and duplicate e-mails.
- `repository/` stores and finds objects. The service only talks to the `Repository<T>` interface, so later we can swap the in-memory version for a JDBC one without changing the service.
- `storage/` has `FileStorage`, which writes the collections to text files and reads them back.
- `model/` has the data classes.

## 7. Project structure

```
Java_Project/
├── backend/
│   ├── run.bat / run.sh            compile + run
│   ├── data/                       saved data (made on first run, not in git)
│   └── src/com/campusmarketplace/
│       ├── Main.java               server, thread pools, monitor, shutdown hook
│       ├── http/                   Router.java, Json.java
│       ├── service/                MarketplaceService.java, SeedData.java (demo data)
│       ├── repository/             Repository.java, InMemoryRepository.java
│       ├── storage/                FileStorage.java
│       ├── model/                  BaseEntity, User, Listing, BuyerRequest, Purchase
│       ├── exception/              ApiException and 5 subclasses
│       └── thread/                 OrderProcessor.java, MarketplaceMonitorThread.java
├── frontend/
│   ├── vite.config.js              dev server + /api proxy
│   └── src/                        App.jsx, views/, components/, store/, context/, lib/api.js
├── src/, lib/                      earlier console version
├── README.md
├── PROJECT_DOCUMENTATION.md
└── BACKEND_VIVA_PREP_GUIDE.md
```

The exception classes are `ValidationException` (400), `UnauthorizedException` (401), `ForbiddenException` (403), `NotFoundException` (404) and `ConflictException` (409). All of them extend `ApiException`, which stores the HTTP status.

## 8. Data model

All model classes extend the abstract class `BaseEntity`, which has the `id` and an abstract `toMap()`.

- `User`: name, email, password, phone, department, year, mode (buyer/seller), bio, role (user/admin), blocked. The password is never sent to the frontend.
- `Listing`: ownerId, title, category, price, condition, description, location, status, createdAt, images and its list of buyer requests. It implements `Comparable` so listings sort newest first.
- `BuyerRequest`: buyerId, message, offerPrice, status, createdAt. It belongs to a listing.
- `Purchase`: listingId, title, price, sellerId, buyerId, purchasedAt. Once created it is not changed.

A listing's status goes like this:
```
Available --(buyer sends request)--> Pending --(purchase)--> Sold
```

FileStorage saves this data in four files inside `backend/data/`: `users.txt`, `listings.txt`, `requests.txt` and `purchases.txt`. Each line is one object with fields separated by `|`. Every model class has `toFields()` to write a line and a static `fromFields()` to read it back.

## 9. REST API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Check server is running |
| GET | `/api/state` | Users, listings and purchases (first load) |
| POST | `/api/auth/login` | Log in |
| POST | `/api/auth/register` | Sign up |
| PUT | `/api/users/{id}` | Update profile or switch mode |
| POST | `/api/users/{id}/toggle-block` | Admin blocks/unblocks a user |
| GET | `/api/listings` | List items (`?category=&search=&sort=`) |
| POST | `/api/listings` | New listing |
| PUT | `/api/listings/{id}` | Edit listing |
| DELETE | `/api/listings/{id}` | Delete listing |
| POST | `/api/listings/{id}/requests` | Send purchase request |
| POST | `/api/listings/{id}/purchase` | Buy (thread-safe) |
| GET | `/api/purchases` | Purchase history |
| GET | `/api/stats` | Stats from the monitor thread |

On error the backend sends `{"error": "message"}` with the right status code (400, 401, 403, 404, 405, 409, 413 or 500).

## 10. Java concepts used in the project

| Topic (lectures) | Concept | Where |
|---|---|---|
| OOP (1-5) | Classes, constructors, `this`, `static` | Model classes, `Main.java`, `BaseEntity.nowIso()` |
| | Encapsulation | Private fields with getters, no getter for password |
| | Inheritance | `BaseEntity` and its 4 children, `ApiException` and 5 subclasses, `MarketplaceMonitorThread extends Thread` |
| | Abstraction | `abstract class BaseEntity`, `interface Repository<T>` |
| | Polymorphism | Overridden `toMap()`, `toString()`, `run()`, `call()`, `compareTo()`; `Repository<User> users = new InMemoryRepository<>()` |
| Strings and wrappers (6-9) | `StringBuilder` | `Json.java` |
| | `trim`, `toLowerCase`, `equalsIgnoreCase`, `split`, `substring`, `format` | Login, register, search, routing, logs |
| | Parsing and autoboxing | `Integer.parseInt`, `Double.parseDouble`, putting a boolean into a Map |
| Exception handling (10-15) | Custom checked exceptions | `exception/` package |
| | `throw`, `throws`, propagation | Service throws, `Router.handle()` catches |
| | try/catch/finally, multi-catch | `Router.handle()`, `Listing.parseTime()` |
| | try-with-resources | `Router.readBody()`, `Router.send()`, `FileStorage` |
| File handling | `File`, `BufferedReader.readLine()`, `BufferedWriter`, `IOException` | `FileStorage.java` |
| | Object to text line and back | `toFields()` / `fromFields()` |
| Collections (16-25) | `ArrayList`, `HashSet`, `HashMap`, `LinkedHashMap`, `TreeMap`, `ConcurrentHashMap` | Repository, service, JSON, stats |
| | `Iterator` with safe remove | `getListings()` filter, `deleteById()` |
| | `Comparable`, `Comparator`, `Collections.sort` | Sorting listings |
| Multithreading (26-31+) | Extending `Thread`, `start()`, `sleep()`, `interrupt()`, daemon thread | `MarketplaceMonitorThread`, `Main.java` |
| | `ExecutorService` | Request pool (8) and order pool (4) in `Main.java` |
| | `Callable`, `Future` | `OrderProcessor`, `MarketplaceService.purchase()` |
| | `synchronized` | `completePurchase()`, `register()`, `createListing()`, repository |
| | `volatile`, `AtomicInteger` | `Listing.status`, monitor snapshot, id counters |
| | Shutdown hook | `shutdown()` and `awaitTermination()` in `Main.java` |

The most important part for us was the purchase. Each purchase is sent as an `OrderProcessor` (a `Callable`) to the order pool, and `completePurchase()` is `synchronized`. So when two threads try to buy the same listing, the second one sees it is already sold and gets a 409 Conflict.

## 11. How to run

You need JDK 21 and Node.js (v20 or newer).

1. Backend: open a terminal, `cd backend`, then run `.\run.bat` on Windows or `sh run.sh` on macOS/Linux. It starts on http://localhost:8080 and http://localhost:8080/api/health should return `{"status":"ok"}`.
2. Frontend: open a second terminal, `cd frontend`, then `npm install` (first time only) and `npm run dev`.
3. Open http://localhost:5173. The top bar should say "Backend: online".

Demo accounts (password for all is `password123`):
- Buyer: `ananya@campus.edu`
- Seller: `aarav@campus.edu`
- Admin: `nikhilpartapsingh@gmail.com`

Data stays in `backend/data/` after you stop the server with Ctrl+C. To go back to the demo data, stop the backend and delete the `backend/data` folder. More details are in `README.md`.

## 12. Current status and testing

The backend and frontend both work and are connected. We tested it like this:

- Called every API endpoint with curl, for both normal and error cases, and got the expected responses and status codes.
- Sent bad input (blank title, negative price, broken JSON, duplicate e-mail) and got 400 or 409 with a clear message.
- A blocked user trying to log in or buy gets 403 Forbidden.
- Race condition test: 3 buyers buy the same item at the same moment using parallel requests. Exactly 1 succeeded and the other 2 got 409 "already sold". The server log showed different worker threads handling them.
- A 6 MB image upload was stored and returned correctly.
- In the browser we went through login, browsing, cart and purchase request, and the data updated on the Java backend.
- With the backend switched off, the frontend showed "Backend: offline" and kept working from localStorage.
- `npm run build` and `npm run lint` pass for the frontend.

## 13. Limitations and pending work

- Data is in text files and the whole file is rewritten on each change. We plan to move it to MySQL using JDBC (`PreparedStatement`, transactions).
- Passwords are stored as plain text. Hashing (like BCrypt) should be added.
- There is no session token, the frontend just sends the user id. Token-based login is pending.
- Filtering and statistics use normal loops. We want to rewrite them with Streams and lambdas once we cover them.
- No notifications yet. One idea is a producer-consumer queue using `wait()` / `notifyAll()`.

## 14. Conclusion

We built a buy and sell app for our campus with a React frontend and a Java backend. The backend uses most of what we studied in this course, mainly OOP, exceptions, collections, file handling and threads. The hardest part was making sure one item is not sold twice, which we solved with a thread pool and a synchronized purchase method. Next we want to add JDBC and Streams after we study them.
