# Campus Marketplace – Java Backend

A small REST backend for the Campus Marketplace React frontend, written in **plain Java 21**:
no Maven/Gradle, no external libraries, no database. It uses the JDK's built-in
`com.sun.net.httpserver.HttpServer`, a hand-written JSON parser and writer, and in-memory collections.
All data resets when the server restarts. It starts with the same users and listings as
`frontend/src/data/seedState.js`, and every seed user's password is `password123`.

## How to run

Requires JDK 21 (`javac` and `java` on the PATH).

```bat
REM Windows
run.bat
```

```sh
# macOS / Linux / Git Bash
./run.sh
```

Both scripts compile `src/` into `out/` and start `com.campusmarketplace.Main` on **port 8080**.
To use another port, pass it as an argument, for example `run.bat 9090`.
Check that it is running: <http://localhost:8080/api/health>. Stop it with **Ctrl+C**, which runs the shutdown hook.

Manual compile and run:

```sh
javac -d out -sourcepath src src/com/campusmarketplace/Main.java
java -cp out com.campusmarketplace.Main
```

## Folder structure

```
src/com/campusmarketplace/
├── Main.java                         starts the server, thread pools, monitor and shutdown hook
├── model/        BaseEntity (abstract), User, Listing, BuyerRequest, Purchase
├── exception/    ApiException + NotFound(404) / Validation(400) / Conflict(409) / Unauthorized(401) / Forbidden(403)
├── repository/   Repository<T> (interface), InMemoryRepository<T>
├── service/      MarketplaceService (business rules), SeedData
├── thread/       OrderProcessor (Callable), MarketplaceMonitorThread (daemon Thread)
└── http/         Router (routing + central error handler + CORS), Json (parser + writer)
```

Flow of a request: `HttpServer` (thread pool) → `Router` → `MarketplaceService` → `Repository`.
Any exception thrown on the way propagates back to `Router.handle()`, which turns it into JSON.

## API

All request and response bodies are JSON. Errors always look like `{"error": "message"}`.
User objects never include the password. CORS is open (`*`), and `OPTIONS` returns 204.

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| GET | `/api/health` | – | `{"status":"ok"}` | |
| GET | `/api/state` | – | `{users, listings, purchaseHistory}` | |
| POST | `/api/auth/login` | `{email, password}` | user | 401 wrong credentials, 403 blocked |
| POST | `/api/auth/register` | `{name?, email, password, phone?, department?, year?, bio?}` | user (201) | 400 missing email/password, 409 duplicate email |
| PUT | `/api/users/{id}` | partial `{name, phone, department, year, bio, mode}` | user | 400 bad mode, 404 |
| POST | `/api/users/{id}/toggle-block` | – | user | 404 |
| GET | `/api/listings?category=&search=&sort=` | – | listing[] | `sort` = `price_asc`, `price_desc` or `newest` |
| POST | `/api/listings` | listing (`ownerId`, `title`, `price` required) | listing (201) | 400 validation, 403 blocked owner |
| PUT | `/api/listings/{id}` | partial listing (`id` and `ownerId` cannot be changed) | listing | 400, 404 |
| DELETE | `/api/listings/{id}` | – | `{"deleted":true}` | 404 |
| POST | `/api/listings/{id}/requests` | `{id?, buyerId, message, offerPrice, status?, createdAt?}` | listing | 400, 403, 404, 409 sold |
| POST | `/api/listings/{id}/purchase` | `{buyerId}` (may be `"unknown"`) | purchase record | 404, 409 already sold |
| GET | `/api/purchases` | – | purchase[] | |
| GET | `/api/stats` | – | `{totalListings, available, pending, sold, totalUsers, ordersProcessed, revenue, byCategory, lastUpdated, monitorThread}` | |

An unknown route returns 404, and a wrong method returns 405. Request bodies can be up to 20 MB
(for base64 images). Unknown JSON fields are ignored.

## Course concepts and where to find them

| Concept | Where in the code |
|---|---|
| Classes, objects, constructors, `this` | `model/User.java`, `model/Listing.java` |
| `static` (fields, methods, nested class) | `BaseEntity.nowIso()`, `SeedData`, `OrderProcessor.ORDERS_PROCESSED`, `Router.Response`, `Main.DEFAULT_PORT` |
| Encapsulation | private fields with getters and setters in `model/*`; the password never leaves `User` (`checkPassword`) |
| Inheritance and abstract class | `BaseEntity` ← `User`, `Listing`, `BuyerRequest`, `Purchase`; `ApiException` ← 5 subclasses |
| Polymorphism | `toMap()` overridden in every entity and called by `Json.writeValue()`; `Repository<User> users = new InMemoryRepository<>()` |
| Interface and generics | `repository/Repository.java`, `InMemoryRepository.java`; `Router implements HttpHandler`; `OrderProcessor implements Callable` |
| Strings (`StringBuilder`, `trim`, `toLowerCase`, `equalsIgnoreCase`, `String.format`) | `http/Json.java` (writer), `MarketplaceService.login/register/getListings`, `Router` log line |
| Wrapper classes and parsing | `Double.parseDouble` in `Json.readNumber()` and `MarketplaceService.number()`; `Integer.parseInt` in `Main` (port) and `Json` (`\uXXXX`); autoboxing in `toMap()` |
| Custom checked exceptions, `throw`/`throws` | `exception/*`, used throughout `MarketplaceService` |
| try/catch/finally, multi-catch, central handler | `Router.handle()`; multi-catch also in `Listing.parseTime()`; try-with-resources in `Router.readBody()` |
| Exception propagation and unwrapping | `MarketplaceService.purchase()` (`ExecutionException` → original `ApiException`) |
| ArrayList, ConcurrentHashMap, Iterator removal, `Collections.unmodifiableList` | `repository/InMemoryRepository.java` |
| HashSet (unique e-mails) | `MarketplaceService.emails` |
| TreeMap (sorted category stats) | `MarketplaceService.computeStats()` |
| LinkedHashMap | `toMap()` methods, `Json.readObject()` |
| Comparable and Comparator sorting | `Listing.compareTo()` (newest first), `MarketplaceService.getListings()` (`Comparator.comparingDouble`, `Collections.sort`) |
| Thread pool (`Executors.newFixedThreadPool`) | `Main.java` (request pool and order pool) |
| Extending `Thread`, daemon, `sleep`, `getState`, `interrupt` | `thread/MarketplaceMonitorThread.java`, `Main.java` |
| `Callable`, `Future.get(timeout)` | `thread/OrderProcessor.java`, `MarketplaceService.purchase()` |
| `synchronized` (race-condition protection) | `MarketplaceService.completePurchase()` (`synchronized (listing)`), `register()`, `createListing()`, `InMemoryRepository` |
| `AtomicInteger`, `volatile` | id counters in `MarketplaceService`, `OrderProcessor`; `Listing.status`, `MarketplaceMonitorThread.snapshot` |
| Shutdown hook (`shutdown`, `awaitTermination`) | `Main.java` |

### Race-condition demo

Three buyers try to buy the same item at the same moment. Exactly one gets the purchase,
and the other two get `409 This item has already been sold.`:

```sh
for b in u2 u3 u5; do
  curl -s -X POST -H "Content-Type: application/json" -d "{\"buyerId\":\"$b\"}" \
       http://localhost:8080/api/listings/l4/purchase & 
done; wait
```

The server log shows which order-pool thread handled each order.
