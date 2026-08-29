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

- [x] Nächsten Spieleabend mit Datum, Uhrzeit, Gastgeber und Ort anzeigen
- [x] Spieler verwalten und Gastgeber turnusmäßig bestimmen
- [x] Brettspiele für einen Spieleabend vorschlagen
- [x] Pro Spieler genau eine Stimme abgeben oder ändern
- [x] Abstimmungsergebnis und Gewinner bestimmen
- [x] Eine Verspätungsmeldung lokal erfassen und anzeigen
- [x] Einen abgeschlossenen Abend bewerten
- [x] Daten nach einem App-Neustart erhalten

### Soll

- [x] Essensrichtung vorschlagen und darüber abstimmen
- [x] Mehrheitsentscheidung anzeigen
- [x] Restaurant und Link zur Speisekarte hinterlegen
- [x] Persönliche Bestellung erfassen
- [x] Bestellübersicht und Gesamtsumme für den Gastgeber anzeigen
- [x] Lokale Erinnerungen über Android Notifications senden

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

- [x] `Player` und `GameNight` als Domain-Modelle anlegen
- [x] Einen In-Memory-Repository-Vertrag definieren
- [x] Beispieldaten für Spieler und nächsten Abend bereitstellen
- [x] Dashboard mit Datum, Uhrzeit, Gastgeber und Adresse gestalten
- [x] Lade-, Leer- und Fehlerzustand darstellen
- [x] `DashboardViewModel` mit unveränderlichem UI-State verwenden

Akzeptanzkriterien:

- Beim Öffnen erscheint der nächste Termin mit allen Kerndaten.
- Ohne zukünftigen Termin erscheint ein verständlicher Leerzustand.
- Die UI-Logik ist nicht in `MainActivity` abgelegt.

Ergebnis/Demo: Die erste vollständig sichtbare User Story, zunächst mit Beispieldaten.

### Iteration 2 – Spieler und Gastgeberrotation

**Ziel:** Die Reihenfolge der Gastgeber wird nachvollziehbar berechnet.

Aufgaben:

- [x] Spielerliste anzeigen
- [x] Spieler lokal hinzufügen und bearbeiten
- [x] Gastgeberreihenfolge festlegen
- [x] Rotation als reine, testbare Kotlin-Funktion implementieren
- [x] Nächsten Termin anlegen und Gastgeber automatisch einsetzen
- [x] Sonderfälle testen: keine Spieler, ein Spieler, Ende der Reihenfolge

Akzeptanzkriterien:

- Die Rotation liefert nach dem letzten wieder den ersten Spieler.
- Der berechnete Gastgeber erscheint auf dem Dashboard.
- Ungültige oder leere Namen können nicht gespeichert werden.

Ergebnis/Demo: Die Gruppe kann gepflegt und der nächste Gastgeber bestimmt werden.

### Iteration 3 – Spielvorschläge

**Ziel:** Gruppenmitglieder können Spiele für einen Termin vorschlagen.

Aufgaben:

- [x] Spielvorschläge als Compose-Liste anzeigen
- [x] Formular oder Dialog für Name und optionale Beschreibung bauen
- [x] Vorschlag einem Spieler und Spieleabend zuordnen
- [x] Validierung und Löschen eines eigenen Vorschlags ergänzen
- [x] Leeren Zustand gestalten

Akzeptanzkriterien:

- Ein gültiger Vorschlag erscheint sofort in der Liste.
- Ein leerer Spielname wird nicht gespeichert.
- Zu jedem Vorschlag werden Urheber und zugehöriger Termin angezeigt.

Ergebnis/Demo: Spiele können vorgeschlagen und wieder entfernt werden.

### Iteration 4 – Spieleabstimmung

**Ziel:** Jeder Spieler kann einen Favoriten wählen und das Ergebnis sehen.

Aufgaben:

