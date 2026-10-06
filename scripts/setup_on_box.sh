#!/system/bin/sh
# 盒子端 root 配置：网络ADB静态层 + TvFix priv-app 化 + 权限白名单
# 用法: adb push 本文件+TvFix.apk+privapp xml 到 /data/local/tmp 后:
#       adb shell "su 0 sh /data/local/tmp/setup_on_box.sh"
set -x

mount -o remount,rw /system 2>/dev/null || mount -o remount,rw /

echo "== 1. build.prop: 网络ADB端口(静态层) =="
grep -q '^service.adb.tcp.port=' /system/build.prop || \
  echo 'service.adb.tcp.port=5555' >> /system/build.prop
tail -2 /system/build.prop

echo "== 2. TvFix 提升为 priv-app(防删) =="
mkdir -p /system/priv-app/TvFix
cp /data/local/tmp/TvFix.apk /system/priv-app/TvFix/TvFix.apk
chmod 644 /system/priv-app/TvFix/TvFix.apk
chown 0.0 /system/priv-app/TvFix/TvFix.apk
restorecon /system/priv-app/TvFix/TvFix.apk 2>/dev/null

echo "== 3. priv-app 权限白名单(WRITE_SECURE_SETTINGS) =="
cp /data/local/tmp/privapp-permissions-com.kaixin.tvfix.xml /system/etc/permissions/privapp-permissions-com.kaixin.tvfix.xml
chmod 644 /system/etc/permissions/privapp-permissions-com.kaixin.tvfix.xml
restorecon /system/etc/permissions/privapp-permissions-com.kaixin.tvfix.xml 2>/dev/null

echo "== 4. busybox(可选, armv7 静态版) =="
if [ -f /data/local/tmp/busybox ]; then
  cp /data/local/tmp/busybox /system/xbin/busybox
  chmod 755 /system/xbin/busybox
fi

echo "== 5. su 权限位(仅放开执行; su 自身仍会拒绝应用, 本项目不依赖它) =="
chmod 4755 /system/xbin/su 2>/dev/null

echo "== 6. 当前会话立即生效 =="
setprop service.adb.tcp.port 5555
setprop persist.adb.tcp.port 5555
getprop service.adb.tcp.port
sync
echo "== SETUP DONE =="
