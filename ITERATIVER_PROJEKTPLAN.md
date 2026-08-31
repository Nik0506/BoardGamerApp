# Würfelrunde – iterativer Projektplan

Stand: 30.08.2026

Dieses Dokument beschreibt den tatsächlich gültigen Architekturstand und die nächsten Entwicklungsschritte. Abgeschlossene lokale Iterationen bleiben als Projekthistorie dokumentiert; ihre Room-Implementierung wurde nach der Firebase-Migration bewusst entfernt.

## 1. Produktziel

Würfelrunde unterstützt private Gruppen bei der gemeinsamen Organisation regelmäßiger Brettspielabende. Mitglieder arbeiten auf mehreren Geräten mit denselben Terminen, Abstimmungen, Meldungen, Essensentscheidungen, Bestellungen und Bewertungen.

## 2. Verbindliche Architekturentscheidungen

- Kotlin und Jetpack Compose bleiben die UI- und Programmiersprachenbasis.
- Firebase Authentication verwaltet Benutzerkonten.
- Cloud Firestore ist die einzige fachliche Datenquelle und Source of Truth.
- Eine Firebase-Identität entspricht einem Gruppenmitglied.
- Nutzer können mehreren Gruppen angehören; `activeGroupId` bestimmt die aktuell verwendete Gruppe.
- Gruppen besitzen die Rollen `HOST` und `MEMBER`.
- Room, SQLite, In-Memory-Produktdaten und lokale Demo-Daten werden nicht weitergeführt.
- Die App setzt eine Internetverbindung voraus. Es gibt keinen fachlichen Offline-Modus.
- Firestore-Persistenz auf dem Gerät ist deaktiviert; fehlgeschlagene Online-Vorgänge müssen sichtbar scheitern.
- Preise werden als ganze Centbeträge gespeichert.
- Navigation, Formulareingaben und ausgewählte Gruppen müssen Konfigurationsänderungen wie Bildschirmdrehungen überstehen.

```text
Compose UI → ViewModel → Repository → Firebase Auth / Cloud Firestore
```

## 3. Aktuelles Firestore-Modell

```text
/users/{uid}
  displayName, email, address, activeGroupId, createdAt, updatedAt
  /groups/{groupId}

/groups/{groupId}
  name, createdBy, memberOrder, createdAt, updatedAt
  /members/{uid}
  /gameNights/{gameNightId}
    /suggestions/{suggestionId}
    /votes/{playerId}
    /lateNotices/{noticeId}
    /reviews/{reviewId}
    /foodCategories/{categoryId}
    /foodVotes/{playerId}
    /restaurants/{restaurantId}
    /orders/{orderId}
```

## 4. Historischer Meilenstein 1 – lokaler MVP

Die Iterationen 0 bis 10 wurden zunächst lokal umgesetzt und dienten zur Erarbeitung der Produktlogik. Nach erfolgreicher Überführung nach Firebase sind Room-Entities, DAOs, Migrationen, das Room-Repository und das In-Memory-Repository nicht mehr Bestandteil der App.

- [x] Iteration 0 – Projektbasis
- [x] Iteration 1 – Dashboard
- [x] Iteration 2 – Spieler und Gastgeberrotation
- [x] Iteration 3 – Spielvorschläge
- [x] Iteration 4 – Spieleabstimmung
- [x] Iteration 5 – damalige lokale Persistenz
- [x] Iteration 6 – Verspätungsmeldung
- [x] Iteration 7 – Abschluss und Bewertung
- [x] Iteration 8 – MVP-Qualität
- [x] Iteration 9 – Essensabstimmung
- [x] Iteration 10 – Restaurant und Bestellungen

## 5. Meilenstein 2 – gemeinsames Online-Produkt

### Iteration M2-1 – Firebase-Grundlage und Datenmodell

