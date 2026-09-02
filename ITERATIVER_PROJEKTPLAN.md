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

### MS2 – Iteration 4: Online-Robustheit & Asynchrone Architektur

Ziel: Online-Fehler werden in jedem Bereich verständlich, nicht blockierend und reproduzierbar behandelt. Alle Firestore-Aufrufe sind asynchron und suspendierend ohne blockierende Aufrufe.

- [x] Zentralen Netzwerk-Monitor (`LiveNetworkMonitor` / `ConnectivityManager`) mit Live-Flow einführen
- [x] Globales Offline-Banner in Compose anzeigen, wenn keine Internetverbindung vorhanden ist
- [x] Alle synchronen `runBlocking`-Aufrufe aus dem Repository entfernen
- [x] Sämtliche Repository-Schnittstellen (`BoardGamerRepository` u. a.) auf echte `suspend`-Funktionen umstellen
- [x] Asynchrone Firestore-Aufrufe mit Coroutines und `.await()` umsetzen
- [x] Nicht blockierende UI und konsistente Fehlerzustände mit Wiederholen-Aktionen sicherstellen
- [x] Firestore-Sicherheitsregeln (`firestore.rules`) im Repository versionieren
- [x] Unit- und Compose-Tests für asynchrone Repository-Aufrufe und Netzwerkzustände aktualisieren

Akzeptanzkriterien:

- Ohne Netzwerk zeigt jeder Datenbereich eine klare Meldung mit Wiederholen-Aktion und ein Offline-Banner.
- Die Oberfläche bleibt während Firebase-Aufrufen vollständig flüssig und bedienbar.
- Schreibfehler verändern den sichtbaren Erfolgszustand nicht.
- Sicherheitsregeln verhindern gruppenfremde Lese- und Schreibzugriffe.

### MS2 – Iteration 5: Benachrichtigungen und echte Spielstatusmeldungen

Ziel: Gruppenmitglieder können für den kommenden Spieleabend zusagen (anwesend/pünktlich), sich verspäten oder mit Grund absagen. Die Statusmeldungen sind für alle Gruppenmitglieder in Echtzeit sichtbar und lösen Benachrichtigungen aus.

- [x] Teilnahmemodell (`AttendanceStatusType`: `ATTENDING`, `LATE`, `DECLINED`, `PENDING`) und `GameNightAttendance` einführen
- [x] `AttendanceRepository` Schnittstelle mit `getAttendances()` und `setAttendance()` definieren und in `FirebaseGameNightRepository` umsetzen
- [x] Firestore-Subcollection `/groups/{groupId}/gameNights/{docId}/attendance/{uid}` und `firestore.rules` anbinden
- [x] FCM-Token-Verwaltung (`saveFcmToken`) im Firestore-Profil hinterlegen
- [x] Interaktiven Teilnahmestatus-Bereich im Dashboard integrieren („Zusagen“, „Verspäten“, „Absagen“)
- [x] Absagedialog mit optionaler Angabe eines Absagegrunds bereitstellen
- [x] Übersicht über den Gruppen-Teilnahmestatus mit Zähler-Zusammenfassung und Status-Badges aller Mitglieder anzeigen
- [x] Automatische Push- und In-App-Benachrichtigungen bei Statusänderungen auslösen
- [x] Unit- und Compose-Tests für Teilnahmestatus, Absagedialog und Benachrichtigungen bereitstellen

Akzeptanzkriterien:

- Ein Gruppenmitglied kann für den nächsten Spieleabend zusagen, sich verspäten oder mit Grund absagen.
- Alle Gruppenmitglieder sehen die aktuellen Statusmeldungen und den Zählerstand im Dashboard.
- Statusänderungen lösen Benachrichtigungen aus.
- Nicht berechtigte Nutzer können keine Statusmeldungen anderer Nutzer ändern.

### MS3 – Absage Gastgeber

Ziel: Sagt der aktuelle Gastgeber eines Spieleabends ab, stehen drei geordnete Handlungsoptionen zur Verfügung, um den Spieleabend für die Gruppe zu organisieren (Alternativer Gastgeber, Spieleabend verschieben, Spieleabend absagen).

- [x] Erkennung, ob der angemeldete Nutzer der aktuelle Gastgeber des Spieleabends ist (`isHost`)
- [x] Option 1 („Alternativer Gastgeber“): Neues Mitglied aus der Gruppe auswählen, Gastgeberschaft und Location auf neue Adresse übertragen, bisherigen Host als `DECLINED` markieren, Gruppe benachrichtigen
- [x] Option 2 („Spieleabend verschieben“): Neues Datum/Uhrzeit festlegen, bisherige Zu-/Absagen der Teilnehmer zurücksetzen, Gruppe über den neuen Termin benachrichtigen
- [x] Option 3 („Spieleabend absagen“): Spieleabend mit optionaler Begründung stornieren (`GameNightStatus.CANCELLED`), aus anstehenden Terminen entfernen, Gruppe benachrichtigen
- [x] Einstieg über „Status melden“ -> „Absage“ sowie direkt über das Optionsmenü („Als Gastgeber absagen“)
- [x] Unit- und Compose-Tests für alle drei Optionen implementieren

