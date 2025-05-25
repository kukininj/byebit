package com.example.byebit;

import android.app.Application;
import android.util.Log;

public class ByeBitApplication extends Application {
    private static final String TAG = "ByeBitApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate called. Scheduling work.");
    }
}
