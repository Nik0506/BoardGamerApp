# BoardGamerApp – iterativer Projektplan

Dieses Dokument dient als Arbeitsgrundlage für die schrittweise Entwicklung der BoardGamerApp. Jede Iteration soll eine kleine, testbare und vorführbare Verbesserung liefern. Eine Iteration gilt erst als abgeschlossen, wenn ihre Akzeptanzkriterien erfüllt sind und die App weiterhin gebaut und gestartet werden kann.

## 1. Produktziel

Die BoardGamerApp unterstützt eine feste Gruppe bei der Organisation regelmäßiger Spieleabende. Sie zeigt den nächsten Termin und den Gastgeber, verwaltet Spielvorschläge und Abstimmungen und ermöglicht nach dem Abend eine Bewertung. Essensplanung, Bestellungen, Erinnerungen und geräteübergreifende Synchronisation sind Erweiterungen nach dem MVP.

## 2. Technischer Rahmen

Der aktuelle Projektstand verwendet:

- Kotlin
- Jetpack Compose und Material 3
- eine `MainActivity`
- Gradle Kotlin DSL und einen Version Catalog
- `minSdk 24`, `targetSdk 36`

Für dieses bestehende Projekt wird deshalb **Jetpack Compose beibehalten**. Die im ursprünglichen Entwurf genannten XML-Layouts, Fragments und RecyclerViews werden nicht parallel eingeführt. Compose-Listen übernehmen die Rolle einer RecyclerView, und die Navigation wird mit Navigation Compose umgesetzt.

Geplante Architektur:

```text
Compose UI → ViewModel → Repository → Room
                    ↘ DataStore (kleine Einstellungen)
```

Zunächst arbeitet die App vollständig lokal auf einem Gerät. Firebase oder eine andere Online-Synchronisation wird erst nach einem stabilen lokalen MVP bewertet.

## 3. Anforderungen und Prioritäten

### Muss – MVP

- [ ] Nächsten Spieleabend mit Datum, Uhrzeit, Gastgeber und Ort anzeigen
- [ ] Spieler verwalten und Gastgeber turnusmäßig bestimmen
- [ ] Brettspiele für einen Spieleabend vorschlagen
- [ ] Pro Spieler genau eine Stimme abgeben oder ändern
- [ ] Abstimmungsergebnis und Gewinner bestimmen
- [ ] Eine Verspätungsmeldung lokal erfassen und anzeigen
- [ ] Einen abgeschlossenen Abend bewerten
- [ ] Daten nach einem App-Neustart erhalten

### Soll

- [ ] Essensrichtung vorschlagen und darüber abstimmen
- [ ] Mehrheitsentscheidung anzeigen
- [ ] Restaurant und Link zur Speisekarte hinterlegen
- [ ] Persönliche Bestellung erfassen
- [ ] Bestellübersicht und Gesamtsumme für den Gastgeber anzeigen
- [ ] Lokale Erinnerungen über Android Notifications senden

### Kann

- [ ] Mehrere Geräte synchronisieren
- [ ] Benutzerkonten und Gruppen bereitstellen
- [ ] Push-Nachrichten an andere Gruppenmitglieder senden
- [ ] Spiele über eine externe Brettspiel-API suchen
- [ ] Daten exportieren oder teilen

## 4. Domänenmodell

Für das MVP werden folgende Modelle benötigt:

```text
Player
├── id
├── name
├── address
└── hostOrder

GameNight
├── id
├── startsAt
├── hostId
├── location
└── status: PLANNED | ACTIVE | FINISHED

BoardGame
├── id
├── name
├── description
├── suggestedByPlayerId
└── gameNightId

Vote
├── id
├── playerId
├── boardGameId
└── gameNightId

LateNotice
├── id
├── playerId
├── gameNightId
├── minutes
└── createdAt

Review
├── id
├── playerId
├── gameNightId
├── hostRating
├── foodRating
├── eveningRating
└── comment
```

