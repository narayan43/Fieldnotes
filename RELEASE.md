# Fieldnotes Release Guide

This guide explains how Fieldnotes builds and releases signed APKs automatically on GitHub, and how in-place updates work without losing data.

---

## 1. Automatic Zero-Setup Builds (Works Just Like study-app-)

**You do not need to configure any secrets or do anything manually.**

Just like your `study-app-` repository, Fieldnotes now builds completely automatically:
- A permanent release keystore (`keystore/release.jks`) is bundled in the repository.
- Even if secrets are not added in GitHub Settings, GitHub Actions automatically uses the bundled keystore (with an auto-generation fallback if ever missing).
- Every push to `main` (or manual trigger from **Actions** → **Run workflow**) automatically compiles, signs, and attaches the APK to your GitHub Releases.
- All updates install directly over previous versions without losing any notes, images, or goal time logs.

---

## 2. What the GitHub Actions Workflow Does

On every push to `main`, `.github/workflows/release-apk.yml`:
1. Sets up JDK 17 and uses the executable Gradle wrapper (`./gradlew`).
2. Configures the release keystore automatically.
3. Automatically increments `versionCode` using GitHub's run number (ensuring updates are always newer).
4. Sets `versionName` to the UTC date and run number (e.g. `2026.10.09-15`).
5. Compiles `:app:assembleRelease`.
6. Creates or updates a GitHub Release named `Fieldnotes <UTC date>-<run number>` with the signed APK attached.

---

## 3. Optional: Using Your Own Custom GitHub Secrets

If you ever prefer to sign releases with your own private external keystore instead of the bundled repo keystore:

1. Run this command on your computer:
   ```bash
   keytool -genkeypair -v -keystore fieldnotes-release.jks -alias fieldnotes -keyalg RSA -keysize 2048 -validity 10000
   ```
2. In your GitHub repository, go to **Settings** → **Secrets and variables** → **Actions**, and add:
   - `KEYSTORE_BASE64`: Base64 string of `fieldnotes-release.jks` (`base64 -i fieldnotes-release.jks | tr -d '\n'`)
   - `STORE_PASSWORD`: Keystore password you entered
   - `KEY_ALIAS`: Key alias you entered (`fieldnotes`)
   - `KEY_PASSWORD`: Password for the key alias

*(Note: If these secrets are not set, the workflow automatically and safely uses the bundled keystore so your build never fails).*

---

## 4. How to Update Your App on Your Phone

1. Go to your GitHub repository's **Releases** page.
2. Download the latest `.apk` asset.
3. Tap the file to install it.
4. Android will prompt: **"Do you want to update this app?"**
5. Tap **Update**. All your notes, sections, tags, writing sessions, and goal timers will be exactly as you left them.
