# SCANNEX - Android Document Scanner & Signing App

SCANNEX is a premium, high-performance, and minimalist monochrome Android application built with Jetpack Compose. It allows users to scan physical documents, import local PDFs/images, draw signatures with multiple ink colors, and save documents to the public gallery or share them.

---

## 📸 Key Features

### 1. Dynamic Document Scanning
- Integrates the **Google Play Services ML Kit Document Scanner API**.
- **Zero Camera Permissions**: Leverages system-level document scanner activities.
- Offers automatic page boundary detection, perspective cropping, image rotation, and image enhancement filters (Color, Grayscale, Stain/Shadow Removal).

### 2. Smart Local Document Import
- Click **IMPORT** to load images or PDFs from system file pickers.
- **Original Filename Retention**: Retains display names (e.g. `Invoice.pdf`) by prefixing files with a unique timestamp (`Imported_yyyyMMdd_HHmmss_original_name.ext`).
- **Clean UI Display Names**: Automatically filters and displays clean names to the user.
- **File Storage Address**: Card listings display the exact local path/address of the file on the device filesystem.

### 3. Interactive Signature Canvas
- Draw smooth vector signature paths on a canvas overlaying the document bitmap.
- **Minimised Controls**: Sized down color selection dots (`20.dp`) and checkmark save button (`32.dp`).
- **Pill-shaped Floating Clear ("X Clear")**: Outlined warning button (red accent) in the top-right corner of the canvas to easily reset drawing paths.
- **Coordinate Scaling**: Translates canvas coordinates to matches the original page resolution to ensure signatures remain sharp on the output JPEGs.
- **Ink Choices**: Supports Black, Blue, Red, and Green ink colors.

### 4. Search and Dashboard Filters
- Search bar to instantly filter files by name.
- Filter chips to categorize items:
  - **All**: View all scanned and imported items.
  - **PDFs**: Filtered list of PDFs.
  - **Images**: JPEG scan files and imported images.
  - **Imported**: Exclusively shows files uploaded via the file picker.
  - **Signed**: Shows documents that have been signed.

### 5. MediaStore Integration & Smooth Performance
- Saves scanned/signed JPEGs directly into the device's public `Pictures/DocScanner` directory using the Android `MediaStore` API.
- Fully asynchronous file listing and image thumbnail decoding using Kotlin Coroutines (`Dispatchers.IO`) to maintain 60fps/120fps scrolling.

---

## 🛠️ Prerequisites

- **Android Device/Emulator**: Android 7.0 (API Level 24) or newer.
- **Development Tools**: Android Studio (or Android SDK tools with JDK 17).
- **USB Connection**: Enabled **USB Debugging** inside Developer Options on your physical testing device.

---

## 🚀 How to Install & Deploy

### 1. Pre-built APK Location
The compiled debug installation package is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Wireless Installation (No USB Cable/Computer Needed)
To run the app on another device without connecting it to a computer:
1. **Send the APK**: Share the file `app-debug.apk` using Google Drive, Email, WhatsApp, or Telegram.
2. **Download on Device**: Open the file on the target Android phone.
3. **Toggle Install Settings**: If prompted that installation is blocked from **"Unknown Sources"**, tap **Settings** and toggle **"Allow from this source"**.
4. **Install**: Tap **Install** and launch **SCANNEX** from the app menu.

### 3. Deploying using Android CLI / ADB
If your device is connected via USB and USB Debugging is active, run the deployment tool:
```powershell
C:\Users\hp202\AppData\AndroidCLI\android.exe run --apks="app/build/outputs/apk/debug/app-debug.apk"
```
Or use the standard `adb` client:
```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🏗️ How to Build from Source

To compile the codebase manually, open the terminal in the project root directory and run the Gradle wrapper command:
```powershell
.\gradlew.bat assembleDebug
```
The newly built APK will be exported to `app/build/outputs/apk/debug/app-debug.apk`.
