#!/system/bin/sh
LOG=/data/local/tmp/dnsfixd.log
echo "dnsfixd start $(date)" >> $LOG
sleep 15
while true; do
  echo "$(date) $(ndc resolver setnetdns 100 '' 223.5.5.5 119.29.29.29 2>&1)" >> $LOG
  sleep 45
done
