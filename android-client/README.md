# Stage 10 - Android student client (NOT BUILT)

The Android SDK and Android Studio were not available when the rest of the platform was built, so no Android code
was written or tested. The server side is ready: see `docs/protocols/protocols.md` section "JSON API".

Suggested screens (brief): Login, Course catalogue, My registrations, Service requests, Campus services.
* Login: `POST /api/login` (keep the `JSESSIONID` cookie).
* Courses and registrations: `GET /api/courses`, `GET /api/my-registrations`, `POST /api/register`, `POST /api/withdraw`.
* Campus services: `GET /api/campus-services`, then an implicit `geo:` or Google Maps intent for 3+ places.
* Run network calls off the main thread (e.g. `ExecutorService` or WorkManager), cache the course list locally,
  ask for location permission only when the user taps "Near me", and test denied-permission, offline, empty data and rotation.
* The emulator reaches the server on the host at `http://10.0.2.2:8080`; start the server with `-Dscasp.bind=0.0.0.0`
  only on a trusted network (HTTP is not encrypted).
