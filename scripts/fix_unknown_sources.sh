#!/system/bin/sh
# 修复"已禁止安装未知来源应用"：
#   1) 全局开关 Settings.Global install_non_market_apps = 1
#   2) 逐应用放行 REQUEST_INSTALL_PACKAGES appop (Android 8+ 真正拦人的是它)
# 用法: adb push 本文件到 /data/local/tmp 后
#       adb shell "su 0 sh /data/local/tmp/fix_unknown_sources.sh [额外包名...]"
set -x

echo "== 1. 全局开关 =="
settings put global install_non_market_apps 1
settings get global install_non_market_apps

echo "== 2. 系统安装器 + 已知商店/工具 =="
for p in com.android.packageinstaller com.dangbeimarket com.dangbei.tvlauncher \
         me.zhanghai.android.files org.mozilla.firefox com.phlox.tvwebbrowser \
         com.topjohnwu.magisk "$@"; do
  [ -n "$p" ] || continue
  appops set --user 0 "$p" REQUEST_INSTALL_PACKAGES allow 2>/dev/null && echo "allow $p"
done

echo "== 3. 全量兜底: 所有第三方应用 =="
for p in $(pm list packages -3 | sed 's/^package://'); do
  appops set --user 0 "$p" REQUEST_INSTALL_PACKAGES allow 2>/dev/null
done

echo "== 4. 验证 =="
appops get com.dangbeimarket REQUEST_INSTALL_PACKAGES 2>/dev/null
echo "TIP: 若日后被远程管理(tms)重置, 重跑本脚本即可。"