Wichtige Regeln:

- `hostOrder` ist innerhalb einer Gruppe eindeutig.
- Der nächste Gastgeber folgt auf den letzten Gastgeber; nach dem letzten beginnt die Reihenfolge von vorn.
- Pro Kombination aus `playerId` und `gameNightId` darf höchstens eine Spielstimme existieren.
- Bewertungen liegen zwischen 1 und 5.
- Ein Gleichstand bei einer Abstimmung wird sichtbar angezeigt; die App erfindet keinen Gewinner.

## 5. Vorgesehene Projektstruktur

Die Struktur wird erst angelegt, wenn die jeweilige Schicht tatsächlich gebraucht wird.

```text
com.example.boardgamerapp
├── data
│   ├── local
│   │   ├── dao
│   │   ├── entity
│   │   └── AppDatabase.kt
│   └── repository
├── domain
│   └── model
├── ui
│   ├── dashboard
│   ├── games
│   ├── voting
│   ├── review
│   ├── food
│   ├── navigation
│   └── theme
└── MainActivity.kt
```

## 6. Definition of Done

Diese Kriterien gelten für jede Iteration:

- [ ] Die vereinbarten Akzeptanzkriterien sind erfüllt.
- [ ] Das Projekt lässt sich ohne Fehler bauen.
- [ ] Neue Geschäftslogik besitzt sinnvolle Unit-Tests.
- [ ] Der wichtigste neue UI-Ablauf wurde manuell oder per UI-Test geprüft.
- [ ] Leere Zustände und offensichtliche Fehleingaben sind behandelt.
- [ ] Es gibt keine neuen Warnungen, die bewusst ignoriert werden müssen.
- [ ] Dieses Dokument und gegebenenfalls die README entsprechen dem Stand.
- [ ] Die Iteration ergibt einen kleinen, nachvollziehbaren Commit.

## 7. Iterationen

### Iteration 0 – Projektbasis stabilisieren

**Ziel:** Die vorhandene Compose-Vorlage wird zu einer verlässlichen Ausgangsbasis.

Aufgaben:

- [x] App auf Emulator oder Gerät starten
- [x] App mit Gradle bauen
- [x] Platzhalter `Greeting` entfernen
- [x] App-Name, Paketname und sichtbare Texte prüfen
- [x] Drei vorläufige Ziele definieren: Termin, Spiele, Profil
- [x] Navigationszustand aus `MainActivity.kt` in ein eigenes UI-Paket verschieben
- [x] Basis für Unit- und Compose-UI-Tests prüfen

Akzeptanzkriterien:

- Die App startet ohne Absturz.
- Alle drei Hauptbereiche sind über die Navigation erreichbar.
- Jeder Bereich zeigt einen klar benannten Platzhalter.
- `./gradlew test` und `./gradlew assembleDebug` laufen erfolgreich.

Ergebnis/Demo: Eine navigierbare App-Hülle ohne fachliche Logik.

### Iteration 1 – Dashboard mit Beispieldaten

**Ziel:** Der nächste Spieleabend ist unmittelbar sichtbar.

Aufgaben:

- [ ] `Player` und `GameNight` als Domain-Modelle anlegen
- [ ] Einen In-Memory-Repository-Vertrag definieren
- [ ] Beispieldaten für Spieler und nächsten Abend bereitstellen
- [ ] Dashboard mit Datum, Uhrzeit, Gastgeber und Adresse gestalten
- [ ] Lade-, Leer- und Fehlerzustand darstellen
- [ ] `DashboardViewModel` mit unveränderlichem UI-State verwenden

Akzeptanzkriterien:

- Beim Öffnen erscheint der nächste Termin mit allen Kerndaten.
- Ohne zukünftigen Termin erscheint ein verständlicher Leerzustand.
- Die UI-Logik ist nicht in `MainActivity` abgelegt.

