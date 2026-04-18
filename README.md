# Bloomington Transit — Real-Time Android App

A real-time Android transit tracking app for Bloomington Transit (BT), built in 24 hours 
for the hackathon. The app integrates with Bloomington Transit's public GTFS and 
GTFS-Realtime APIs to show live bus positions, schedules, and individual bus tracking.

---

## Features

- **Live Map** — Real-time bus positions on Google Maps, updating every 10 seconds
- **Bus Tracker** — Tap any bus to track its live position with an auto-following camera
- **Schedule** — Upcoming arrivals by route with predicted arrival times and directions
- **Offline-ready static data** — Routes, stops, trips, and shapes loaded once from the 
  GTFS static feed and cached locally in a Room database

---

## Architecture

This app follows **MVVM (Model-View-ViewModel) with a Repository pattern**, as 
recommended by Google for Android development.

GTFS-RT API (live)          GTFS Static ZIP (one-time load)
|                                |
Retrofit                     StaticGtfsLoader

OkHttp                           |
|                            Room DB
↓                         (stops, routes,
GtfsRealtimeParser            trips, shapes)
|                                |
└──────────┬─────────────────────┘
↓
TransitRepository
(polls every 10s,
exposes StateFlow)
↓
TransitViewModel
(transforms data,
resolves IDs)
↓
Compose UI Screens
(Map, Schedule, Tracker)

### Key architectural decisions

- **Single ViewModel** shared across all three screens via `viewModel()` — no duplicated 
  network calls
- **StateFlow** for reactive UI updates — screens automatically re-render on every poll
- **Room database** caches static GTFS data (stops, routes, trips, shapes) on first launch 
  so subsequent launches are instant
- **SupervisorJob** on the polling coroutine scope ensures one failed poll never cancels 
  the loop

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Repository |
| Async | Coroutines + StateFlow |
| Networking | Retrofit + OkHttp |
| GTFS-RT Parsing | gtfs-realtime-bindings (org.mobilitydata) |
| Local Database | Room (with KSP) |
| Maps | Google Maps SDK + Maps Compose |

---

## API Sources

All data comes from Bloomington Transit's publicly available GTFS feeds — no API key 
required for the transit data:

| Feed | URL |
|------|-----|
| Vehicle Positions | `https://s3.amazonaws.com/etatransit.gtfs/bloomingtontransit.etaspot.net/position_updates.pb` |
| Trip Updates | `https://s3.amazonaws.com/etatransit.gtfs/bloomingtontransit.etaspot.net/trip_updates.pb` |
| Static GTFS ZIP | `https://s3.amazonaws.com/etatransit.gtfs/bloomingtontransit.etaspot.net/gtfs.zip` |

---

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog or newer
- Android SDK API 26+
- A Google Maps API key with **Maps SDK for Android** enabled

### 1. Clone the repo
```bash
git clone https://github.com/yourusername/BloomingtonTransit.git
cd BloomingtonTransit
```

### 2. Add your Maps API key
Create a `local.properties` file in the root of the project and add:

sdk.dir=/path/to/your/Android/sdk
MAPS_API_KEY=YOUR_KEY_HERE

To get a Maps API key:
1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Enable **Maps SDK for Android**
3. Create an API key under **Credentials**

### 3. Add static GTFS data
Download the static GTFS ZIP and place it in `app/src/main/assets/`:

https://s3.amazonaws.com/etatransit.gtfs/bloomingtontransit.etaspot.net/gtfs.zip

Save it as `google_transit.zip` inside the `assets/` folder.

### 4. Build and run
Open in Android Studio, let Gradle sync, press ▶ Run.

On first launch the app loads stops, routes, trips, and shapes from the ZIP into Room 
(~20 seconds). Every launch after that is instant.

---

## Project Structure

app/src/main/java/com/jeba/bloomingtontransit/
├── data/
│   ├── db/
│   │   ├── dao/          # StopDao, RouteDao, TripDao, ShapePointDao
│   │   ├── entity/       # StopEntity, RouteEntity, TripEntity, ShapePointEntity
│   │   ├── StaticGtfsLoader.kt   # Parses GTFS ZIP into Room on first launch
│   │   └── TransitDatabase.kt    # Room database singleton
│   ├── model/
│   │   ├── VehiclePosition.kt    # Live bus data model
│   │   └── StopArrival.kt        # Predicted arrival data model
│   ├── network/
│   │   ├── GtfsRealtimeApi.kt    # Retrofit interface
│   │   ├── GtfsRealtimeParser.kt # Parses binary GTFS-RT feed
│   │   └── NetworkModule.kt      # OkHttp + Retrofit singleton
│   └── repository/
│       ├── ArrivalTimeCalculator.kt  # Calculates minutes-away from Unix timestamps
│       ├── RepositoryProvider.kt     # Singleton holder
│       └── TransitRepository.kt      # 10-second polling loop + StateFlow exposure
└── ui/
└── theme/
├── screens/
│   ├── MapScreen.kt        # Live bus map
│   ├── ScheduleScreen.kt   # Arrivals by route
│   └── TrackerScreen.kt    # Individual bus tracker
└── ViewModel/
└── TransitViewModel.kt # Shared ViewModel, ID resolution, caches

---

## How Polling Works

```kotlin
fun startPolling(scope: CoroutineScope) {
    scope.launch {
        while (true) {
            fetchVehiclePositions()
            fetchTripUpdates()
            delay(10_000L) // 10 seconds
        }
    }
}
```

Data is exposed via `StateFlow` so the UI automatically updates on every poll with no 
manual refresh needed.

---

## Service Hours

Bloomington Transit operates:
- **Monday – Friday:** ~6am – 11pm
- **Saturday:** ~8am – 8pm  
- **Sunday:** No service

Live feeds will be empty outside these hours — expected behavior from the transit agency.

---

## Deliverables Checklist

- [x] Working integration with Bloomington Transit GTFS-RT API
- [x] Automatic API polling every 10 seconds
- [x] Accurate expected arrival time calculations
- [x] Individual bus tracking with live position
- [x] Route view displaying buses on interactive map
- [x] Schedule table showing upcoming departures by route
- [x] MVVM architecture documented in this README
- [x] Source code on GitHub

