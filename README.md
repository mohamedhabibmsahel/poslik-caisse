# Poslik Caisse

Mini-application de caisse Android (Kotlin, Jetpack Compose, Room, WorkManager, Firebase Realtime Database), conçue pour fonctionner hors ligne d'abord.

Voir **[DOCUMENTATION.md](DOCUMENTATION.md)** (également en [PDF](DOCUMENTATION.pdf)) : lancement, architecture, schéma de données, unicité des numéros, non-perte des tickets, comportement hors ligne.

```bash
./gradlew testDebugUnitTest   # tests
./gradlew installDebug        # installer sur un émulateur ou une tablette
```

Configuration Firebase : `app/google-services.json`, `firebase.json`, `.firebaserc`, `database.rules.json`.