Ergebnis/Demo: Die erste vollständig sichtbare User Story, zunächst mit Beispieldaten.

### Iteration 2 – Spieler und Gastgeberrotation

**Ziel:** Die Reihenfolge der Gastgeber wird nachvollziehbar berechnet.

Aufgaben:

- [ ] Spielerliste anzeigen
- [ ] Spieler lokal hinzufügen und bearbeiten
- [ ] Gastgeberreihenfolge festlegen
- [ ] Rotation als reine, testbare Kotlin-Funktion implementieren
- [ ] Nächsten Termin anlegen und Gastgeber automatisch einsetzen
- [ ] Sonderfälle testen: keine Spieler, ein Spieler, Ende der Reihenfolge

Akzeptanzkriterien:

- Die Rotation liefert nach dem letzten wieder den ersten Spieler.
- Der berechnete Gastgeber erscheint auf dem Dashboard.
- Ungültige oder leere Namen können nicht gespeichert werden.

Ergebnis/Demo: Die Gruppe kann gepflegt und der nächste Gastgeber bestimmt werden.

### Iteration 3 – Spielvorschläge

**Ziel:** Gruppenmitglieder können Spiele für einen Termin vorschlagen.

Aufgaben:

- [ ] Spielvorschläge als Compose-Liste anzeigen
- [ ] Formular oder Dialog für Name und optionale Beschreibung bauen
- [ ] Vorschlag einem Spieler und Spieleabend zuordnen
- [ ] Validierung und Löschen eines eigenen Vorschlags ergänzen
- [ ] Leeren Zustand gestalten

Akzeptanzkriterien:

- Ein gültiger Vorschlag erscheint sofort in der Liste.
- Ein leerer Spielname wird nicht gespeichert.
- Zu jedem Vorschlag werden Urheber und zugehöriger Termin angezeigt.

Ergebnis/Demo: Spiele können vorgeschlagen und wieder entfernt werden.

### Iteration 4 – Spieleabstimmung

**Ziel:** Jeder Spieler kann einen Favoriten wählen und das Ergebnis sehen.

Aufgaben:

- [ ] Aktiven Spieler für die lokale Demo auswählbar machen
- [ ] Pro Spieler und Abend genau eine Stimme speichern
- [ ] Bereits abgegebene Stimme änderbar machen
- [ ] Stimmen zählen und sortiert darstellen
- [ ] Gewinner beziehungsweise Gleichstand anzeigen
- [ ] Zählung und Eindeutigkeitsregel testen

Akzeptanzkriterien:

- Ein Spieler kann nicht mehrere Stimmen gleichzeitig besitzen.
- Das Ändern einer Stimme korrigiert das Ergebnis sofort.
- Stimmenzahl, Beteiligung und Gleichstand werden korrekt angezeigt.

Ergebnis/Demo: Der zentrale MVP-Ablauf von Vorschlag bis Entscheidung funktioniert.

### Iteration 5 – Dauerhafte lokale Speicherung mit Room

**Ziel:** Alle bisherigen Daten überstehen App-Neustarts.

Aufgaben:

- [ ] Room-Abhängigkeiten im Version Catalog ergänzen
- [ ] Entities, DAOs und `AppDatabase` implementieren
- [ ] Beziehungen und eindeutige Indizes definieren
- [ ] In-Memory-Repository durch Room-Repository ersetzen
- [ ] Seed-Daten nur für Debug/Demo kontrolliert bereitstellen
- [ ] DAO- und Migrationstests anlegen

Akzeptanzkriterien:

- Spieler, Termine, Vorschläge und Stimmen bleiben nach einem Neustart erhalten.
- Die Datenbank verhindert eine zweite Stimme desselben Spielers für denselben Abend.
- Für Schemaänderungen besteht eine dokumentierte Migrationsstrategie.

Ergebnis/Demo: Ein vollständiges, lokal persistentes MVP.

