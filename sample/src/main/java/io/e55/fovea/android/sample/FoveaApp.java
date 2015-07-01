package io.e55.fovea.android.sample;

import android.app.Application;
import android.os.StrictMode;

import io.e55.fovea.android.sdk.camera.lib.CameraConfig;
import timber.log.Timber;

public class FoveaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //StrictMode.enableDefaults();

        CameraConfig.init(this);
        Timber.plant(new Timber.DebugTree());
    }
}