Akzeptanzkriterien:

- Der aktuelle Gastgeber erhält bei einer Absage die drei Handlungsoptionen.
- Nicht-Gastgeber können weiterhin nur eine reguläre persönliche Absage erteilen.
- Jede Option führt die zugehörigen Datenänderungen und Gruppen-Benachrichtigungen durch.

## 7. Teststrategie

| Ebene | Zweck |
|---|---|
| Unit-Test | UI-Mapping, asynchrone Coroutines, RSVP-/Teilnahmelogik, Validierungs-/Berechnungslogik |
| Compose-Test | Formulare, Navigation, Zustandswiederherstellung, Offline-Banner, RSVP-Aktionen und Dialoge |
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

- Firebase-Tests dürfen keine produktiven Gruppen- oder Benutzerdaten verändern.
- Stabile numerische IDs werden teilweise aus Firebase-Dokument-IDs abgeleitet; langfristig sollten Domain-IDs als Strings modelliert werden.
- Direkte Pushes zwischen Geräten im Hintergrund erfordern serverseitige Auslösung (z. B. Firebase Cloud Functions); clientseitige und In-App-Benachrichtigungen sind vollständig aktiv.

### MS4 – Unit-Tests (#20)

Ziel: Umfassende Unit-Test-Abdeckung aller Domain-Modelle, Datenstrukturen, Berechnungslogiken, Zustandsübergänge und Validierungen.

- [x] JaCoCo-Testabdeckungsbericht in Gradle integriert (`createDebugUnitTestCoverageReport`)
- [x] Domain-Modelle getestet (`Attendance`, `GameNight`, `Player`, `BoardGame`, `FoodCategory`, `Restaurant`, `FoodOrder`, `Review`, `Vote`, `LateNotice`)
- [x] Datenmodelle & Firestore-Serialisierung getestet (`UserProfile`, `Group`, `GroupMember`, `VotingSnapshot`, `FoodVotingSnapshot`, `OrderingSnapshot`, `ReviewSnapshot`)
- [x] Dashboard-Logik getestet (`isHost`, `currentAttendance`, Zähler-Aggregationen, `recentNotices`-Sortierung)
- [x] Essens-Logik & Berechnungen getestet (Gleichstand, Führender, Leerstand, Cent-/Preiseingabe-Validierung, Erinnerung fehlender Wähler)
- [x] Spiele-Logik & Abstimmungen getestet (Ergebnis-Text, UI-Mapping mit `isSelected`, Gruppenberechtigungen, Stimmabgabe)
- [x] Bewertungs-Logik getestet (Durchschnittswerte-Formatierung, Punkte-Validierung 1–5, Spieleabend-Abschluss)
- [x] `CoroutineDispatcher`-Injection in allen ViewModels für deterministisches Testen ohne blockierende Threads
- [x] Behebung eines latenten `ClassCastException`-Fehlers in `ReviewViewModel`

### MS4 – Repository-/DAO-Test (#21)

Ziel: Umfassende Tests der Daten- und Repository-Schicht nach der Migration von Room (DAO) auf Firebase Firestore.

