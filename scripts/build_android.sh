#!/usr/bin/env bash
# Builds an installable debug APK (signed with the debug key) into dist/.
set -e
cd "$(dirname "$0")/.."
[ -f debug.keystore ] || keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey \
  -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
if [ -x ./gradlew ]; then ./gradlew assembleDebug; else gradle assembleDebug; fi
mkdir -p dist && cp app/build/outputs/apk/debug/app-debug.apk dist/LocalCam.apk
echo "Android APK -> dist/LocalCam.apk"
