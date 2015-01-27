package com.dpg.crowdscout;

import android.app.Application;
import android.content.Context;

public class CrowdScoutApp extends Application {
    /**
     * Application Context stored as a singleton to allow for Android resources/assets
     * to be retrieved via the Helpers utility class.
     */
    private static Context s_context;

    public static Context getAppContext() {
        return s_context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        s_context = getApplicationContext();
    }
}