- [x] MockK-Testbibliothek in Gradle integriert (`io.mockk:mockk:1.13.10`)
- [x] In-Memory-Testdouble [`FakeBoardGamerRepository`](file:///home/red/Documents/BoardGamerApp/app/src/test/java/com/example/boardgamerapp/fake/FakeBoardGamerRepository.kt) angelegt (vollständige Implementierung von `BoardGamerRepository`)
- [x] [`BoardGamerRepositoryTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/test/java/com/example/boardgamerapp/BoardGamerRepositoryTest.kt): Vertrags- und Verhaltentests für alle Repository-Bereiche (Terminplanung, Reschedule, Reassign Host, Absagen, Anwesenheiten, Verspätungen, Spielvorschläge, Stimmabgabe, Essensabstimmung, Bestellungen, Bewertungen)
- [x] [`FirebaseGameNightRepositoryTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/test/java/com/example/boardgamerapp/FirebaseGameNightRepositoryTest.kt): Tests für Berechtigungsprüfungen (`requireCurrentPlayer`), unautorisierte Zugriffe bei fehlender Authentifizierung und `addPlayer`-Validierung
- [x] [`AuthAndGroupRepositoryTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/test/java/com/example/boardgamerapp/AuthAndGroupRepositoryTest.kt): Tests für `AuthRepository` (Status, Logout, Delegation) und `GroupRepository` (Auth-Validierung bei Gruppenerstellung)

### MS4 – ViewModel-Test (#22)

Ziel: Dedizierte, vollumfängliche ViewModel-Test-Suites für alle 4 ViewModels der Applikation (`DashboardViewModel`, `FoodViewModel`, `GamesViewModel`, `ReviewViewModel`) über alle Benutzeraktionen, Zustandsübergänge und Fehlerpfade.

- [x] [`DashboardViewModelTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/test/java/com/example/boardgamerapp/DashboardViewModelTest.kt) (41 Tests): Initiales Laden, Empty-State, Terminauswahl, RSVP-Zusagen/Absagen, Termin-Editor, Gastgeber-Absage (Verschieben, Neuer Gastgeber, Absagen), Push-Benachrichtigungen, `planNextGameNight`, benutzerdefinierte Verspätungsminuten, `clearMessage` und Fehlerszenarien. Abdeckung: **83.4 %**.
- [x] [`FoodViewModelTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/test/java/com/example/boardgamerapp/FoodViewModelTest.kt) (12 Tests): Initiales Laden, Restaurant-Editor (Rechteprüfung: nur Gastgeber darf bearbeiten), Bestell-Editor (Preisformatierung & Cent-Validierung, Prepopulation, Löschen), Essenskategorien anlegen/löschen, Essensstimme abgeben, Ergebnis-Texte (Führender, Gleichstand, leer), Erinnerung fehlender Wähler, ViewModel-Factory. Abdeckung: **97.1 %**.
- [x] [`GamesViewModelTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/test/java/com/example/boardgamerapp/GamesViewModelTest.kt) (10 Tests): Spielvorschläge laden, Fehlerbehandlung bei Netzwerkausfall, Spielvorschlag einreichen (Gruppenmitgliedschafts- & Terminprüfung), Spielvorschlag löschen (Rechteprüfung: nur Ersteller), Stimmabgabe (`castVote`), dynamische Abstimmungsergebnisse, UI-Mapping mit `isSelected`, `clearMessage`, ViewModel-Factory. Abdeckung: **96.4 %**.
- [x] [`ReviewViewModelTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/test/java/com/example/boardgamerapp/ReviewViewModelTest.kt) (8 Tests): Bewertungs-Übersicht, Empty- & Error-State, Spieleabend abschließen (`finishGameNight`), Bewertungsberechtigung (nur bei abgeschlossenem Abend und noch nicht bewertet), Sterne-/Punkte-Validierung (1–5 in allen Kategorien), Speichern von Reviews mit Erfolgsmeldung, `clearMessage`, ViewModel-Factory. Abdeckung: **94.6 %**.

### MS4 – Compose-UI-Test (#23)

Ziel: Umfassende Compose-UI-Tests (`createComposeRule`) für alle Screens und interaktiven UI-Komponenten der Applikation (`DashboardScreen`, `FoodScreen`, `GamesScreen`, `PlayersScreen`, `ReviewScreen`, `AuthStateRestoration`).

- [x] [`FoodScreenTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/androidTest/java/com/example/boardgamerapp/FoodScreenTest.kt) (4 UI-Tests): Essensabstimmung & Stimmabgabe, Kategorie-Hinzufügen-Dialog & Texterfassung, Restaurant-Verwaltung (Rechteprüfung: nur Gastgeber darf bearbeiten), Bestell-Editor & Löschen eigener Bestellungen.
- [x] [`GamesScreenTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/androidTest/java/com/example/boardgamerapp/GamesScreenTest.kt) (4 UI-Tests): Vorschlagsliste & Stimmabgabe, Spiel-vorschlagen-Dialog & Texteingabe, Löschberechtigung (nur Ersteller sieht „Löschen“-Button), Empty-State-Anzeige bei fehlenden Vorschlägen.
- [x] [`PlayersScreenTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/androidTest/java/com/example/boardgamerapp/PlayersScreenTest.kt) (4 UI-Tests): Spieler- und Gruppenübersicht mit Gruppen-ID und Kopier-Button, Reihenfolgen-Verschiebung (Up/Down-Events), Spieler-Editor-Dialog mit Speichern, Button „Nächsten Spieleabend planen“.
- [x] [`DashboardScreenTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/androidTest/java/com/example/boardgamerapp/DashboardScreenTest.kt) (5 UI-Tests): Optionsmenü & Termin-Editier-Dialog, RSVP-Zusagen & Status melden, 3-Optionen-Gastgeberabsage, Terminwechsler (`GameNightPicker`) & Empty-State-Planungsaktion.
- [x] [`ReviewScreenTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/androidTest/java/com/example/boardgamerapp/ReviewScreenTest.kt): Spieleabend abschließen und Bewertungsdialog mit Sternen/Punkten öffnen.
- [x] [`AuthStateRestorationTest.kt`](file:///home/red/Documents/BoardGamerApp/app/src/androidTest/java/com/example/boardgamerapp/AuthStateRestorationTest.kt): State-Restoration / Rotationstest für Texteingabefelder.

## 9. Nächster Schritt

Nach erfolgreicher Umsetzung von **MS4 – Compose-UI-Test** folgen: **Manueller Test (#24)**, **Responsive Design (#41)** und **Firebase Rulings (#49)**.
