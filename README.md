# BoardGamerApp

Eine Android-App zur Organisation regelmäßiger Brettspielabende. Das Projekt wird iterativ entwickelt; der aktuelle Stand zeigt den nächsten Spieleabend mit Datum, Uhrzeit, Gastgeber und Adresse anhand lokaler Beispieldaten.

## Aktueller Funktionsumfang

- Adaptive Navigation zu Termin, Spielen und Profil
- Dashboard für den nächsten Spieleabend
- Lade-, Leer-, Inhalts- und Fehlerzustand
- Spieler hinzufügen und bearbeiten
- Gastgeberreihenfolge verändern
- Folgetermin mit automatisch rotierendem Gastgeber planen
- Domain-Modelle für Spieler und Spieleabende
- Gemeinsames, austauschbares In-Memory-Repository mit Beispieldaten
- ViewModel-basierte Zustandsverwaltung

Der Bereich Spiele wird in den nächsten Iterationen umgesetzt. Die Daten bleiben bis zur geplanten Room-Integration nur während der laufenden App-Sitzung erhalten.

## Technik

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel
- Gradle Kotlin DSL
- `minSdk 24`, `targetSdk 36`

## Projekt bauen und testen

Für die Kommandozeile muss `JAVA_HOME` auf ein kompatibles JDK zeigen. Das mit Android Studio gebündelte JDK kann verwendet werden.

```bash
./gradlew test
./gradlew assembleDebug
```

Die Debug-APK wird unter `app/build/outputs/apk/debug/app-debug.apk` erzeugt.

## Planung

Aufgaben, Akzeptanzkriterien und der aktuelle Entwicklungsstand stehen in [ITERATIVER_PROJEKTPLAN.md](ITERATIVER_PROJEKTPLAN.md).