- [x] Aktiven Spieler für die lokale Demo auswählbar machen
- [x] Pro Spieler und Abend genau eine Stimme speichern
- [x] Bereits abgegebene Stimme änderbar machen
- [x] Stimmen zählen und sortiert darstellen
- [x] Gewinner beziehungsweise Gleichstand anzeigen
- [x] Zählung und Eindeutigkeitsregel testen

Akzeptanzkriterien:

- Ein Spieler kann nicht mehrere Stimmen gleichzeitig besitzen.
- Das Ändern einer Stimme korrigiert das Ergebnis sofort.
- Stimmenzahl, Beteiligung und Gleichstand werden korrekt angezeigt.

Ergebnis/Demo: Der zentrale MVP-Ablauf von Vorschlag bis Entscheidung funktioniert.

### Iteration 5 – Dauerhafte lokale Speicherung mit Room

**Ziel:** Alle bisherigen Daten überstehen App-Neustarts.

Aufgaben:

- [x] Room-Abhängigkeiten im Version Catalog ergänzen
- [x] Entities, DAOs und `AppDatabase` implementieren
- [x] Beziehungen und eindeutige Indizes definieren
- [x] In-Memory-Repository durch Room-Repository ersetzen
- [x] Seed-Daten nur für Debug/Demo kontrolliert bereitstellen
- [x] DAO- und Migrationstests anlegen

Akzeptanzkriterien:

- [x] Spieler, Termine, Vorschläge und Stimmen bleiben nach einem Neustart erhalten.
- [x] Die Datenbank verhindert eine zweite Stimme desselben Spielers für denselben Abend.
- [x] Für Schemaänderungen besteht eine dokumentierte Migrationsstrategie.

Ergebnis/Demo: Ein vollständiges, lokal persistentes MVP.

### Iteration 6 – Verspätungsmeldung

**Ziel:** Eine Verspätung kann schnell für den aktuellen Abend gemeldet werden.

Aufgaben:

- [x] Schnellaktion auf dem Dashboard ergänzen
- [x] Auswahl für 10, 20, 30 oder freie Minuten anbieten
- [x] Meldung mit Spieler und Zeitpunkt speichern
- [x] Aktuelle Meldungen auf dem Dashboard anzeigen
- [x] Klar kennzeichnen, dass Meldungen im lokalen MVP nur simuliert werden

Akzeptanzkriterien:

- [x] Eine Meldung benötigt höchstens wenige Interaktionen.
- [x] Ungültige Minutenwerte werden abgewiesen.
- [x] Alle lokal erfassten Meldungen zum Termin sind sichtbar.

Ergebnis/Demo: Die User Story ist lokal nutzbar, ohne eine echte Nachrichtenzustellung vorzutäuschen.

### Iteration 7 – Abschluss und Bewertung

**Ziel:** Ein beendeter Spieleabend kann bewertet werden.

Aufgaben:

- [x] Statuswechsel zu `FINISHED` ermöglichen
- [x] Bewertung für Gastgeber, Essen und Gesamtabend anbieten
- [x] Optionales Kommentarfeld ergänzen
- [x] Doppelte Bewertung pro Spieler und Abend verhindern
- [x] Durchschnittswerte anzeigen

Akzeptanzkriterien:

- Nur abgeschlossene Abende können bewertet werden.
- Alle Pflichtwerte liegen zwischen 1 und 5.
- Pro Spieler und Abend existiert höchstens eine Bewertung.
- Durchschnittswerte werden korrekt berechnet.

Ergebnis/Demo: Der komplette Lebenszyklus eines Spieleabends ist abgebildet.

### Iteration 8 – MVP-Qualität und Abgabe

**Ziel:** Das MVP ist robust, verständlich und präsentierbar.

Aufgaben:

- [x] Eingabevalidierung und Fehlermeldungen vereinheitlichen
- [x] Accessibility prüfen: Beschreibungen, Kontrast, Touch-Ziele, Schriftgrößen
- [x] Kleine und große Displays sowie Hoch-/Querformat prüfen
- [x] Tests für kritische Abläufe vervollständigen
- [x] Demo-Daten und einen reproduzierbaren Vorführablauf vorbereiten
- [x] README um Setup, Architektur, Funktionen und Grenzen ergänzen
- [x] Release-Build erzeugen

