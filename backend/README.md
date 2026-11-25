# Campus Marketplace – Java Console App

Plain Java 21 console application with no external libraries.

## How to run

```bat
run.bat
```

Or manually:

```sh
javac -encoding UTF-8 -d out -sourcepath src src/com/campusmarketplace/Main.java
java -cp out com.campusmarketplace.Main
```

## Packages

```
src/com/campusmarketplace/
├── Main.java                         starts order pool, monitor thread, and ConsoleApp
├── console/      ConsoleApp          menu-driven console UI
├── model/        BaseEntity, User, Listing, BuyerRequest, Purchase
├── exception/    MarketplaceException, ItemSoldException
├── repository/   Repository<T>, InMemoryRepository<T>
├── storage/      FileStorage (saves/loads data/*.txt)
├── service/      MarketplaceService, SeedData
└── thread/       OrderProcessor (Callable), MarketplaceMonitorThread (daemon)
```
