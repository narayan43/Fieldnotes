# Fieldnotes Release Guide

This guide describes how to configure GitHub Actions secrets for automatic, signed APK builds and in-place updates.

---

## 1. Important: Safe Updates & Data Preservation

Android allows updates over an already installed app only when:
1. The **applicationId** never changes (`app.fieldnotes`).
2. The **signing certificate** never changes.
3. The **versionCode** is strictly higher on each update (handled automatically by CI using `github.run_number`).

> **CRITICAL**: Keep your release keystore backed up safely. If the keystore is lost or changed, Android will refuse to update the app and will require an uninstall, which wipes all local notes, images, and goal time logs.

---

## 2. Generate Your Release Keystore (One-Time Setup)

Run this command once on your local machine to create your permanent release keystore:

```bash
keytool -genkeypair -v -keystore fieldnotes-release.jks -alias fieldnotes -keyalg RSA -keysize 2048 -validity 10000
```

- Choose strong passwords for the store and key.
- **Never commit `fieldnotes-release.jks` or any `.jks` / `.keystore` file to git.** It is already excluded in `.gitignore`.

---

## 3. Configure GitHub Repository Secrets

In your GitHub repository, navigate to:
**Settings** → **Secrets and variables** → **Actions** → **New repository secret**

Add the following four repository secrets:

1. **`KEYSTORE_BASE64`**
   The base64-encoded content of your keystore file. Generate it on your machine with:
   - **macOS / Linux**:
     ```bash
     base64 -i fieldnotes-release.jks | tr -d '\n'
     ```
   - **Windows (PowerShell)**:
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes("fieldnotes-release.jks"))
     ```
2. **`STORE_PASSWORD`**
   The password used when creating the keystore.
3. **`KEY_ALIAS`**
   The key alias you specified (e.g. `fieldnotes`).
4. **`KEY_PASSWORD`**
   The password for the key alias.

---

## 4. Local Builds (Optional)

To sign release builds locally on your workstation:
1. Copy `keystore.properties.example` to `keystore.properties`:
   ```bash
   cp keystore.properties.example keystore.properties
   ```
2. Fill in the absolute or relative path to your `.jks` file and passwords.
3. Run:
   ```bash
   ./gradlew :app:assembleRelease
   ```
*(Note: `keystore.properties` is ignored by git and will not be committed).*

---

## 5. Push to GitHub

Once you are ready to push:

```bash
git add .
git commit -m "Prepare Fieldnotes for release and safe in-place updates"
git branch -M main
git remote add origin git@github.com:<YOUR_USERNAME>/<YOUR_REPOSITORY>.git
git push -u origin main
```

Every push to `main` (or manual trigger from **Actions** → **Run workflow**) will:
1. Validate your signing secrets.
2. Build a release APK with an auto-incrementing `versionCode`.
3. Create a dated GitHub Release (e.g. `v2026.10.09-14`) and attach the APK.
4. Allow you to download and install the new APK directly over your existing installation without losing any data.
