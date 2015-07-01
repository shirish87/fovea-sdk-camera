/*
 * Copyright (c) 2015 Shirish Kamath.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.e55.fovea.android.sdk.camera.lib.modules;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import io.e55.fovea.android.sdk.camera.lib.CameraConfig;
import io.e55.fovea.android.sdk.camera.lib.CameraManager;
import timber.log.Timber;

public class CameraSensorModule extends CameraModule {

    private final SensorManager mSensorManager;
    private LightSensorListener mLightSensorListener;

    public CameraSensorModule(Context context, CameraConfig config) {
        super(context, config);
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Timber.d("CameraSensorModule");
    }

    @Override
    public void start(final CameraManager cameraManager) {
        Timber.d("CameraSensorModule.start");

        Sensor lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor != null) {
            stop();

            mLightSensorListener = new LightSensorListener(cameraManager);
            boolean isSupported = mSensorManager.registerListener(
                    mLightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

            if (!isSupported) {
                mSensorManager.unregisterListener(mLightSensorListener);
            }
        } else {
            Timber.d("Light sensor not available");
        }
    }

    @Override
    public void stop() {
        Timber.d("CameraSensorModule.stop");

        if (mLightSensorListener != null) {
            mSensorManager.unregisterListener(mLightSensorListener);
            mLightSensorListener = null;
        }
    }

    private class LightSensorListener implements SensorEventListener {
        final CameraManager mCameraManager;

        public LightSensorListener(CameraManager cameraManager) {
            mCameraManager = cameraManager;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values.length > 0) {
                float value = event.values[0];

                if (mConfig.isBrightEnough(value) && !mCameraManager.isFlashOn()) {
                    mCameraManager.setFlash(false);
                } else if (mConfig.isLightTooDark(value)) {
                    mCameraManager.setFlash(true);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}
