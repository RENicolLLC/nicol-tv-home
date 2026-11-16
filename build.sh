#!/bin/zsh
set -euo pipefail

PROJECT_DIR=${0:A:h}
SDK_DIR="$HOME/Library/Android/sdk"
BUILD_TOOLS="$SDK_DIR/build-tools/35.0.0"
ANDROID_JAR="$SDK_DIR/platforms/android-35/android.jar"
BUILD_DIR="$PROJECT_DIR/build"
GENERATED_DIR="$BUILD_DIR/generated"
CLASSES_DIR="$BUILD_DIR/classes"
COMPILED_RES="$BUILD_DIR/compiled-res"
APK_DIR="$BUILD_DIR/apk"
KEYSTORE="$PROJECT_DIR/signing/nicol-tv-preview.jks"

rm -rf "$BUILD_DIR"
mkdir -p "$GENERATED_DIR" "$CLASSES_DIR" "$COMPILED_RES" "$APK_DIR" "$PROJECT_DIR/signing"

if [[ ! -f "$KEYSTORE" ]]; then
  keytool -genkeypair -keystore "$KEYSTORE" -storepass nicol-preview-local -keypass nicol-preview-local \
    -alias nicol-preview -keyalg RSA -keysize 2048 -validity 3650 \
    -dname "CN=Nicol TV Preview, OU=Local Development, O=R E Nicol LLC, C=US"
fi

"$BUILD_TOOLS/aapt2" compile --dir "$PROJECT_DIR/res" -o "$COMPILED_RES/resources.zip"
"$BUILD_TOOLS/aapt2" link -o "$APK_DIR/base-unsigned.apk" -I "$ANDROID_JAR" \
  --manifest "$PROJECT_DIR/AndroidManifest.xml" --java "$GENERATED_DIR" \
  --min-sdk-version 28 --target-sdk-version 35 --version-code 11 --version-name 0.5.2-preview \
  -A "$PROJECT_DIR/assets" \
  "$COMPILED_RES/resources.zip"

find "$PROJECT_DIR/src" "$GENERATED_DIR" -name '*.java' -print0 | \
  xargs -0 javac -source 8 -target 8 -encoding UTF-8 -classpath "$ANDROID_JAR" -d "$CLASSES_DIR"

jar cf "$BUILD_DIR/classes.jar" -C "$CLASSES_DIR" .
"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --min-api 28 --output "$APK_DIR" "$BUILD_DIR/classes.jar"
cd "$APK_DIR"
zip -q -j base-unsigned.apk classes.dex
"$BUILD_TOOLS/zipalign" -f 4 base-unsigned.apk Nicol-TV-Home-v0.4-preview-aligned.apk
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-key-alias nicol-preview \
  --ks-pass pass:nicol-preview-local --key-pass pass:nicol-preview-local \
  --out Nicol-TV-Home-v0.4-preview.apk Nicol-TV-Home-v0.4-preview-aligned.apk
"$BUILD_TOOLS/apksigner" verify --verbose Nicol-TV-Home-v0.4-preview.apk
echo "$APK_DIR/Nicol-TV-Home-v0.4-preview.apk"
