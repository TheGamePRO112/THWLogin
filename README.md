# 🔷 THW Login v2.0.5

Eine moderne, benutzerfreundliche Android-App zum schnellen Scannen, Verwalten und Vorzeigen von THW-Barcodes sowie zur persönlichen Dienstzeiterfassung.

![Android SDK](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Version](https://img.shields.io/badge/Version-v2.0.5-0055FF?style=for-the-badge)

---

## 🚀 Funktionen & Highlights

### ⏱️ Dienstzeiterfassung & Stundenzähler
- **Ein- / Ausloggen**: Starte und beende deinen Dienst mit nur einem Tippen.
- **Live-Timer**: Zeigt bei aktivem Dienst sekundengenau die verstrichene Zeit (`HH:mm:ss`) mit optischem Status-Indikator an.
- **Dauerhafte Statusleisten-Benachrichtigung**: Ongoing Notification mit Live-Chronometer und direktem **„Dienst beenden“**-Aktionsbutton.
- **Stunden-Kalender & Monatsübersicht**: 
  - Übersichtliche Auflistung aller erfassten Dienstzeiten inklusive Gesamtsumme.
  - Interaktive Monatsraster-Ansicht mit Tagesfilter, Navigation und Stundensummen pro Tag.
  - Nachträgliches Erfassen, Bearbeiten und Löschen von Dienstzeiten inklusive Datumsauswahl (Calendar Picker).
- **Datenschutz-Hinweis**: Die erfasste Zeiten werden rein lokal auf dem Gerät zur persönlichen Übersicht gespeichert und es findet keine Online-Synchronisation statt.

### 📸 Barcode & Kamera-Scanner
- **Echter CODE_128 Barcode**: Generierung und Anzeige des Barcodes mit hoher Schärfe.
- **Stufenlos verschiebbar**: Vertikale Drag-and-Drop Positionierung ohne Layout-Sprünge.
- **Position & Drehung sperren**: Fixiert die Barcode-Position oder die Bildschirmausrichtung.
- **Optimierter Kamera-Scanner**: Eigene Scanner-Ansicht (`CustomCaptureActivity`) mit kontinuierlichem Autofokus, dunklem Vignette-Maskenoverlay, schwebendem Hinweistext und iOS-Abbrechen-Button.
- **Manuelle Eingabe**: Ziffern- & Bindestrich-Tastatur zur manuellen Eingabe der Barcode-Nummer (z. B. `12345678-99`).

### 📱 Home-Screen Widget
- **Freistehend & Transparent**: Transparentes Widget direkt auf dem Android-Startbildschirm.
- **90° Rotation**: Über das App-Menü stufenlos um 90° drehbar (Horizontal & Vertikal).

### 🔄 Automatische GitHub Update-Prüfung
- Prüft beim App-Start automatisch über die GitHub API (`User-Agent`-Header) auf neue Releases.
- Manueller Update-Check im Hilfe-Dialog mit direktem APK-Download.

---

## 📱 Screenshots

### App & Zeiterfassung
| Dunkelmodus (iOS Slate-Navy) | Menü & Einstellungen | Hellmodus (Porcelain) |
| :---: | :---: | :---: |
| <img width="400" alt="Screenshot_2026-09-28-16-06-25-563_com example thwlogin" src="https://github.com/user-attachments/assets/61911870-fcd7-41d2-9bd7-cee86667d5a1" /> | <img width="400" alt="Screenshot_2026-09-28-16-06-41-481_com example thwlogin" src="https://github.com/user-attachments/assets/8ab0e965-4e9c-4e18-bae8-1cb3e5395c7f" /> | <img width="400" alt="Screenshot_2026-09-28-16-06-33-200_com example thwlogin" src="https://github.com/user-attachments/assets/1600fbf0-6dca-4307-a06e-2e79fb4b1dfb" /> |

---

## 📥 Installation

### APK direkt herunterladen
1. Lade die aktuellste `.apk`-Datei unter [GitHub Releases](https://github.com/TheGamePRO112/THWLogin/releases) herunter.
2. Öffne die heruntergeladene Datei auf deinem Android-Gerät und bestätige die Installation.

### 🛡️ Hinweis zu Google Play Protect (Unbekannte Quellen)
Da die App direkt als APK-Datei heruntergeladen wird und nicht aus dem Google Play Store stammt, blendet Google Play Protect beim ersten Installieren möglicherweise ein Warnfenster ein:

1. Klicke im Warnfenster auf **„Weitere Details“** (siehe Screenshot 1).
2. Klicke anschließend unten auf **„Trotzdem installieren“** (siehe Screenshot 2).

| 1. Weitere Details anklicken | 2. Trotzdem installieren wählen |
| :---: | :---: |
| <img width="200" alt="Screenshot_2026-09-28-16-46-14-366_com android vending" src="https://github.com/user-attachments/assets/8caa24aa-f7bd-4732-9f22-8a254f6702cd" /> | <img width="200" alt="Screenshot_2026-09-28-16-46-17-503_com android vending" src="https://github.com/user-attachments/assets/b8e911a9-5f9e-4867-966b-889412d23f5e" /> |

---

### Systemvoraussetzungen
- **Android OS**: Android 8.0 (API Level 26) oder neuer
- **Berechtigungen**: Kamera (optional zum Barcode-Scannen), Benachrichtigungen (optional für Live-Chronometer)

---

## 🛠 Tech Stack & Architektur

- **Programmiersprache**: [Kotlin](https://kotlinlang.org/) (JVM 21 Target)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) mit Material 3 & Custom iOS Design System
- **Barcode Engine**: [ZXing Android Embedded](https://github.com/zxing-android-embedded/zxing-android-embedded)
- **Asynchronität**: Kotlin Coroutines
- **Build System**: Gradle (Kotlin DSL)
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35 (Android 15)

---

## 💬 Feedback & Beitragen

Du hast einen Fehler gefunden oder möchtest eine neue Funktion vorschlagen?
- [Neues Issue erstellen](https://github.com/TheGamePRO112/THWLogin/issues/new)
- Pull Requests sind nach Absprache herzlich willkommen!

---

## ⚖️ Lizenz & Rechtlicher Hinweis

Dieses Projekt ist eine private Entwicklung zur Unterstützung von Helferinnen und Helfern des Technischen Hilfswerks (THW). Es steht in keinem offiziellen Verhältnis zur Bundesanstalt Technisches Hilfswerk, sofern nicht anders angegeben.