- [x] Firebase-Projekt und Android-App verbinden
- [x] Firebase Authentication einrichten
- [x] Firestore als Source of Truth festlegen
- [x] Benutzer-, Gruppen- und Mitgliedsmodell definieren
- [x] Rollen `HOST` und `MEMBER` definieren
- [x] Gruppenbeitritt über Gruppen-ID definieren
- [x] Fachliche Unterstrukturen für Termine, Stimmen, Meldungen und Bestellungen definieren
- [x] Entscheidung gegen lokalen Offline-Speicher dokumentieren

### Iteration M2-2 – Authentifizierung und Gruppenverwaltung

- [x] Registrierung, Login und Logout integrieren
- [x] Nutzerprofile unter `/users/{uid}` speichern
- [x] Gruppen erstellen und per ID beitreten
- [x] Gruppen und Mitglieder anzeigen
- [x] Aktive Gruppe online im Nutzerprofil speichern
- [x] Mitgliederreihenfolge online speichern
- [x] Termine und bestehende MVP-Funktionen an die aktive Firestore-Gruppe anbinden
- [ ] Firestore-Sicherheitsregeln im Repository versionieren und automatisiert testen

### Konsistenz-Iteration – reine Online-Architektur

- [x] Markenname überall sichtbar auf „Würfelrunde“ vereinheitlichen
- [x] Room, SQLite, DAOs, Entities und Migrationen entfernen
- [x] In-Memory-Produktrepository und lokale Demo-Daten entfernen
- [x] Room-/KSP-Abhängigkeiten entfernen
- [x] Firebase-Repository ohne lokalen Fallback verdrahten
- [x] Aktive Gruppe eindeutig für alle Fachbereiche auswählen
- [x] Internet- und Netzwerkstatusberechtigung explizit deklarieren
- [x] Persistenten Firestore-Cache deaktivieren
- [x] Navigation und Eingabezustände für Bildschirmdrehung speicherbar machen
- [x] Persönliche Aktionen fest an das angemeldete Firebase-Konto binden
- [x] Manuelle Spielerauswahl für Meldungen, Vorschläge, Stimmen, Bestellungen und Bewertungen entfernen
- [x] Fremde Schreib- und Löschaufrufe zusätzlich im Firebase-Repository abweisen
- [x] Veraltete lokale Tests und ungenutzte UI-/Domain-Klassen entfernen
- [x] README und Projektplan auf den Online-Stand bringen
- [ ] Vollständige Firebase-Integration mit einem Testprojekt oder Emulator automatisieren

## 6. Meilenstein 3 – Spieleabend planen mit Optionen

### Iteration MS3-1 – Dialog für Termin und Gastgeber

- [x] Dialog für „Nächsten Spieleabend planen“ ergänzen
- [x] Bestätigen und Abbrechen im Dialog umsetzen
- [x] Standardtermin auf zwei Wochen in die Zukunft vordefinieren
- [x] Datum im Format DD.MM.YYYY anzeigen
- [x] Gastgeber aus vorhandenen Gruppenmitgliedern per Dropdown wählen
- [x] Reihenfolge nach bevorzugtem Gastgeber neu rotieren
- [x] Auswahl und Termin in Firestore als neuer Spieleabend speichern
- [x] Gruppen-ID als kopierbares Feld in der Detailansicht bereitstellen

Akzeptanzkriterien:

- Der Benutzer kann einen neuen Spieleabend mit Datum und Gastgeber in einem Dialog planen.
- Der gewählte Host wird vor der normalen Reihenfolge platziert und danach abgespeichert.
- Die Angabe ist in der Gruppe sofort nachvollziehbar und kann per Kopieren geteilt werden.

### Iteration MS3-2 – Spieleabend editieren

- [x] Optionsmenü auf der Seite des Spieleabends ergänzen
- [x] Editierenmaske mit Datumsänderung (DatePicker)
- [x] Editierenmaske mit Uhrzeitänderung (TimePicker)
- [x] Editierenmaske mit Gastgeber-Auswahl (Dropdown aller Gruppenmitglieder)
- [x] Speichern- und Abbrechen-Aktionen in der Maske umsetzen
- [x] Aktualisierung des Termins, des Gastgebers und der Adresse in Firestore
- [x] Push-Benachrichtigung über Änderungen an Spieleabenden auslösen
- [x] Erfolgs- und Fehlermeldungen in der Benutzeroberfläche anzeigen
- [x] Unit- und UI-Tests für den Editierablauf bereitstellen

