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

package io.e55.fovea.android.sdk.camera.lib;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.e55.fovea.android.sdk.camera.lib.modules.CameraCaptureModule;
import io.e55.fovea.android.sdk.camera.lib.modules.CameraFlashModule;
import io.e55.fovea.android.sdk.camera.lib.modules.CameraModule;
import io.e55.fovea.android.sdk.camera.lib.modules.CameraModuleFactory;
import io.e55.fovea.android.sdk.camera.lib.modules.CameraModuleFactory.CameraModules;
import io.e55.fovea.android.sdk.camera.lib.modules.CameraShadeModule;
import io.e55.fovea.android.sdk.camera.ui.view.CameraPreviewTracker;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Manages operations of the Camera hardware.
 *
 * Use this class to gain access to the Camera hardware, start the Camera preview
 * and release related resources once done.
 *
 * Use the `setCameraPreviewTracker()` method to supply the {@link CameraPreviewTracker}
 * so that this class can tap into it's {@link Observable}s and react to changes in
 * the Camera's drawing surface ({@link android.view.SurfaceView}).
 */
@SuppressWarnings("deprecation")
public class CameraManager {
    private static final String TAG = CameraManager.class.getSimpleName();

    private final CameraConfig mConfig;
    private final CameraModuleFactory mCameraModuleFactory;
    private final WindowManager mWindowManager;

    private int mRearCameraId = -1;
    private Camera mCamera;

    private boolean mIsOpening, mIsPreviewing, mIsCapturing;

    private final List<Subscription> mSubscriptions;

    private final Map<String, CameraModule> mModules;

    private Observable<MotionEvent> mTouchEventObservable;

    private View mCameraShadeView;

    public CameraManager(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Activity.WINDOW_SERVICE);
        mConfig = new CameraConfig(context);

        mCameraModuleFactory = new CameraModuleFactory(context, mConfig);
        mModules = new HashMap<>();
        initModules();

