# 🔷 THW Login

Eine moderne, benutzerfreundliche Android-App zum schnellen Scannen, Verwalten und Vorzeigen von THW-Barcodes (z. B. für Dienstzeiterfassung, Anmeldungen oder Zeiterfassungssysteme).

![Android SDK](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Version](https://img.shields.io/badge/Version-v2.0.1-003399?style=for-the-badge)

---

## 🚀 Funktionen

- 📸 **Kamera-Barcode-Scanner**: Schnelles Einscannen von Barcodes mit integriertem ZXing-Scanner.
- ↕️ **Stufenlos verschiebbar**: Der Barcode kann per Drag-and-Drop flexibel auf dem Bildschirm verschoben werden (bis ganz an den oberen Rand), um optimal an Scanner-Gegebenheiten angepasst zu werden.
- 🔒 **Position & Drehung sperren**:
  - *Position fixieren*: Verhindert versehentliches Verrutschen beim Anfassen des Smartphones.
  - *Drehung sperren*: Fixiert das Hochformat für problemloses Einscannen an stationären Lesegeräten.
- 💧 **Liquid Glass UI**: Elegantes Glaskunst-Design auf Basis von Jetpack Compose mit flüssigen Übergängen und Animationen.
- 🌙 **Dunkel- & Hellmodus**: Dynamisches Theme, das nach Belieben zwischen Dark Mode und Light Mode umgeschaltet werden kann.
- 🔄 **In-App Updater**: Automatische und manuelle Prüfung auf neue Updates direkt über die GitHub Releases API inklusive Direkt-Download.
- 🐛 **Fehler & Feedback**: Integrierter Button zur direkten Meldung von Problemen oder Verbesserungsvorschlägen auf GitHub.

---

## 📱 Screenshots

| Dunkelmodus (Barcode fixiert) | Menü & Einstellungen | Hellmodus |
| :---: | :---: | :---: |
| *(Screenshot hier einfügen)* | *(Screenshot hier einfügen)* | *(Screenshot hier einfügen)* |

---

## 📥 Installation

### APK direkt herunterladen
1. Lade die aktuellste `.apk`-Datei unter [GitHub Releases](https://github.com/TheGamePRO112/THWLogin/releases) herunter.
2. Öffne die heruntergeladene Datei auf deinem Android-Gerät und bestätige die Installation (ggf. „Installation aus unbekannten Quellen“ erlauben).

### Systemvoraussetzungen
- **Android OS**: Android 8.0 (API Level 26) oder neuer
- **Berechtigungen**: Kamera (nur zum Scannen von Barcodes)

---

## 🛠 Tech Stack & Architektur

- **Programmiersprache**: [Kotlin](https://kotlinlang.org/) (JVM 21 Target)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) mit Material 3
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
