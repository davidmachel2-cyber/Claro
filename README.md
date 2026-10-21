# FotoOrdner – Mein Speicher-Assistent (Android)

## Stand: erste Quellcode-Version 0.1

**Funktioniert als bewusst nicht-destruktiver Prototyp:** Deutsche Android-Oberfläche; Ordnerauswahl über den Android-Systemdialog; rekursives Lesen freigegebener Dateien; exakte Duplikatsuche mittels SHA-256; Liste identischer Dateien und theoretisch mehrfach belegten Speicherplatzes. Keine Uploads, keine Dateiveränderung. Die Scan-Auswertung erfolgt im Arbeitsthread.

**Noch nicht umgesetzt:** Ähnlichkeits-/KI-Erkennung, echte Papierkorb-Funktion, selektives Löschen, Cloud-Synchronisation (Google Fotos), Dateien-Vorschau, automatische Sortierung. Die Anzeige „theoretisch mehrfach belegt“ bedeutet **nicht**, dass dieser Platz bereits freigegeben wurde.

## Installation mit Android Studio

1. Android Studio auf einem Computer öffnen und diesen Projektordner als Projekt auswählen.
2. Android SDK 35 und JDK 17 installieren bzw. in Android Studio auswählen.
3. Gradle-Synchronisation durchführen (Internet für erstmalige Dependencies erforderlich; kompatible Gradle-Version 8.9).
4. Das Ulefone per USB-Debugging anschließen und in Android Studio „Run“ wählen, oder eine Debug-APK über „Build > Build APK(s)“ erzeugen und manuell installieren.
5. App öffnen, **Ordner auswählen** tippen und einen zugänglichen Ordner auswählen. Für weitere Bereiche erneut „Ordner auswählen“ verwenden; danach **Speicher prüfen**.

Die App fordert bewusst **keinen pauschalen Zugriff auf alle Dateien** an. Androids Dateiauswahl erlaubt nur freigegebene Bereiche. App-private Speicherbereiche, WhatsApp-Datenbanken und die vollständige Google-Fotos-Cloud sind nicht automatisch erreichbar. Bei sehr großen Ordnern dauert die erste SHA-256-Auswertung; große Datenmengen werden gelesen, aber nicht hochgeladen.

## Sicherheit

- Keine `INTERNET`-Berechtigung und keine Berechtigung zum Löschen.
- SHA-256 identifiziert bitgenau gleiche Inhalte (keine visuell ähnlichen Bilder).
- Auch identische Dateien können an unterschiedlichen Orten bewusst benötigt werden; prüfe sie später einzeln, bevor eine künftige Version Löschfunktionen erhält.
- Dieses Projekt wurde als Quellcode erzeugt, **noch nicht auf dem Ulefone gebaut oder getestet**.

## APK ohne eigenen PC mit GitHub Actions erstellen

In diesem Projekt liegt `.github/workflows/build-apk.yml`. Mit einem GitHub-Konto
kannst du die Dateien in ein **privates** neues Repository hochladen (einschließlich
verstecktem `.github`-Ordner). Wichtig: Entpacke die ZIP, bevor du sie hochlädst.
Im Repository unter **Actions → FotoOrdner APK erstellen → Run workflow** starten.
Nach erfolgreichem Build das Artefakt **FotoOrdner-v0.1-debug-apk** herunterladen,
entpacken und `app-debug.apk` auf dem Android-Smartphone installieren.
GitHub kann für Actions eine einmalige Freigabe oder Kontoeinrichtung verlangen.
Das APK ist **nicht hier erstellt oder auf dem Ulefone getestet**; der Workflow
ist eine Build-Anleitung, keine Bestätigung eines erfolgreichen Builds.
Installiere nur eine APK aus deinem eigenen vertrauenswürdigen Build, prüfe
Androids Installationswarnungen und deaktiviere die Erlaubnis für unbekannte Apps
anschließend wieder.