### Iteration 6 – Verspätungsmeldung

**Ziel:** Eine Verspätung kann schnell für den aktuellen Abend gemeldet werden.

Aufgaben:

- [ ] Schnellaktion auf dem Dashboard ergänzen
- [ ] Auswahl für 10, 20, 30 oder freie Minuten anbieten
- [ ] Meldung mit Spieler und Zeitpunkt speichern
- [ ] Aktuelle Meldungen auf dem Dashboard anzeigen
- [ ] Klar kennzeichnen, dass Meldungen im lokalen MVP nur simuliert werden

Akzeptanzkriterien:

- Eine Meldung benötigt höchstens wenige Interaktionen.
- Ungültige Minutenwerte werden abgewiesen.
- Alle lokal erfassten Meldungen zum Termin sind sichtbar.

Ergebnis/Demo: Die User Story ist lokal nutzbar, ohne eine echte Nachrichtenzustellung vorzutäuschen.

### Iteration 7 – Abschluss und Bewertung

**Ziel:** Ein beendeter Spieleabend kann bewertet werden.

Aufgaben:

- [ ] Statuswechsel zu `FINISHED` ermöglichen
- [ ] Bewertung für Gastgeber, Essen und Gesamtabend anbieten
- [ ] Optionales Kommentarfeld ergänzen
- [ ] Doppelte Bewertung pro Spieler und Abend verhindern
- [ ] Durchschnittswerte anzeigen

Akzeptanzkriterien:

- Nur abgeschlossene Abende können bewertet werden.
- Alle Pflichtwerte liegen zwischen 1 und 5.
- Pro Spieler und Abend existiert höchstens eine Bewertung.
- Durchschnittswerte werden korrekt berechnet.

Ergebnis/Demo: Der komplette Lebenszyklus eines Spieleabends ist abgebildet.

### Iteration 8 – MVP-Qualität und Abgabe

**Ziel:** Das MVP ist robust, verständlich und präsentierbar.

Aufgaben:

- [ ] Eingabevalidierung und Fehlermeldungen vereinheitlichen
- [ ] Accessibility prüfen: Beschreibungen, Kontrast, Touch-Ziele, Schriftgrößen
- [ ] Kleine und große Displays sowie Hoch-/Querformat prüfen
- [ ] Tests für kritische Abläufe vervollständigen
- [ ] Demo-Daten und einen reproduzierbaren Vorführablauf vorbereiten
- [ ] README um Setup, Architektur, Funktionen und Grenzen ergänzen
- [ ] Release-Build erzeugen

Akzeptanzkriterien:

- Alle Muss-Anforderungen sind demonstrierbar.
- Ein frischer Checkout kann anhand der README gebaut werden.
- Bekannte Einschränkungen sind dokumentiert.
- Es gibt keine bekannten Abstürze im vorgesehenen Demo-Ablauf.

Ergebnis/Demo: Abgabefähige lokale Version der BoardGamerApp.

## 8. Erweiterungsiterationen nach dem MVP

### Iteration 9 – Essensabstimmung

- [ ] Kategorien verwalten
- [ ] Genau eine Stimme pro Spieler speichern
- [ ] Ergebnis und Gleichstand anzeigen
- [ ] Lokale Erinnerung an noch fehlende Stimmen planen

### Iteration 10 – Restaurant und Bestellungen

- [ ] Restaurantname und Menü-Link durch Gastgeber hinterlegen
- [ ] Bestellung mit Gericht, Hinweis und Preis erfassen
- [ ] Bestellungen gruppiert anzeigen
- [ ] Gesamtsumme zuverlässig als Dezimal-/Cent-Wert berechnen

### Iteration 11 – Mehrere Geräte

Vor Beginn dieser Iteration muss eine Architekturentscheidung dokumentiert werden. Zu klären sind Authentifizierung, Gruppenmitgliedschaft, Datenschutz, Konfliktauflösung, Offline-Verhalten und Betriebskosten. Erst danach wird Firebase oder eine Alternative ausgewählt.

