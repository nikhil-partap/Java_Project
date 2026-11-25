# Campus Marketplace

Simple Java 21 console application for Programming in Java (`25CSE0202`). Students can register/login, browse/search/sort campus listings, send purchase requests, post/delete listings, and test thread-safe concurrent purchases.

## Project structure

- `backend/` - the console application (models, exceptions, repository, file storage, service, threads, console menu). See `backend/README.md`.
- `src/`, `lib/` - earlier multithreading prototype.

## How to run

Requires **JDK 21** (`java -version`).

```powershell
cd backend
.\run.bat
```

On macOS/Linux: `sh run.sh`.

## Demo accounts

Password for all accounts: `password123`

- `ananya@campus.edu` (buyer)
- `aarav@campus.edu` (seller)
- `nikhilpartapsingh@gmail.com` (admin)

Data is saved automatically in `backend/data/` (`users.txt`, `listings.txt`, `requests.txt`, `purchases.txt`). Delete `backend/data/` to reset to demo data.
