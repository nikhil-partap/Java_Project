# Backend viva notes

Programming in Java (25CSE0202). Project: Campus Marketplace, peer-to-peer trading site for students.
Backend: plain Java 21, JDK only. `com.sun.net.httpserver.HttpServer`, collections, `java.util.concurrent`, `java.io` file handling.
Syllabus: lectures 1-27+ (OOP, strings, exceptions, collections, multithreading).

---

## Overview

React frontend is just the UI. The Java backend holds all the data (users, listings, buyer requests, purchases) and checks the rules: valid price, no duplicate e-mails, blocked users can't do anything, an item sells only once. Talks to the frontend with HTTP + JSON.

Why no Spring Boot / database:
- Course is core Java. A framework would hide threads, collections, exceptions, which is what we have to show.
- Data is in collections while running and saved to text files in `backend/data/` after every change, so it survives a restart. No DB install or driver jar, still runs with just `javac` + `java`. JDBC can go behind the same `Repository<T>` interface later (Q31).

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

Packages (`backend/src/com/campusmarketplace/`):
- root: `Main.java` (server, pools, monitor thread, shutdown hook)
- `http`: `Router`, `Json` (routing, CORS, errors, JSON)
- `service`: `MarketplaceService` (rules), `SeedData` (demo data)
- `repository`: `Repository` interface + `InMemoryRepository`
- `model`: `BaseEntity`, `User`, `Listing`, `BuyerRequest`, `Purchase`
- `exception`: `ApiException` + 5 subclasses with status codes
- `thread`: `OrderProcessor` (Callable order job), `MarketplaceMonitorThread` (daemon stats)
- `storage`: `FileStorage` (save/load text files)

21 files, about 1,700 lines. Run `backend\run.bat`, check http://localhost:8080/api/health.

---

## Flow of one request

Buyer clicks "Mark Sold", which sends `POST /api/listings/l4/purchase {"buyerId":"u2"}`.

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

## File notes

### Main.java
- Port arg: `Integer.parseInt(args[0].trim())` in try/catch `NumberFormatException`.
- `Executors.newFixedThreadPool(8)` = requestPool (HTTP requests). `Executors.newFixedThreadPool(4)` = orderPool (purchases).
- `monitor.getState()` printed before `start()`, prints `NEW`.
- `HttpServer.create(...)`, `createContext("/", new Router(...))`, `setExecutor(requestPool)`, `start()`.
- `Runtime.getRuntime().addShutdownHook(new Thread(() -> {...}))`. On Ctrl+C: stop server, `monitor.interrupt()`, `shutdown()` + `awaitTermination(5s)`, `shutdownNow()` as fallback.

### model/BaseEntity.java
- `private final String id` + `getId()`. `protected BaseEntity(String id)`, only subclasses construct it.
- `public abstract Map<String,Object> toMap();` every subclass must override.
- `private static final DateTimeFormatter ISO` and `static nowIso()`.

### model/User.java
- `extends BaseEntity`, `super(id)`.
- Password `private final`, no getter, not in `toMap()`. Only `checkPassword(attempt)`. So it's never sent in JSON.
- Setters and `toggleBlocked()` are `synchronized` (two admins clicking at once).

### model/Listing.java
- `implements Comparable<Listing>`, `compareTo()` = newest first.
- `private volatile String status` so all threads see the latest value.
- `parseTime()` has multi-catch `DateTimeParseException | NullPointerException`.
- `getBuyerRequests()` returns `new ArrayList<>(...)`, defensive copy.

### model/BuyerRequest.java, Purchase.java
- `Purchase` is immutable (all `final`, no setters), so thread-safe.

### exception/
- `ApiException extends Exception`, checked, carries HTTP `status`.
- `NotFoundException (404)`, `ValidationException (400)`, `UnauthorizedException (401)`, `ForbiddenException (403)`, `ConflictException (409)`, each `super(code, message)`.

