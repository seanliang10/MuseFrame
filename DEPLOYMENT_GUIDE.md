# MuseFrame Android App Deployment Guide

## 🔄 Version Bump

When releasing a new version, update the following files:

### 1. **app/build.gradle.kts**
Update both `versionCode` and `versionName`:
```kotlin
defaultConfig {
    applicationId = "com.museframe.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 30010  // Increment this (e.g., 30011, 30012, etc.)
    versionName = "3.0.1" // Update semantic version (e.g., "3.0.2", "3.1.0", etc.)
    ...
}
```

**Version Code Rules:**
- Must be incremented for each release
- Currently using format: Major(3) + Minor(00) + Patch(10) = 30010
- Example: 3.0.2 → 30020, 3.1.0 → 31000

---

## 🌐 API Endpoint Changes

### 1. **app/src/main/java/com/museframe/app/data/api/ApiConstants.kt**
```kotlin
object ApiConstants {
    const val BASE_URL = "https://api.museframe.com/"  // Change this URL
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
```

### 2. **Backend URLs in Code**
If you have any hardcoded URLs, search and update in:
- `app/src/main/java/com/museframe/app/data/api/MuseFrameApiService.kt`
- Any repository implementations that might have endpoint overrides

---

## 🔨 Building Release APK

### Prerequisites
1. **Keystore file**: Already configured at `app/muse-tv.jks`
2. **Signing config**: Already set in `app/build.gradle.kts`

### Build Steps

#### Option 1: Command Line
```bash
# Navigate to project directory
cd /Users/seanliang/AndroidStudioProjects/MuseFrame

# Clean previous builds (optional but recommended)
./gradlew clean

# Build release APK
./gradlew assembleRelease
```

#### Option 2: Android Studio
1. Open Android Studio
2. Select `Build` → `Generate Signed Bundle / APK...`
3. Choose `APK`
4. Select `release` build variant
5. Click `Build`

### Output Location
The signed APK will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 📱 Installation

### Local Installation via ADB
```bash
# Connect your Android TV/device
adb devices

# Install the APK
adb install app/build/outputs/apk/release/app-release.apk

# Or reinstall (if app already exists)
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Manual Installation
1. Copy the APK to your device via USB or network
2. Use a file manager on the device to install
3. Enable "Unknown Sources" in device settings if needed

---

## 📋 Pre-Release Checklist

Before building a release:

- [ ] Update version code and name in `build.gradle.kts`
- [ ] Verify API endpoint in `ApiConstants.kt`
- [ ] Test on debug build first
- [ ] Check that all features work with production API
- [ ] Verify push notifications are working
- [ ] Test on actual Android TV device
- [ ] Clean and rebuild project

---

## 🚀 Quick Release Commands

```bash
# Complete release build sequence
cd /Users/seanliang/AndroidStudioProjects/MuseFrame
./gradlew clean
./gradlew assembleRelease

# Install on connected device
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 🔑 Keystore Information

**Location**: `app/muse-tv.jks`
**Key Alias**: `key0`
**Store Password**: `lZVrMpQzyVtxBA7AZ`
**Key Password**: `lZVrMpQzyVtxBA7AZ`

⚠️ **Important**: Keep these credentials secure and never commit them to version control!

---

## 🐛 Troubleshooting

### Build Failures
```bash
# Clear build cache
./gradlew clean
rm -rf .gradle
rm -rf app/build

# Rebuild
./gradlew assembleRelease
```

### Signing Issues
- Verify keystore file exists at `app/muse-tv.jks`
- Check passwords in `build.gradle.kts` match the keystore
- Ensure keystore hasn't expired

### Installation Issues
```bash
# Check connected devices
adb devices

# Uninstall existing version first
adb uninstall com.museframe.app

# Install fresh
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 📊 Version History

| Version | Code  | Date       | Notes                                      |
|---------|-------|------------|--------------------------------------------|
| 3.0.1   | 30010 | 2025-09-26 | Push notifications, optimizations         |
| 3.0.0   | 30000 | -          | Initial native Android version             |

---

## 🔗 Useful Resources

- [Android Build Documentation](https://developer.android.com/build)
- [ADB Commands Reference](https://developer.android.com/tools/adb)
- [Android TV Development](https://developer.android.com/tv)