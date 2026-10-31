# GUARDIAN — Personal Accountability & Online Protection

**GUARDIAN** is a native Android application built in Kotlin and Jetpack Compose, designed to prevent access to adult content and enforce digital wellbeing through system-wide DNS filtering, hardware-backed cryptography, and anti-tamper friction.

Tailored specifically for modern Android releases (Android 13, 14, 15) and tested against **Xiaomi HyperOS (Redmi 13)**.

---

## Architecture & Protection Layers

1. **Primary Network Layer — Android Private DNS (NextDNS over TLS):**
   * Operates at the OS networking stack via native DNS-over-TLS (DoT).
   * System-wide coverage across all browsers (Chrome, Firefox, Brave, Edge), Incognito/Private tabs, WebViews, and native apps.
   * Zero battery drain compared to continuous VPN loopbacks.
2. **Cryptographic Layer — Hardware-Backed PIN & Keystore:**
   * **PBKDF2WithHmacSHA256** with **210,000 iterations** and a 16-byte cryptographically secure random salt.
   * Constant-time verification (`MessageDigest.isEqual`) to prevent timing side channels.
   * Enforces exponential lockout backoff (3 fails = 30s; 5 fails = 5m; 7 fails = 30m; 10 fails = 24h).
   * Sensitive state is encrypted via the **Android Keystore** (`AndroidKeyStore`) using **AES-256-GCM** (128-bit authentication tag, 12-byte IV).
   * Zero plaintext PIN storage.
3. **Supplementary Layer — Scoped Accessibility Watchdog:**
   * Best-effort address-bar inspection for supported browsers.
   * System Settings anti-tamper guard (detects attempts to access GUARDIAN App Info or Device Admin screens).
4. **Friction Layer — Non-Destructive Device Administrator:**
   * Strictly omits all `wipe-data` or destructive policies.
   * Requires revoking device admin privileges before standard uninstallation is permitted.

---

## Building the APK via GitHub Actions (No Android Studio Required)

You do **not** need to install Android Studio or the Android SDK on your personal computer. The repository includes an automated GitHub Actions CI pipeline (`.github/workflows/build-apk.yml`).

### Steps:
1. Create a new private repository on GitHub (e.g. `guardian-android`).
2. Push this project code to your repository:
   ```bash
   git init
   git add .
   git commit -m "Initial commit of GUARDIAN"
   git branch -M main
   git remote add origin https://github.com/<YOUR_USERNAME>/guardian-android.git
   git push -u origin main
   ```
3. Open your repository on GitHub and click the **Actions** tab.
4. You will see the **Build GUARDIAN Android APK** workflow running.
5. Once completed (typically ~2-3 minutes), click on the completed run and scroll to the **Artifacts** section at the bottom.
6. Click **guardian-debug-apk** to download the `app-debug.apk` directly to your computer or phone.

---

## Xiaomi Redmi 13 (HyperOS) Setup Manual

Because Xiaomi HyperOS aggressively restricts background processes and sideloaded accessibility permissions, follow these steps in order:

### Step 1: Install APK & Bypass "Restricted Setting"
1. Transfer `app-debug.apk` to your Redmi 13 and install it using the Files app.
2. Open the GUARDIAN app and create the **Trusted Person PIN** (known only to your accountability partner).
3. If HyperOS displays *"Restricted setting"* when attempting to enable Accessibility:
   * Go to phone **Settings → Apps → Manage apps → GUARDIAN**.
   * Tap the **three vertical dots (⋮)** in the top-right corner.
   * Tap **Allow restricted settings** and confirm with your fingerprint/lock pattern.

### Step 2: Enable the Accessibility Watchdog
* Go to **Settings → Additional settings → Accessibility → Downloaded apps → GUARDIAN**.
* Toggle **Use GUARDIAN** to **ON**.

### Step 3: Prevent HyperOS Battery Murder
* Go to **Settings → Apps → Manage apps → GUARDIAN**.
* Set **Autostart** to **Enabled**.
* Scroll to **Battery saver** and change from *"MIUI Battery saver (recommended)"* to **No restrictions**.
* Open your **Recent Apps** screen, press and hold the GUARDIAN app card, and tap the **Lock icon (padlock)**.

### Step 4: Configure System-Wide Private DNS (NextDNS)
1. Go to **Settings → Connection & sharing → Private DNS**.
2. Select **Private DNS provider hostname**.
3. Enter your NextDNS profile hostname (e.g., `xxxxxx.dns.nextdns.io`).
4. Tap **Save**. All network traffic is now routed through your NextDNS parental control filters.

### Step 5: Prevent Browser DNS Bypasses
* In Chrome / Brave / Edge:
  * Open **Settings → Privacy and security → Use secure DNS**.
  * Ensure it is set to **Use current provider** (which uses your system Private DNS) or toggle it OFF. Do NOT set it to a custom unblocked provider.

---

## Optional: Grant Secure Settings Permission via ADB

To allow GUARDIAN to monitor and verify Private DNS at the OS level:
1. Enable Developer Options and USB Debugging on your phone:
   * **Settings → About phone → tap OS version 7 times**.
   * **Settings → Additional settings → Developer options → enable USB Debugging**.
   * (On HyperOS, also enable **USB debugging (Security settings)**).
2. Connect your phone via USB to a PC and run:
   ```bash
   adb shell pm grant com.guardian.app android.permission.WRITE_SECURE_SETTINGS
   ```

---

## Technical Security Boundaries & Honesty Policy

* **Safe Mode:** Like all Android user-installed applications, booting the phone into Android Safe Mode temporarily disables third-party services.
* **Factory Reset:** An unrooted phone can be factory-reset from the recovery partition unless protected by Google FRP / Mi Account cloud locks.
* **Accessibility Limitations:** Browsers running in Incognito mode or browsers with custom UI trees may restrict accessibility inspection for privacy; this is why **Private DNS (DoT)** is the primary system shield.