- [ ] Backend-Optionen vergleichen und Entscheidung festhalten
- [ ] Benutzer- und Gruppenkonzept definieren
- [ ] Room als Offline-Cache bewerten
- [ ] Synchronisation implementieren
- [ ] Verspätungen als echte Push-Nachrichten versenden

## 9. Teststrategie

| Ebene | Was wird getestet? | Beispiele |
|---|---|---|
| Unit-Test | Reine Geschäftslogik | Gastgeberrotation, Stimmenzählung, Gleichstand, Durchschnitt |
| Repository-/DAO-Test | Speicherung und Constraints | eindeutige Stimme, Beziehungen, Löschen, Migration |
| ViewModel-Test | UI-State und Aktionen | Laden, leerer Zustand, Validierungsfehler |
| Compose-UI-Test | Kritische Nutzerabläufe | Vorschlag anlegen, abstimmen, Bewertung speichern |
| Manueller Test | Darstellung und Geräteeigenschaften | Rotation, große Schrift, Dark Mode, Zurück-Navigation |

Mindestens vor Abschluss jeder Iteration ausführen:

```bash
./gradlew test
./gradlew assembleDebug
```

Instrumentierte Tests werden zusätzlich auf einem Emulator oder Gerät ausgeführt, sobald entsprechende Tests vorhanden sind.

## 10. Entscheidungen, Risiken und offene Fragen

### Bereits entschieden

- Kotlin bleibt die Programmiersprache.
- Das vorhandene Compose-Projekt wird weiterverwendet.
- Das MVP funktioniert zunächst vollständig lokal.
- Room ist die geplante persistente Datenquelle.

### Vor Umsetzung zu entscheiden

- [ ] Wie wird in der lokalen Version der aktive Spieler gewählt?
- [ ] Darf nur der Gastgeber einen Abend abschließen?
- [ ] Darf eine Stimme bis zum Abschluss beliebig geändert werden?
- [ ] Wie wird bei Gleichstand endgültig entschieden?
- [ ] Sollen Bewertungen anonym oder namentlich sein?

### Hauptrisiken

- Eine frühe Online-Synchronisation vergrößert Umfang und Fehlerfläche deutlich.
- Ein Wechsel zwischen Compose und XML erzeugt doppelte UI-Strukturen.
- Zu viele Funktionen vor einem stabilen MVP gefährden die Abgabe.
- Datums-, Zeit- und Geldwerte dürfen nicht als unstrukturierte Texte modelliert werden.

## 11. Arbeitsprotokoll

Für jede begonnene Iteration wird dieser Block kopiert und ausgefüllt:

```markdown
### Iteration X – Titel

- Startdatum:
- Abschlussdatum:
- Verantwortlich:
- Ziel:
- Geplante Aufgaben:
  - [ ] ...
- Tatsächlich umgesetzt:
  - [ ] ...
- Ausgeführte Tests:
  - [ ] `./gradlew test`
  - [ ] `./gradlew assembleDebug`
- Offene Fehler/Schulden:
- Getroffene Entscheidungen:
- Nächster konkreter Schritt:
```

## 12. Aktueller Stand und nächster Schritt

**Iteration 0 – Projektbasis stabilisieren** wurde am 24.08.2026 abgeschlossen und am 26.08.2026 bestätigt. Unit-Tests und Debug-Build waren erfolgreich; der Debug-Build wurde auf einem Emulator installiert und ohne Absturz kalt gestartet.

**Iteration 1 – Dashboard mit Beispieldaten** wartet auf ausdrückliche Freigabe. Bis dahin werden keine Aufgaben dieser Iteration begonnen. Nach der Freigabe wird sie vollständig abgeschlossen, bevor Room oder optionale Funktionen eingeführt werden.
