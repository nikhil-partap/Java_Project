# Campus Marketplace — Programming in Java Project

A campus buy/sell marketplace with a **React frontend** and a **plain Java 21 backend** (no frameworks).

| Folder / File | What it is |
|---|---|
| `backend/` | Java REST API (built-in `HttpServer`, in-memory collections, threads). See `backend/README.md` for endpoints. |
| `frontend/` | React + Vite + Redux UI. Talks to the backend through `/api` (Vite proxy to port 8080). Falls back to localStorage if the backend is not running. |
| `src/`, `lib/` | Earlier console version of the marketplace (multithreading demo). |
| `PROJECT_DOCUMENTATION.md` | What the project is, technologies used and where, architecture, features. |
| `BACKEND_VIVA_PREP_GUIDE.md` | Viva questions, code walkthrough and demo script for the backend. |

---

## How to Start the Project

You need **two terminals**: one for the backend and one for the frontend.

### Requirements
- **JDK 21** — check with `java -version`
- **Node.js** (v20 or newer) — check with `node -v`

### Step 1 — Start the backend (Java)
Open the first terminal:
```powershell
cd backend
.\run.bat
```
(macOS/Linux: `sh run.sh`)

Wait until you see:
```
Campus Marketplace backend running on http://localhost:8080/api/health
```
Leave this terminal open. You can check it in the browser at http://localhost:8080/api/health → `{"status":"ok"}`.

### Step 2 — Start the frontend (React)
Open a second terminal:
```powershell
cd frontend
npm install
npm run dev
```
`npm install` is only needed the first time (or after pulling new changes).

### Step 3 — Open the app
Go to **http://localhost:5173**. The top bar should show **● Backend: online**.

### Demo accounts
All passwords are `password123`.

| Email | Role |
|---|---|
| `ananya@campus.edu` | Buyer |
| `aarav@campus.edu` | Seller |
| `nikhilpartapsingh@gmail.com` | Admin |

### Stopping
Press `Ctrl+C` in both terminals. Restarting the backend resets all data to the demo listings (data is kept in memory).

### Troubleshooting
| Problem | Fix |
|---|---|
| `java` is not recognized | Install JDK 21 and reopen the terminal |
| Top bar says **Backend: offline** | The backend terminal isn't running or showed an error — check it |
| "Address already in use" / port busy | An old copy is still running — close the old terminal (or restart the PC) |
| `npm` is not recognized | Install Node.js and reopen the terminal |

---

## Course topics covered (lectures 1–27+)

OOP (abstract class, inheritance, interface, polymorphism), Strings/StringBuilder, wrapper parsing,
custom exceptions with a central error handler, Collections (List/Set/Map/TreeMap, Comparator sorting, Iterator),
and Multithreading (thread pool `ExecutorService`, `Callable`/`Future`, `synchronized` purchase to stop
race conditions, a daemon monitor thread with `sleep`/`interrupt`, `AtomicInteger`, shutdown hook).
Full details: `PROJECT_DOCUMENTATION.md`.
