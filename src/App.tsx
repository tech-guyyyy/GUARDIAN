/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import {
  Shield,
  ShieldCheck,
  ShieldAlert,
  Terminal,
  Cpu,
  Lock,
  Smartphone,
  CheckCircle2,
  AlertTriangle,
  Copy,
  Check,
  FileCode,
  BookOpen,
  ArrowRight,
  RefreshCw,
  ExternalLink,
  ChevronRight,
  Info,
  Server
} from 'lucide-react';
import { normalizeDomain, evaluateLockout, validateNextDnsProfile } from './utils/security';

export default function App() {
  const [activeTab, setActiveTab] = useState<'build' | 'hyperos' | 'architecture' | 'playground' | 'files'>('build');
  const [copiedText, setCopiedText] = useState<string | null>(null);

  // Playground state
  const [testPin, setTestPin] = useState('849201');
  const [isDeriving, setIsDeriving] = useState(false);
  const [pbkdf2Result, setPbkdf2Result] = useState<{
    hashHex: string;
    saltHex: string;
    iterations: number;
    durationMs: number;
  } | null>(null);

  // Lockout simulator state
  const [failedAttempts, setFailedAttempts] = useState(0);
  const [lastFailedTime, setLastFailedTime] = useState(0);
  const [currentTime, setCurrentTime] = useState(Date.now());

  // Domain normalizer state
  const [inputUrl, setInputUrl] = useState('https://www.example.com/videos/content?ref=home:8080');
  const [nextDnsInput, setNextDnsInput] = useState('my-shield-id.dns.nextdns.io');

  // File browser state
  const [selectedFile, setSelectedFile] = useState<string>('workflow');

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(Date.now()), 500);
    return () => clearInterval(timer);
  }, []);

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedText(id);
    setTimeout(() => setCopiedText(null), 2500);
  };

  // WebCrypto PBKDF2 benchmark
  const runPbkdf2Benchmark = async () => {
    if (!testPin) return;
    setIsDeriving(true);
    const start = performance.now();
    try {
      const enc = new TextEncoder();
      const salt = crypto.getRandomValues(new Uint8Array(16));
      const keyMaterial = await crypto.subtle.importKey(
        'raw',
        enc.encode(testPin),
        { name: 'PBKDF2' },
        false,
        ['deriveBits']
      );

      const iterations = 210000;
      const derivedBits = await crypto.subtle.deriveBits(
        {
          name: 'PBKDF2',
          salt,
          iterations,
          hash: 'SHA-256'
        },
        keyMaterial,
        256
      );

      const end = performance.now();
      const hashArray = Array.from(new Uint8Array(derivedBits));
      const saltArray = Array.from(salt);
      const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
      const saltHex = saltArray.map(b => b.toString(16).padStart(2, '0')).join('');

      setPbkdf2Result({
        hashHex,
        saltHex,
        iterations,
        durationMs: Math.round(end - start)
      });
    } catch (err) {
      console.error(err);
    } finally {
      setIsDeriving(false);
    }
  };

  const lockoutStatus = evaluateLockout(failedAttempts, lastFailedTime, currentTime);

  const simulateFailedPin = () => {
    const now = Date.now();
    setFailedAttempts(prev => prev + 1);
    setLastFailedTime(now);
  };

  const resetLockout = () => {
    setFailedAttempts(0);
    setLastFailedTime(0);
  };

  const adbCommand = 'adb shell pm grant com.guardian.app android.permission.WRITE_SECURE_SETTINGS';
  const gitCommands = `# Initialize and push GUARDIAN to your private GitHub repo:
git init
git add .
git commit -m "Initialize GUARDIAN Android project"
git branch -M main
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/guardian-android.git
git push -u origin main`;

  const filesMap: Record<string, { title: string; path: string; lang: string; content: string }> = {
    workflow: {
      title: 'GitHub Actions Workflow',
      path: '.github/workflows/build-apk.yml',
      lang: 'yaml',
      content: `name: Build GUARDIAN Android APK

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    name: Build & Verify GUARDIAN APK
    runs-on: ubuntu-latest
    timeout-minutes: 25

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v4
        with:
          packages: 'platform-tools'

      - name: Install Android 35 Platform & Build Tools
        run: sdkmanager "platforms;android-35" "build-tools;35.0.0"

      - name: Setup Compatible Gradle 8.9
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.9'
          cache-disabled: true

      - name: Run Unit Tests
        run: gradle testDebugUnitTest --no-daemon --info --stacktrace --console=plain

      - name: Build Debug APK
        run: gradle assembleDebug --no-daemon --stacktrace

      - name: Upload Debug APK
        uses: actions/upload-artifact@v4
        with:
          name: guardian-debug-apk
          path: app/build/outputs/apk/debug/*.apk
          if-no-files-found: error
          retention-days: 30`
    },
    buildGradle: {
      title: 'App Build Gradle (Kotlin DSL)',
      path: 'app/build.gradle.kts',
      lang: 'kotlin',
      content: `plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.guardian.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.guardian.app"
        minSdk = 28 // Android 9.0 Pie (Private DNS DoT introduced)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.4.4")
}`
    },
    manifest: {
      title: 'Android Manifest',
      path: 'app/src/main/AndroidManifest.xml',
      lang: 'xml',
      content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS" />

    <application
        android:name=".GuardianApplication"
        android:allowBackup="false"
        android:icon="@drawable/ic_shield_logo"
        android:label="@string/app_name"
        android:theme="@style/Theme.Guardian">

        <activity android:name=".ui.MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.GuardianAccessibilityService"
            android:permission="android:permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <receiver
            android:name=".service.GuardianDeviceAdminReceiver"
            android:permission="android:permission.BIND_DEVICE_ADMIN"
            android:exported="true">
            <meta-data
                android:name="android.app.device_admin"
                android:resource="@xml/device_admin_policies" />
        </receiver>

    </application>
</manifest>`
    },
    pinHasher: {
      title: 'PBKDF2 PIN Hasher',
      path: 'app/src/main/java/com/guardian/app/core/crypto/PinHasher.kt',
      lang: 'kotlin',
      content: `package com.guardian.app.core.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    const val ITERATIONS = 210_000
    const val KEY_LENGTH_BITS = 256
    const val SALT_LENGTH_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        return salt
    }

    fun hashPin(pin: CharArray, salt: ByteArray = generateSalt(), iterations: Int = ITERATIONS): HashResult {
        val spec = PBEKeySpec(pin, salt, iterations, KEY_LENGTH_BITS)
        val skf = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = skf.generateSecret(spec).encoded
        spec.clearPassword()

        return HashResult(
            hashBase64 = Base64.getEncoder().encodeToString(hashBytes),
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            iterations = iterations
        )
    }

    fun verifyPin(candidatePin: CharArray, storedHashBase64: String, storedSaltBase64: String): Boolean {
        val salt = Base64.getDecoder().decode(storedSaltBase64)
        val expectedHash = Base64.getDecoder().decode(storedHashBase64)
        val spec = PBEKeySpec(candidatePin, salt, ITERATIONS, KEY_LENGTH_BITS)
        val skf = SecretKeyFactory.getInstance(ALGORITHM)
        val candidateHash = skf.generateSecret(spec).encoded
        spec.clearPassword()

        return MessageDigest.isEqual(expectedHash, candidateHash) // Constant-time
    }
}`
    },
    cryptoEngine: {
      title: 'Android Keystore AES-256-GCM',
      path: 'app/src/main/java/com/guardian/app/core/crypto/CryptoEngine.kt',
      lang: 'kotlin',
      content: `package com.guardian.app.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoEngine {
    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "GUARDIAN_MASTER_KEY"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER)
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray())
        return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText)
    }
}`
    },
    healthManager: {
      title: 'Protection Health Manager',
      path: 'app/src/main/java/com/guardian/app/core/health/ProtectionHealthManager.kt',
      lang: 'kotlin',
      content: `package com.guardian.app.core.health

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.guardian.app.service.GuardianAccessibilityService
import com.guardian.app.service.GuardianDeviceAdminReceiver

