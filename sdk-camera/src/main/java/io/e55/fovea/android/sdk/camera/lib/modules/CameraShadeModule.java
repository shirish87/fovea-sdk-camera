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
import android.view.View;

import java.util.concurrent.TimeUnit;

import io.e55.fovea.android.sdk.camera.lib.CameraConfig;
import io.e55.fovea.android.sdk.camera.lib.CameraManager;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * This module manages the visibility of a view that overlays the Camera's SurfaceView and
 * other UI components so that the overall experience is smooth.
 *
 * Without this, the lifecycle changes of Camera-related objects in the UI
 * can appear jarring to the user.
 */
public class CameraShadeModule extends CameraModule {

    private Camera mCamera;
    private View mCameraShadeView;

    private final boolean mUseCameraShade;
    private final int mCameraShadeInitDelay;

    CameraShadeModule(Context context, CameraConfig config) {
        super(context, config);

        mUseCameraShade = config.useCameraShade();
        mCameraShadeInitDelay = config.cameraShadeInitDelay();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void start(CameraManager cameraManager) {
        mCamera = cameraManager.getCamera();
        mCameraShadeView = cameraManager.getCameraShadeView();

        if (mCameraShadeView == null) {
            Timber.d("No CameraShadeView found. Stopping module.");
            stop();
            return;
        }

        if (mCamera != null) {
            toggleShade(true);

            if (mUseCameraShade) {
                mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        if (mCameraShadeInitDelay > 0) {
                            Observable.timer(mCameraShadeInitDelay,
                                    TimeUnit.MILLISECONDS, Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Action1<Long>() {
                                        @Override
                                        public void call(Long aLong) {
                                            if (mCamera != null) {
                                                toggleShade(false);
                                            }
                                        }
                                    });
                        } else {
                            toggleShade(false);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void stop() {
        toggleShade(true);

        mCameraShadeView = null;
        mCamera = null;
    }

    public void toggleShade(boolean show) {
        if (mCameraShadeView != null) {
            Timber.d("displayShade: %s", show);
            mCameraShadeView.setVisibility(mUseCameraShade && show ? View.VISIBLE : View.GONE);
        }
    }
}
