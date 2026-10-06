#!/usr/bin/env bash
# 无 Gradle 构建 TV APK：aapt2 link → javac → d8 → zip → zipalign → apksigner
# 用法: ./build_apk.sh <项目目录> <输出.apk>
# 项目目录结构:
#   AndroidManifest.xml
#   src/.../*.java
# 依赖环境变量(有默认值):
#   SDK=~/opt/android-sdk  BT=build-tools版本  PLAT=platforms/android-XX/android.jar
set -euo pipefail

PROJ="${1:?usage: build_apk.sh <project_dir> <out.apk>}"
OUT="${2:?usage: build_apk.sh <project_dir> <out.apk>}"

SDK="${SDK:-$HOME/opt/android-sdk}"
BT="${BT:-$SDK/build-tools/34.0.0}"
PLAT="${PLAT:-$SDK/platforms/android-30/android.jar}"
KS="${KS:-$PROJ/debug.keystore}"

B="$PROJ/build"
rm -rf "$B/classes" "$B"/*.apk; mkdir -p "$B/classes"

echo "[1/6] aapt2 link (manifest only)"
"$BT/aapt2" link -o "$B/base.apk" -I "$PLAT" \
  --manifest "$PROJ/AndroidManifest.xml" \
  --min-sdk-version 21 --target-sdk-version 28

echo "[2/6] javac"
find "$PROJ/src" -name '*.java' > "$B/sources.txt"
javac --release 8 -cp "$PLAT" -d "$B/classes" @"$B/sources.txt" 2>&1 \
  | grep -vE '过时|警告|deprecat|warning' || true

echo "[3/6] d8"
"$BT/d8" --lib "$PLAT" --release --min-api 21 --output "$B" \
  $(find "$B/classes" -name '*.class')

echo "[4/6] pack dex"
cp "$B/base.apk" "$B/app.apk"
(cd "$B" && zip -q -j app.apk classes.dex)

echo "[5/6] zipalign"
"$BT/zipalign" -f -p 4 "$B/app.apk" "$B/aligned.apk"

echo "[6/6] sign"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias androiddebugkey \
    -storepass android -keypass android \
    -dname "CN=Android Debug,O=Android,C=US" \
    -keyalg RSA -keysize 2048 -validity 10000
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey --out "$OUT" "$B/aligned.apk"

echo "DONE: $OUT"
"$BT/apksigner" verify --print-certs "$OUT" | head -2
