# BoardGamerApp

Eine Android-App zur Organisation regelmäßiger Brettspielabende. Das Projekt wird iterativ entwickelt; der aktuelle Stand zeigt den nächsten Spieleabend mit Datum, Uhrzeit, Gastgeber und Adresse aus einer lokalen Room-Datenbank.

## Aktueller Funktionsumfang

- Adaptive Navigation zu Termin, Spielen, Bewertung und Profil
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
- Spieleabend im eigenen Bewertungsbereich abschließen
- Gastgeber, Essen und Gesamtabend mit 1 bis 5 Punkten bewerten
- Optionalen Kommentar und unmittelbar berechnete Durchschnittswerte anzeigen
- Pro Spieler und Spieleabend höchstens eine Bewertung speichern
- Kontrollierte Demo-Daten beim ersten Start einer leeren Datenbank
- Gemeinsames, austauschbares Repository mit Room als produktiver Datenquelle
- ViewModel-basierte Zustandsverwaltung

Die Kernabläufe von Terminplanung, Gastgeberrotation, Spielvorschlägen, Abstimmung, lokaler Verspätungsmeldung und Bewertung funktionieren. Ein Spieleabend kann lokal von jedem Gruppenmitglied abgeschlossen und danach namentlich bewertet werden. Beim ersten Start wird eine leere Datenbank kontrolliert mit Demo-Daten befüllt; vorhandene Daten werden nicht überschrieben. Die Datenbank verwendet Schema-Version 3. `late_notices` und `reviews` werden über die expliziten Migrationen `1 -> 2` und `2 -> 3` ergänzt; es gibt keine destructive fallback migration.

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

Die Domain-Modelle bleiben unabhängig von Room. Datenbank-Entities werden im Repository in Domain-Modelle umgewandelt. Stimmen und Bewertungen besitzen jeweils einen eindeutigen Index aus Spieler und Spieleabend. Bewertungen sind nur nach dem Statuswechsel zu `FINISHED` zulässig und müssen in allen drei Pflichtkategorien zwischen 1 und 5 liegen. Verspätungsmeldungen besitzen eigene Indizes für Spieler, Spieleabend und Erstellzeitpunkt und werden bei gelöschten Spielern oder Terminen mit entfernt.

## Planung

Aufgaben, Akzeptanzkriterien und der aktuelle Entwicklungsstand stehen in [ITERATIVER_PROJEKTPLAN.md](ITERATIVER_PROJEKTPLAN.md).