        mSubscriptions = new ArrayList<>();
    }


    protected void toggleCameraOpen(SurfaceHolder holder) {
        if (holder != null) {
            openCamera(holder);
        } else {
            closeCamera();
        }
    }

    protected void openCamera(final SurfaceHolder holder) {
        if (mIsOpening) {
            return;
        }

        Timber.d("start");
        mIsOpening = true;

        startCamera().subscribe(new Subscriber<Camera>() {
            @Override
            public void onCompleted() {
                mIsOpening = false;
            }

            @Override
            public void onError(Throwable e) {
                mIsOpening = false;
                Timber.e(e, TAG);
            }

            @Override
            public void onNext(Camera camera) {
                mCamera = camera;

                if (mCamera != null) {
                    try {
                        // set Parameters for the Camera
                        mConfig.configure(CameraManager.this);

                        setPreviewSurface(holder);
                        startPreview();
                    } catch (IOException | RuntimeException e) {
                        Timber.e(e, TAG);
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    protected void closeCamera() {
        mIsOpening = false;

        try {
            Timber.d("release");
            setPreviewSurface(null);
            stopPreview();
            releaseCamera();
        } catch (IOException e) {
            Timber.e(e, TAG);
            e.printStackTrace();
        }
    }


    protected void setPreviewSurface(SurfaceHolder holder) throws IOException {
        if (mCamera != null) {
            if (holder != null) {
                holder.setSizeFromLayout();
                holder.setKeepScreenOn(true);
            }

            mCamera.setPreviewDisplay(holder);
            Timber.d("setPreviewDisplay: %b", (holder != null));
        }
    }


    protected Observable<Camera> startCamera() {
        if (mCamera == null) {
            if (mRearCameraId < 0) {
                mRearCameraId = findRearCameraIndex();
            }

            if (mRearCameraId >= 0) {
                Timber.d("startCamera");

                // We start the Camera async on an IO thread
                // Without this, there's a slight freeze in the UI.
                // (Choreographer shows 'skipped frames').

                return Observable.create(new Observable.OnSubscribe<Camera>() {
                    @Override
                    public void call(Subscriber<? super Camera> subscriber) {
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }

                        subscriber.onStart();
                        subscriber.onNext(Camera.open(mRearCameraId));
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
            }
        }

        return Observable.error(new IOException("Rear camera unavailable."));
    }


    protected void releaseCamera() {
        if (mCamera != null) {
            Timber.d("releaseCamera");
            mCamera.release();
            mCamera = null;
        }
    }


    /**
     * Configure the Camera's orientation according to the screen's current orientation.
     */
    protected void updateCameraOrientation() {
        if (mCamera != null) {
            int displayOrientation = getDisplayOrientation(getDisplay(), mRearCameraId);
            if (displayOrientation >= 0) {
                mCamera.setDisplayOrientation(displayOrientation);
            }
        }
    }


    protected void startPreview() {
        if (mCamera != null && !mIsPreviewing) {
            try {
                mIsPreviewing = true;
                updateCameraOrientation();
                mCamera.startPreview();
                startModules();
                Timber.d("startPreview");
            } catch (RuntimeException e) {
                Timber.e(e, TAG);
            }
        }
    }


    protected void stopPreview() {
        if (mCamera != null && mIsPreviewing) {
            try {
                mIsOpening = false;
                mIsPreviewing = false;
                mIsCapturing = false;

                mCamera.stopPreview();
                stopModules();
                Timber.d("stopPreview");
            } catch (RuntimeException e) {
                Timber.e(e, TAG);
            }
        }
    }


    /**
     * Status of the Camera preview.
     *
     * @return true if the Camera preview is currently active.
     */
    public boolean isPreviewing() {
        return mIsPreviewing;
    }


    /**
     * Reference to the Camera object that was opened.
     *
     * @return Camera
     */
    public Camera getCamera() {
        return mCamera;
    }


    /**
     * Returns the Observable for the MotionEvents on the SurfaceView.
     *
     * @return Observable for touch events on the SurfaceView
     */
    public Observable<MotionEvent> getTouchEventObservable() {
        return mTouchEventObservable;
    }


    /**
     * Status of the Flash light.
     *
     * @return true if Flash is currently on.
     */
    public boolean isFlashOn() {
        return (mCamera != null) &&
                mModules.containsKey(CameraModules.FLASH) &&
                ((CameraFlashModule) mModules.get(CameraModules.FLASH)).isFlashOn();
    }


    /**
     * Toggles the Flash light.
     *
     * @param on if true, it switched the flash on (if not already on), else off.
     * @return true if the operation was successful.
     */
    public boolean setFlash(boolean on) {
        return (mCamera != null) &&
                mModules.containsKey(CameraModules.FLASH) &&
                ((CameraFlashModule) mModules.get(CameraModules.FLASH)).setFlash(on);
    }


    /**
     * Persists the currently visible Camera frame to the specified file path.
     *
     * @param path Write-accessible path for the image file to be stored.
     * @param subscriber {@link Subscriber} that should receive status feedback.
     * @return true, if the operation was successfully initiated.
     */
    public boolean takePicture(String path, final Subscriber<String> subscriber) {
        Timber.d("takePicture");

        if (mIsCapturing || mCamera == null || !mModules.containsKey(CameraModules.CAPTURE)) {
            return false;
        }

        mIsCapturing = true;

        Observable<String> ob = ((CameraCaptureModule) mModules.get(CameraModules.CAPTURE))
                .takePicture(path)
                .share();

        ob.subscribe(subscriber);
        ob.subscribe(new Subscriber<String>() {
            @Override
            public void onStart() {
                super.onStart();

                // Cover the camera preview with the Camera shade
                toggleCameraShade(true);
            }

            @Override
            public void onCompleted() {
                mIsCapturing = false;

                // stop and release the Camera
                toggleCameraOpen(null);
            }

            @Override
            public void onError(Throwable e) {
                mIsCapturing = false;
            }

            @Override
            public void onNext(String s) {
                //nothing
            }
        });

        return true;
    }


    /**
     * Returns the view that covers the Camera when it's not active.
     * Visibility for the view is controller by the {@link CameraShadeModule} module, if active.
     *
     * @return View for the Camera shade.
     */
    public View getCameraShadeView() {
        return mCameraShadeView;
    }


    /**
     * View that should cover the Camera when it's not active.
     *
     * @param cameraShadeView View for the Camera shade.
     */
    public void setCameraShadeView(View cameraShadeView) {
        mCameraShadeView = cameraShadeView;
    }


    /**
     * Toggles the visibility of the Camera shade.
     *
     * @param show if true, it covers the Camera preview with the Camera shade view.
     * @return true, if the operation was successfully initiated.
     */
    public boolean toggleCameraShade(boolean show) {
        if (!mModules.containsKey(CameraModules.SHADE)) {
            return false;
        }

        ((CameraShadeModule) mModules.get(CameraModules.SHADE))
                .toggleShade(show);
        return true;
    }


    /**
     * Set the {@link CameraPreviewTracker} so that this class can utilize its {@link Observable}s.
     *
     * @param cpt {@link CameraPreviewTracker}
     */
    public void setCameraPreviewTracker(CameraPreviewTracker cpt) {
        clearSubscriptions();

        mSubscriptions.add(cpt.surfaceHolderObservable.subscribe(new Action1<SurfaceHolder>() {
            @Override
            public void call(SurfaceHolder holder) {
                toggleCameraOpen(holder);
            }
        }));

        mSubscriptions.add(cpt.surfaceChangeObservable.subscribe(new Action1<Point>() {
            @Override
            public void call(Point point) {
                if (mIsPreviewing) {
                    stopPreview();
                }

                startPreview();
            }
        }));

        mTouchEventObservable = cpt.touchEventObservable;
    }


    /**
     * Returns the current display.
     *
     * @return Display
     */
    public Display getDisplay() {
        return mWindowManager.getDefaultDisplay();
    }


    protected void clearSubscriptions() {
        for (Subscription s : mSubscriptions) {
            if (!s.isUnsubscribed()) {
                s.unsubscribe();
            }
        }

        mSubscriptions.clear();
    }

    protected void initModules() {
        List<String> enabledModules = mConfig.getCameraModules();

        for (String mod : enabledModules) {
            if (!mModules.containsKey(mod)) {
                CameraModule c = mCameraModuleFactory.get(mod);
                if (c != null) {
                    mModules.put(mod, c);
                }
            }
        }
    }

    protected void startModules() {
        Timber.d("startModules");
        for (CameraModule c : mModules.values()) {
            c.start(this);
        }
    }

    protected void stopModules() {
        Timber.d("stopModules");
        for (CameraModule c : mModules.values()) {
            c.stop();
        }
    }


    protected static int findRearCameraIndex() {
        int noCameraIndex = -1;

        int camCount = Camera.getNumberOfCameras();
        if (camCount == 0) {
            Timber.w("No cameras");
            return noCameraIndex;
        }

        for (int i = 0; i < camCount; i++) {
            CameraInfo cameraInfo = new CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }

        return noCameraIndex;
    }


    public static int getDisplayOrientation(Display display, int cameraId) {
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        int rotation = display.getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        return (cameraInfo.orientation - degrees + 360) % 360;
    }

}
