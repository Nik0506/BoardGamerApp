# BoardGamerApp

Eine Android-App zur Organisation regelmäßiger Brettspielabende. Das Projekt wird iterativ entwickelt; der aktuelle Stand umfasst sowohl das lokale MVP als auch den Start einer gruppenfähigen Mehrgeräte-Architektur mit Firebase.

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
- Restaurantname und Menü-Link durch den Gastgeber hinterlegen
- Eigene Bestellung mit Gericht, optionalem Hinweis und Preis erfassen oder ändern
- Bestellungen nach Personen anzeigen und die Gesamtsumme centgenau berechnen
- Kontrollierte Demo-Daten beim ersten Start einer leeren Datenbank
- Gemeinsames, austauschbares Repository mit Room als produktiver Datenquelle
- ViewModel-basierte Zustandsverwaltung
- Firebase-Setup für Authentication und Firestore
- E-Mail/Passwort-Registrierung und Login in der App
- Nutzerprofil- Speicherung in Firestore unter `/users/{uid}`

Die Kernabläufe von Terminplanung, Gastgeberrotation, Spiel- und Essensabstimmung, Restaurant- und Bestellplanung, lokaler Verspätungsmeldung und Bewertung funktionieren. Ein Spieleabend kann lokal von jedem Gruppenmitglied abgeschlossen und danach namentlich bewertet werden. Beim ersten Start wird eine leere Datenbank kontrolliert mit Demo-Daten befüllt; vorhandene Daten werden nicht überschrieben. Die Datenbank verwendet Schema-Version 5. Die Erweiterungen werden über die expliziten Migrationen `1 -> 2`, `2 -> 3`, `3 -> 4` und `4 -> 5` ergänzt; es gibt keine destructive fallback migration.

Im Mehrgeräte-Teil ist die Grundlage für MS2-1 umgesetzt: Firebase Authentication und Firestore sind integriert, und ein Benutzerprofil wird in Firestore gespeichert. Die lokale App zeigt einen Auth-Gate an, bevor die lokale App-Navigation sichtbar wird.

## Technik

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel
- Room / SQLite
- Firebase Authentication
- Firestore
- Gradle Kotlin DSL
- `minSdk 24`, `targetSdk 36`

## Projekt bauen und testen

### Voraussetzungen

- Android Studio mit Android SDK 36.1
- JDK 21; das mit Android Studio gebündete JDK kann verwendet werden
- Firebase-Projekt mit Authentication und Firestore
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
5. Als Gastgeber Restaurant und Menü-Link hinterlegen; anschließend für beide Spieler Bestellungen erfassen und die Gesamtsumme prüfen.
6. Unter **Bewertung** den Spieleabend abschließen.
7. Für Max und Lea unterschiedliche Bewertungen samt optionalem Kommentar speichern.
8. Die angezeigten Durchschnittswerte prüfen, die App vollständig schließen und erneut öffnen.
9. Kontrollieren, dass Spieler, Stimmen, Restaurant, Bestellungen, Meldungen, Abschlussstatus und Bewertungen erhalten geblieben sind.
10. Im neuen Mehrgeräte-Teil: registrieren oder anmelden, dann prüfen, dass der Nutzer in Firestore unter `/users/{uid}` gespeichert wurde.

## Architektur

Die Compose-Oberfläche kommuniziert über ViewModels und Repository-Verträge mit den Daten. Die produktive lokale Implementierung verwendet Room; für Compose-Previews und isolierte Tests steht weiterhin das In-Memory-Repository zur Verfügung. Firebase Authentication und Firestore ergänzen das lokale MVP jetzt um eine Multi-User-/Gruppenarchitektur.

```text
Compose UI → ViewModel → Repository → Room / SQLite
                                      ↘ In-Memory (Preview/Test)

Firebase Auth / Firestore → User- und Gruppenmodell für Mehrgeräte-Umsetzung
```

Die Domain-Modelle bleiben unabhängig von Room. Datenbank-Entities werden im Repository in Domain-Modelle umgewandelt. Spielstimmen, Essensstimmen, Bestellungen und Bewertungen besitzen jeweils einen eindeutigen Index aus Spieler und Spieleabend. Preise werden als ganze Centbeträge gespeichert und erst zur Anzeige formatiert. Restaurant und Essenskategorien gehören zu genau einem Abend. Bewertungen sind nur nach dem Statuswechsel zu `FINISHED` zulässig und müssen in allen drei Pflichtkategorien zwischen 1 und 5 liegen.

Die Mehrgeräte-Architektur beginnt mit einer klaren Trennung zwischen lokaler App-Logik und Firestore-User-/Gruppenmodell. Das lokale `Player`-Modell wird in einer späteren Iteration in ein Cloud-/Gruppenmodell überführt; im aktuellen Stand ist der Nutzer-Login und das Anlegen des Nutzerprofils in Firestore erfüllt.

## Grenzen des lokalen MVP und der aktuellen Mehrgeräte-Phase

- Alle bisherigen Daten liegen ausschließlich auf einem Gerät; die Cloud-Lösung erweitert die App erst schrittweise.
- Verspätungsmeldungen werden weiterhin lokal gespeichert und nicht an andere Personen gesendet.
- Die Erinnerung an fehlende Essensstimmen ist eine lokale Übersicht und versendet keine Benachrichtigung.
- Der aktive Spieler wird in der Demo manuell ausgewählt; es gibt noch keine echte Gruppen- oder Nutzerauswahl via Cloud.
- Jedes Gruppenmitglied kann einen Abend lokal abschließen.
- Bewertungen sind dem ausgewählten Spieler zugeordnet und nicht anonym.
- Demo-Daten werden nur in einer vollständig leeren Debug-Datenbank angelegt.
- Die Cloud-Variante ist noch nicht vollständig auf Gruppen- und Sync-Logik erweitert.
- Der Release-Build ist technisch erzeugbar, benötigt für eine Veröffentlichung aber noch einen produktiven Signierschlüssel.

## Planung

Aufgaben, Akzeptanzkriterien und der aktuelle Entwicklungsstand stehen in [ITERATIVER_PROJEKTPLAN.md](ITERATIVER_PROJEKTPLAN.md).