Akzeptanzkriterien:

- Alle Muss-Anforderungen sind demonstrierbar.
- Ein frischer Checkout kann anhand der README gebaut werden.
- Bekannte Einschränkungen sind dokumentiert.
- Es gibt keine bekannten Abstürze im vorgesehenen Demo-Ablauf.

Ergebnis/Demo: Abgabefähige lokale Version der BoardGamerApp.

## 8. Meilenstein 1 – Lokales MVP und lokale Produktreife

Der erste Meilenstein umfasst die bisherige, lokal laufende Produktentwicklung der BoardGamerApp. Ziel ist ein stabiler MVP mit funktionaler Gruppenlogik auf einem Gerät, ohne externe Infrastruktur.

### Iteration 0 – Projektbasis stabilisieren

- [x] App auf Emulator oder Gerät starten
- [x] App mit Gradle bauen
- [x] Platzhalter `Greeting` entfernen
- [x] App-Name, Paketname und sichtbare Texte prüfen
- [x] Drei vorläufige Ziele definieren: Termin, Spiele, Profil
- [x] Navigationszustand aus `MainActivity.kt` in ein eigenes UI-Paket verschieben
- [x] Basis für Unit- und Compose-UI-Tests prüfen

### Iteration 1 – Dashboard mit Beispieldaten

- [x] `Player` und `GameNight` als Domain-Modelle anlegen
- [x] Einen In-Memory-Repository-Vertrag definieren
- [x] Beispieldaten für Spieler und nächsten Abend bereitstellen
- [x] Dashboard mit Datum, Uhrzeit, Gastgeber und Adresse gestalten
- [x] Lade-, Leer- und Fehlerzustand darstellen
- [x] `DashboardViewModel` mit unveränderlichem UI-State verwenden

### Iteration 2 – Spieler und Gastgeberrotation

- [x] Spielerliste anzeigen
- [x] Spieler lokal hinzufügen und bearbeiten
- [x] Gastgeberreihenfolge festlegen
- [x] Rotation als reine, testbare Kotlin-Funktion implementieren
- [x] Nächsten Termin anlegen und Gastgeber automatisch einsetzen
- [x] Sonderfälle testen: keine Spieler, ein Spieler, Ende der Reihenfolge

### Iteration 3 – Spielvorschläge

- [x] Spielvorschläge als Compose-Liste anzeigen
- [x] Formular oder Dialog für Name und optionale Beschreibung bauen
- [x] Vorschlag einem Spieler und Spieleabend zuordnen
- [x] Validierung und Löschen eines eigenen Vorschlags ergänzen
- [x] Leeren Zustand gestalten

### Iteration 4 – Spieleabstimmung

- [x] Aktiven Spieler für die lokale Demo auswählbar machen
- [x] Pro Spieler und Abend genau eine Stimme speichern
- [x] Bereits abgegebene Stimme änderbar machen
- [x] Stimmen zählen und sortiert darstellen
- [x] Gewinner beziehungsweise Gleichstand anzeigen
- [x] Zählung und Eindeutigkeitsregel testen

### Iteration 5 – Dauerhafte lokale Speicherung mit Room

- [x] Room-Abhängigkeiten im Version Catalog ergänzen
- [x] Entities, DAOs und `AppDatabase` implementieren
- [x] Beziehungen und eindeutige Indizes definieren
- [x] In-Memory-Repository durch Room-Repository ersetzen
- [x] Seed-Daten nur für Debug/Demo kontrolliert bereitstellen
- [x] DAO- und Migrationstests anlegen

### Iteration 6 – Verspätungsmeldung

- [x] Schnellaktion auf dem Dashboard ergänzen
- [x] Auswahl für 10, 20, 30 oder freie Minuten anbieten
- [x] Meldung mit Spieler und Zeitpunkt speichern
- [x] Aktuelle Meldungen auf dem Dashboard anzeigen
- [x] Klar kennzeichnen, dass Meldungen im lokalen MVP nur simuliert werden

