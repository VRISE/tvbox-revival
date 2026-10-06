package com.sumavision.loader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Patched build: the operator login flow is gone. Hand straight to the Dangbei desktop.
        try {
            Intent i = new Intent();
            i.setClassName("com.dangbei.tvlauncher", "com.dangbei.tvlauncher.activity.SplashActivity");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent h = new Intent(Intent.ACTION_MAIN);
                h.addCategory(Intent.CATEGORY_HOME);
                h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(h);
            } catch (Exception ignored) {
            }
        }
        finish();
    }
}