Akzeptanzkriterien:

- Ein bestehender Spieleabend kann über das Optionsmenü editiert werden.
- Datum, Uhrzeit und Gastgeber können in einer Editierenmaske angepasst werden.
- Änderungen können gespeichert oder verworfen werden.
- Bei erfolgreicher Aktualisierung wird eine Push-Benachrichtigung ausgelöst und die UI synchronisiert.

### Iteration M2-3 – Online-Robustheit

Ziel: Online-Fehler werden in jedem Bereich verständlich, nicht blockierend und reproduzierbar behandelt.

- [ ] Netzwerkstatus zentral beobachten
- [ ] Einheitlichen Online-Fehlerzustand bereitstellen
- [ ] Alle blockierenden Firestore-Aufrufe auf suspendierende ViewModel-Coroutines umstellen
- [ ] Lade-, Retry- und Timeout-Verhalten vereinheitlichen
- [ ] Keine Schreiboperation bei fehlender Verbindung vortäuschen
- [ ] Firebase Emulator Suite für Repository-Integrationstests einrichten
- [ ] Firestore-Sicherheitsregeln versionieren und testen

Akzeptanzkriterien:

- Ohne Netzwerk zeigt jeder Datenbereich eine klare Meldung mit Wiederholen-Aktion.
- Die Oberfläche bleibt während Firebase-Aufrufen bedienbar.
- Schreibfehler verändern den sichtbaren Erfolgszustand nicht.
- Sicherheitsregeln verhindern gruppenfremde Lese- und Schreibzugriffe.

### Iteration M2-4 – echte Benachrichtigungen

- [ ] Firebase Cloud Messaging einrichten
- [ ] FCM-Token pro Nutzer verwalten
- [ ] Teilnahme, Verspätung und Absage als Gruppenstatus modellieren
- [ ] Push- und In-App-Benachrichtigungen umsetzen
- [ ] Berechtigungen und Datenschutz prüfen

## 7. Teststrategie

| Ebene | Zweck |
|---|---|
| Unit-Test | UI-Mapping und reine Validierungs-/Berechnungslogik |
| Compose-Test | Formulare, Navigation, Zustandswiederherstellung und kritische Bedienabläufe |
| Firebase-Integrationstest | Firestore-Struktur, Rechte, Gruppenisolation und Schreibregeln |
| Manueller Mehrgerätetest | Gleichzeitige Nutzung derselben Gruppe auf mindestens zwei Geräten |

Vor Abschluss jeder Iteration:

```bash
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

Zusätzlich sind Bildschirmdrehung, fehlende Netzwerkverbindung und ein zweites Benutzerkonto zu prüfen.

## 8. Risiken und offene Punkte

- Das derzeit synchrone Repository kann die Oberfläche während langsamer Netzaufrufe blockieren.
- Firestore-Sicherheitsregeln sind noch nicht im Repository versioniert und automatisiert geprüft.
- Firebase-Tests dürfen keine produktiven Gruppen- oder Benutzerdaten verändern.
- Stabile numerische IDs werden teilweise aus Firebase-Dokument-IDs abgeleitet; langfristig sollten Domain-IDs als Strings modelliert werden.
- Push-Benachrichtigungen benötigen neben der App auch eine vertrauenswürdige serverseitige Auslösung.

## 9. Nächster Schritt

Nach erfolgreicher technischer Prüfung der aktuellen MS3-Umsetzung folgt **Iteration M2-3 – Online-Robustheit**. Priorität haben nicht blockierende Firebase-Aufrufe, ein zentraler Verbindungszustand sowie versionierte und getestete Firestore-Sicherheitsregeln.
