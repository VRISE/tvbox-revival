#!/system/bin/sh
# 一键把 DNS 守护 + Termux sshd 开机自启焊进 init。
# 原理: 这类盒子的 init 不解析新增 rc 文件、也不解析属性触发器,
#       但【已存在】rc 文件里的 service 定义 + class main 会被正常解析并自动启动。
#       所以把 service 挂到已被解析的 /system/etc/init/audioserver.rc 尾部。
#
# 前置: 以下文件已 push 到 /data/local/tmp:
#   dnsfixd.sh    (DNS 守护, 45s 循环 ndc setnetdns)
#   start-sshd.sh (以 su 10037 拉起 termux sshd)
# 用法: adb shell "su 0 sh /data/local/tmp/install_dns_service.sh"
set -e

mount -o remount,rw /system 2>/dev/null || mount -o remount,rw /

echo "== 1. 部署脚本本体 =="
cp /data/local/tmp/dnsfixd.sh /system/bin/dnsfixd.sh
cp /data/local/tmp/start-sshd.sh /system/bin/start-sshd.sh
chmod 755 /system/bin/dnsfixd.sh /system/bin/start-sshd.sh
chown 0.0 /system/bin/dnsfixd.sh /system/bin/start-sshd.sh

echo "== 2. 追加 service 定义(幂等) =="
grep -q tvfixdns /system/etc/init/audioserver.rc || \
  printf "\nservice tvfixdns /system/bin/sh /system/bin/dnsfixd.sh\n    class main\n    user root\n    oneshot\n    seclabel u:r:su:s0\n" >> /system/etc/init/audioserver.rc
grep -q tvfixsshd /system/etc/init/audioserver.rc || \
  printf "\nservice tvfixsshd /system/bin/sh /system/bin/start-sshd.sh\n    class main\n    user root\n    oneshot\n    seclabel u:r:su:s0\n" >> /system/etc/init/audioserver.rc
tail -12 /system/etc/init/audioserver.rc

echo "== 3. 立即启动验证 =="
sh /system/bin/dnsfixd.sh &
sh /system/bin/start-sshd.sh &
sleep 30
ndc resolver setnetdns 100 "" 223.5.5.5 119.29.29.29
ping -c1 -W2 www.baidu.com | head -1
sync
echo "== DONE: 重启后 init 会自动拉起 tvfixdns / tvfixsshd =="
