# Backend (Java)

This is the REST backend for the Campus Marketplace frontend. It's plain Java 21 with no Maven/Gradle
and no external jars. We used the JDK's own `com.sun.net.httpserver.HttpServer` for the server and wrote
a small JSON parser/writer ourselves (`http/Json.java`).

Data is kept in collections while the server runs. After every change it's also written to text files
in `backend/data/`, and loaded back when the server starts:

- `users.txt`
- `listings.txt`
- `requests.txt` (buyer requests, each line starts with the listing id)
- `purchases.txt`

One record per line, fields separated by `|`. The code for this is in `storage/FileStorage.java`.
On the first run there's no `data/` folder, so it starts with the same demo users and listings as
`frontend/src/data/seedState.js` (password `password123` for all of them). Delete `data/` if you want
to reset back to that.

## Running

You need JDK 21.

```bat
run.bat
```

or on Mac/Linux/Git Bash:

```sh
./run.sh
```

Both compile `src/` into `out/` and start the server on port 8080. You can pass a different port,
e.g. `run.bat 9090`. Check it at http://localhost:8080/api/health. Ctrl+C stops it (the shutdown
hook closes the thread pools properly).

Without the scripts:

```sh
javac -d out -sourcepath src src/com/campusmarketplace/Main.java
java -cp out com.campusmarketplace.Main
```

## Packages

```
src/com/campusmarketplace/
  Main.java      starts the server, thread pools, monitor thread, shutdown hook
  model/         BaseEntity (abstract), User, Listing, BuyerRequest, Purchase
  exception/     ApiException and NotFound(404), Validation(400), Conflict(409), Unauthorized(401), Forbidden(403)
  repository/    Repository<T> interface, InMemoryRepository<T>
  service/       MarketplaceService (all the rules), SeedData
  storage/       FileStorage (saving/loading the .txt files)
  thread/        OrderProcessor (Callable), MarketplaceMonitorThread (daemon thread)
  http/          Router (routes, CORS, error handling), Json
```

A request goes HttpServer (thread pool) -> Router -> MarketplaceService -> Repository.
If anything throws on the way, the exception comes back up to `Router.handle()` and gets turned into
a JSON error, so we only handle errors in one place.

## API

Bodies are JSON. Errors look like `{"error": "message"}`. The password is never sent back in a user object.
CORS is open (`*`) and `OPTIONS` returns 204.

| Method | Path | Body | Returns | Errors |
|---|---|---|---|---|
| GET | `/api/health` | | `{"status":"ok"}` | |
| GET | `/api/state` | | `{users, listings, purchaseHistory}` | |
| POST | `/api/auth/login` | `{email, password}` | user | 401 wrong login, 403 blocked |
| POST | `/api/auth/register` | `{name?, email, password, phone?, department?, year?, bio?}` | user (201) | 400 missing fields, 409 email taken |
| PUT | `/api/users/{id}` | any of `name, phone, department, year, bio, mode` | user | 400 bad mode, 404 |
| POST | `/api/users/{id}/toggle-block` | | user | 404 |
| GET | `/api/listings?category=&search=&sort=` | | listings | sort is `price_asc`, `price_desc` or `newest` |
| POST | `/api/listings` | listing (`ownerId`, `title`, `price` needed) | listing (201) | 400, 403 if owner is blocked |
| PUT | `/api/listings/{id}` | changed fields (can't change `id`/`ownerId`) | listing | 400, 404 |
| DELETE | `/api/listings/{id}` | | `{"deleted":true}` | 404 |
| POST | `/api/listings/{id}/requests` | `{id?, buyerId, message, offerPrice, status?, createdAt?}` | listing | 400, 403, 404, 409 if sold |
| POST | `/api/listings/{id}/purchase` | `{buyerId}` (can be `"unknown"`) | purchase | 404, 409 already sold |
| GET | `/api/purchases` | | purchases | |
| GET | `/api/stats` | | counts, revenue, `byCategory`, `monitorThread` etc. | |

Unknown routes give 404 and a wrong method gives 405. Bodies can be up to 20 MB because images
come in as base64. Extra JSON fields are just ignored.

## Where the course topics are

| Topic | Where |
|---|---|
| Classes, constructors, `this` | `model/User.java`, `model/Listing.java` |
| `static` | `BaseEntity.nowIso()`, `SeedData`, `OrderProcessor.ORDERS_PROCESSED`, `Router.Response`, `Main.DEFAULT_PORT` |
| Encapsulation | private fields in `model/*`; password only checked inside `User.checkPassword()` |
| Inheritance, abstract class | `BaseEntity` and its 4 subclasses; `ApiException` and its 5 subclasses |
| Polymorphism | `toMap()` and `toFields()` overridden in each model; `Repository<User> users = new InMemoryRepository<>()` |
| Interfaces, generics | `Repository<T>`, `Router implements HttpHandler`, `OrderProcessor implements Callable` |
| Strings | `StringBuilder` in `Json.java` and `FileStorage`; `trim`, `equalsIgnoreCase`, `split`, `String.format` in the service |
| Wrapper classes | `Double.parseDouble`, `Integer.parseInt`, `Boolean.parseBoolean`; autoboxing in `toMap()` |
| Custom exceptions, throw/throws | `exception/*`, used all over `MarketplaceService` |
| try/catch/finally, multi-catch | `Router.handle()`, `Listing.parseTime()` |
| try-with-resources | `Router.readBody()`, `FileStorage` |
| File handling | `storage/FileStorage.java` (FileReader/BufferedReader, FileWriter/BufferedWriter, IOException) |
| ArrayList, ConcurrentHashMap, Iterator | `repository/InMemoryRepository.java` |
| HashSet, TreeMap, LinkedHashMap | `emails` set and `computeStats()` in the service; `toMap()` |
| Comparable / Comparator | `Listing.compareTo()`, sorting in `getListings()` |
| Thread pools | `Main.java` (8 threads for requests, 4 for orders) |
| Extending Thread, daemon, sleep, interrupt | `thread/MarketplaceMonitorThread.java` |
| Callable and Future | `OrderProcessor`, `MarketplaceService.purchase()` |
| `synchronized` | `completePurchase()`, `register()`, `createListing()`, `InMemoryRepository`, `FileStorage.saveAll()` |
| AtomicInteger, volatile | id counters, `Listing.status`, monitor snapshot |
| Shutdown hook | `Main.java` |

## Trying the race condition

Three buyers try to buy the same item at the same time. Only one should get it, the other two
get `409 This item has already been sold.`

```sh
for b in u2 u3 u5; do
  curl -s -X POST -H "Content-Type: application/json" -d "{\"buyerId\":\"$b\"}" \
       http://localhost:8080/api/listings/l4/purchase & 
done; wait
```

The server log shows which order-pool thread handled each one.
