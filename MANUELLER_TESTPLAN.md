# Leitfaden für den manuellen Test (MS4 – #24)

Dieser Testplan dient der strukturierten manuellen Überprüfung der **BoardGamerApp** auf echten Testgeräten oder Emulatoren. Er deckt alle Kernfunktionen der App sowie die serverseitigen Sicherheitsregeln (**Firebase Rulings – #49**) ab.

---

## 1. Testvoraussetzungen & Setup
- **Geräte**: Mindestens zwei Android-Geräte oder Emulatoren (z. B. Gerät A und Gerät B)
- **Konten**: Zwei separate Testkonten:
  - **Benutzer 1 (Host/Admin)**: z. B. `host@test.local` / Name: "Max Host"
  - **Benutzer 2 (Mitglied)**: z. B. `member@test.local` / Name: "Erika Member"
  - *(Optional)* **Benutzer 3 (Fremdnutzer)**: `external@test.local` (gehört einer anderen Gruppe an)

---

## 2. Testfälle: Authentifizierung & Profil

| ID | Testfall | Schritte | Erwartetes Ergebnis | Status |
|:---|:---|:---|:---|:---|
| **AUTH-1** | Registrierung | 1. App starten<br>2. Auf "Registrieren" wechseln<br>3. Name, Adresse, E-Mail und Passwort eingeben<br>4. "Registrieren" drücken | Konto wird angelegt, Benutzer landet auf der Gruppenübersicht / Hauptseite. Profil in Firestore angelegt. | [ ] |
| **AUTH-2** | Validierung | Ungültige E-Mail oder zu kurzes Passwort (< 6 Zeichen) eingeben. | Fehlermeldung wird unter dem Formular angezeigt; Registrierung wird blockiert. | [ ] |
| **AUTH-3** | Abmelden & Anmelden | 1. Profil-Tab öffnen<br>2. "Abmelden" tippen<br>3. Mit bestehenden Anmeldedaten wieder anmelden | Benutzer wird ausgeloggt (Login-Maske erscheint) und nach Login erfolgreich wieder mit seinem Profil geladen. | [ ] |
| **AUTH-4** | State Restoration bei Rotation | Text in Eingabefelder eingeben, Gerät ins Querformat drehen. | Eingaben bleiben erhalten; kein Absturz. | [ ] |

---

## 3. Testfälle: Gruppenverwaltung & Isolation (Rulings #49)

| ID | Testfall | Schritte | Erwartetes Ergebnis | Status |
|:---|:---|:---|:---|:---|
| **GRP-1** | Gruppe erstellen | 1. Im Gruppen-Tab Gruppennamen eingeben (z. B. "Freitagsrunde")<br>2. "Gruppe erstellen" tippen | Gruppe wird erstellt, Ersteller ist automatisch HOST/ADMIN, Gruppen-ID wird angezeigt. | [ ] |
| **GRP-2** | Gruppe beitreten | 1. Mit Benutzer 2 auf Gerät B einloggen<br>2. Gruppen-ID von Gruppe 1 eingeben<br>3. "Gruppe beitreten" tippen | Benutzer 2 wird der Gruppe hinzugefügt und erscheint in der Mitgliederliste. | [ ] |
| **GRP-3** | Mandantenisolation (Rulings) | Benutzer 3 erstellt eine eigene separate Gruppe "Sonntagsrunde". | Benutzer 3 sieht **nur** seine eigene Gruppe und Termine; Daten von "Freitagsrunde" sind für ihn unzugänglich (`PERMISSION_DENIED`). | [ ] |
| **GRP-4** | Profilschutz (Rulings) | Benutzer 2 versucht das Profil von Benutzer 1 abzufragen/zu ändern. | Änderungen an fremden Profilen werden serverseitig abgewiesen (`isUser(userId)`-Regel). | [ ] |

---

## 4. Testfälle: Spieleabend-Organisation (Dashboard)

| ID | Testfall | Schritte | Erwartetes Ergebnis | Status |
|:---|:---|:---|:---|:---|
| **DASH-1** | Spieleabend planen | Auf "Nächsten Spieleabend planen" tippen, Datum & Gastgeber wählen. | Neuer Termin wird für die Gruppe angelegt; alle Mitglieder sehen den Termin live. | [ ] |
| **DASH-2** | Termin editieren | Über das 3-Punkte-Menü "Spieleabend editieren" wählen, Uhrzeit und Datum anpassen. | Änderungen werden sofort gespeichert und auf allen Geräten aktualisiert. | [ ] |
| **DASH-3** | Teilnahme zusagen (RSVP) | Button "Ich bin dabei" drücken. | Status des aktuellen Benutzers wechselt zu "Dabei" (grünes Badge). | [ ] |
| **DASH-4** | Verspätung melden | "Status melden" $\rightarrow$ "Verspätung" (10, 20 oder 30 Min.) wählen. | Badge wechselt auf Verspätung mit Minutenangabe; Meldung erscheint in der Übersicht. | [ ] |
| **DASH-5** | Absage als Teilnehmer | "Status melden" $\rightarrow$ "Absage" mit Begründung absenden. | Status wechselt zu "Abgesagt" (rotes Badge). | [ ] |
| **DASH-6** | Gastgeber-Absage (3 Optionen) | Als Gastgeber "Status melden" $\rightarrow$ "Absage" wählen:<br>a) Ersatz-Gastgeber bestimmen<br>b) Termin verschieben<br>c) Komplett absagen | Je nach Wahl: Gastgeberrolle wechselt / Termin verschiebt sich / Termin wird als ABGESAGT markiert. | [ ] |

