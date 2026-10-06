package com.kaixin.tvfix;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    static final String SCRIPT_PERSIST = ""
        + "mount -o remount,rw /system 2>/dev/null || mount -o remount,rw / 2>/dev/null\n"
        + "if ! grep -q '^service.adb.tcp.port=' /system/build.prop; then echo 'service.adb.tcp.port=5555' >> /system/build.prop; fi\n"
        + "sync\n"
        + "echo RESULT $(grep '^service.adb.tcp.port=' /system/build.prop)\n";

    static final String SCRIPT_HOME = ""
        + "pm enable com.dangbei.tvlauncher\n"
        + "cmd package set-home-activity com.dangbei.tvlauncher/com.dangbei.tvlauncher.activity.SplashActivity\n"
        + "echo RESULT_HOME $(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)\n";

    static final String SCRIPT_ENSURE_HOME = ""
        + "FOCUS=$(dumpsys window | grep mCurrentFocus)\n"
        + "case \"$FOCUS\" in\n"
        + "  *sumavision.loader*|*yst.whitebox*)\n"
        + "    cmd package set-home-activity com.dangbei.tvlauncher/com.dangbei.tvlauncher.activity.SplashActivity\n"
        + "    am start -a android.intent.action.MAIN -c android.intent.category.HOME\n"
        + "    echo RESULT_ENFORCED: $FOCUS\n"
        + "    ;;\n"
        + "  *) echo RESULT_OK: $FOCUS ;;\n"
        + "esac\n";

    static final String SCRIPT_STATUS = ""
        + "echo ROOT: $(id)\n"
        + "echo ADB_TCP: $(getprop service.adb.tcp.port)\n"
        + "echo ADBD_SVC: $(getprop init.svc.adbd)\n"
        + "echo HOME: $(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)\n"
        + "ip -f inet addr show | grep inet\n";

    private TextView log;

    /** 全局未知来源开关：priv-app + WRITE_SECURE_SETTINGS 即可写，免 root。
     *  个别商店若仍被拦，是按应用 appops(REQUEST_INSTALL_PACKAGES) 被单独 deny，
     *  需 adb 跑 scripts/fix_unknown_sources.sh 逐个放行。 */
    private void allowUnknownSources() {
        append("\n>>> 放行未知来源安装...");
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                android.provider.Settings.Global.putInt(
                        getContentResolver(), "install_non_market_apps", 1);
                sb.append("install_non_market_apps=1 ok\n");
            } catch (Throwable t) {
                sb.append("global failed: ").append(t.getMessage()).append('\n');
            }
            append(sb + "若个别商店仍被拦(按应用 appops 屏蔽)，需 adb 跑\nscripts/fix_unknown_sources.sh\n");
            LogFile.append(this, "unknown sources: " + sb.toString().replace('\n', ' '));
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 14);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("M401H 修复助手 v7\n不动运营商应用，只提优先级");
        title.setTextSize(20);
        root.addView(title);

        log = new TextView(this);
        log.setTextSize(14);
        log.setText("就绪。");

        addButton(root, "① 查看当前状态", () -> runScript(SCRIPT_STATUS));
        addButton(root, "② 开启网络ADB（免root·属性直达）", this::enableAdbNoRoot);
        addButton(root, "③ ADB永久自启（写入build.prop）", () -> runScript(SCRIPT_PERSIST));
        addButton(root, "④ 当贝设为默认桌面（提优先级）", () -> runScript(SCRIPT_HOME));
        addButton(root, "⑤ 桌面被抢占？一键拉回当贝", () -> runScript(SCRIPT_ENSURE_HOME));
        addButton(root, "⑥ 允许安装未知来源（免root）", this::allowUnknownSources);

        root.addView(log);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void addButton(LinearLayout root, String text, final Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(v -> action.run());
        root.addView(b);
    }

    /** No-root ADB enable: permissive SELinux lets a plain app set props and ctl.restart adbd. */
    private void enableAdbNoRoot() {
        append("\n>>> 免root开启ADB...\n");
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append(SysProp.set("service.adb.tcp.port", "5555")).append('\n');
            sb.append(SysProp.set("persist.adb.tcp.port", "5555")).append('\n');
            sb.append(SysProp.set("ctl.restart", "adbd")).append('\n');
            try {
                android.provider.Settings.Global.putInt(
                        getContentResolver(), "adb_enabled", 1);
                sb.append("adb_enabled=1 ok\n");
            } catch (Throwable t) {
                sb.append("adb_enabled: ").append(t.getMessage()).append('\n');
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
            sb.append("init.svc.adbd=").append(SysProp.get("init.svc.adbd")).append('\n');
            sb.append("port=").append(SysProp.get("service.adb.tcp.port")).append('\n');
            LogFile.append(this, "button2: " + sb.toString().replace('\n', ' '));
            append(sb + "\n>>> 完成\n");
        }).start();
    }

    private void runScript(final String script) {
        append("\n>>> 执行中...");
        new Thread(() -> {
            String out = RootShell.run(script);
            append("\n" + out + "\n>>> 完成\n");
        }).start();
    }

    private void append(final String s) {
        runOnUiThread(() -> log.setText(log.getText() + s));
    }
}
