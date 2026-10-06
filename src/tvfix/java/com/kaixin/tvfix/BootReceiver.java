package com.kaixin.tvfix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        final PendingResult result = goAsync();
        LogFile.append(context, "BOOT_COMPLETED received");
        new Thread(() -> {
            try {
                Thread.sleep(15000); // wait for boot to settle
            } catch (InterruptedException ignored) {
            }
            // No-su ADB persistence: SELinux is permissive on this box, plain property
            // sets from an app are allowed. ctl.restart brings adbd back with the tcp port.
            StringBuilder sb = new StringBuilder();
            sb.append(SysProp.set("service.adb.tcp.port", "5555")).append('\n');
            sb.append(SysProp.set("persist.adb.tcp.port", "5555")).append('\n');
            sb.append(SysProp.set("ctl.restart", "adbd")).append('\n');
            try {
                android.provider.Settings.Global.putInt(
                        context.getContentResolver(), "adb_enabled", 1);
                sb.append("adb_enabled=1 (settings ok)").append('\n');
            } catch (Throwable t) {
                sb.append("adb_enabled write failed: ").append(t).append('\n');
            }
            LogFile.append(context, sb.toString().replace('\n', ' '));

            try {
                Thread.sleep(10000); // give operator watchdogs their window
            } catch (InterruptedException ignored) {
            }
            // nudge home: HOME intent resolves to the patched loader (priority 1) -> Dangbei
            try {
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.addCategory(Intent.CATEGORY_HOME);
                home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(home);
                LogFile.append(context, "home nudge sent");
            } catch (Throwable t) {
                LogFile.append(context, "home nudge failed: " + t);
            }
            // best effort with su (may be refused by the su whitelist)
            LogFile.append(context, "su HOME: " + RootShell.run(MainActivity.SCRIPT_HOME));
            result.finish();
        }).start();
    }
}
