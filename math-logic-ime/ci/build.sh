#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk" || ! -d "$sdk/platforms" ]]; then echo "ANDROID_HOME must point to an installed Android SDK" >&2; exit 1; fi
platform="$sdk/platforms/android-35/android.jar"
[[ -f "$platform" ]] || { echo "Android SDK Platform 35 is required" >&2; exit 1; }
build_tools="$(find "$sdk/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
[[ -x "$build_tools/aapt" && -x "$build_tools/d8" && -x "$build_tools/apksigner" ]] || { echo "Android build-tools with aapt, d8 and apksigner are required" >&2; exit 1; }
rm -rf build
mkdir -p build/gen build/classes build/dex build/testclasses
javac -encoding UTF-8 -d build/testclasses app/src/main/java/jp/example/mathime/KeyLayouts.java tests/KeyLayoutsTest.java
java -cp build/testclasses jp.example.mathime.KeyLayoutsTest
"$build_tools/aapt" package -f -M app/src/main/AndroidManifest.xml -S app/src/main/res -I "$platform" -F build/base.apk -J build/gen
find app/src/main/java build/gen -name '*.java' > build/sources.txt
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$platform" -d build/classes @build/sources.txt
find build/classes -name '*.class' > build/classes.txt
"$build_tools/d8" --min-api 26 --lib "$platform" --output build/dex @build/classes.txt
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$build_tools/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [[ ! -f debug-signing.keystore ]]; then
    keytool -genkeypair -keystore debug-signing.keystore -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Math Flick,C=JP' -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1
fi
"$build_tools/apksigner" sign --ks debug-signing.keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out build/math-flick.apk build/aligned.apk
"$build_tools/apksigner" verify --verbose build/math-flick.apk
"$build_tools/aapt" dump badging build/math-flick.apk | head -n 8
sha256sum build/math-flick.apk