### Iteration 7 – Abschluss und Bewertung

- [x] Statuswechsel zu `FINISHED` ermöglichen
- [x] Bewertung für Gastgeber, Essen und Gesamtabend anbieten
- [x] Optionales Kommentarfeld ergänzen
- [x] Doppelte Bewertung pro Spieler und Abend verhindern
- [x] Durchschnittswerte anzeigen

### Iteration 8 – MVP-Qualität und Abgabe

- [x] Eingabevalidierung und Fehlermeldungen vereinheitlichen
- [x] Accessibility prüfen: Beschreibungen, Kontrast, Touch-Ziele, Schriftgrößen
- [x] Kleine und große Displays sowie Hoch-/Querformat prüfen
- [x] Tests für kritische Abläufe vervollständigen
- [x] Demo-Daten und einen reproduzierbaren Vorführablauf vorbereiten
- [x] README um Setup, Architektur, Funktionen und Grenzen ergänzen
- [x] Release-Build erzeugen

### Iteration 9 – Essensabstimmung

- [x] Kategorien verwalten
- [x] Genau eine Stimme pro Spieler speichern
- [x] Ergebnis und Gleichstand anzeigen
- [x] Lokale Erinnerung an noch fehlende Stimmen planen

### Iteration 10 – Restaurant und Bestellungen

- [x] Restaurantname und Menü-Link durch Gastgeber hinterlegen
- [x] Bestellung mit Gericht, Hinweis und Preis erfassen
- [x] Bestellungen gruppiert anzeigen
- [x] Gesamtsumme zuverlässig als Dezimal-/Cent-Wert berechnen

## 9. Meilenstein 2 – Mehrere Geräte und gemeinsame Gruppen

Im zweiten Meilenstein wird aus der lokalen App ein echtes Gruppenprodukt. Die bestehende Room-Architektur bleibt als lokaler Cache erhalten; darüber wird eine Cloud-basierte Synchronisationsschicht ergänzt.

Ziel: Mehrere Personen mit ihren Geräten sollen dieselbe Gruppe nutzen, Abende gemeinsam verwalten und Daten über mehrere Geräte hinweg konsistent sehen.

### Architekturentscheidung und Ziele

Vor der Umsetzung müssen die folgenden Grundentscheidungen dokumentiert werden:

- [ ] Welche Plattform wird genutzt: Firebase, Supabase, eigener Backend oder Hybridmodell?
- [ ] Wie werden Benutzer- und Gruppenkonzept modelliert?
- [ ] Wie werden Daten zwischen Room und Cloud synchronisiert?
- [ ] Wie werden Offline-Änderungen und Konflikte behandelt?
- [ ] Welche Daten bleiben lokal und welche werden zentral gespeichert?
- [ ] Welche Benachrichtigungen sind im MVP relevant?

### Mögliche technische Varianten

#### Variante A – Firebase

- [ ] Firebase Auth für Benutzerkonto und Login
- [ ] Firestore oder Realtime Database für Gruppen- und Abenddaten
- [ ] Room als lokaler Cache und Offline-Store
- [ ] Synchronisation mit Cloud-Listenern oder Batch-Uploads
- [ ] Push-Benachrichtigungen für Verspätungen und Absagen

Vorteile: schnell umsetzbar, gut für Android-Ökosystem, wenig eigenes Backend nötig.

#### Variante B – Supabase

- [ ] Supabase Auth für Login und Nutzerverwaltung
- [ ] PostgreSQL mit Row Level Security für Gruppen und Zugriffe
- [ ] Realtime-Subscriptions für gemeinsame Updates
- [ ] Room als lokaler Cache für Offline-Nutzung
- [ ] Realtime- oder Webhook-basierte Benachrichtigungen

Vorteile: sehr gut für relationale Daten und Gruppenmodelle mit klarer Datenstruktur.

