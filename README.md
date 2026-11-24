# Campus Marketplace — Programming in Java Project

A campus buy/sell marketplace with a **React frontend** and a **plain Java 21 backend** (no frameworks).

| Folder | What it is |
|---|---|
| `backend/` | Java REST API (built-in `HttpServer`, in-memory collections, threads). See `backend/README.md` for endpoints and a "course concept → file" table. |
| `frontend/` | React + Vite + Redux UI. Talks to the backend through `/api` (Vite proxy to port 8080). Falls back to localStorage if the backend is not running. |
| `src/`, `lib/` | Earlier console version of the marketplace (multithreading demo). |

## Run it

1. Backend (needs JDK 21):
   - Windows: `backend\run.bat`
   - macOS/Linux: `sh backend/run.sh`
   - Starts on http://localhost:8080 (check http://localhost:8080/api/health)
2. Frontend (needs Node.js), in a second terminal:
   ```
   cd frontend
   npm install
   npm run dev
   ```
   Open http://localhost:5173. The top bar shows **Backend: online** when connected.

Demo accounts (password `password123`): `ananya@campus.edu` (buyer), `aarav@campus.edu` (seller).

## Course topics covered (lectures 1–27+)

OOP (abstract class, inheritance, interface, polymorphism), Strings/StringBuilder, wrapper parsing,
custom exceptions with a central error handler, Collections (List/Set/Map/TreeMap, Comparator sorting, Iterator),
and Multithreading (thread pool `ExecutorService`, `Callable`/`Future`, `synchronized` purchase to stop
race conditions, a daemon monitor thread with `sleep`/`interrupt`, `AtomicInteger`, shutdown hook).
