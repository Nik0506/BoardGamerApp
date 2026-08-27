# BoardGamerApp

Eine Android-App zur Organisation regelmäßiger Brettspielabende. Das Projekt wird iterativ entwickelt; der aktuelle Stand zeigt den nächsten Spieleabend mit Datum, Uhrzeit, Gastgeber und Adresse aus einer lokalen Room-Datenbank.

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
- Persistente Room-Datenbank für Spieler, Spieleabende, Spielvorschläge und Stimmen
- Verspätung für den kommenden Spieleabend mit 10, 20, 30 oder freien Minuten lokal melden
- Aktuelle, dem Spieler und Termin zugeordnete Verspätungsmeldungen auf dem Dashboard anzeigen
- Kontrollierte Demo-Daten beim ersten Start einer leeren Datenbank
- Gemeinsames, austauschbares Repository mit Room als produktiver Datenquelle
- ViewModel-basierte Zustandsverwaltung

Die Kernabläufe von Terminplanung, Gastgeberrotation, Spielvorschlägen, Abstimmung und lokaler Verspätungsmeldung funktionieren. Auf dem Dashboard kann ein aktives Gruppenmitglied ausgewählt und eine Meldung in wenigen Schritten gespeichert werden. Diese Meldungen sind im lokalen MVP ausdrücklich nur gespeicherte/simulierte Einträge; es werden keine Android- oder Push-Benachrichtigungen versendet. Beim ersten Start wird eine leere Datenbank kontrolliert mit Demo-Daten befüllt; vorhandene Daten werden nicht überschrieben. Die Datenbank verwendet Schema-Version 2. Die Tabelle `late_notices` wird über die explizite Migration `1 -> 2` ergänzt; es gibt keine destructive fallback migration.

## Technik

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel
- Room / SQLite
- Gradle Kotlin DSL
- `minSdk 24`, `targetSdk 36`

## Projekt bauen und testen

Für die Kommandozeile muss `JAVA_HOME` auf ein kompatibles JDK zeigen. Das mit Android Studio gebündelte JDK kann verwendet werden.

```bash
./gradlew test
./gradlew assembleDebug
```

Die Debug-APK wird unter `app/build/outputs/apk/debug/app-debug.apk` erzeugt.

## Architektur

Die Compose-Oberfläche kommuniziert über ViewModels und Repository-Verträge mit den Daten. Die produktive lokale Implementierung verwendet Room; für Compose-Previews und isolierte Tests steht weiterhin das In-Memory-Repository zur Verfügung. Das `LateNoticeRepository` kapselt das Laden der Meldungen des kommenden Spieleabends und das Speichern mit Spieler, Termin, Minuten und Erstellzeitpunkt. Room erzwingt die Zuordnung über Fremdschlüssel.

```text
Compose UI → ViewModel → Repository → Room / SQLite
                                      ↘ In-Memory (Preview/Test)
```

Die Domain-Modelle bleiben unabhängig von Room. Datenbank-Entities werden im Repository in Domain-Modelle umgewandelt. Stimmen besitzen einen eindeutigen Index aus Spieler und Spieleabend, sodass pro Spieler und Termin nur eine Stimme gespeichert werden kann. Verspätungsmeldungen besitzen eigene Indizes für Spieler, Spieleabend und Erstellzeitpunkt und werden bei gelöschten Spielern oder Terminen mit entfernt.

## Planung

Aufgaben, Akzeptanzkriterien und der aktuelle Entwicklungsstand stehen in [ITERATIVER_PROJEKTPLAN.md](ITERATIVER_PROJEKTPLAN.md).
