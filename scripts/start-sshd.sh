#!/system/bin/sh
# started by init (service tvfixsshd, class main) - bring up termux sshd as uid 10037
LOG=/data/local/tmp/dnsfixd.log
sleep 20
su 10037 sh -c 'export HOME=/data/data/com.termux/files/home LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib; pgrep -f usr/bin/sshd >/dev/null 2>&1 || /data/data/com.termux/files/usr/bin/sshd' >> $LOG 2>&1