---

## 5. Testfälle: Essensabstimmung & Bestellungen (Food)

| ID | Testfall | Schritte | Erwartetes Ergebnis | Status |
|:---|:---|:---|:---|:---|
| **FOOD-1** | Kategorie hinzufügen & wählen | 1. "Kategorie hinzufügen" (z. B. "Griechisch")<br>2. Chip antippen zur Stimmabgabe | Kategorie erscheint; Stimme wird gewertet und Führender berechnet. | [ ] |
| **FOOD-2** | Restaurant festlegen | Als Gastgeber Restaurantname und Menü-Link eintragen und speichern. | Restaurant-Info wird für alle Mitglieder in der Gruppe angezeigt. | [ ] |
| **FOOD-3** | Eigene Bestellung erfassen | 1. "Bestellung hinzufügen/bearbeiten"<br>2. Gericht ("Gyros Teller") und Preis ("12,50") eingeben | Bestellung erscheint in der Liste; Gesamtsumme aktualisiert sich korrekt. | [ ] |
| **FOOD-4** | Fremdschutz (Rulings) | Benutzer 2 versucht die Bestellung von Benutzer 1 zu löschen. | Jeder Nutzer sieht nur den "Löschen"-Button für seine eigene Bestellung; serverseitig geschützt. | [ ] |

---

## 6. Testfälle: Spielvorschläge & Voting (Games)

| ID | Testfall | Schritte | Erwartetes Ergebnis | Status |
|:---|:---|:---|:---|:---|
| **GAME-1** | Spiel vorschlagen | "Spiel vorschlagen" tippen, Titel ("Terraforming Mars") und Beschreibung eingeben. | Spielkarte erscheint in der Liste mit Erstellernamen. | [ ] |
| **GAME-2** | Stimme abgeben | Vorschlagskarte antippen. | Stimme wird gezählt; Führendes Spiel wird im Header hervorgehoben. | [ ] |
| **GAME-3** | Löschen nur durch Ersteller | 1. Eigener Vorschlag: Lösch-Icon sichtbar und funktional.<br>2. Fremder Vorschlag: Kein Lösch-Icon vorhanden. | Nur der Ersteller kann seinen eigenen Vorschlag zurückziehen. | [ ] |

---

## 7. Testfälle: Abschluss & Bewertung (Review)

| ID | Testfall | Schritte | Erwartetes Ergebnis | Status |
|:---|:---|:---|:---|:---|
| **REV-1** | Spieleabend abschließen | "Spieleabend abschließen" tippen. | Status wechselt zu ABGESCHLOSSEN; Bewertungsformular wird freigeschaltet. | [ ] |
| **REV-2** | Bewertung abgeben | 1. "Bewertung abgeben" tippen<br>2. Sterne/Punkte (1–5) für Gastgeber, Essen und Gesamtabend vergeben<br>3. Kommentar hinzufügen und speichern | Bewertung wird gespeichert; Notendurchschnitte werden berechnet; erneute Bewertung ist gesperrt. | [ ] |
| **REV-3** | Rulings-Gültigkeit | Ungültige Punktwerte manipulieren. | Firebase Rules erzwingen, dass `hostRating`, `foodRating` und `eveningRating` zwischen 1 und 5 liegen. | [ ] |

---

## 8. Testfälle: Responsive Design & Ausrichtung

| ID | Testfall | Schritte | Erwartetes Ergebnis | Status |
|:---|:---|:---|:---|:---|
| **RESP-1** | Querformat (Landscape) | Gerät während geöffnetem Dialog (z. B. Statusmeldung oder Bestellung) ins Querformat drehen. | Dialoginhalt ist vollständig vertikal scrollbar; Aktionsbuttons ("Speichern", "Abbrechen") sind erreichbar. | [ ] |
| **RESP-2** | Tablet / Großes Display | App auf Tablet oder im Querformat öffnen. | Hauptnavigation schaltet automatisch auf die seitliche `NavigationRail` um; Karten bleiben angenehm zentriert. | [ ] |
