#!/system/bin/sh
# Pin mirror IPs into /system/etc/hosts (bypass broken per-uid DNS), then termux sshd
TUID=10037
PREFIX=/data/data/com.termux/files/usr
export HOME=/data/data/com.termux/files/home
export LD_LIBRARY_PATH=$PREFIX/lib
export PATH=$PREFIX/bin:$PATH
export TMPDIR=$PREFIX/tmp
export TERM=xterm
BB=/system/xbin/busybox

echo "== 1. hosts pin (root) =="
mount -o remount,rw /system 2>/dev/null || mount -o remount,rw /
grep -q mirrors.ustc.edu.cn /system/etc/hosts || \
  printf "202.141.160.110 mirrors.ustc.edu.cn\n202.38.95.110 mirrors.ustc.edu.cn\n" >> /system/etc/hosts
cat /system/etc/hosts
sync

echo "== 2. point termux at ustc mirror =="
echo "deb https://mirrors.ustc.edu.cn/termux/apt/termux-main stable main" > $PREFIX/etc/apt/sources.list
chown $TUID:$TUID $PREFIX/etc/apt/sources.list

echo "== 3. apt update as termux uid =="
$BB setuidgid $TUID apt update 2>&1 | tail -4

echo "== 4. apt install openssh =="
$BB setuidgid $TUID apt install -y openssh 2>&1 | tail -6
ls -la $PREFIX/bin/sshd 2>&1

echo "== 5. start sshd =="
$BB setuidgid $TUID $PREFIX/bin/sshd && echo SSHD_STARTED
sleep 1
ps -A | grep sshd
echo "== DONE =="
