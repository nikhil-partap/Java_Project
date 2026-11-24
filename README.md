# Campus Marketplace

Java project for Programming in Java (25CSE0202). It's a buy/sell site for students on campus.
The frontend is React and the backend is plain Java 21 (no Spring, no Maven, nothing extra to install).

What's in the repo:

- `backend/` - the Java server (REST API on port 8080). Endpoints are listed in `backend/README.md`.
- `frontend/` - the React + Vite app. It calls the backend through `/api`. If the backend isn't running it still works using localStorage.
- `src/`, `lib/` - our older console version of the marketplace (the multithreading demo we did first).
- `PROJECT_DOCUMENTATION.md` - project report: problem, objectives, tech used, design.
- `BACKEND_VIVA_PREP_GUIDE.md` - our notes for the viva.

## How to run it

You need JDK 21 (`java -version`) and Node.js 20 or newer (`node -v`).
Use two terminals, one for the backend and one for the frontend.

**1. Backend**

```powershell
cd backend
.\run.bat
```

On Mac/Linux use `sh run.sh` instead. When it's ready it prints:

```
Campus Marketplace backend running on http://localhost:8080/api/health
```

Keep this terminal open. Opening http://localhost:8080/api/health in the browser should show `{"status":"ok"}`.

**2. Frontend**

```powershell
cd frontend
npm install
npm run dev
```

You only need `npm install` the first time (or after pulling new changes).

**3. Open the app**

Go to http://localhost:5173. The top bar should say "Backend: online".

### Demo logins

Password for all of them is `password123`.

- `ananya@campus.edu` - buyer
- `aarav@campus.edu` - seller
- `nikhilpartapsingh@gmail.com` - admin

### Stopping and resetting

Press Ctrl+C in both terminals. The backend saves everything to text files in `backend/data/`,
so your listings, users and purchases are still there next time you start it.

To go back to the demo data, stop the backend, delete the `backend/data` folder and start it again.

### If something doesn't work

- "java is not recognized": install JDK 21 and open a new terminal.
- Top bar says "Backend: offline": the backend isn't running, or it crashed. Check that terminal for errors.
- "Address already in use": an old copy of the backend is still running. Close that terminal (or restart the PC).
- "npm is not recognized": install Node.js and open a new terminal.
- Data looks weird: stop the backend and delete `backend/data`.

## Java topics used

Everything from the syllabus up to multithreading is used somewhere in the backend:
OOP (abstract class, inheritance, interfaces, polymorphism), Strings and StringBuilder, wrapper classes,
our own exception classes with one central error handler, file handling (saving data with
BufferedReader/BufferedWriter), collections (List, Set, Map, TreeMap, sorting with Comparator, Iterator)
and multithreading (thread pools, Callable/Future, `synchronized` so an item can't be sold twice,
a daemon monitor thread, AtomicInteger, a shutdown hook).

`PROJECT_DOCUMENTATION.md` has the full list of which file uses what.
