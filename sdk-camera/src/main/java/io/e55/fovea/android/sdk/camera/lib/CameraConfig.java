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
 * ---
 *
 * This file contains code from the zxing project at:
 * https://github.com/zxing/zxing
 *
 * LICENSE:
 *
 * Copyright (C) 2014 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.e55.fovea.android.sdk.camera.lib;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Display;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import io.e55.fovea.android.sdk.camera.R;
import io.e55.fovea.android.sdk.camera.lib.modules.CameraModuleFactory.CameraModules;
import timber.log.Timber;


/**
 * Singleton that loads configuration from resource files and provides them to various components.
 *
 * It is also used to set {@link android.hardware.Camera.Parameters}
 * for a given {@link Camera} instance.
 */
@SuppressWarnings("deprecation")
public class CameraConfig {

    private static final String TAG = CameraConfig.class.getSimpleName();

    private static final int MIN_PREVIEW_PIXELS = 720 * 480; // normal screen

    private static final String CAPTURE_TMP_FILENAME = "fovea.jpg";

    public static final String[] FLASH_ON_MODES = new String[]{
            Camera.Parameters.FLASH_MODE_TORCH,
            Camera.Parameters.FLASH_MODE_ON
    };

    public static final String[] FLASH_OFF_MODES = new String[]{
            Camera.Parameters.FLASH_MODE_OFF
    };

    public static final String[] FOCUS_ON_MODES = new String[]{
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
            Camera.Parameters.FOCUS_MODE_AUTO
    };

    public static final String ORIENTATION_PORTRAIT = "portrait";
    public static final String ORIENTATION_LANDSCAPE = "landscape";

    private static volatile CameraConfig instance;

    private final int mAutoFocusInitialDelay;

    private final int mAutoFocusIntervalBusy;

    private final int mContinuousFocusInterval;

    private final boolean mUseContinuousFocus;

    private final float mFocusOnAcceleration;

    private final boolean mUseFocusOnAcceleration;

    private final float mLuxTooDark;

    private final float mLuxBrightEnough;

    private final String mCaptureOrientation;

    private final boolean mIsCaptureOrientationLandscape;

    private final int mCaptureAdjustOrientation;

    private final String mCaptureTmpFilename;

    private final int mCaptureJpegQuality;

    private final int mCaptureMaxSide;

    private final boolean mCapturePreviewFrame;

    private final boolean mUseCameraShade;

    private final int mCameraShadeInitDelay;

    private final int mMinPictureSide;

    private final float mMaxAspectDistortion;

    public static CameraConfig init(Application app) {
        return getInstance(app.getApplicationContext());
    }

    public static CameraConfig getInstance(final Context context) {
        if (instance == null) {
            synchronized (CameraConfig.class) {
                if (instance == null) {
                    instance = new CameraConfig(context);
                }
            }
        }

        return instance;
    }


    CameraConfig(Context context) {
        Resources res = context.getResources();

        mAutoFocusInitialDelay = res.getInteger(R.integer.auto_focus_init_delay);
        mAutoFocusIntervalBusy = res.getInteger(R.integer.auto_focus_interval_busy);

        mUseContinuousFocus = res.getBoolean(R.bool.use_continuous_focus);
        mContinuousFocusInterval = res.getInteger(R.integer.continuous_focus_interval);

        mUseFocusOnAcceleration = res.getBoolean(R.bool.use_focus_on_accel);
        mFocusOnAcceleration = readFloat(res, R.dimen.focus_on_accel);

        mLuxTooDark = readFloat(res, R.dimen.lux_too_dark);
        mLuxBrightEnough = readFloat(res, R.dimen.lux_bright_enough);

        mCaptureOrientation = res.getString(R.string.capture_orientation).toLowerCase();

        if (!(mCaptureOrientation.equals(ORIENTATION_PORTRAIT) ||
                mCaptureOrientation.equals(ORIENTATION_LANDSCAPE))) {
            throw new IllegalStateException("Invalid orientation: " + mCaptureOrientation);
        }

        mIsCaptureOrientationLandscape = mCaptureOrientation.equals(ORIENTATION_LANDSCAPE);
        mCaptureAdjustOrientation = res.getInteger(R.integer.capture_adjust_orientation);

        mCaptureJpegQuality = res.getInteger(R.integer.capture_jpeg_quality);
        mCaptureMaxSide = res.getInteger(R.integer.capture_max_side);

        mCapturePreviewFrame = res.getBoolean(R.bool.capture_preview_frame);

        mUseCameraShade = res.getBoolean(R.bool.use_camera_shade);
        mCameraShadeInitDelay = res.getInteger(R.integer.camera_shade_init_delay);

        mMinPictureSide = res.getInteger(R.integer.min_picture_side);
        mMaxAspectDistortion = readFloat(res, R.dimen.max_aspect_distortion);

        mCaptureTmpFilename = res.getString(R.string.capture_tmp_filename);
    }

