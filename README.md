# Wromble — Android

Native Android version of the Wromble food-ordering app (companion to the iOS app).
Built with **Kotlin + Jetpack Compose**, talking to the same `wromble.dk` REST APIs
as the iOS app — same screens, same login, same live data.

## Tech
- Kotlin, Jetpack Compose (Material 3)
- Retrofit + OkHttp + Gson (networking)
- Coil (image loading)
- Navigation Compose
- Paparazzi (screenshot rendering for UI verification)

## Modules / screens
- Splash + brand intro
- Login & registration with role selector (Privat / Forretning / Chauffør)
- Home: greeting, "Scan bordet" banner, categories, favourites, restaurants nearby
- Restaurant detail + menu, add to cart
- Cart: quantity, delivery/pickup, note, payment, place order
- Orders list + live order tracking (status ring)
- Profile
- Staff: driver dashboard (mark delivered) and company dashboard (accept/reject orders)

## Backend
All data comes from the existing `https://wromble.dk/api/*.php` endpoints —
no separate backend. Same account works across web, iOS and Android.

## Build
```
./gradlew :app:assembleDebug        # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:recordPaparazziDebug # render UI screenshots
```
Requires Android SDK 34, JDK 17.
