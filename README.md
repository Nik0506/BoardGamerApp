# Würfelrunde

Würfelrunde organisiert gemeinsame Brettspielabende über mehrere Geräte. Anmeldung, Gruppen, Mitglieder, Termine, Spiel- und Essensabstimmungen, Verspätungsmeldungen, Bestellungen und Bewertungen werden zentral über Firebase bereitgestellt.

## Funktionsumfang

- Registrierung, Anmeldung und Abmeldung mit Firebase Authentication
- Private Spielgruppen erstellen und per Gruppen-ID beitreten
- Aktive Gruppe auswählen und Mitgliederreihenfolge verwalten
- Nächsten Spieleabend mit Dialog, Datum, Gastgeber-Auswahl und Reihenfolge-Logik planen
- Bestehenden Spieleabend über Optionsmenü editieren (Datum, Uhrzeit, Gastgeber per Dropdown) inklusive Push-Benachrichtigung
- Absage durch Gastgeber mit 3 Handlungsoptionen: alternativer Gastgeber, Spieleabend verschieben oder Spieleabend absagen
- Echter Teilnahmestatus (RSVP): Zusagen, Verspäten und Absagen (mit optionalem Grund)
- Gruppen-Statusübersicht im Dashboard mit Zähler-Zusammenfassung und Mitglieder-Badges
- Live-Netzwerküberwachung mit automatischem Offline-Banner bei Verbindungsverlust
- Vollständig asynchrone, nicht blockierende Coroutine-Architektur für alle Firestore-Aufrufe
- Spiele vorschlagen und pro Mitglied abstimmen
- Essenskategorien verwalten und darüber abstimmen
- Restaurant, Menü-Link und persönliche Bestellungen erfassen
- Preise als ganze Centbeträge speichern und exakt summieren
- Abende abschließen und bewerten
- Adaptives Compose-Layout für unterschiedliche Displaygrößen
- Wiederherstellung von Navigation und laufenden Eingaben bei Bildschirmdrehung
- Persönliche Meldungen, Vorschläge, Stimmen, Bestellungen und Bewertungen ausschließlich für das angemeldete Konto

## Datenhaltung und Online-Anforderung

Firestore ist die einzige fachliche Datenquelle und Source of Truth. Die frühere Room-Datenbank und das In-Memory-Repository wurden entfernt. Die App besitzt keinen fachlichen Offline-Modus und deklariert Internet- sowie Netzwerkstatuszugriff ausdrücklich. Firestore verwendet keinen persistenten Datenträger-Cache; ohne Verbindung werden Fehler angezeigt und Änderungen nicht lokal vorgemerkt.

Aktueller Funktionsfortschritt: Mit **MS2 – Iteration 5: Benachrichtigungen und echte Spielstatusmeldungen** wurde ein vollständiges Teilnahme- und RSVP-System für den Spieleabend realisiert. Gruppenmitglieder können mit einem Klick zusagen, eine Verspätung mit Minuten angeben oder mit Grund absagen. Die Statusmeldungen werden in Echtzeit für die Gruppe visualisiert und lösen Benachrichtigungen aus.

Die aktive Gruppe wird im Firestore-Benutzerprofil gespeichert. Dadurch greifen Dashboard, Termine, Abstimmungen und Bestellungen auf dieselbe ausgewählte Gruppe zu.

Persönliche Aktionen sind fest an die Firebase-UID gebunden. Es gibt keine manuelle Spielerauswahl; für Angaben eines anderen Mitglieds muss dieses sich mit seinem eigenen Konto anmelden.

```text
Jetpack Compose → ViewModel → Repository (suspend / async) → Firebase Auth / Firestore
```

## Technik

- Kotlin und Jetpack Compose
- Material 3 mit adaptiver Navigation
- ViewModel und speicherbarer Compose-Zustand
- Asynchrone Kotlin Coroutines (`suspend fun`, `.await()`)
- Live Network Callback Observer (`ConnectivityManager`)
- Firebase Authentication
- Cloud Firestore mit versionierten `firestore.rules`
- Gradle Kotlin DSL
- `minSdk 24`, `targetSdk 36`

## Voraussetzungen

- Android Studio mit Android SDK 36.1
- JDK 21, beispielsweise das mit Android Studio ausgelieferte JDK
- ein erreichbares Firebase-Projekt
- aktivierte E-Mail/Passwort-Anmeldung
- passende Firestore-Sicherheitsregeln (`firestore.rules`)
- gültige `app/google-services.json`
- Internetverbindung auf Emulator oder Gerät

## Aktueller Status und Meilensteine

- MS2: gemeinsame Firebase-Gruppe, Auth, Mitglieder, gemeinsame Datenhaltung und Spiel-/Essens-/Bewertungsfunktionen umgesetzt
- MS2 – Iteration 4: Online-Robustheit, asynchrone Coroutines, Live-Netzwerkmonitor und versionierte `firestore.rules` umgesetzt
- MS2 – Iteration 5: Teilnahmestatus (RSVP), Absagen mit Grund, Gruppen-Statusübersicht und Benachrichtigungen umgesetzt
- MS3: Spieleabend planen und editieren mit Dialogen, DatePicker, TimePicker, Host-Auswahl, Reihenfolge-Logik und Push-Benachrichtigungen umgesetzt

## Bauen und testen

```bash
./gradlew test
./gradlew assembleDebug
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

Die Debug-APK entsteht unter `app/build/outputs/apk/debug/app-debug.apk`.

## Manueller Prüffluss

1. Registrieren oder anmelden.
2. Eine Gruppe erstellen oder per ID beitreten.
3. Die Gruppe öffnen und einen Spieleabend über den Dialog planen; Datum, Gastgeber und Reihenfolge festlegen.
4. App drehen und prüfen, dass Navigation, geöffnete Gruppe und Formulare erhalten bleiben beziehungsweise aus Firestore neu geladen werden.
5. Spiel- und Essensstimmen von mehreren Konten abgeben.
6. Restaurant und Bestellungen erfassen und die Gesamtsumme kontrollieren.
7. Ohne Internet prüfen, dass die App einen Fehler meldet und keine lokale Änderung vortäuscht.

## Bewusste Grenzen

- Ohne Internetverbindung ist die App fachlich nicht nutzbar.
- Push-Benachrichtigungen über Firebase Cloud Messaging sind noch nicht umgesetzt.
- Automatisierte Firebase-Integrationstests benötigen künftig ein separates Testprojekt oder die Firebase Emulator Suite.
- Der Release-Build benötigt für eine Veröffentlichung noch einen produktiven Signierschlüssel.

Die Entwicklungsschritte und Architekturentscheidungen stehen im [ITERATIVER_PROJEKTPLAN.md](ITERATIVER_PROJEKTPLAN.md).
