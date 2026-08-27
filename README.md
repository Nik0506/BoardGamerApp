# BoardGamerApp

Eine Android-App zur Organisation regelmäßiger Brettspielabende. Das Projekt wird iterativ entwickelt; der aktuelle Stand zeigt den nächsten Spieleabend mit Datum, Uhrzeit, Gastgeber und Adresse anhand lokaler Beispieldaten.

## Aktueller Funktionsumfang

- Adaptive Navigation zu Termin, Spielen und Profil
- Dashboard für den nächsten Spieleabend
- Lade-, Leer-, Inhalts- und Fehlerzustand
- Spieler hinzufügen und bearbeiten
- Gastgeberreihenfolge verändern
- Folgetermin mit automatisch rotierendem Gastgeber planen
- Aktives Gruppenmitglied in der lokalen Demo auswählen
- Spiele mit optionaler Beschreibung vorschlagen
- Vorschläge mit Urheber und zugehörigem Termin anzeigen
- Eigene Spielvorschläge löschen
- Pro Spieler und Spieleabend genau eine Stimme abgeben oder ändern
- Beteiligung und Stimmenzahlen anzeigen
- Ergebnisse nach Stimmen sortieren
- Gewinner oder Gleichstand unmittelbar darstellen
- Domain-Modelle für Spieler und Spieleabende
- Gemeinsames, austauschbares In-Memory-Repository mit Beispieldaten
- ViewModel-basierte Zustandsverwaltung

Die Kernabläufe von Terminplanung, Gastgeberrotation, Spielvorschlägen und Abstimmung funktionieren lokal. Die Daten bleiben bis zur nächsten Iteration nur während der laufenden App-Sitzung erhalten; anschließend wird das In-Memory-Repository durch Room ersetzt.

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
