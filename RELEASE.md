# Fieldnotes Release Guide

Fieldnotes builds and publishes an APK on every push to `main`, following the exact pattern as `study-app-`.

---

## Zero-Setup Automated Builds

You do **not** need to run `keytool`, create a keystore, or add any GitHub secrets.

1. **Push your code to `main`** (or trigger manually via **Actions** → **Build and Release APK** → **Run workflow**).
2. GitHub Actions automatically:
   - Sets up JDK 17.
   - Ensures `.env` exists so the Secrets Gradle plugin succeeds.
   - Signs the build with the permanent committed `debug-upload.keystore` (`storePassword=android`, `keyAlias=androiddebugkey`, `keyPassword=android`).
   - Runs `./gradlew :app:assembleDebug` with `VERSION_CODE=$((100 + GITHUB_RUN_NUMBER))` and `VERSION_NAME` set to the date and run number.
   - Verifies `app/build/outputs/apk/debug/app-debug.apk`.
   - Uploads the APK as an Actions artifact.
   - Publishes a new GitHub Release with the APK attached.

---

## How to Install and Update on Your Phone

1. Push to `main`.
2. Go to your GitHub repository's **Releases** page (or the **Actions** tab artifacts).
3. Download the latest `.apk` file onto your Android phone.
4. Tap the APK to install it.
5. Android will prompt: **"Do you want to update this app?"**
6. Tap **Update** (do **not** uninstall).

Because every build uses the exact same committed keystore and `applicationId` (`app.fieldnotes`), Android preserves all your Room database records (`fieldnotes.db`), app-private images, and goal time logs across every update.
