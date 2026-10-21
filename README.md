# Clean Recorder

A lightweight, ad-free, battery-efficient lecture and voice recording app for Android built with **Kotlin** and **Jetpack Compose**.

## Features

- **Continuous Background Recording**: Runs in a dedicated Android Foreground Service (`FOREGROUND_SERVICE_MICROPHONE` + `PARTIAL_WAKE_LOCK`). Never drops or mutes audio when the screen is locked, even for 75+ minute recordings.
- **Ultra-Lightweight & Compact**: Standalone Release APK is only **2.77 MB** (compared to 40 MB+ commercial apps).
- **Sub-Second Lossless Audio Trimmer**: Built with Android's native `MediaExtractor` and `MediaMuxer` to slice audio at the packet level in milliseconds with zero re-encoding or quality degradation.
- **Selectable Formats & Bitrates**:
  - **AAC (`.m4a`)** & **MP3 (`.mp3`)**
  - **16 kbps (Ultra Compact, ~7 MB / hour)**, **32 kbps**, **64 kbps**, **128 kbps**.
- **Dark Audio Studio Aesthetic**:
  - 28-bar dynamic level meter reacting in real-time to microphone amplitude.
  - Studio waveform with draggable IN/OUT brackets and an amber playhead scrubber.
  - Monospace digital timer that automatically resets to `00:00` in idle state.
- **Recordings Library**:
  - Compact cards with inline audio playback and scrubber.
  - Multi-select mode with batch delete.
  - Native file sharing via Android `FileProvider`.

## Download Standalone APK

The standalone release APK is available directly in this repository:
- [`CleanRecorder.apk`](CleanRecorder.apk) (2.77 MB)

## Tech Stack

- **Language**: Kotlin 2.1
- **UI Toolkit**: Jetpack Compose + Material 3
- **Target SDK**: Android 15 (API 35) / Min SDK: Android 8.0 (API 26)
- **Architecture**: MVI / MVVM with Kotlin Coroutines & StateFlow
- **Build System**: Gradle 9.3 + Android Gradle Plugin 8.12
