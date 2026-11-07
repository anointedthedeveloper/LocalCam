@echo off
cd /d "%~dp0.."
if not exist debug.keystore keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
if exist gradlew.bat (call gradlew.bat assembleDebug) else (call gradle assembleDebug) || exit /b 1
if not exist dist mkdir dist
copy /y app\build\outputs\apk\debug\app-debug.apk dist\LocalCam.apk
