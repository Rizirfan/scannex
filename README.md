# SCANNEX - Android Document Scanner & Signing App

[![Platform](https://img.shields.io/badge/Platform-Android-black.svg?style=flat-square)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-black.svg?style=flat-square)](https://kotlinlang.org)
[![Build Status](https://img.shields.io/badge/Build-Passing-black.svg?style=flat-square)](https://github.com/Rizirfan/scannex)

SCANNEX is a premium, high-performance, and minimalist monochrome Android application built with Jetpack Compose. It allows users to scan physical documents, import local PDFs/images, draw signatures with multiple ink colors, and save documents to the public gallery or share them.

---

## Key Features

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
- Fully asynchronous file listing and image thumbnail decoding using Kotlin Coroutines (`Dispatchers.IO`) to maintain smooth 60fps/120fps scrolling.

---

## Prerequisites

- **Android Device/Emulator**: Android 7.0 (API Level 24) or newer.
- **Development Tools**: Android Studio (or Android SDK tools with JDK 17).
- **USB Connection**: Enabled **USB Debugging** inside Developer Options on your physical testing device.

---

## Download & Installation Guide

You can download and install the SCANNEX application directly on any Android device without connecting it to a computer.

### Step 1: Download the APK
Click the link below on your Android device to download the installer directly:
**[Download SCANNEX.apk](https://github.com/Rizirfan/scannex/raw/main/SCANNEX.apk)**

### Step 2: Enable Unknown Sources
Because this is a custom-built app, Android requires permission to install it:
1. Open the downloaded `.apk` file.
2. If blocked, tap **Settings** in the popup warning.
3. Toggle **"Allow from this source"** (or enable **Unknown Sources** under Security settings depending on your Android version).

### Step 3: Install
1. Tap the APK file again.
2. Click **Install** and open **SCANNEX** from your app drawer!

---

## How to Build from Source

To compile the codebase manually, open the terminal in the project root directory and run the Gradle wrapper command:
```powershell
.\gradlew.bat assembleDebug
```
The newly built APK will be exported to `app/build/outputs/apk/debug/app-debug.apk`.