    public List<String> getCameraModules() {
        List<String> modules = new ArrayList<>();
        modules.add(CameraModules.AUTO_FOCUS);
        modules.add(CameraModules.FLASH);
        modules.add(CameraModules.CAPTURE);
        modules.add(CameraModules.SHADE);

        //LUX values vary far too much across devices
        //modules.add(CameraModules.LIGHT_SENSOR);

        return modules;
    }

    public String getCaptureOrientation() {
        return mCaptureOrientation;
    }

    public boolean isCaptureOrientationLandscape() {
        return mIsCaptureOrientationLandscape;
    }

    public int autoFocusInitialDelay() {
        return mAutoFocusInitialDelay;
    }

    public int autoFocusIntervalBusy() {
        return mAutoFocusIntervalBusy;
    }

    public int continuousFocusInterval() {
        return mContinuousFocusInterval;
    }

    public boolean useContinuousFocus() {
        return mUseContinuousFocus;
    }

    public float focusOnAcceleration() {
        return mFocusOnAcceleration;
    }

    public boolean useFocusOnAcceleration() {
        return mUseFocusOnAcceleration;
    }

    public boolean isLightTooDark(float value) {
        return (value <= mLuxTooDark);
    }

    public boolean isBrightEnough(float value) {
        return (value >= mLuxBrightEnough);
    }

    public int captureJpegQuality() {
        return mCaptureJpegQuality;
    }

    public int captureAdjustOrientation() {
        return mCaptureAdjustOrientation;
    }

    public int captureMaxSide() {
        return mCaptureMaxSide;
    }

    public boolean capturePreviewFrame() {
        return mCapturePreviewFrame;
    }

    public boolean useCameraShade() {
        return mUseCameraShade;
    }

    public int cameraShadeInitDelay() {
        return mCameraShadeInitDelay;
    }

    public int getMinPictureSide() {
        return mMinPictureSide;
    }


    public static String getOutputFilePath() {
        return Environment.getExternalStorageDirectory().getPath() +
                File.separator +
                ((instance != null) ? instance.mCaptureTmpFilename : CAPTURE_TMP_FILENAME);
    }


    public boolean configure(CameraManager cameraManager) {
        Camera camera = cameraManager.getCamera();
        if (camera == null) {
            return false;
        }

        Camera.Parameters parameters = camera.getParameters();
        Display display = cameraManager.getDisplay();
        Point screenRes = new Point();
        display.getSize(screenRes);

        Point resForPreview = new Point(
                Math.max(screenRes.x, screenRes.y),
                Math.min(screenRes.x, screenRes.y)
        );

        setAutoFocus(parameters);

        try {
            Point previewSize = findBestPreviewSize(parameters, resForPreview,
                    mMaxAspectDistortion);

            if (previewSize != null) {
                parameters.setPreviewSize(previewSize.x, previewSize.y);
                Timber.d("Preview Size: %d x %d", previewSize.x, previewSize.y);
            }
        } catch (IOException e) {
            Timber.e(e, TAG);
        }

        try {
            Point pictureSize = findBestPictureSize(parameters, getMinPictureSide());
            if (pictureSize != null) {
                parameters.setPictureSize(pictureSize.x, pictureSize.y);
                Timber.d("Picture Size: %d x %d", pictureSize.x, pictureSize.y);
            }
        } catch (IOException e) {
            Timber.e(e, TAG);
        }

        try {
            camera.setParameters(parameters);
            return true;
        } catch (RuntimeException e) {
            // occurs on some Android 4.4s, in which case we proceed with defaults
            Timber.e(e, TAG);
        }

        return false;
    }