class ProtectionHealthManager(private val context: Context) {

    suspend fun evaluateHealth(): ProtectionHealthReport {
        val contentResolver = context.contentResolver
        val privateDnsMode = Settings.Global.getString(contentResolver, "private_dns_mode")
        val privateDnsSpecifier = Settings.Global.getString(contentResolver, "private_dns_specifier")

        val isConfigured = (privateDnsMode == "hostname") &&
                (!privateDnsSpecifier.isNullOrBlank() && privateDnsSpecifier.contains("nextdns"))

        val isResolutionWorking = NetworkDnsProbe.testBasicResolution()
        val isFilterVerified = if (isConfigured && isResolutionWorking) {
            NetworkDnsProbe.verifyNextDnsFiltering()
        } else false

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val isAdminActive = dpm.isAdminActive(ComponentName(context, GuardianDeviceAdminReceiver::class.java))
        val isAccessibilityActive = GuardianAccessibilityService.isRunning

        // Computes SHIELDED, DEGRADED, or AT_RISK
        ...
    }
}`
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* Top Navigation */}
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-40 px-4 lg:px-8 py-3.5">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-sky-600 to-sky-400 flex items-center justify-center shadow-lg shadow-sky-500/20">
              <Shield className="w-6 h-6 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl font-bold tracking-tight text-white font-mono">GUARDIAN</span>
                <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-medium">
                  CI-Ready
                </span>
              </div>
              <p className="text-xs text-slate-400">Native Android (Kotlin & Compose) Protection Subsystem</p>
            </div>
          </div>

          <div className="flex items-center gap-2 text-xs text-slate-300">
            <span className="px-2.5 py-1 rounded-md bg-slate-800 border border-slate-700">
              Target: <strong className="text-sky-400">Android 14/15</strong>
            </span>
            <span className="px-2.5 py-1 rounded-md bg-slate-800 border border-slate-700">
              Device: <strong className="text-sky-400">Redmi 13 (HyperOS)</strong>
            </span>
          </div>
        </div>
      </header>

      {/* Main Tab Navigation */}
      <div className="border-b border-slate-800 bg-slate-900/40 px-4 lg:px-8">
        <div className="max-w-7xl mx-auto flex overflow-x-auto gap-1 py-2">
          <button
            onClick={() => setActiveTab('build')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'build'
                ? 'bg-sky-500 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
            }`}
          >
            <Server className="w-4 h-4" />
            1. Cloud APK Build (GitHub Actions)
          </button>
          <button
            onClick={() => setActiveTab('hyperos')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'hyperos'
                ? 'bg-sky-500 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
            }`}
          >
            <Smartphone className="w-4 h-4" />
            2. Redmi 13 (HyperOS) Setup
          </button>
          <button
            onClick={() => setActiveTab('architecture')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'architecture'
                ? 'bg-sky-500 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
            }`}
          >
            <ShieldCheck className="w-4 h-4" />
            3. Protection Architecture
          </button>
          <button
            onClick={() => setActiveTab('playground')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'playground'
                ? 'bg-sky-500 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
            }`}
          >
            <Cpu className="w-4 h-4" />
            4. Crypto & Lockout Simulator
          </button>
          <button
            onClick={() => setActiveTab('files')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'files'
                ? 'bg-sky-500 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
            }`}
          >
            <FileCode className="w-4 h-4" />
            5. Android Source Code Files
          </button>
        </div>
      </div>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl mx-auto w-full p-4 lg:p-8">
        {/* TAB 1: CLOUD BUILD VIA GITHUB ACTIONS */}
        {activeTab === 'build' && (
          <div className="space-y-6">
            <div className="bg-gradient-to-r from-sky-950/60 to-slate-900 border border-sky-800/40 rounded-2xl p-6 lg:p-8">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
                <div>
                  <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-sky-500/10 border border-sky-500/20 text-sky-400 text-xs font-semibold mb-3">
                    <Server className="w-3.5 h-3.5" /> Self-Contained Cloud CI/CD
                  </div>
                  <h1 className="text-2xl lg:text-3xl font-bold text-white tracking-tight">
                    Build Your GUARDIAN APK in the Cloud
                  </h1>
                  <p className="mt-2 text-slate-300 max-w-2xl text-sm leading-relaxed">
                    You do <strong>not</strong> need to install Android Studio or the Android SDK on your computer.
                    This repository includes an automated <code>.github/workflows/build-apk.yml</code> workflow that builds and signs the debug APK on GitHub's cloud runners.
                  </p>
                </div>
                <div className="shrink-0 bg-slate-900/90 border border-slate-700/80 p-4 rounded-xl text-center space-y-1">
                  <span className="text-xs uppercase font-mono text-slate-400 block">Workflow Target</span>
                  <div className="text-lg font-bold text-emerald-400 font-mono">app-debug.apk</div>
                  <span className="text-xs text-slate-500 block">Direct Download Artifact</span>
                </div>
              </div>
            </div>

            {/* 3-Step Build Workflow */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-3">
                <div className="w-8 h-8 rounded-lg bg-sky-500/10 border border-sky-500/20 text-sky-400 font-bold flex items-center justify-center font-mono">
                  1
                </div>
                <h3 className="font-semibold text-white text-base">Push to Private GitHub Repo</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Create a new private repository on GitHub and push the files from this directory.
                </p>
              </div>

              <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-3">
                <div className="w-8 h-8 rounded-lg bg-sky-500/10 border border-sky-500/20 text-sky-400 font-bold flex items-center justify-center font-mono">
                  2
                </div>
                <h3 className="font-semibold text-white text-base">GitHub Actions Runs Automated Build</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  The workflow spins up an Ubuntu VM with JDK 17, executes all unit tests, and compiles the APK with Gradle 8.9.
                </p>
              </div>

              <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-3">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 font-bold flex items-center justify-center font-mono">
                  3
                </div>
                <h3 className="font-semibold text-white text-base">Download Artifact to Redmi 13</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Download the generated <code>guardian-debug-apk</code> artifact straight from the completed Actions run page to your phone.
                </p>
              </div>
            </div>

            {/* Git Push Commands */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Terminal className="w-4 h-4 text-sky-400" />
                  <span className="text-sm font-semibold text-white font-mono">Terminal Commands to Push</span>
                </div>
                <button
                  onClick={() => handleCopy(gitCommands, 'git')}
                  className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200 transition-colors flex items-center gap-1.5"
                >
                  {copiedText === 'git' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  {copiedText === 'git' ? 'Copied!' : 'Copy Commands'}
                </button>
              </div>
              <pre className="p-4 rounded-lg bg-slate-950 border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto leading-relaxed">
                {gitCommands}
              </pre>
            </div>
          </div>
        )}

        {/* TAB 2: XIAOMI HYPEROS SETUP GUIDE */}
        {activeTab === 'hyperos' && (
          <div className="space-y-6">
            <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6">
              <div className="flex items-start gap-4">
                <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
                  <AlertTriangle className="w-6 h-6" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-white">Xiaomi Redmi 13 (HyperOS) Setup Manual</h2>
                  <p className="text-sm text-slate-300 mt-1">
                    Xiaomi's HyperOS contains two aggressive mechanisms that disrupt security apps: <strong>Restricted Settings</strong> and the <strong>Background Process Killer</strong>. Follow these exact steps to ensure permanent protection without a factory reset.
                  </p>
                </div>
              </div>
            </div>

            <div className="space-y-4">
              {/* Step 1 */}
              <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5">
                <div className="flex items-center gap-3">
                  <span className="w-7 h-7 rounded-full bg-sky-500 text-white font-bold flex items-center justify-center text-xs">1</span>
                  <h3 className="font-semibold text-white">Bypass HyperOS "Restricted Setting"</h3>
                </div>
                <div className="mt-3 pl-10 text-sm text-slate-300 space-y-2">
                  <p>When you install the APK and try to turn on Accessibility, HyperOS will show a dialogue saying <em>"Restricted setting"</em>.</p>
                  <ol className="list-decimal pl-5 space-y-1 text-slate-400 text-xs">
                    <li>Go to phone <strong>Settings → Apps → Manage apps → GUARDIAN</strong>.</li>
                    <li>Tap the <strong>three vertical dots (⋮)</strong> in the top-right corner.</li>
                    <li>Tap <strong>Allow restricted settings</strong>. Confirm with your fingerprint or screen lock.</li>
                  </ol>
                </div>
              </div>

              {/* Step 2 */}
              <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5">
                <div className="flex items-center gap-3">
                  <span className="w-7 h-7 rounded-full bg-sky-500 text-white font-bold flex items-center justify-center text-xs">2</span>
                  <h3 className="font-semibold text-white">Enable the Accessibility Watchdog</h3>
                </div>
                <div className="mt-3 pl-10 text-sm text-slate-300 space-y-2">
                  <p>Now that restricted settings are allowed:</p>
                  <ol className="list-decimal pl-5 space-y-1 text-slate-400 text-xs">
                    <li>Go to <strong>Settings → Additional settings → Accessibility</strong>.</li>
                    <li>Tap <strong>Downloaded apps</strong> and tap <strong>GUARDIAN</strong>.</li>
                    <li>Toggle <strong>Use GUARDIAN</strong> to ON. Accept the Xiaomi 10-second warning countdown.</li>
                  </ol>
                </div>
              </div>

              {/* Step 3 */}
              <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5">
                <div className="flex items-center gap-3">
                  <span className="w-7 h-7 rounded-full bg-sky-500 text-white font-bold flex items-center justify-center text-xs">3</span>
                  <h3 className="font-semibold text-white">Disable HyperOS Battery Killer</h3>
                </div>
                <div className="mt-3 pl-10 text-sm text-slate-300 space-y-2">
                  <p>Without this, HyperOS will kill the watchdog service after 10 minutes of screen-off:</p>
                  <ol className="list-decimal pl-5 space-y-1 text-slate-400 text-xs">
                    <li>Go to <strong>Settings → Apps → Manage apps → GUARDIAN</strong>.</li>
                    <li>Toggle <strong>Autostart</strong> to ON.</li>
                    <li>Scroll down to <strong>Battery saver</strong> and change it to <strong>No restrictions</strong>.</li>
                    <li>Open your Recent Apps screen, long-press the GUARDIAN card, and tap the <strong>Lock (padlock) icon</strong> so HyperOS does not purge it from RAM.</li>
                  </ol>
                </div>
              </div>

              {/* Step 4 */}
              <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5">
                <div className="flex items-center gap-3">
                  <span className="w-7 h-7 rounded-full bg-sky-500 text-white font-bold flex items-center justify-center text-xs">4</span>
                  <h3 className="font-semibold text-white">Configure System-Wide Private DNS (Primary Barrier)</h3>
                </div>
                <div className="mt-3 pl-10 text-sm text-slate-300 space-y-2">
                  <p>This is your primary non-VPN filter covering all apps and browsers:</p>
                  <ol className="list-decimal pl-5 space-y-1 text-slate-400 text-xs">
                    <li>Go to <strong>Settings → Connection & sharing → Private DNS</strong>.</li>
                    <li>Select <strong>Private DNS provider hostname</strong>.</li>
                    <li>Enter your NextDNS profile endpoint (e.g. <code>xxxxxx.dns.nextdns.io</code>).</li>
                    <li>Tap <strong>Save</strong>.</li>
                  </ol>
                </div>
              </div>

              {/* Optional ADB Command */}
              <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Terminal className="w-5 h-5 text-sky-400" />
                    <h3 className="font-semibold text-white">Optional One-Time ADB Command</h3>
                  </div>
                  <button
                    onClick={() => handleCopy(adbCommand, 'adb')}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200 transition-colors flex items-center gap-1.5"
                  >
                    {copiedText === 'adb' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    {copiedText === 'adb' ? 'Copied' : 'Copy ADB Command'}
                  </button>
                </div>
                <p className="mt-2 text-xs text-slate-400">
                  Enables GUARDIAN to monitor and lock Private DNS settings directly from within the app:
                </p>
                <code className="block mt-2 p-3 bg-slate-950 rounded border border-slate-800 font-mono text-xs text-sky-300 overflow-x-auto">
                  {adbCommand}
                </code>
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: PROTECTION ARCHITECTURE & BOUNDARIES */}
        {activeTab === 'architecture' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Layer 1 */}
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center font-bold">1</div>
                    <h3 className="font-bold text-white">Private DNS via NextDNS (DoT)</h3>
                  </div>
                  <span className="text-xs px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-semibold">PRIMARY</span>
                </div>
                <p className="text-sm text-slate-300 leading-relaxed">
                  Operates at the Android OS networking stack. Encrypts all DNS lookups over TLS (port 853) system-wide across Chrome, Firefox, Brave, Edge, Incognito mode, WebViews, and native apps.
                </p>
                <div className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-slate-400 space-y-1">
                  <div>• <strong>Battery Impact:</strong> 0% additional drain (native OS stack)</div>
                  <div>• <strong>VPN Required:</strong> No (avoids VPN icon and loopback overhead)</div>
                  <div>• <strong>SafeSearch:</strong> Enforced at DNS level on Google, Bing, DuckDuckGo</div>
                </div>
              </div>

              {/* Layer 2 */}
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-lg bg-sky-500/10 text-sky-400 flex items-center justify-center font-bold">2</div>
                    <h3 className="font-bold text-white">Hardware Keystore & PBKDF2</h3>
                  </div>
                  <span className="text-xs px-2 py-0.5 rounded bg-sky-500/20 text-sky-300 font-semibold">SECURITY</span>
                </div>
                <p className="text-sm text-slate-300 leading-relaxed">
                  Trusted Person PIN is hashed using <strong>PBKDF2WithHmacSHA256</strong> with <strong>210,000 iterations</strong> and a cryptographically secure random 16-byte salt.
                </p>
                <div className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-slate-400 space-y-1">
                  <div>• <strong>Timing Side-Channels:</strong> Prevented via <code>MessageDigest.isEqual</code></div>
                  <div>• <strong>Local Data:</strong> AES-256-GCM via <code>AndroidKeyStore</code></div>
                  <div>• <strong>Plaintext Storage:</strong> Zero PIN storage anywhere</div>
                </div>
              </div>

              {/* Layer 3 */}
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-lg bg-purple-500/10 text-purple-400 flex items-center justify-center font-bold">3</div>
                    <h3 className="font-bold text-white">Scoped Accessibility Watchdog</h3>
                  </div>
                  <span className="text-xs px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 font-semibold">SUPPLEMENTARY</span>
                </div>
                <p className="text-sm text-slate-300 leading-relaxed">
                  Restricted to high-confidence tasks to conserve battery and eliminate UI lag.
                </p>
                <div className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-slate-400 space-y-1">
                  <div>• <strong>Settings Anti-Tamper:</strong> Intercepts visits to GUARDIAN App Info</div>
                  <div>• <strong>Browser URL Fallback:</strong> Scans address bars for custom domains</div>
                  <div>• <strong>Scope:</strong> Best-effort supplementary check (not primary)</div>
                </div>
              </div>

              {/* Layer 4 */}
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center font-bold">4</div>
                    <h3 className="font-bold text-white">Non-Destructive Device Admin</h3>
                  </div>
                  <span className="text-xs px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 font-semibold">FRICTION</span>
                </div>
                <p className="text-sm text-slate-300 leading-relaxed">
                  Provides friction against casual uninstallation. Android prevents uninstalling any app while active as a Device Administrator.
                </p>
                <div className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-slate-400 space-y-1">
                  <div>• <strong>Zero Destructive Policies:</strong> No wipe-data or reset-password</div>
                  <div>• <strong>Revocation Warning:</strong> Informs accountability partner</div>
                  <div>• <strong>Honest Boundary:</strong> Does not survive Safe Mode or root</div>
                </div>
              </div>
            </div>

            {/* Boundary Disclaimer Card */}
            <div className="bg-slate-900/40 border border-slate-800/80 rounded-xl p-5">
              <div className="flex items-start gap-3">
                <Info className="w-5 h-5 text-sky-400 shrink-0 mt-0.5" />
                <div className="text-xs text-slate-400 space-y-1.5 leading-relaxed">
                  <div className="font-semibold text-slate-200">Honest Security Boundary Notice</div>
                  <p>
                    No user-space application on Android can physically prevent an uninstallation if a user boots into <strong>Android Safe Mode</strong> (which disables all third-party services) or uses ADB with root. GUARDIAN introduces high cognitive and practical friction against impulsive deactivation, backed by network-level DNS filtering.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 4: CRYPTO & LOCKOUT SIMULATOR */}
        {activeTab === 'playground' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* PBKDF2 Benchmark Card */}
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
                <div className="flex items-center gap-2">
                  <Lock className="w-5 h-5 text-sky-400" />
                  <h3 className="font-bold text-white">Live PBKDF2-HMAC-SHA256 Benchmark</h3>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Tests the exact password derivation algorithm configured in GUARDIAN with 210,000 iterations and a 128-bit random salt using native WebCrypto API.
                </p>

                <div className="space-y-3">
                  <div>
                    <label className="text-xs font-medium text-slate-300 block mb-1">Test PIN Input</label>
                    <input
                      type="password"
                      value={testPin}
                      onChange={e => setTestPin(e.target.value)}
                      maxLength={10}
                      className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm font-mono text-white focus:outline-none focus:border-sky-500"
                    />
                  </div>

                  <button
                    onClick={runPbkdf2Benchmark}
                    disabled={isDeriving || !testPin}
                    className="w-full py-2.5 rounded-lg bg-sky-500 hover:bg-sky-400 disabled:opacity-50 text-white font-medium text-sm transition-colors flex items-center justify-center gap-2"
                  >
                    {isDeriving ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Cpu className="w-4 h-4" />}
                    {isDeriving ? 'Deriving (210,000 iterations)...' : 'Run Benchmark (210,000 Iterations)'}
                  </button>

                  {pbkdf2Result && (
                    <div className="p-4 bg-slate-950 rounded-lg border border-slate-800 font-mono text-xs space-y-2">
                      <div className="flex items-center justify-between text-emerald-400 font-semibold">
                        <span>Status: Derived Successfully</span>
                        <span>{pbkdf2Result.durationMs} ms</span>
                      </div>
                      <div className="text-slate-400 break-all">
                        <span className="text-slate-500">Salt (Hex): </span>
                        {pbkdf2Result.saltHex}
                      </div>
                      <div className="text-slate-300 break-all">
                        <span className="text-slate-500">Derived Key (Hex): </span>
                        {pbkdf2Result.hashHex}
                      </div>
                      <div className="text-slate-500 text-[11px]">Iterations: {pbkdf2Result.iterations.toLocaleString()}</div>
                    </div>
                  )}
                </div>
              </div>

              {/* Lockout Policy Simulator Card */}
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
                <div className="flex items-center gap-2">
                  <ShieldAlert className="w-5 h-5 text-amber-400" />
                  <h3 className="font-bold text-white">Rate-Limiting & Lockout Policy Simulator</h3>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Simulates GUARDIAN's progressive exponential lockout: 3 fails = 30s; 5 fails = 5m; 7 fails = 30m; 10 fails = 24h.
                </p>

                <div className="p-4 rounded-lg bg-slate-950 border border-slate-800 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-slate-400">Failed Attempts:</span>
                    <span className="text-sm font-bold font-mono text-white">{failedAttempts}</span>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-xs text-slate-400">Lockout State:</span>
                    <span
                      className={`text-xs font-bold px-2 py-0.5 rounded ${
                        lockoutStatus.isLockedOut ? 'bg-rose-500/20 text-rose-300' : 'bg-emerald-500/20 text-emerald-300'
                      }`}
                    >
                      {lockoutStatus.isLockedOut ? `LOCKED (${lockoutStatus.remainingSeconds}s cooldown)` : 'UNLOCKED'}
                    </span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <button
                    onClick={simulateFailedPin}
                    className="py-2 px-3 rounded-lg bg-rose-600/80 hover:bg-rose-600 text-white text-xs font-medium transition-colors"
                  >
                    Simulate Failed PIN
                  </button>
                  <button
                    onClick={resetLockout}
                    className="py-2 px-3 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition-colors"
                  >
                    Reset Attempts
                  </button>
                </div>
              </div>
            </div>

            {/* Domain Normalization & NextDNS Verification Tool */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-3">
                <h3 className="font-bold text-white text-sm">Domain Normalizer Test</h3>
                <p className="text-xs text-slate-400">Strips protocols, www, query params, ports, and trailing paths.</p>
                <input
                  type="text"
                  value={inputUrl}
                  onChange={e => setInputUrl(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs font-mono text-white focus:outline-none focus:border-sky-500"
                />
                <div className="p-3 bg-slate-950 rounded border border-slate-800 text-xs font-mono text-sky-400">
                  Normalized: <strong>{normalizeDomain(inputUrl) || '(empty)'}</strong>
                </div>
              </div>

              <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-3">
                <h3 className="font-bold text-white text-sm">NextDNS Hostname Syntax Validator</h3>
                <p className="text-xs text-slate-400">Validates DoT hostname syntax for Android Private DNS.</p>
                <input
                  type="text"
                  value={nextDnsInput}
                  onChange={e => setNextDnsInput(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs font-mono text-white focus:outline-none focus:border-sky-500"
                />
                {(() => {
                  const check = validateNextDnsProfile(nextDnsInput);
                  return (
                    <div
                      className={`p-3 rounded border text-xs font-mono ${
                        check.isValid
                          ? 'bg-emerald-950/30 border-emerald-800/50 text-emerald-300'
                          : 'bg-rose-950/30 border-rose-800/50 text-rose-300'
                      }`}
                    >
                      {check.message}
                    </div>
                  );
                })()}
              </div>
            </div>
          </div>
        )}

        {/* TAB 5: ANDROID SOURCE CODE FILES */}
        {activeTab === 'files' && (
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* File List */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-1.5 h-fit">
              <span className="text-xs font-mono uppercase text-slate-500 px-2 block mb-2">Android Project Files</span>
              {Object.entries(filesMap).map(([key, item]) => (
                <button
                  key={key}
                  onClick={() => setSelectedFile(key)}
                  className={`w-full text-left px-3 py-2 rounded-lg text-xs font-medium transition-colors flex items-center justify-between ${
                    selectedFile === key
                      ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
                  }`}
                >
                  <span className="truncate">{item.title}</span>
                  <ChevronRight className="w-3.5 h-3.5 opacity-50 shrink-0" />
                </button>
              ))}
            </div>

            {/* Code Viewer */}
            <div className="lg:col-span-3 bg-slate-900 border border-slate-800 rounded-xl overflow-hidden flex flex-col">
              <div className="px-5 py-3 border-b border-slate-800 bg-slate-950/80 flex items-center justify-between">
                <div>
                  <span className="text-xs font-mono text-slate-400 block">{filesMap[selectedFile].path}</span>
                  <span className="text-sm font-bold text-white">{filesMap[selectedFile].title}</span>
                </div>
                <button
                  onClick={() => handleCopy(filesMap[selectedFile].content, selectedFile)}
                  className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200 transition-colors flex items-center gap-1.5"
                >
                  {copiedText === selectedFile ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  {copiedText === selectedFile ? 'Copied' : 'Copy File Content'}
                </button>
              </div>

              <div className="p-4 bg-slate-950 overflow-x-auto flex-1 font-mono text-xs text-slate-300 leading-relaxed max-h-[600px] overflow-y-auto">
                <pre>{filesMap[selectedFile].content}</pre>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 py-4 px-6 text-center text-xs text-slate-500">
        GUARDIAN Protection System • Built for Android 14/15 & Xiaomi HyperOS • Zero-Subscription, Offline-First Architecture
      </footer>
    </div>
  );
}