#### Variante C – Eigener Backend

- [ ] Authentifizierung, API-Design und Group-Management
- [ ] Postgres oder andere persistente Datenbank
- [ ] Synchronisations- und Conflict-Handling auf Serverseite
- [ ] Room für lokale Persistenz und Offline-Support
- [ ] Push-/Polling-Lösung für Benachrichtigungen

Vorteile: volle Kontrolle und hohe Flexibilität; Nachteil: höherer Aufwand und Betrieb.

### Mögliche Iterationen für Meilenstein 2

#### Iteration M2-1 – Architekturentscheidung und Datenmodell

- [ ] Zielarchitektur für Auth, Gruppen, Synchronisation und Offline-Verhalten dokumentieren
- [ ] Nutzer-, Gruppen- und Rollenmodell definieren
- [ ] Datenmodell für Abende, Spieler, Stimmen und Meldungen für mehrere Geräte erweitern
- [ ] Room- und Cloud-Modelle sauber trennen
- [ ] Datenschutz- und Sicherheitsanforderungen festlegen

Akzeptanzkriterien:
- Die gewählte Architektur ist dokumentiert und verständlich.
- Gruppen- und Benutzer-Identitäten sind im Datenmodell sauber abgebildet.
- Offline- und Sync-Anforderungen sind explizit berücksichtigt.

#### Iteration M2-2 – Authentifizierung und Gruppenverwaltung

- [ ] Login/Register/Logout implementieren
- [ ] Gruppen erstellen, beitreten, verlassen und verwalten
- [ ] Mitglieder- und Rechtekonzept definieren
- [ ] Spielerprofil oder Benutzerprofil im Kontext der Gruppe modellieren
- [ ] Gruppencode, Einladung oder Invite-Mechanismus ergänzen

Akzeptanzkriterien:
- Mehrere Personen können auf derselben Gruppe arbeiten.
- Zugriffsrechte sind klar und ohne Mehrdeutigkeit definiert.
- Ein Nutzer ist eindeutig einer Gruppe und seinen Daten zugeordnet.

#### Iteration M2-3 – Cloud-Synchronisation und Offline-Support

- [ ] Room als lokalem Cache erweitern
- [ ] Synchronisationslogik zwischen Device und Cloud definieren
- [ ] Daten für mehrere Geräte übermitteln und konsistent zusammenführen
- [ ] Konflikte bei gleichzeitigen Änderungen behandeln
- [ ] Wiederherstellung nach Verbindungsabbruch sicherstellen

Akzeptanzkriterien:
- Änderungen werden nach Wiederherstellung der Verbindung übernommen.
- Die App bleibt nutzbar, auch wenn keine Verbindung besteht.
- Konfliktfälle sind dokumentiert und nachvollziehbar gelöst.

#### Iteration M2-4 – Benachrichtigungen und echte Spielstatusmeldungen

- [ ] Verspätungen und Absagen als echte Gruppenmeldungen versenden
- [ ] Erinnerung oder Teilnahme-Status für Mitglieder anzeigen
- [ ] Benachrichtigungen in der App und optional per Push umsetzen
- [ ] Zustandslogik für „teilgenommen / verspätet / abgesagt“ vereinheitlichen
- [ ] UX-Reihenfolge und Validierung der Gruppenkommunikation prüfen

Akzeptanzkriterien:
- Gruppenmitglieder erhalten den Statuswechsel in nachvollziehbarer Form.
- Die Darstellung ist konsistent und nicht nur lokal simuliert.
- Die Funktionalität ist auf mehreren Geräten prüfbar.

## 10. Teststrategie

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

## 11. Entscheidungen, Risiken und offene Fragen

### Bereits entschieden

- Kotlin bleibt die Programmiersprache.
- Das vorhandene Compose-Projekt wird weiterverwendet.
- Das MVP funktioniert zunächst vollständig lokal.
- Room ist die geplante persistente Datenquelle.

### Vor Umsetzung zu entscheiden

