# BigBlueButton Android

A Meet-style Android app for joining BigBlueButton classes, as a participant or as a moderator/presenter.

- **Class link** – paste any join link (BBB `api/join` URL or a Greenlight room link).
- **Server (admin)** – enter server, shared secret (`bbb-conf --secret`), meeting ID and your name. The app calls `create` and builds a signed `join` URL with role `VIEWER` or `MODERATOR`.
- The class itself is the official BBB HTML5 client in a full-screen WebView (camera/mic permissions handled natively, file upload, fullscreen video), with a top bar and a Leave button.
- English and Persian (RTL) UI.

## Build
Open this folder in Android Studio, or run `gradle assembleDebug` (JDK 17, Android SDK 35).
Without a local SDK, run the **Build Android app** GitHub Action and download the APK artifact.

## Limitations
- Screen sharing *from* the phone is not supported by Android WebView (viewing shares works).
- Audio may stop when the app is in the background.
- The shared secret is stored encrypted on the device; use server mode only on trusted devices.
