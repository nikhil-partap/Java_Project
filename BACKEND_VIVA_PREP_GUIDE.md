# 🎓 Campus Marketplace — Backend Viva Preparation Guide
**Course / Subject**: Programming in Java (25CSE0202)  
**Project Name**: Campus Marketplace (Peer-to-Peer Student Trading Platform)  
**Backend Technologies**: Plain Java 21 (JDK only), `com.sun.net.httpserver.HttpServer`, Collections, `java.util.concurrent`, file handling (`java.io`)  
**Syllabus covered**: Lectures 1–27+ (OOP, Strings, Exceptions, Collections, Multithreading)

---

## 📋 Table of Contents
1. [Project Overview & Architecture](#1-project-overview--architecture)
2. [How One Request Flows Through the Code](#2-how-one-request-flows-through-the-code)
3. [File-by-File Walkthrough](#3-file-by-file-walkthrough)
4. [Course Concepts → Where They Are in the Code](#4-course-concepts--where-they-are-in-the-code)
   - OOP · Strings & Wrappers · Exceptions · Collections · Multithreading · File Handling
5. [The Race Condition — Explained and Demonstrated](#5-the-race-condition--explained-and-demonstrated)
6. [REST API Reference](#6-rest-api-reference)
7. [Live Demo Script (5 minutes)](#7-live-demo-script-5-minutes)
8. [Top 35 Viva Questions & Model Answers](#8-top-35-viva-questions--model-answers)
9. [Known Limitations & Future Work (say these honestly)](#9-known-limitations--future-work)

---

## 1. Project Overview & Architecture

### What does the backend do?
The React frontend shows the marketplace. The **Java backend is the "single source of truth"**: it stores users, listings, buyer requests and purchases, checks the rules (valid price, no duplicate e-mails, blocked users can't act, an item can only be sold once) and answers the frontend over HTTP with JSON.

### Why no Spring Boot / no database?
- The course requires **core Java**; a framework would hide the concepts we must show (threads, collections, exceptions).
- Data is kept in Collections while the server runs and **saved to text files** (`backend/data/`) after every change, so it survives a restart. File handling needs no database install and no driver jar, so the project still runs with just `javac` + `java`. JDBC could be added later behind the same `Repository<T>` interface (see Q27).
- Zero external jars → runs with just `javac` + `java`.

### Layered Architecture
```
┌──────────────────────────────────────────────────────────────┐
│          React Frontend (Vite dev server :5173)              │
│      fetch('/api/...')  ── Vite proxy ──►  localhost:8080    │
└──────────────────────────────┬───────────────────────────────┘
                               │ HTTP + JSON
┌──────────────────────────────▼───────────────────────────────┐
│  Main.java   HttpServer + fixed thread pool (8 threads)      │
│              daemon MarketplaceMonitorThread, shutdown hook  │
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│  http/Router   (implements HttpHandler)                      │
│   • CORS + routing "METHOD /path"                            │
│   • ONE central try/catch/finally → {"error": "..."}         │
│  http/Json     hand-written JSON parser + StringBuilder writer│
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│  service/MarketplaceService   all business rules             │
│   • validation → throws custom exceptions                    │
│   • purchase → OrderProcessor (Callable) on order pool (4)   │
│   • synchronized(listing) stops double-selling               │
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│  repository/Repository<T>  ◄── InMemoryRepository<T>         │
│        ArrayList (order) + ConcurrentHashMap (id lookup)     │
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│  model/  BaseEntity (abstract) ◄─ User, Listing,             │
│                                   BuyerRequest, Purchase     │
│  exception/ ApiException ◄─ NotFound, Validation, Conflict,  │
│                             Unauthorized, Forbidden          │
└──────────────────────────────────────────────────────────────┘
```

### Folder structure (`backend/src/com/campusmarketplace/`)
| Package | Files | Responsibility |
|---|---|---|
| (root) | `Main.java` | Starts server, thread pools, monitor thread, shutdown hook |
| `http` | `Router.java`, `Json.java` | Routing, CORS, error handling, JSON |
| `service` | `MarketplaceService.java`, `SeedData.java` | Business rules, demo data |
| `repository` | `Repository.java`, `InMemoryRepository.java` | Storage contract + in-memory implementation |
| `model` | `BaseEntity`, `User`, `Listing`, `BuyerRequest`, `Purchase` | Data classes |
| `exception` | `ApiException` + 5 subclasses | Errors with HTTP status codes |
| `thread` | `OrderProcessor.java`, `MarketplaceMonitorThread.java` | Callable order job, daemon stats thread |
| `storage` | `FileStorage.java` | Saves data to text files and loads it on startup |

21 files, ~1,700 lines. Run with `backend\run.bat` → http://localhost:8080/api/health.

---

## 2. How One Request Flows Through the Code

**Example: a buyer clicks "Mark Sold" → `POST /api/listings/l4/purchase {"buyerId":"u2"}`**

```
1. HttpServer accepts the connection and hands it to a thread from requestPool
   (thread name like "pool-1-thread-3").
2. Router.handle()            → adds CORS headers, calls route()
3. Router.route()             → splits "/api/listings/l4/purchase" into parts,
                                 allow(method,"POST") else 405
4. Router.readBody()          → reads bytes (try-with-resources) → Json.parse() → Map
5. MarketplaceService.purchase()
       → findListing("l4")      (404 if missing)
       → orderExecutor.submit(new OrderProcessor(...))   returns a Future<Purchase>
       → future.get(5, SECONDS) waits for the worker thread
6. OrderProcessor.call()  (runs on orderPool thread "pool-2-thread-1")
       → service.completePurchase()
            synchronized (listing) {
                if Sold → throw ConflictException (409)
                setStatus("Sold"); create Purchase; save it
            }
7. Result goes back through Future.get() → Router.send() → Json.write() → HTTP 200
   If an exception was thrown anywhere: it PROPAGATES up to Router.handle()'s catch,
   which sends {"error": "..."} with the right status code.
8. finally{} in Router.handle() logs  "POST /api/listings/l4/purchase -> 200 (104 ms) [pool-1-thread-3]"
   and closes the exchange.
```

---

## 3. File-by-File Walkthrough

### `Main.java` — the entry point
- `Integer.parseInt(args[0].trim())` inside try/catch `NumberFormatException` → optional port argument (wrapper class parsing).
- `Executors.newFixedThreadPool(8)` → **requestPool**: serves HTTP requests in parallel.
- `Executors.newFixedThreadPool(4)` → **orderPool**: runs purchase jobs.
- `monitor.getState()` printed **before** `start()` → prints `NEW` (thread lifecycle).
- `HttpServer.create(...)`, `createContext("/", new Router(...))`, `setExecutor(requestPool)`, `start()`.
- `Runtime.getRuntime().addShutdownHook(new Thread(() -> {...}))` → on Ctrl+C: stop server, `monitor.interrupt()`, `shutdown()` + `awaitTermination(5s)` + `shutdownNow()` fallback.

### `model/BaseEntity.java` — abstract parent of every entity
- `private final String id` + `getId()` → encapsulation.
- `protected BaseEntity(String id)` → only subclasses construct it; `this.id = id`.
- `public abstract Map<String,Object> toMap();` → every subclass **must** override → polymorphism.
- `private static final DateTimeFormatter ISO` + `static nowIso()` → static members shared by the class.

### `model/User.java`
- `extends BaseEntity`, calls `super(id)`.
- Password is `private final` and **never leaves the class** — only `checkPassword(attempt)` exists, and `toMap()` does not include it (so it is never sent in JSON).
- Setters are `synchronized`; `toggleBlocked()` is synchronized so two admins clicking at once can't corrupt it.

### `model/Listing.java`
- `implements Comparable<Listing>` → `compareTo()` = newest first (natural ordering).
- `private volatile String status` → every thread sees the newest value.
- `parseTime()` uses **multi-catch** `DateTimeParseException | NullPointerException`.
- `getBuyerRequests()` returns `new ArrayList<>(...)` → a **defensive copy**, so callers can't modify internal state.

### `model/BuyerRequest.java`, `model/Purchase.java`
- `Purchase` is **immutable** (all fields `final`, no setters) → automatically thread-safe.

### `exception/*`
- `ApiException extends Exception` → a **checked** exception carrying an HTTP `status`.
- `NotFoundException (404)`, `ValidationException (400)`, `UnauthorizedException (401)`, `ForbiddenException (403)`, `ConflictException (409)` each call `super(code, message)`.

### `repository/Repository.java` + `InMemoryRepository.java`
- `interface Repository<T extends BaseEntity>` → generic contract: `add`, `addFirst`, `findById`, `findAll`, `deleteById`, `count`.
- Implementation uses **two collections**: `ArrayList` (keeps display order) + `ConcurrentHashMap` (O(1) lookup by id).
- `deleteById` uses an explicit `Iterator` and `it.remove()`.
- `findAll()` returns `Collections.unmodifiableList(new ArrayList<>(items))` → read-only snapshot.
- Methods touching the `ArrayList` are `synchronized` (ArrayList itself is not thread-safe).

### `storage/FileStorage.java` — saving data to files
- Four files in `backend/data/`: `users.txt`, `listings.txt`, `requests.txt`, `purchases.txt`. **One record per line, fields separated by `|`**, e.g.
  `l1|u1|Study Table|Furniture|2400.0|Good|...|Pending|2026-08-20T09:30:00.000Z|...`
- **Writing**: `saveAll()` → `new BufferedWriter(new FileWriter(file))` in **try-with-resources**, `write(line)` + `newLine()`. It is `synchronized` so two threads never write the same file at once.
- **Reading**: `new BufferedReader(new FileReader(file))`, `while ((line = reader.readLine()) != null)`, then `line.split("\\|", -1)` (`-1` keeps empty fields at the end).
- **Escaping** (`escape()` / `unescape()`): a title like `Calc, Book | v2` would break the format, so `|` → `\p`, `,` → `\c`, newline → `\n`, `\` → `\\`. Unescape reads the text **char by char** with a `StringBuilder`.
- **Polymorphism + generics**: `writeEntities(String file, List<? extends BaseEntity> items)` calls `item.toFields()` — works for any entity. Each model has `toFields()` (abstract in `BaseEntity`) and a static `fromFields(String[])` that uses `Double.parseDouble` / `Boolean.parseBoolean` (wrappers).
- **Exceptions**: a bad line (e.g. missing field → `ArrayIndexOutOfBoundsException`, bad price → `NumberFormatException`) is **skipped with a message**, the rest still loads. If the files can't be read at all (`IOException`), the service starts with the demo data.
- **When is it called?** `MarketplaceService` loads in its constructor (or seeds + saves on the first run) and calls a private `save()` at the end of every method that changes data (register, update user, block, create/edit/delete listing, request, purchase).

### `service/MarketplaceService.java` — the brain
| Method | What to say |
|---|---|
| `login()` | `trim()` + `equalsIgnoreCase()` on e-mail, `checkPassword()`; blocked → 403, wrong → 401 |
| `register()` | `synchronized`; `emails.add(email)` on a **HashSet** returns `false` for duplicates → 409 |
| `updateUser()` | Partial update; ignores `id/role/password/isBlocked` so a user can't make themselves admin |
| `getListings()` | Filter with `Iterator.remove()`, sort with `Comparator.comparingDouble(Listing::getPrice)` (+ `.reversed()`) or `Collections.sort()` (Comparable) |
| `createListing()` | `synchronized`; validates owner, title, price; generates id with `AtomicInteger` |
| `updateListing()` | `synchronized (listing) { ... }` block → whole edit applied atomically |
| `addRequest()` | Adds buyer request; Available → Pending; Sold → 409 |
| `purchase()` | Submits `OrderProcessor` (Callable) to the ExecutorService, waits with `Future.get(5, SECONDS)`, unwraps `ExecutionException` |
| `completePurchase()` | **The race-condition fix** — `synchronized (listing)` check-then-act |
| `computeStats()` | `TreeMap` for category counts (sorted keys), `merge(cat, 1, Integer::sum)` (autoboxing) |
| `number()` | `instanceof Number` → unboxing; else `Double.parseDouble` with `NumberFormatException` → `ValidationException` |

### `thread/OrderProcessor.java`
- `implements Callable<Purchase>` → returns a value and can throw (Runnable can do neither).
- `private static final AtomicInteger ORDERS_PROCESSED` → one counter shared by all threads, lock-free.
- Logs the worker thread name with `Thread.currentThread().getName()`.

### `thread/MarketplaceMonitorThread.java`
- `extends Thread`, `super("MarketplaceMonitor")` names it, `setDaemon(true)`.
- `run()`: `while (!isInterrupted()) { compute stats; Thread.sleep(interval); }` — `InterruptedException` → break.
- `private volatile Map snapshot` → HTTP threads always read the latest snapshot.
- `getSnapshot()` reports `getState()` (normally `TIMED_WAITING` because it's sleeping) and `isDaemon()`.

### `http/Router.java`
- `implements HttpHandler` (JDK interface) → `handle(HttpExchange)`.
- **Central exception handler** — the order of catch blocks matters (specific → general):
  ```java
  } catch (ApiException e) {                               // our 400/401/403/404/409
  } catch (NumberFormatException | ClassCastException e) { // multi-catch → 400
  } catch (Exception e) {                                  // anything else → 500
  } finally {                                              // always: log + exchange.close()
  ```
- `allow(String method, String... allowed)` → **varargs**; wrong method → 405.
- `private static class Response` → static nested class.
- `readBody()` → **try-with-resources** on the InputStream; body > 20 MB → 413.

### `http/Json.java`
- **Recursive-descent parser**: `readValue()` looks at the first character → `readObject()` / `readArray()` / `readString()` / `readNumber()`; objects and arrays call `readValue()` again (recursion) for nested values.
- Numbers → `Double.parseDouble` (wrapper), `true/false` → `Boolean.TRUE/FALSE`.
- Writer uses **one `StringBuilder`** for the whole response; `instanceof` pattern matching (`value instanceof Map<?,?> map`).
- `writeValue()` on a `BaseEntity` calls `entity.toMap()` — doesn't know if it's a User or Listing → **runtime polymorphism**.

---

## 4. Course Concepts → Where They Are in the Code

### A. OOP (Lectures 1–5)
| Concept | Where |
|---|---|
| Class & object | Every model; `new Listing(...)`, `new OrderProcessor(...)` |
| Constructor + `this` | `User(...)` → `this.name = name` |
| Constructor chaining `super(...)` | `User` → `BaseEntity(id)`; `ConflictException` → `ApiException(409, msg)` |
| `static` | `DEFAULT_PORT`, `BaseEntity.nowIso()`, `OrderProcessor.ORDERS_PROCESSED`, static helpers in service |
| Encapsulation | `private` fields + getters; password hidden in `User` |
| Inheritance | `BaseEntity` → 4 models; `ApiException` → 5 exceptions; `MarketplaceMonitorThread extends Thread` |
| Abstraction | `abstract class BaseEntity`, `abstract toMap()`; `interface Repository<T>` |
| Polymorphism (overriding / runtime) | `toMap()`, `toString()`, `run()`, `call()`, `handle()`, `compareTo()`; `Repository<User> users = new InMemoryRepository<>()` |
| Interfaces | `Repository<T>`, `Comparable<Listing>`, `Callable<Purchase>`, `HttpHandler` |
| Generics (bonus) | `Repository<T extends BaseEntity>` (bounded type), diamond `<>` |

### B. Strings, Arrays & Wrapper Classes (Lectures 6–9)
| Concept | Where |
|---|---|
| `StringBuilder` (mutable) | `Json.write()`, `Json.readString()` |
| `trim`, `toLowerCase`, `equalsIgnoreCase`, `contains`, `indexOf`, `substring`, `split`, `isBlank` | `login()`, `register()`, `getListings()`, `Router.route()` |
| `String.format` | Log lines, error messages, `BaseEntity.toString()` |
| Immutability / String pool awareness | `"Sold".equals(listing.getStatus())` (literal first → null-safe, and `equals` not `==`) |
| Wrapper parsing | `Integer.parseInt` (port, `\u` hex), `Double.parseDouble` (prices, JSON numbers) |
| Autoboxing / unboxing | `map.put("isBlocked", blocked)`, `byCategory.merge(cat, 1, Integer::sum)`, `n.doubleValue()` |
| Array | `new String[] {"Available","Pending","Sold"}` in `normalizeStatus()` |

### C. Exception Handling (Lectures 10–15)
| Concept | Where |
|---|---|
| Custom **checked** exception | `ApiException extends Exception` |
| Exception hierarchy | 5 subclasses of `ApiException` |
| `throw` / `throws` | `throw new ConflictException(...)`; `public Purchase purchase(...) throws ApiException` |
| Propagation | Service throws → not caught there → reaches `Router.handle()` |
| try / catch / finally | `Router.handle()` |
| Multi-catch | `NumberFormatException \| ClassCastException` (Router), `DateTimeParseException \| NullPointerException` (Listing) |
| try-with-resources | `Router.readBody()`, `Router.send()` |
| Wrapping / unwrapping | `ExecutionException.getCause()` in `purchase()`; `NumberFormatException` → `ValidationException` in `number()` |
| Input validation | Price ≥ 0, title non-blank, mode ∈ {buyer, seller}, status ∈ {Available, Pending, Sold} |

### D. Collections Framework (Lectures 16–25)
| Collection | Why this one? | Where |
|---|---|---|
| `ArrayList` | Ordered list, fast index access, newest-first display | `InMemoryRepository.items`, filtering results |
| `HashSet` | Uniqueness check in O(1); `add()` returns `false` on duplicate | `MarketplaceService.emails` |
| `ConcurrentHashMap` | O(1) lookup by id, thread-safe without locking the whole map | `InMemoryRepository.index` |
| `HashMap` | Query-string params, empty body | `Router.query()`, `readBody()` |
| `LinkedHashMap` | Keeps insertion order → tidy JSON field order | every `toMap()`, JSON objects |
| `TreeMap` | Sorted keys (categories alphabetically) | `computeStats()` |
| `Iterator` + `remove()` | Safe removal during traversal | `getListings()`, `deleteById()` |
| `Comparable` | Natural order (newest first) | `Listing.compareTo()` + `Collections.sort(result)` |
| `Comparator` | Alternative orders (price ↑ / ↓) | `Comparator.comparingDouble(Listing::getPrice).reversed()` |
| `Collections` utilities | `sort`, `unmodifiableList` | service, repository |

### E. Multithreading & Concurrency (Lectures 26–31, + executors)
| Concept | Where |
|---|---|
| Extending `Thread` | `MarketplaceMonitorThread` |
| `Callable` + `Future` | `OrderProcessor`; `future.get(5, TimeUnit.SECONDS)` |
| `ExecutorService` / thread pool | `newFixedThreadPool(8)` (requests), `newFixedThreadPool(4)` (orders) |
| `start()` vs `run()` | `monitor.start()` creates a new thread which calls `run()` |
| `sleep()` | Monitor loop (3 s), `completePurchase()` (100 ms fake payment delay) |
| `interrupt()` + `InterruptedException` | Shutdown hook interrupts monitor; `Thread.currentThread().interrupt()` restores the flag in `purchase()` |
| Thread states | `NEW` printed before start; `RUNNABLE` printed in `run()`; `TIMED_WAITING` shown in `/api/stats`; `TERMINATED` after interrupt |
| Daemon thread | `setDaemon(true)` on the monitor |
| Thread names | `super("MarketplaceMonitor")`, logs show `pool-1-thread-N` |
| `synchronized` **method** | `register()`, `createListing()`, repository methods, `User` setters |
| `synchronized` **block** | `synchronized (listing)` in `completePurchase()`, `addRequest()`, `updateListing()` |
| Intrinsic lock / reentrancy | Inside `synchronized(listing)` we call `listing.getBuyerRequests()` (also synchronized on `listing`) — no deadlock because Java locks are **reentrant** |
| Atomicity | `AtomicInteger.incrementAndGet()` for ids and order count |
| Visibility | `volatile` on `Listing.status` and monitor `snapshot` |
| Graceful shutdown | `shutdown()`, `awaitTermination()`, `shutdownNow()` in the shutdown hook |
| Thread-safe collection | `ConcurrentHashMap` |

### F. File Handling (see `storage/FileStorage.java`)
| Concept | Where |
|---|---|
| `File`, `exists()`, `mkdirs()` | `FileStorage.hasSavedData()`, `saveAll()` creates `data/` |
| `FileWriter` + `BufferedWriter`, `newLine()` | `FileStorage.writeLines()` |
| `FileReader` + `BufferedReader.readLine()` | `FileStorage.readRecords()` |
| try-with-resources (auto close) | Both reader and writer |
| `IOException` (checked) | `saveAll()` / `loadUsers()` declare `throws IOException`; `MarketplaceService` catches it |
| Object ↔ text line | `toFields()` / `fromFields()` in every model |

---

## 5. The Race Condition — Explained and Demonstrated

### The problem (what would go wrong without `synchronized`)
Buying is a **check-then-act** operation:
```java
if (!"Sold".equals(listing.getStatus())) {   // CHECK
    listing.setStatus("Sold");                // ACT
    purchases.add(...);
}
```
If buyers A and B arrive at the same moment on two threads:
```
Thread A: checks → "Available"  ✔
Thread B: checks → "Available"  ✔   (A hasn't written "Sold" yet!)
Thread A: sets Sold, records purchase for A
Thread B: sets Sold, records purchase for B     ← item SOLD TWICE ❌
```
The gap between check and act is the **critical section**.

### The fix (`MarketplaceService.completePurchase()`)
```java
synchronized (listing) {                       // lock THIS listing only
    if ("Sold".equals(listing.getStatus())) {
        throw new ConflictException("This item has already been sold.");  // → 409
    }
    Thread.sleep(100);                         // fake payment delay (makes the race visible)
    listing.setStatus("Sold");
    ... create + save Purchase ...
}
```
- Only **one thread at a time** can hold the lock of a given listing object. The others **block** (state `BLOCKED`) until it is released.
- The second thread then sees `"Sold"` and gets **409 Conflict**.
- We lock the **listing**, not the whole service → buyers of *different* items don't wait for each other (better concurrency than a `synchronized` method).
- `Thread.sleep()` **does not release the lock** (unlike `wait()`), so the 100 ms delay widens the race window and proves the lock works.

### Live demo (PowerShell, with the backend running)
```powershell
$jobs = 'u2','u3','u5' | ForEach-Object { Start-Job -ArgumentList $_ -ScriptBlock { param($b) try { Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/listings/l4/purchase -ContentType 'application/json' -Body (@{buyerId=$b} | ConvertTo-Json) | Out-Null; "$b -> 200 OK (bought it)" } catch { "$b -> " + [int]$_.Exception.Response.StatusCode + " (already sold)" } } }; $jobs | Wait-Job | Receive-Job
```
Or from Git Bash (truly simultaneous):
```sh
for b in u2 u3 u5; do curl -s -w " -> %{http_code}\n" -X POST -H "Content-Type: application/json" -d "{\"buyerId\":\"$b\"}" http://localhost:8080/api/listings/l4/purchase & done; wait
```
**Expected**: exactly one `200`, two `409`. Point at the server console: the `order OK` / `order FAIL` lines show **different `pool-2-thread-N` names**. Then open `GET /api/stats` → `sold` went up by 1, `ordersProcessed` by 1. (Restart the backend to reset data.)

---

## 6. REST API Reference

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| GET | `/api/health` | – | `{"status":"ok"}` | |
| GET | `/api/state` | – | `{users, listings, purchaseHistory}` | |
| POST | `/api/auth/login` | `{email, password}` | user | 401, 403 |
| POST | `/api/auth/register` | `{name, email, password, ...}` | 201 user | 400, 409 |
| PUT | `/api/users/{id}` | partial `{name, phone, department, year, bio, mode}` | user | 400, 404 |
| POST | `/api/users/{id}/toggle-block` | – | user | 404 |
| GET | `/api/listings?category=&search=&sort=` | – | `[listing]` | |
| POST | `/api/listings` | listing | 201 listing | 400, 403 |
| PUT | `/api/listings/{id}` | partial listing | listing | 400, 404 |
| DELETE | `/api/listings/{id}` | – | `{"deleted":true}` | 404 |
| POST | `/api/listings/{id}/requests` | `{buyerId, message, offerPrice}` | listing | 400, 403, 404, 409 |
| POST | `/api/listings/{id}/purchase` | `{buyerId}` | purchase | 403, 404, 409 |
| GET | `/api/purchases` | – | `[purchase]` | |
| GET | `/api/stats` | – | monitor snapshot | |

Every error body is `{"error": "message"}`. Unknown path → 404, wrong method → 405, body > 20 MB → 413.

**HTTP status codes used**: 200 OK · 201 Created · 204 No Content (CORS preflight) · 400 Bad Request · 401 Unauthorized · 403 Forbidden · 404 Not Found · 405 Method Not Allowed · 409 Conflict · 413 Payload Too Large · 500 Internal Server Error · 503 Service Unavailable (order timeout).

---

## 7. Live Demo Script (5 minutes)

1. **Start backend**: `backend\run.bat`. Point at the console:
   - `Monitor thread state before start(): NEW`
   - `[MarketplaceMonitor] started, state = RUNNABLE`
2. **Health check** in the browser: http://localhost:8080/api/health → `{"status":"ok"}`.
3. **Start frontend**: `cd frontend` → `npm run dev` → http://localhost:5173. Top bar shows **● Backend: online**.
4. **Log in** as `ananya@campus.edu` / `password123`. Show the console line `POST /api/auth/login -> 200 (... ms) [pool-1-thread-N]` — that's a request-pool thread.
5. **Wrong password** → toast "Invalid email or password." → console shows `-> 401` (custom `UnauthorizedException`).
6. **Add to cart → Submit Purchase Inquiries** → `POST /api/listings/l3/requests -> 200`; the listing becomes **Pending**.
7. **Race demo** (Section 5) → one 200, two 409, different order-pool thread names.
8. **Stats**: http://localhost:8080/api/stats → `monitorThread: "MarketplaceMonitor (TIMED_WAITING, daemon=true)"`, category counts sorted (TreeMap).
9. **File handling**: open `backend\data\listings.txt` in Notepad — every listing is one line. Create a listing in the app, open the file again → the new line is there.
10. **Stop backend** (Ctrl+C) → shutdown hook prints `Shutting down...` / `interrupted, stopping.` / `Bye.` Refresh frontend → **Backend: offline**, app still works from localStorage (graceful fallback). Start the backend again → console shows `Loaded ... from ...\data` and your new listing is still there.

---

## 8. Top 35 Viva Questions & Model Answers

### Architecture
**Q1: Why did you build the backend without Spring Boot?**  
The course is core Java. Using only the JDK lets us show OOP, collections, exceptions and threads directly. The JDK's built-in `com.sun.net.httpserver.HttpServer` is enough for a small REST API, and there are no external jars to install.

**Q2: What is REST? What makes your API RESTful?**  
REST exposes **resources** at URLs and uses HTTP **methods** for actions: `GET /api/listings` reads, `POST` creates, `PUT /api/listings/{id}` updates, `DELETE` removes. It is stateless (each request carries everything needed) and uses standard status codes (201, 404, 409…).

**Q3: What is CORS and why does the Router add those headers?**  
Browsers block JavaScript from calling a different origin (port 5173 → 8080) unless the server allows it. `Access-Control-Allow-Origin: *` allows it, and the browser's **preflight `OPTIONS`** request is answered with `204`. (In dev we also use the Vite proxy, so the browser sees the same origin.)

**Q4: Why is there a service layer and a repository layer?**  
Separation of concerns: `Router` only knows HTTP, `MarketplaceService` only knows business rules, `Repository` only knows storage. Each can change without breaking the others — e.g. replacing `InMemoryRepository` with a JDBC version (Q27).

**Q5: How do you convert Java objects to JSON without a library?**  
`Json.write()` walks the value recursively with a `StringBuilder`: Map → `{...}`, Collection → `[...]`, String → escaped `"..."`. For entities it calls `toMap()` — polymorphism, each class decides its own fields. The parser is recursive descent: it looks at the next character to decide whether to read an object, array, string, number, boolean or null.

### OOP
**Q6: Why is `BaseEntity` abstract and not an interface?**  
It has **state** (`id`) and **code** (`getId`, `nowIso`, `toString`) shared by all entities, plus one abstract method `toMap()`. An interface can't hold instance fields. Since it's abstract, nobody can write `new BaseEntity(...)`.

**Q7: Where is runtime polymorphism?**  
`Json.writeValue()` calls `entity.toMap()` on a `BaseEntity` reference; at runtime the JVM calls `User.toMap()` or `Listing.toMap()` (dynamic method dispatch). Also `Repository<User> users = new InMemoryRepository<>()` — interface reference, implementation object.

**Q8: How did you apply encapsulation to the password?**  
`password` is `private final`, has **no getter**, and is excluded from `toMap()`. Other classes can only call `checkPassword(attempt)`, which returns a boolean. So it is never sent to the frontend.

**Q9: What does `super(...)` do in `ConflictException`?**  
It calls the parent constructor `ApiException(int status, String message)`, which itself calls `Exception(message)` — constructor chaining up the hierarchy.

**Q10: What is `Repository<T extends BaseEntity>`?**  
A **generic bounded type**: the repository works for any `T`, but `T` must be a `BaseEntity`, so we can safely call `item.getId()`. One class serves Users, Listings and Purchases — type-safe, no casting.

### Strings & Wrappers
**Q11: Why `StringBuilder` in the JSON writer instead of `+`?**  
Strings are **immutable**; `s = s + x` creates a new String object every time (O(n²) for large output, e.g. base64 images). `StringBuilder` is a mutable buffer that appends in place. (`StringBuffer` is the synchronized version — not needed since each request has its own builder.)

**Q12: Why `"Sold".equals(listing.getStatus())` instead of `status == "Sold"`?**  
`==` compares references; `equals` compares content. Strings from JSON are new objects, not from the string pool, so `==` could be false. Putting the literal first also avoids `NullPointerException` if status is null.

**Q13: How do you handle a price that arrives as `"650"` (a string)?**  
`number()` checks `instanceof Number` (then unboxes with `doubleValue()`); otherwise it calls `Double.parseDouble(...)`. If that throws `NumberFormatException`, we convert it into a `ValidationException` → 400 with a clear message.

### Exceptions
**Q14: Checked vs unchecked — which did you use and why?**  
`ApiException extends Exception` → **checked**. The compiler forces every service method to declare `throws ApiException`, so no error path can be forgotten. Unchecked (`RuntimeException`) ones like `NumberFormatException` are caught separately in the Router.

**Q15: Explain exception propagation in your project.**  
`findListing()` throws `NotFoundException`; `purchase()` doesn't catch it, so it moves up to `Router.route()`, which doesn't catch it either, and finally `Router.handle()` catches `ApiException`, reads `getStatus()` (404) and sends JSON. One central handler instead of try/catch in every method.

**Q16: Why does the order of catch blocks matter?**  
Java checks catch blocks top to bottom. The specific `ApiException` must come before the general `Exception`; otherwise everything would be caught as 500 — and the compiler rejects an unreachable catch block.

**Q17: What does `finally` do in `Router.handle()`? Does it run after `return`?**  
It always runs — after normal completion, after an exception, and even after the `return` in the OPTIONS branch. We use it to log the request (method, path, status, time, thread name) and `exchange.close()` to free the connection.

**Q18: What is try-with-resources?**  
`try (InputStream in = ex.getRequestBody()) {...}` — any `AutoCloseable` declared in the parentheses is closed automatically at the end, even if an exception occurs. No manual `close()` in `finally`.

**Q19: Why unwrap `ExecutionException` in `purchase()`?**  
When the `Callable` throws inside the worker thread, `Future.get()` wraps it in `ExecutionException`. We call `e.getCause()` to get our original `ConflictException` and re-throw it, so the client gets the correct 409 instead of a generic 500.

### Collections
**Q20: Why `HashSet` for e-mails?**  
Uniqueness in O(1). `emails.add(email)` returns `false` if it already exists → immediate 409. Looping through all users would be O(n).

**Q21: Why both an `ArrayList` and a `ConcurrentHashMap` in the repository?**  
`ArrayList` keeps the display order (newest first) for listing pages; the map gives O(1) `findById`. It's a time-space trade-off.

**Q22: Comparable vs Comparator in your code?**  
`Listing implements Comparable` → the **natural** order (newest first), used by `Collections.sort(result)`. `Comparator.comparingDouble(Listing::getPrice)` defines **other** orders without changing the class; `.reversed()` gives high-to-low.

**Q23: Why use `Iterator.remove()` while filtering?**  
Calling `list.remove()` inside a for-each loop throws `ConcurrentModificationException`. The Iterator's own `remove()` is the safe way to delete during traversal.

**Q24: Why `TreeMap` for category stats and `LinkedHashMap` for JSON?**  
`TreeMap` keeps keys **sorted** (categories alphabetically). `LinkedHashMap` keeps **insertion order**, so JSON fields always appear as id, name, email… `HashMap` gives no order guarantee.

### Multithreading
**Q25: How many threads does your backend use?**  
`main` (exits after startup) + HttpServer's own dispatcher thread (accepts connections) + up to **8** request-pool threads + up to **4** order-pool threads + **1** daemon monitor thread + the shutdown-hook thread (only at exit). Thread names appear in the console log.

**Q26: Why a thread pool instead of `new Thread()` per request?**  
Creating threads is expensive and unbounded creation can crash the server under load. A fixed pool **reuses** a limited number of threads; extra tasks wait in the pool's queue.

**Q27: Callable vs Runnable?**  
`Runnable.run()` returns `void` and can't throw checked exceptions. `Callable<V>.call()` returns a value (`Purchase`) and can throw. We submit it with `executor.submit()`, get a `Future`, and read the result with `get()` (with a 5 s timeout → 503 if it takes too long).

**Q28: `synchronized` method vs `synchronized` block — where and why?**  
Method: `register()` locks the whole service object — fine because registration is rare and the HashSet check+add must be atomic. Block: `synchronized (listing)` in `completePurchase()` locks **only that one listing**, so purchases of different items run in parallel. Smaller critical section = better performance.

**Q29: What is a daemon thread and why is the monitor one?**  
A daemon thread is a background helper; the JVM **doesn't wait** for daemon threads when exiting. The stats monitor shouldn't keep the program alive, so `setDaemon(true)` (must be called before `start()`).

**Q30: Why `volatile` on `snapshot` and `status`? Why `AtomicInteger` for ids?**  
`volatile` guarantees **visibility**: a write by one thread is immediately seen by others (no stale cached value). But `count++` is three steps (read-modify-write) and not atomic even if volatile — two threads could generate the same id. `AtomicInteger.incrementAndGet()` does it atomically using CPU compare-and-swap, without locks.

### File Handling
**Q31: How is data saved? Why files and not JDBC?**
`FileStorage` writes users, listings, requests and purchases to four text files in `backend/data/` after every change, and reads them back on startup. Files need no database server or driver jar, so the project still runs with only `javac` + `java`. The service uses the `Repository<T>` interface, so a JDBC version could be added later.

**Q32: Why `BufferedReader`/`BufferedWriter` instead of plain `FileReader`/`FileWriter`?**
Buffering reads/writes a big chunk at a time instead of one character at a time (much fewer disk operations), and `BufferedReader.readLine()` gives us one record per call.

**Q33: What if a title contains `|` or a comma?**
`escape()` replaces them with codes (`\p`, `\c`) before writing, and `unescape()` turns them back when reading, so the separator never appears inside a value.

**Q34: What if the file is broken?**
Each line is parsed inside a `try/catch`: a bad line is skipped with a message and the rest still loads. If the file can't be read at all (`IOException`), the backend starts with the demo data instead of crashing.

**Q35: Can two threads write the file at the same time?**
No — `saveAll()` is `synchronized`, and `save()` takes the snapshot and writes it inside `synchronized (storage)`, so the file always ends up with the latest data. `save()` is called only **after** leaving the `synchronized (listing)` block, so we never hold a listing lock while waiting for the storage lock (no deadlock).

### Bonus quick-fire
- **`start()` vs `run()`?** `start()` creates a new thread that calls `run()`; calling `run()` directly runs it on the *current* thread.
- **`sleep()` vs `wait()`?** `sleep()` keeps the lock; `wait()` releases it and must be called inside `synchronized`. We don't use `wait/notify` (lectures 32–33 come next).
- **Why `Thread.currentThread().interrupt()` in the catch?** Catching `InterruptedException` clears the interrupt flag; we restore it so code higher up still knows the thread was interrupted.
- **What happens if the backend is down?** The frontend's `api.js` marks it offline and falls back to localStorage, so the demo still works.
- **Can two threads deadlock in your code?** Locks are always taken in the same order: storage lock first, then (briefly) one listing at a time while saving. We never ask for the storage lock while holding a listing lock (`save()` runs after the `synchronized (listing)` block), so there is no circular wait. Java locks are reentrant, so re-locking the same listing (calling its synchronized getters inside the block) is fine.

---

## 9. Known Limitations & Future Work
Be ready to say these yourself — examiners appreciate honesty:

| Limitation | Planned fix (upcoming syllabus) |
|---|---|
| Whole data file is rewritten on every change (fine for a campus demo, slow for huge data) | **JDBC** (lectures 34–42): `JdbcListingRepository implements Repository<Listing>` with `PreparedStatement` |
| Passwords stored as plain text | Hash with BCrypt / SHA-256 + salt |
| No login token/session — the frontend sends `buyerId` itself | Session token or JWT checked on each request |
| A seller can purchase their own listing | One extra `if (buyerId.equals(listing.getOwnerId())) throw new ValidationException(...)` |
| Loops for filtering/stats | **Streams & lambdas** (lectures 43–46): `listings.stream().filter(...).sorted(...)`, `Collectors.groupingBy` |
| Notifications | **Producer–consumer** with `wait()/notifyAll()` (lectures 32–33) for an order-notification queue |

---

**Last-minute checklist**: JDK 21 installed (`java -version`) · `backend\run.bat` starts cleanly · `npm install` done in `frontend/` · Know the race-condition demo by heart · Open `MarketplaceService.completePurchase()` and `Router.handle()` in the editor before the viva starts.