    public static boolean setAutoFocus(Camera.Parameters parameters) {
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        String focusMode = null;

        for (String mode : CameraConfig.FOCUS_ON_MODES) {
            if (supportedFocusModes.contains(mode)) {
                focusMode = mode;
                break;
            }
        }

        if (focusMode != null) {
            if (focusMode.equals(parameters.getFocusMode())) {
                Timber.d("Focus mode already set to " + focusMode);
            } else {
                parameters.setFocusMode(focusMode);
                return true;
            }
        }

        return false;
    }


    public static Point findBestPictureSize(final Camera.Parameters parameters,
                                            final int minSize) throws IOException {
        Camera.Size pictureSize = null;
        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPictureSizes();

        if (rawSupportedSizes != null) {
            List<Camera.Size> supportedPictureSizes = sortCameraSizes(rawSupportedSizes, true);
            for (Camera.Size size : supportedPictureSizes) {
                if (Math.min(size.width, size.height) >= minSize) {
                    pictureSize = size;
                    break;
                }
            }
        } else {
            Timber.w("Device returned no supported picture sizes; using default");
        }


        if (pictureSize == null) {
            // use whatever picture size is currently set; whatever works
            pictureSize = parameters.getPictureSize();
        }

        if (pictureSize == null) {
            throw new IOException("Parameters contained no picture size!");
        }

        return new Point(pictureSize.width, pictureSize.height);
    }


    public static Point findBestPreviewSize(final Camera.Parameters parameters,
                                            final Point screenResolution,
                                            final float maxAspectDistortion) throws IOException {

        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Timber.w("Device returned no supported preview sizes; using default");

            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null) {
                throw new IOException("Parameters contained no preview size!");
            }

            return new Point(defaultSize.width, defaultSize.height);
        }

        List<Camera.Size> supportedPreviewSizes = sortCameraSizes(rawSupportedSizes, false);
        double screenAspectRatio = (double) screenResolution.x / (double) screenResolution.y;

        // Remove sizes that are unsuitable
        Iterator<Camera.Size> it = supportedPreviewSizes.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewSize = it.next();
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
                it.remove();
                continue;
            }

            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > maxAspectDistortion) {
                it.remove();
                continue;
            }

            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Timber.i("Found preview size exactly matching screen size: " + exactPoint);
                return exactPoint;
            }
        }

        // If no exact match, use largest preview size. This was not a great idea on older devices because
        // of the additional computation needed. We're likely to get here on newer Android 4+ devices, where
        // the CPU is much more powerful.
        if (!supportedPreviewSizes.isEmpty()) {
            Camera.Size largestPreview = supportedPreviewSizes.get(0);
            Point largestSize = new Point(largestPreview.width, largestPreview.height);
            Timber.i("Using largest suitable preview size: " + largestSize);
            return largestSize;
        }

        // If there is nothing at all suitable, return current preview size
        Camera.Size defaultPreview = parameters.getPreviewSize();
        if (defaultPreview == null) {
            throw new IOException("Parameters contained no preview size!");
        }

        Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
        Timber.i("No suitable preview sizes, using default: " + defaultSize);
        return defaultSize;
    }


    public static List<Camera.Size> sortCameraSizes(final List<Camera.Size> rawSupportedSizes,
                                                    final boolean asc) {
        List<Camera.Size> supportedPreviewSizes = new ArrayList<>(rawSupportedSizes);

        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;

                if (bPixels == aPixels) {
                    return 0;
                }

                //Integer.compare()
                int result = (bPixels < aPixels) ? 1 : -1;
                return asc ? result : (-1) * result;
            }
        });

        return supportedPreviewSizes;
    }


    public static float readFloat(Resources res, int prop) {
        TypedValue outValue = new TypedValue();
        res.getValue(prop, outValue, true);
        return outValue.getFloat();
    }

}
