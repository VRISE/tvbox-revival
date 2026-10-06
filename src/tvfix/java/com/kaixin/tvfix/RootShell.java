package com.kaixin.tvfix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class RootShell {

    /** Runs a shell script as root. This box has an open /system/xbin/su (syntax: su <uid> <cmd>). */
    public static String run(String script) {
        String[][] attempts = {
                {"/system/xbin/su", "0", "sh"},
                {"su", "0", "sh"},
                {"su", "sh"},
        };
        String last = "no su attempt succeeded";
        for (String[] cmd : attempts) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                OutputStream in = p.getOutputStream();
                in.write(script.getBytes("UTF-8"));
                in.flush();
                in.close();
                StringBuilder sb = new StringBuilder();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                    if (sb.length() > 8000) break;
                }
                p.waitFor();
                String out = sb.toString().trim();
                if (!out.isEmpty()) return out;
                last = cmd[0] + " produced no output (exit " + p.exitValue() + ")";
            } catch (Exception e) {
                last = cmd[0] + " failed: " + e;
            }
        }
        return "ROOT_FAIL: " + last + "\n(若失败：先在官方设置里打开调试模式，或确认ROM带su)";
    }
}
