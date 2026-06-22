# SCANNEX: Document Scanner & Signing App Setup & Deployment Guide

Welcome to **SCANNEX**, a premium, high-performance, and minimalist monochrome Android application built with Jetpack Compose. SCANNEX enables users to scan physical documents, import local PDFs/images, draw signatures with multiple ink colors, and save documents to the public gallery or share them.

---

## 1. App Features Overview

### 📸 Dynamic Document Scanning
- Integrates the **Google Play Services ML Kit Document Scanner API**.
- **No camera permissions required**: Leverages system-level scanning activity.
- Automatic page edge detection, perspective correction, crop adjustments, rotation, and high-quality filters (grayscale, color, stain removal).

### 📂 Smart Document Import / Upload
- Click **IMPORT** to pick files (images or PDFs) from system storage.
- **Original Filename Retention**: Keeps the original name (e.g. `Invoice.pdf`) by storing files with a unique timestamp prefix (`Imported_yyyyMMdd_HHmmss_original_name.ext`).
- **Clean Display Names**: Parses and displays user-friendly names, stripping internal timestamps.
- **Absolute Path Display**: Every card displays its exact storage "address" on the device filesystem (e.g., `/data/user/0/com.example.docscanner/files/scanned_docs/...`).

### ✍️ Document Signing (Edit Screen)
- Interactive signing canvas to draw signatures.
- **Minimised Save Button**: Low-emphasis `Save` text-button with checkmark icon to save signed pages as JPEGs.
- **Labeled Canvas Clear Button ("X Clear")**: Outlined warning button (red outline/text) floating in the top-right corner of the canvas to easily clear drawn strokes.
- **Minimised Ink Selector**: Centers small `20.dp` ink color dots (Black, Blue, Red, Green) with inner indicators.
- **Coordinate Scaling**: Signature coordinates are scaled from canvas dimensions to original image dimensions to maintain sharp signatures on high-resolution output.

### 🔍 Filter and Search
- Compact monochrome search bar to filter by name.
- Tiny filter chips row to view files by category:
  - **All**: All documents.
  - **PDFs**: Filtered list of PDF files (scanned and imported).
  - **Images**: JPEG scans and imported images.
  - **Imported**: Exclusively shows files imported via the picker.
  - **Signed**: Shows signed document modifications.

### ⚡ Performance & Gallery Saving
- Uses Android `MediaStore` to save signed/scanned JPEGs directly into the public `Pictures/DocScanner` folder.
- Fully asynchronous file listing and thumbnail decoding using Kotlin Coroutines (`Dispatchers.IO`) to maintain smooth 60fps/120fps scrolling.

---

## 2. Prerequisites for Installation & Deployment

To build, install, or run the SCANNEX application, ensure you have:
1. An Android device or emulator running **Android 7.0 (API Level 24) or newer**.
2. **USB Debugging** enabled on your physical Android device.
3. Gradle and JDK 17 installed if building from source.

---

## 3. How to Download the App (Pre-built APK)

The compiled debug APK is located in the project's build output directory:
- **Relative Path**: `app/build/outputs/apk/debug/app-debug.apk`
- **Absolute Path**: `c:\Users\hp202\Downloads\shdesignmeld projects\projects\open\Android apps\new_app_without name\app\build\outputs\apk\debug\app-debug.apk`

### Transferring the APK to your Phone:
- **Method 1 (Recommended - USB)**: Connect your device via USB and run the CLI command (see below).
- **Method 2 (Cloud)**: Upload `app-debug.apk` to Google Drive or email it to yourself, then download and install it on your phone.
- **Method 3 (Local Transfer)**: Copy the APK file to your phone's Internal Storage via Windows Explorer, open a File Manager app on your phone, and tap the APK to install it.

---

## 4. How to Deploy / Install the App

### Option A: Using the Android CLI (Recommended)
If you have the Android CLI setup at `C:\Users\hp202\AppData\AndroidCLI\android.exe`, make sure your phone is connected with USB Debugging enabled, and run this PowerShell command:
```powershell
C:\Users\hp202\AppData\AndroidCLI\android.exe run --apks="c:\Users\hp202\Downloads\shdesignmeld projects\projects\open\Android apps\new_app_without name\app\build\outputs\apk\debug\app-debug.apk"
```

### Option B: Using ADB (Android Debug Bridge) directly
If you have `adb` in your system environment variables, run:
```powershell
adb install -r "c:\Users\hp202\Downloads\shdesignmeld projects\projects\open\Android apps\new_app_without name\app\build\outputs\apk\debug\app-debug.apk"
```

### Option C: Manual Installation on the Phone
1. Transfer the `app-debug.apk` to your device.
2. Tap the `.apk` file using any File Manager app.
3. If prompted with a warning about installing from **"Unknown Sources"**, click **Settings** and toggle **"Allow from this source"**.
4. Confirm by clicking **Install** and open the **SCANNEX** app from your app drawer.

---

## 5. How to Build the App from Source

If you modify the source code, compile a new APK using the Gradle Wrapper script in the project directory:
```powershell
.\gradlew.bat assembleDebug
```
Once the build completes successfully, the updated APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