- [ ] Wie wird in der lokalen Version der aktive Spieler gewählt?
- [x] Darf nur der Gastgeber einen Abend abschließen? Lokales MVP: Nein, jedes Gruppenmitglied darf abschließen.
- [ ] Darf eine Stimme bis zum Abschluss beliebig geändert werden?
- [ ] Wie wird bei Gleichstand endgültig entschieden?
- [x] Sollen Bewertungen anonym oder namentlich sein? Lokal wird die Bewertung einem Spieler zugeordnet.

### Hauptrisiken

- Eine frühe Online-Synchronisation vergrößert Umfang und Fehlerfläche deutlich.
- Ein Wechsel zwischen Compose und XML erzeugt doppelte UI-Strukturen.
- Zu viele Funktionen vor einem stabilen MVP gefährden die Abgabe.
- Datums-, Zeit- und Geldwerte dürfen nicht als unstrukturierte Texte modelliert werden.

## 12. Arbeitsprotokoll

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

## 13. Aktueller Stand und nächster Schritt

**Iteration 0 – Projektbasis stabilisieren** wurde am 24.08.2026 abgeschlossen und am 26.08.2026 bestätigt. Unit-Tests und Debug-Build waren erfolgreich; der Debug-Build wurde auf einem Emulator installiert und ohne Absturz kalt gestartet.

**Iteration 1 – Dashboard mit Beispieldaten** wurde am 26.08.2026 abgeschlossen. Alle acht Unit-Tests und der Debug-Build waren erfolgreich. Die APK wurde auf einem Emulator installiert und kalt gestartet; Datum, Uhrzeit, Gastgeber, Adresse und Navigation wurden in der UI-Hierarchie geprüft. Es wurden keine AndroidRuntime-Abstürze protokolliert.

**Iteration 2 – Spieler und Gastgeberrotation** wurde am 26.08.2026 abgeschlossen. Alle 19 Unit-Tests und der Debug-Build waren erfolgreich. Auf dem Emulator wurden Spielerliste, Änderung der Reihenfolge, Hinzufügen-Dialog und das Anlegen des Folgetermins geprüft. Die Rotation plante den 11.09.2026 bei Lea als nächste Gastgeberin; es wurden keine AndroidRuntime-Abstürze protokolliert.

**Iteration 3 – Spielvorschläge** wurde am 26.08.2026 abgeschlossen. Alle 25 Unit-Tests und der Debug-Build waren erfolgreich. Auf dem Emulator wurden Vorschlagsliste, Nutzerwahl, Löschberechtigung, Eingabedialog und das unmittelbare Hinzufügen von „Azul“ geprüft. Jeder Vorschlag zeigt Urheber und Termin; es wurden keine AndroidRuntime-Abstürze protokolliert.

**Iteration 4 – Spieleabstimmung** wurde am 27.08.2026 abgeschlossen. Alle 33 Unit-Tests und der Debug-Build waren erfolgreich. Auf dem Emulator wurden Gleichstand, Beteiligung, hervorgehobene eigene Stimme und der Stimmenwechsel von Max zu Heat geprüft. Nach dem Wechsel wurde Heat mit zwei Stimmen als Gewinner nach oben sortiert; es wurden keine AndroidRuntime-Abstürze protokolliert.

**Iteration 5 – Dauerhafte lokale Speicherung mit Room** wurde am 27.08.2026 umgesetzt. Room speichert Spieler, Spieleabende, Spielvorschläge und Stimmen; die Kombination aus Spieler und Spieleabend ist als eindeutiger Datenbankindex abgesichert. Demo-Daten werden nur beim ersten Start einer vollständig leeren Datenbank angelegt. Die lokalen Unit- und Build-Prüfungen waren erfolgreich; instrumentierte Room-Tests sind angelegt und für die Ausführung auf einem Emulator vorgesehen.