### repository/
- `interface Repository<T extends BaseEntity>`: `add`, `addFirst`, `findById`, `findAll`, `deleteById`, `count`.
- `InMemoryRepository`: `ArrayList` for display order + `ConcurrentHashMap` for O(1) id lookup.
- `deleteById` uses `Iterator` and `it.remove()`.
- `findAll()` returns `Collections.unmodifiableList(new ArrayList<>(items))`.
- Methods touching the ArrayList are `synchronized` (ArrayList isn't thread-safe).

### storage/FileStorage.java
- `backend/data/`: `users.txt`, `listings.txt`, `requests.txt`, `purchases.txt`. One record per line, `|` between fields, e.g.
  `l1|u1|Study Table|Furniture|2400.0|Good|...|Pending|2026-08-20T09:30:00.000Z|...`
- Write: `saveAll()` with `new BufferedWriter(new FileWriter(file))` in try-with-resources, `write(line)` + `newLine()`. `synchronized` so two threads never write together.
- Read: `new BufferedReader(new FileReader(file))`, `while ((line = reader.readLine()) != null)`, `line.split("\\|", -1)` (`-1` keeps trailing empty fields).
- Escaping: `Calc, Book | v2` would break the format, so `escape()` does `|` to `\p`, `,` to `\c`, newline to `\n`, `\` to `\\`. `unescape()` goes char by char with a `StringBuilder`.
- `writeEntities(String file, List<? extends BaseEntity> items)` calls `item.toFields()`, works for any entity. Each model has `toFields()` (abstract in `BaseEntity`) and static `fromFields(String[])` using `Double.parseDouble` / `Boolean.parseBoolean`.
- Bad line (`ArrayIndexOutOfBoundsException` for a missing field, `NumberFormatException` for a bad price) is skipped with a message. If files can't be read at all (`IOException`), service starts with demo data.
- `MarketplaceService` loads in its constructor (or seeds + saves on first run) and calls private `save()` after every change: register, update user, block, create/edit/delete listing, request, purchase.

### service/MarketplaceService.java
- `login()`: `trim()` + `equalsIgnoreCase()` on e-mail, `checkPassword()`. Blocked 403, wrong 401.
- `register()`: `synchronized`. `emails.add(email)` on HashSet returns false for duplicate, gives 409.
- `updateUser()`: partial, ignores `id/role/password/isBlocked` so nobody makes themselves admin.
- `getListings()`: filter with `Iterator.remove()`, sort with `Comparator.comparingDouble(Listing::getPrice)` (+ `.reversed()`) or `Collections.sort()`.
- `createListing()`: `synchronized`, validates owner/title/price, id from `AtomicInteger`.
- `updateListing()`: `synchronized (listing) { ... }` so the edit is applied in one go.
- `addRequest()`: Available becomes Pending, Sold gives 409.
- `purchase()`: submits `OrderProcessor` to the ExecutorService, `Future.get(5, SECONDS)`, unwraps `ExecutionException`.
- `completePurchase()`: race fix, check-then-act in `synchronized (listing)`.
- `computeStats()`: `TreeMap` for categories, `merge(cat, 1, Integer::sum)`.
- `number()`: `instanceof Number` then unbox, else `Double.parseDouble`, `NumberFormatException` becomes `ValidationException`.

### thread/OrderProcessor.java
- `implements Callable<Purchase>` (returns a value and can throw, Runnable can't).
- `private static final AtomicInteger ORDERS_PROCESSED`, shared lock-free counter.
- Logs `Thread.currentThread().getName()`.

### thread/MarketplaceMonitorThread.java
- `extends Thread`, `super("MarketplaceMonitor")`, `setDaemon(true)`.
- `run()`: `while (!isInterrupted()) { compute stats; Thread.sleep(interval); }`, breaks on `InterruptedException`.
- `private volatile Map snapshot` so HTTP threads read the newest one.
- `getSnapshot()` includes `getState()` (normally `TIMED_WAITING`, it's sleeping) and `isDaemon()`.

### http/Router.java
- `implements HttpHandler`, `handle(HttpExchange)`.
- Central exception handler, specific catches first:
  ```java
  } catch (ApiException e) {                               // our 400/401/403/404/409
  } catch (NumberFormatException | ClassCastException e) { // multi-catch → 400
  } catch (Exception e) {                                  // anything else → 500
  } finally {                                              // always: log + exchange.close()
  ```
- `allow(String method, String... allowed)` varargs, wrong method 405.
- `private static class Response`, static nested class.
- `readBody()`: try-with-resources on the InputStream, body > 20 MB gives 413.

### http/Json.java
- Recursive descent parser. `readValue()` checks the first char and calls `readObject()` / `readArray()` / `readString()` / `readNumber()`. Objects and arrays call `readValue()` again for nested values.
- Numbers via `Double.parseDouble`, `true/false` as `Boolean.TRUE/FALSE`.
- Writer: one `StringBuilder` per response, `instanceof` pattern matching (`value instanceof Map<?,?> map`).
- `writeValue()` on a `BaseEntity` calls `entity.toMap()` without knowing the actual class (runtime polymorphism).

---

## Syllabus concepts in the code

### OOP (lectures 1-5)
| Concept | Where |
|---|---|
| Class, object | models, `new Listing(...)`, `new OrderProcessor(...)` |
| Constructor, `this` | `User(...)`: `this.name = name` |
| `super(...)` | `User` to `BaseEntity(id)`, `ConflictException` to `ApiException(409, msg)` |
| `static` | `DEFAULT_PORT`, `nowIso()`, `ORDERS_PROCESSED`, service helpers |
| Encapsulation | private fields, hidden password |
| Inheritance | `BaseEntity` (4), `ApiException` (5), monitor `extends Thread` |
| Abstraction | `abstract class BaseEntity`, `interface Repository<T>` |
| Polymorphism | `toMap()`, `toString()`, `run()`, `call()`, `handle()`, `compareTo()`; `Repository<User> users = new InMemoryRepository<>()` |
| Interfaces | `Repository<T>`, `Comparable<Listing>`, `Callable<Purchase>`, `HttpHandler` |
| Generics (extra) | `Repository<T extends BaseEntity>`, `<>` |

### Strings, arrays, wrappers (lectures 6-9)
| Concept | Where |
|---|---|
| `StringBuilder` | `Json.write()`, `Json.readString()` |
| `trim`, `toLowerCase`, `equalsIgnoreCase`, `contains`, `indexOf`, `substring`, `split`, `isBlank` | `login()`, `register()`, `getListings()`, `Router.route()` |
| `String.format` | logs, error messages, `BaseEntity.toString()` |
| `equals` not `==` | `"Sold".equals(listing.getStatus())` |
| Parsing | `Integer.parseInt` (port, `\u` hex), `Double.parseDouble` |
| Boxing | `map.put("isBlocked", blocked)`, `merge(cat, 1, Integer::sum)`, `n.doubleValue()` |
| Array | `new String[] {"Available","Pending","Sold"}` in `normalizeStatus()` |

### Exceptions (lectures 10-15)
| Concept | Where |
|---|---|
| Custom checked | `ApiException extends Exception`, 5 subclasses |
| `throw` / `throws` | `throw new ConflictException(...)`; `purchase(...) throws ApiException` |
| Propagation | service to `Router.handle()` |
| try/catch/finally | `Router.handle()` |
| Multi-catch | Router, `Listing.parseTime()` |
| try-with-resources | `Router.readBody()`, `Router.send()` |
| Wrap/unwrap | `ExecutionException.getCause()`; `NumberFormatException` to `ValidationException` |
| Validation | price >= 0, title not blank, mode buyer/seller, status Available/Pending/Sold |

### Collections (lectures 16-25)
| Collection | Why | Where |
|---|---|---|
| `ArrayList` | ordered, newest first | `InMemoryRepository.items` |
| `HashSet` | O(1) duplicate check | `MarketplaceService.emails` |
| `ConcurrentHashMap` | O(1) id lookup, thread-safe | `InMemoryRepository.index` |
| `HashMap` | query params, empty body | `Router.query()`, `readBody()` |
| `LinkedHashMap` | fixed JSON field order | `toMap()`, JSON objects |
| `TreeMap` | sorted keys | `computeStats()` |
| `Iterator.remove()` | remove while looping | `getListings()`, `deleteById()` |
| `Comparable` | newest first | `Listing.compareTo()` |
| `Comparator` | price order | `comparingDouble(Listing::getPrice).reversed()` |
| `Collections` | `sort`, `unmodifiableList` | service, repository |

### Multithreading (lectures 26-31, plus executors)
| Concept | Where |
|---|---|
| `extends Thread` | `MarketplaceMonitorThread` |
| `Callable` + `Future` | `OrderProcessor`, `future.get(5, TimeUnit.SECONDS)` |
| Pools | 8 for requests, 4 for orders |
| `start()` | `monitor.start()` |
| `sleep()` | monitor (3 s), `completePurchase()` (100 ms) |
| `interrupt()` | shutdown hook; flag restored in `purchase()` |
| States | `NEW`, `RUNNABLE`, `TIMED_WAITING` (`/api/stats`), `TERMINATED` |
| Daemon | monitor |
| Names | `super("MarketplaceMonitor")`, `pool-1-thread-N` |
| sync method | `register()`, `createListing()`, repository, `User` setters |
| sync block | `synchronized (listing)` in `completePurchase()`, `addRequest()`, `updateListing()` |
| Reentrancy | `listing.getBuyerRequests()` called inside `synchronized(listing)` |
| Atomic | `AtomicInteger` for ids, order count |
| `volatile` | `Listing.status`, monitor `snapshot` |
| Shutdown | `shutdown()`, `awaitTermination()`, `shutdownNow()` |

### File handling
| Concept | Where |
|---|---|
| `File`, `exists()`, `mkdirs()` | `hasSavedData()`, `saveAll()` |
| `BufferedWriter`, `newLine()` | `writeLines()` |
| `BufferedReader.readLine()` | `readRecords()` |
| `IOException` | thrown by `saveAll()` / `loadUsers()`, caught in service |
| object to line | `toFields()` / `fromFields()` |

---

## Race condition

Buying is check-then-act:
```java
if (!"Sold".equals(listing.getStatus())) {   // CHECK
    listing.setStatus("Sold");                // ACT
    purchases.add(...);
}
```
Two buyers at the same time without a lock:
```
Thread A: checks, sees "Available"
Thread B: checks, sees "Available"   (A hasn't written "Sold" yet)
Thread A: sets Sold, records purchase for A
Thread B: sets Sold, records purchase for B     <- sold twice
```
The gap between check and act is the critical section.

Fix in `completePurchase()`:
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
- One thread at a time holds a listing's lock, others are BLOCKED. The next one then sees "Sold" and gets 409.
- Locking the listing and not the service means different items can be bought in parallel.
- `sleep()` keeps the lock (`wait()` wouldn't), so the 100 ms delay widens the race window and proves the lock works.

PowerShell demo (backend running):
```powershell
$jobs = 'u2','u3','u5' | ForEach-Object { Start-Job -ArgumentList $_ -ScriptBlock { param($b) try { Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/listings/l4/purchase -ContentType 'application/json' -Body (@{buyerId=$b} | ConvertTo-Json) | Out-Null; "$b -> 200 OK (bought it)" } catch { "$b -> " + [int]$_.Exception.Response.StatusCode + " (already sold)" } } }; $jobs | Wait-Job | Receive-Job
```
Git Bash (more truly simultaneous):
```sh
for b in u2 u3 u5; do curl -s -w " -> %{http_code}\n" -X POST -H "Content-Type: application/json" -d "{\"buyerId\":\"$b\"}" http://localhost:8080/api/listings/l4/purchase & done; wait
```
Expect one 200, two 409. Console `order OK` / `order FAIL` lines have different `pool-2-thread-N` names. `GET /api/stats` after: `sold` and `ordersProcessed` up by 1. Restart backend to reset.

---

## API list

| Method | Path | Body | OK | Errors |
|---|---|---|---|---|
| GET | `/api/health` | - | `{"status":"ok"}` | |
| GET | `/api/state` | - | `{users, listings, purchaseHistory}` | |
| POST | `/api/auth/login` | `{email, password}` | user | 401, 403 |
| POST | `/api/auth/register` | `{name, email, password, ...}` | 201 user | 400, 409 |
| PUT | `/api/users/{id}` | partial `{name, phone, department, year, bio, mode}` | user | 400, 404 |
| POST | `/api/users/{id}/toggle-block` | - | user | 404 |
| GET | `/api/listings?category=&search=&sort=` | - | `[listing]` | |
| POST | `/api/listings` | listing | 201 listing | 400, 403 |
| PUT | `/api/listings/{id}` | partial listing | listing | 400, 404 |
| DELETE | `/api/listings/{id}` | - | `{"deleted":true}` | 404 |
| POST | `/api/listings/{id}/requests` | `{buyerId, message, offerPrice}` | listing | 400, 403, 404, 409 |
| POST | `/api/listings/{id}/purchase` | `{buyerId}` | purchase | 403, 404, 409 |
| GET | `/api/purchases` | - | `[purchase]` | |
| GET | `/api/stats` | - | monitor snapshot | |

Errors are `{"error": "message"}`. Unknown path 404, wrong method 405, body > 20 MB 413.
Codes used: 200, 201, 204 (CORS preflight), 400, 401, 403, 404, 405, 409, 413, 500, 503 (order timeout).

---

## Demo steps

1. `backend\run.bat`. Console: `Monitor thread state before start(): NEW`, `[MarketplaceMonitor] started, state = RUNNABLE`.
2. http://localhost:8080/api/health gives `{"status":"ok"}`.
3. `cd frontend`, `npm run dev`, http://localhost:5173. Top bar: Backend: online.
4. Login `ananya@campus.edu` / `password123`. Console: `POST /api/auth/login -> 200 (... ms) [pool-1-thread-N]`.
5. Wrong password: toast "Invalid email or password.", console `-> 401`.
6. Add to cart, Submit Purchase Inquiries: `POST /api/listings/l3/requests -> 200`, listing goes Pending.
7. Race demo. One 200, two 409, different order-pool threads.
8. http://localhost:8080/api/stats: `monitorThread: "MarketplaceMonitor (TIMED_WAITING, daemon=true)"`, categories sorted (TreeMap).
9. Open `backend\data\listings.txt` in Notepad, one listing per line. Add a listing in the app, reopen, new line is there.
10. Ctrl+C backend: `Shutting down...`, `interrupted, stopping.`, `Bye.` Frontend shows Backend: offline but still works from localStorage. Restart: `Loaded ... from ...\data`, new listing still there.

---

## Viva questions

### Architecture
Q1. Why no Spring Boot?
Course is core Java. With just the JDK we show OOP, collections, exceptions, threads directly. Built-in `com.sun.net.httpserver.HttpServer` is enough for a small REST API, no jars to install.

Q2. What is REST, why is ours RESTful?
Resources at URLs, HTTP methods as actions: `GET /api/listings` reads, `POST` creates, `PUT /api/listings/{id}` updates, `DELETE` removes. Stateless (each request has everything needed), standard status codes (201, 404, 409).

Q3. What is CORS?
Browser blocks JS calling another origin (5173 to 8080) unless the server allows it. `Access-Control-Allow-Origin: *` allows it and the preflight `OPTIONS` gets 204. In dev the Vite proxy also makes it same-origin.

Q4. Why service and repository layers?
Separation of concerns. Router = HTTP, MarketplaceService = rules, Repository = storage. Change one without breaking the others, e.g. replace `InMemoryRepository` with JDBC.

Q5. JSON without a library?
`Json.write()` walks the value recursively with a `StringBuilder`: Map to `{...}`, Collection to `[...]`, String to escaped `"..."`. Entities call `toMap()` so each class picks its fields. Parser is recursive descent, next character decides object/array/string/number/boolean/null.

### OOP
Q6. Why is BaseEntity abstract and not an interface?
It has state (`id`) and shared code (`getId`, `nowIso`, `toString`) plus abstract `toMap()`. Interfaces can't have instance fields. Abstract also means no `new BaseEntity(...)`.

Q7. Where is runtime polymorphism?
`Json.writeValue()` calls `entity.toMap()` on a `BaseEntity` reference, JVM picks `User.toMap()` or `Listing.toMap()` at runtime (dynamic dispatch). Also `Repository<User> users = new InMemoryRepository<>()`.

Q8. Password encapsulation?
`private final`, no getter, not in `toMap()`. Others can only call `checkPassword(attempt)` (boolean). Never reaches the frontend.

Q9. `super(...)` in ConflictException?
Calls `ApiException(int status, String message)`, which calls `Exception(message)`. Constructor chaining.

Q10. `Repository<T extends BaseEntity>`?
Bounded generic. Any `T` that is a `BaseEntity`, so `item.getId()` is safe. One class for Users, Listings, Purchases, type-safe, no casts.

### Strings and wrappers
Q11. StringBuilder instead of `+`?
Strings are immutable, `s = s + x` makes a new object each time, O(n²) for big output like base64 images. StringBuilder appends in place. StringBuffer is the synchronized one, not needed since each request has its own builder.

Q12. `"Sold".equals(listing.getStatus())` vs `status == "Sold"`?
`==` compares references, `equals` compares content. JSON strings are new objects, not from the pool, so `==` can be false. Literal first avoids NullPointerException.

Q13. Price comes as `"650"`?
`number()`: if `instanceof Number`, unbox with `doubleValue()`. Else `Double.parseDouble(...)`. NumberFormatException is turned into ValidationException, so 400 with a clear message.

### Exceptions
Q14. Checked or unchecked?
Checked (`ApiException extends Exception`). Compiler makes every service method declare `throws ApiException`, so no error path gets forgotten. Unchecked ones like NumberFormatException are caught separately in Router.

Q15. Propagation?
`findListing()` throws NotFoundException. `purchase()` and `Router.route()` don't catch it. `Router.handle()` catches `ApiException`, reads `getStatus()` (404), sends JSON. One handler instead of try/catch everywhere.

Q16. Why does catch order matter?
Checked top to bottom. `ApiException` must be before `Exception` or everything becomes 500, and the compiler rejects an unreachable catch.

Q17. finally in Router.handle(), runs after return?
Always runs: normal end, exception, even after the `return` in the OPTIONS branch. Logs method, path, status, time, thread name and calls `exchange.close()`.

Q18. try-with-resources?
`try (InputStream in = ex.getRequestBody()) {...}`. Any `AutoCloseable` in the brackets is closed automatically, even on exception. No manual `close()`.

Q19. Why unwrap ExecutionException?
`Future.get()` wraps whatever the Callable threw. `e.getCause()` gives back our ConflictException, we rethrow it, client gets 409 and not 500.

### Collections
Q20. HashSet for e-mails?
O(1) uniqueness. `emails.add(email)` false means duplicate, 409 straight away. Looping users would be O(n).

Q21. ArrayList and ConcurrentHashMap both?
ArrayList keeps display order (newest first), map gives O(1) `findById`. Extra memory for speed.

Q22. Comparable vs Comparator?
`Listing implements Comparable` is the natural order (newest first) for `Collections.sort(result)`. `Comparator.comparingDouble(Listing::getPrice)` adds other orders without changing the class, `.reversed()` for high to low.

Q23. Iterator.remove() while filtering?
`list.remove()` inside for-each throws ConcurrentModificationException. Iterator's `remove()` is safe.

Q24. TreeMap for stats, LinkedHashMap for JSON?
TreeMap sorts keys (categories A-Z). LinkedHashMap keeps insertion order so fields come as id, name, email... HashMap has no order.

### Multithreading
Q25. How many threads?
`main` (exits after startup), HttpServer dispatcher thread (accepts connections), up to 8 request-pool, up to 4 order-pool, 1 daemon monitor, shutdown hook thread at exit. Names are in the console log.

Q26. Pool vs `new Thread()` per request?
Threads are expensive to create and unlimited creation can crash the server under load. Fixed pool reuses threads, extra tasks wait in the queue.

Q27. Callable vs Runnable?
`run()` returns void, can't throw checked exceptions. `call()` returns a value (`Purchase`) and can throw. `executor.submit()` gives a Future, `get()` with 5 s timeout (503 if too slow).

Q28. synchronized method vs block?
Method: `register()` locks the whole service, OK since registering is rare and HashSet check+add must be atomic. Block: `synchronized (listing)` in `completePurchase()` locks one listing so different items sell in parallel. Smaller critical section, faster.

Q29. Daemon thread?
Background thread, JVM doesn't wait for it on exit. Monitor shouldn't keep the program alive so `setDaemon(true)`, called before `start()`.

Q30. volatile on snapshot/status, AtomicInteger for ids?
volatile = visibility, other threads see a write immediately, no stale cache. But `count++` is read-modify-write, not atomic even if volatile, two threads could get the same id. `AtomicInteger.incrementAndGet()` is atomic using CPU compare-and-swap, no locks.

### File handling
Q31. How is data saved, why not JDBC?
`FileStorage` writes users, listings, requests, purchases to four text files in `backend/data/` after every change and reads them on startup. No DB server or driver jar, still only `javac` + `java`. Service uses `Repository<T>`, so JDBC can be added later.

Q32. Why Buffered reader/writer?
Reads/writes big chunks instead of char by char, far fewer disk operations. `readLine()` gives one record per call.

Q33. Title with `|` or comma?
`escape()` replaces them (`\p`, `\c`) before writing, `unescape()` reverses on reading. Separator never appears inside a value.

Q34. Broken file?
Each line parsed in try/catch, bad line skipped with a message, rest loads. File unreadable (IOException): starts with demo data, no crash.

Q35. Two threads writing the file at once?
No. `saveAll()` is synchronized, and `save()` takes the snapshot and writes it inside `synchronized (storage)`, so the file has the latest data. `save()` runs only after leaving `synchronized (listing)`, so we never hold a listing lock while waiting for the storage lock. No deadlock.

### Quick ones
- `start()` vs `run()`: `start()` makes a new thread that calls `run()`. Direct `run()` runs on the current thread.
- `sleep()` vs `wait()`: sleep keeps the lock, wait releases it and must be inside synchronized. We don't use wait/notify (lectures 32-33 are next).
- `Thread.currentThread().interrupt()` in catch: catching InterruptedException clears the flag, so we set it back for code higher up.
- Backend down: `api.js` marks it offline and uses localStorage, demo still works.
- Deadlock? Locks always in the same order, storage first then briefly one listing at a time while saving. Never ask for storage lock while holding a listing lock (`save()` is after the `synchronized (listing)` block), so no circular wait. Locks are reentrant, so a listing's synchronized getters inside its own block are fine.

---

## Limitations

- Whole data file rewritten on every change. Fine for a campus demo, slow for big data. Later: JDBC (lectures 34-42), `JdbcListingRepository implements Repository<Listing>` with `PreparedStatement`.
- Plain text passwords. Should hash (BCrypt or SHA-256 + salt).
- No session/token, frontend sends `buyerId` itself. Need a session token or JWT per request.
- Seller can buy own listing. Fix: `if (buyerId.equals(listing.getOwnerId())) throw new ValidationException(...)`.
- Loops for filtering and stats. Streams and lambdas later (lectures 43-46): `listings.stream().filter(...).sorted(...)`, `Collectors.groupingBy`.
- No notifications. Producer-consumer with `wait()/notifyAll()` (lectures 32-33) for an order notification queue.

Before viva: `java -version` (JDK 21), `backend\run.bat` starts, `npm install` done in `frontend/`, practise race demo, keep `completePurchase()` and `Router.handle()` open.
