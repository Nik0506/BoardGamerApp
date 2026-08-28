# BoardGamerApp

Eine Android-App zur Organisation regelmäßiger Brettspielabende. Das Projekt wird iterativ entwickelt; der aktuelle Stand zeigt den nächsten Spieleabend mit Datum, Uhrzeit, Gastgeber und Adresse aus einer lokalen Room-Datenbank.

## Aktueller Funktionsumfang

- Adaptive Navigation zu Termin, Spielen, Essen, Bewertung und Profil
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
- Essenskategorien für den kommenden Spieleabend verwalten
- Pro Spieler genau eine Essensstimme abgeben oder ändern
- Ergebnis, Gleichstand und noch fehlende Stimmen anzeigen
- Noch nicht abstimmende Personen über eine rein lokale Erinnerungsaktion auflisten
- Kontrollierte Demo-Daten beim ersten Start einer leeren Datenbank
- Gemeinsames, austauschbares Repository mit Room als produktiver Datenquelle
- ViewModel-basierte Zustandsverwaltung

Die Kernabläufe von Terminplanung, Gastgeberrotation, Spiel- und Essensabstimmung, lokaler Verspätungsmeldung und Bewertung funktionieren. Ein Spieleabend kann lokal von jedem Gruppenmitglied abgeschlossen und danach namentlich bewertet werden. Beim ersten Start wird eine leere Datenbank kontrolliert mit Demo-Daten befüllt; vorhandene Daten werden nicht überschrieben. Die Datenbank verwendet Schema-Version 4. `late_notices`, `reviews`, `food_categories` und `food_votes` werden über die expliziten Migrationen `1 -> 2`, `2 -> 3` und `3 -> 4` ergänzt; es gibt keine destructive fallback migration.

## Technik

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel
- Room / SQLite
- Gradle Kotlin DSL
- `minSdk 24`, `targetSdk 36`

## Projekt bauen und testen

### Voraussetzungen

- Android Studio mit Android SDK 36.1
- JDK 21; das mit Android Studio gebündelte JDK kann verwendet werden
- ein Emulator oder Gerät ab Android 7.0/API 24 für instrumentierte Tests

Projektordner in Android Studio öffnen und **File → Sync Project with Gradle Files** ausführen. Für die Kommandozeile muss `JAVA_HOME` auf ein kompatibles JDK zeigen.

```bash
./gradlew test
./gradlew assembleDebug
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleRelease
```

Die Debug-APK wird unter `app/build/outputs/apk/debug/app-debug.apk` erzeugt.

## Reproduzierbarer Demo-Ablauf

Bei einer leeren Debug-Datenbank werden Max und Lea, der Spieleabend am 28.08.2026 sowie Catan und Heat angelegt. Vor einer Vorführung kann die App über die Android-Systemeinstellungen zurückgesetzt werden.

1. Unter **Termin** Datum, Gastgeber und lokale Verspätungsmeldung zeigen.
2. Unter **Profil** einen Spieler ergänzen und die Gastgeberreihenfolge ändern.
3. Unter **Spiele** den aktiven Spieler wechseln, einen Vorschlag ergänzen und abstimmen.
4. Unter **Essen** eine Kategorie ergänzen, für beide Spieler abstimmen und Ergebnis beziehungsweise Gleichstand prüfen.
5. Unter **Bewertung** den Spieleabend abschließen.
6. Für Max und Lea unterschiedliche Bewertungen samt optionalem Kommentar speichern.
7. Die angezeigten Durchschnittswerte prüfen, die App vollständig schließen und erneut öffnen.
8. Kontrollieren, dass Spieler, Spiel- und Essensstimmen, Meldungen, Abschlussstatus und Bewertungen erhalten geblieben sind.

## Architektur

Die Compose-Oberfläche kommuniziert über ViewModels und Repository-Verträge mit den Daten. Die produktive lokale Implementierung verwendet Room; für Compose-Previews und isolierte Tests steht weiterhin das In-Memory-Repository zur Verfügung. Das `LateNoticeRepository` kapselt das Laden der Meldungen des kommenden Spieleabends und das Speichern mit Spieler, Termin, Minuten und Erstellzeitpunkt. Room erzwingt die Zuordnung über Fremdschlüssel.

```text
Compose UI → ViewModel → Repository → Room / SQLite
                                      ↘ In-Memory (Preview/Test)
```

Die Domain-Modelle bleiben unabhängig von Room. Datenbank-Entities werden im Repository in Domain-Modelle umgewandelt. Spielstimmen, Essensstimmen und Bewertungen besitzen jeweils einen eindeutigen Index aus Spieler und Spieleabend. Essenskategorien gehören zu genau einem Abend; beim Löschen einer Kategorie werden ihre Stimmen mit entfernt. Bewertungen sind nur nach dem Statuswechsel zu `FINISHED` zulässig und müssen in allen drei Pflichtkategorien zwischen 1 und 5 liegen. Verspätungsmeldungen besitzen eigene Indizes für Spieler, Spieleabend und Erstellzeitpunkt und werden bei gelöschten Spielern oder Terminen mit entfernt.

## Grenzen des lokalen MVP

- Alle Daten liegen ausschließlich auf einem Gerät; es gibt noch keine Konten, Gruppen oder Synchronisation.
- Verspätungsmeldungen werden nur lokal gespeichert und nicht an andere Personen gesendet.
- Die Erinnerung an fehlende Essensstimmen ist eine lokale Übersicht und versendet keine Benachrichtigung.
- Der aktive Spieler wird in der Demo manuell ausgewählt; es gibt keine Anmeldung.
- Jedes Gruppenmitglied kann einen Abend lokal abschließen.
- Bewertungen sind dem ausgewählten Spieler zugeordnet und nicht anonym.
- Demo-Daten werden nur in einer vollständig leeren Debug-Datenbank angelegt.
- Der Release-Build ist technisch erzeugbar, benötigt für eine Veröffentlichung aber noch einen produktiven Signierschlüssel.

## Planung

Aufgaben, Akzeptanzkriterien und der aktuelle Entwicklungsstand stehen in [ITERATIVER_PROJEKTPLAN.md](ITERATIVER_PROJEKTPLAN.md).
