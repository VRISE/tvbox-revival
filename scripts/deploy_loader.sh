#!/system/bin/sh
# Replace operator SkyLoader with patched stub (login flow removed).
# Original is backed up to /data/local/tmp/SkyLoaderA9_orig_backup.apk
set -x
mount -o remount,rw /system 2>/dev/null || mount -o remount,rw /

echo "== backup original =="
cp /system/app/SkyLoaderA9_release/SkyLoaderA9_release.apk /data/local/tmp/SkyLoaderA9_orig_backup.apk
ls -la /data/local/tmp/SkyLoaderA9_orig_backup.apk

echo "== clear stale odex =="
rm -rf /system/app/SkyLoaderA9_release/oat

echo "== install patched stub =="
cp /data/local/tmp/SkyLoader_patched.apk /system/app/SkyLoaderA9_release/SkyLoaderA9_release.apk
chmod 644 /system/app/SkyLoaderA9_release/SkyLoaderA9_release.apk
chown 0.0 /system/app/SkyLoaderA9_release/SkyLoaderA9_release.apk
restorecon /system/app/SkyLoaderA9_release/SkyLoaderA9_release.apk 2>/dev/null
ls -la /system/app/SkyLoaderA9_release/

sync
echo "== DEPLOY DONE =="
