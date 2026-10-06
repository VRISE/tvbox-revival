package com.kaixin.tvfix;

import android.content.Context;

import java.io.OutputStream;

public class LogFile {
    public static void append(Context ctx, String msg) {
        try {
            OutputStream os = ctx.openFileOutput("boot.log", Context.MODE_APPEND);
            os.write((System.currentTimeMillis() + " " + msg + "\n").getBytes("UTF-8"));
            os.flush();
            os.close();
        } catch (Exception ignored) {
        }
    }
}