**Iteration 6 – Verspätungsmeldung** wurde am 27.08.2026 umgesetzt. Das Dashboard bietet eine lokale Schnellaktion mit Spielerwahl, den Minutenoptionen 10, 20, 30 sowie einer positiven freien Eingabe. Meldungen werden mit Spieler, kommendem Spieleabend und Erstellzeitpunkt in Room gespeichert und auf dem Dashboard angezeigt. Die simulierte lokale Funktion ist in der UI und README klar von echter Benachrichtigung abgegrenzt. Room verwendet Schema-Version 2 mit expliziter Migration 1→2; Unit- und instrumentierte Persistenz-/Validierungstests wurden ergänzt.

**Iteration 7 – Abschluss und Bewertung** wurde am 28.08.2026 abgeschlossen. Ein eigener Bewertungsbereich ermöglicht den Statuswechsel zu `FINISHED` und anschließend genau eine namentlich zugeordnete Bewertung pro Spieler und Abend. Gastgeber, Essen und Gesamtabend werden mit 1 bis 5 Punkten bewertet; Kommentare sind optional und Durchschnittswerte werden unmittelbar angezeigt. Room verwendet Schema-Version 3 mit expliziter Migration 2→3 und einem eindeutigen Index für Spieler und Spieleabend. Unit-Tests, Debug-Build und acht instrumentierte Tests auf dem Emulator waren erfolgreich; die Migration einer vorhandenen Installation und der neue Navigationsbereich wurden dort ebenfalls geprüft.

**Iteration 8 – MVP-Qualität und Abgabe** wurde am 28.08.2026 abgeschlossen. Core-Library-Desugaring stellt die verwendeten `java.time`-APIs nun auch für API 24 und 25 bereit. Android Lint meldet keine Fehler; ungenutzte Vorlagenressourcen und ein redundantes Manifest-Label wurden entfernt. Bewertungselemente besitzen verständliche Accessibility-Beschreibungen und passen sich horizontal an. Der kritische Ablauf vom Abschluss bis zum Bewertungsdialog wird durch einen Compose-UI-Test abgedeckt. README, Grenzen und reproduzierbarer Demo-Ablauf sind vollständig dokumentiert. Unit-Tests, neun instrumentierte Tests, Debug-Build, Lint und der unsignierte Release-Build waren erfolgreich; große Schrift sowie Hoch- und Querformat wurden auf dem Emulator geprüft.

**Iteration 9 – Essensabstimmung** wurde am 29.08.2026 abgeschlossen. Ein eigener Compose-Bereich ermöglicht das Verwalten von Essenskategorien und genau eine änderbare Stimme pro Spieler und Abend. Ergebnis, Gleichstand, Beteiligung und fehlende Stimmen werden unmittelbar angezeigt. Die Erinnerungsaktion bleibt bewusst lokal und behauptet keinen Versand. Room verwendet Schema-Version 4 mit expliziter Migration 3→4 und Fremdschlüsseln für Kategorien und Stimmen. Unit-, ViewModel- und instrumentierte Persistenztests decken Abstimmung, Stimmenwechsel, Gleichstand, Kategorien sowie fehlende Stimmen ab.

**Iteration 10 – Restaurant und Bestellungen** wurde am 29.08.2026 abgeschlossen. Der Gastgeber kann Restaurantname und einen validierten Menü-Link für den kommenden Abend hinterlegen. Jede Person kann ihre eigene Bestellung mit Gericht, optionalem Hinweis und Preis erfassen, ändern oder löschen. Die gruppierte Übersicht weist Bestellungen Personen zu und summiert Preise ohne Gleitkommafehler als ganze Centbeträge. Room verwendet Schema-Version 5 mit expliziter Migration 4→5 und eindeutigen Beziehungen für Restaurant und Bestellungen.

**Nächster Schritt:** Vor dem Start von Meilenstein 2 ist die Architekturentscheidung für Authentifizierung, Gruppenzugehörigkeit, Datenschutz, Offline-Verhalten, Synchronisation und Betriebskosten gemeinsam zu treffen und als Grundlage für die nächsten Iterationen festzuhalten.
