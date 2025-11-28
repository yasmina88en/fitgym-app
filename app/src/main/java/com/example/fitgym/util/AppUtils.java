package com.example.fitgym.util;

import android.content.Context;

public final class AppUtils {
    private AppUtils() {}

    public static String getCurrentClientId(Context ctx) {
        if (ctx == null) return null;
        return ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("current_client_id", null);
    }
}
