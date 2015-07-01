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
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;

import java.util.concurrent.TimeUnit;

import io.e55.fovea.android.sdk.camera.lib.CameraConfig;
import io.e55.fovea.android.sdk.camera.lib.CameraManager;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;


/**
 * This module manages the focus for the Camera.
 *
 * It focuses the Camera just after Camera preview has been started (InitialFocus).
 *
 * It performs an auto focus when the user generates a {@link MotionEvent} on the
 * {@link android.view.SurfaceView} (TouchEvent focus).
 *
 * It makes use of sensors such as the Accelerometer to automatically refocus when the phone
 * is moved a significant distance (Accelerometer focus).
 *
 * It can also refocus periodically at a given time interval (ContinuousFocus).
 */
public class CameraFocusModule extends CameraModule {

    private static final String TAG = CameraFocusModule.class.getSimpleName();

    private final SensorManager mSensorManager;
    private final int mAutoFocusInitialDelay, mContinuousFocusInterval, mAutoFocusBusy;
    private final float mFocusOnAcceleration;

    private Camera mCamera;

    private Subscription mInitialFocusSubscription, mContinuousFocusSubscription, mTouchEventSubscription;
    private AccelerometerListener mAccelerometerListener;

    private long mLastUpdated;

    public CameraFocusModule(Context context, CameraConfig config) {
        super(context, config);
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        mAutoFocusInitialDelay = config.autoFocusInitialDelay();
        mContinuousFocusInterval = config.continuousFocusInterval();
        mAutoFocusBusy = mConfig.autoFocusIntervalBusy();
        mFocusOnAcceleration = mConfig.focusOnAcceleration();
    }

    @Override
    public void start(final CameraManager cameraManager) {
        stop();
        mCamera = cameraManager.getCamera();

        Observable<MotionEvent> touchEventObservable = cameraManager.getTouchEventObservable();
        mTouchEventSubscription = touchEventObservable.subscribe(new Action1<MotionEvent>() {
            @Override
            public void call(MotionEvent motionEvent) {
                Timber.d("Touch");
                autoFocus(null);
            }
        });

        if (mConfig.useFocusOnAcceleration()) {
            Sensor accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accSensor != null) {
                mAccelerometerListener = new AccelerometerListener(cameraManager);
                boolean isSupported = mSensorManager.registerListener(
                        mAccelerometerListener, accSensor, SensorManager.SENSOR_DELAY_NORMAL);

                if (!isSupported) {
                    mSensorManager.unregisterListener(mAccelerometerListener);
                }
            } else {
                Timber.w("Accelerometer sensor not available");
            }
        }

        if (mConfig.useContinuousFocus()) {
            mContinuousFocusSubscription = Observable.interval(mContinuousFocusInterval,
                    TimeUnit.MILLISECONDS, Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            if (mCamera == null) {
                                return;
                            }

                            if (isBusy()) {
                                return;
                            }

                            autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    if (camera == null || !cameraManager.isPreviewing()) {
                                        Timber.d("mCamera not previewing or null");
                                        //stop();
                                    }
                                }
                            });
                        }
                    });
        }

        mInitialFocusSubscription = Observable.timer(mAutoFocusInitialDelay,
                TimeUnit.MILLISECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        autoFocus(null);
                    }
                });
        Timber.d("start()");
    }

    @Override
    public void stop() {
        if (mAccelerometerListener != null) {
            mSensorManager.unregisterListener(mAccelerometerListener);
        }

        if (mInitialFocusSubscription != null) {
            mInitialFocusSubscription.unsubscribe();
        }

        if (mContinuousFocusSubscription != null) {
            mContinuousFocusSubscription.unsubscribe();
        }

        if (mTouchEventSubscription != null) {
            mTouchEventSubscription.unsubscribe();
        }

        mCamera = null;
        Timber.d("stop()");
    }


    private boolean isBusy() {
        return (mLastUpdated > 0 && System.currentTimeMillis() - mLastUpdated < mAutoFocusBusy);
    }


    public void autoFocus(Camera.AutoFocusCallback callback) {
        boolean didFocus = false;

        if (mCamera != null) {
            try {
                mCamera.autoFocus(callback);
                mLastUpdated = System.currentTimeMillis();
                didFocus = true;
            } catch (RuntimeException e) {
                Timber.e(e, TAG);
            }
        }

        if (callback != null) {
            callback.onAutoFocus(didFocus, mCamera);
        }

        //Timber.d("FOCUS: %s", didFocus);
    }


    private class AccelerometerListener implements SensorEventListener {
        final CameraManager mCameraManager;

        private float[] mGravity;
        private float mAccel;
        private float mAccelCurrent;
        private float mAccelLast;


        public AccelerometerListener(CameraManager cameraManager) {
            mCameraManager = cameraManager;

            mAccel = 0.00f;
            mAccelCurrent = SensorManager.GRAVITY_EARTH;
            mAccelLast = SensorManager.GRAVITY_EARTH;
        }

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (mCamera == null) {
                    return;
                }

                if (isBusy()) {
                    return;
                }

                mGravity = event.values.clone();
                // Shake detection
                float x = mGravity[0];
                float y = mGravity[1];
                float z = mGravity[2];

                mAccelLast = mAccelCurrent;
                mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);

                float delta = mAccelCurrent - mAccelLast;
                mAccel = mAccel * 0.9f + delta;
                // Make this higher or lower according to how much
                // motion you want to detect

                if (mAccel > mFocusOnAcceleration) {
                    autoFocus(null);
                }
            }
        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // nothing
        }
    }
}
