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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.e55.fovea.android.sdk.camera.lib.CameraConfig;
import io.e55.fovea.android.sdk.camera.lib.CameraManager;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;


/**
 * This module captures the current Camera frame when requested using `takePicture()`
 * and persists the image data to an image file stored at the specified path.
 *
 * Depending on the configuration in {@link CameraConfig}, it can persist a raw Camera frame buffer
 * or a processed/compressed JPEG. Transforms of resize and rotate are applied on the image data
 * before it is store to file.
 */
public class CameraCaptureModule extends CameraModule {

    private static final String TAG = CameraCaptureModule.class.getSimpleName();

    private Camera mCamera;

    private final int mJpegQuality, mAdjustOrientation, mMaxSide;
    private final boolean mCapturePreviewFrame;

    CameraCaptureModule(Context context, CameraConfig config) {
        super(context, config);

        mJpegQuality = config.captureJpegQuality();
        mAdjustOrientation = config.captureAdjustOrientation();
        mMaxSide = config.captureMaxSide();
        mCapturePreviewFrame = config.capturePreviewFrame();
    }

    @Override
    public void start(CameraManager cameraManager) {
        mCamera = cameraManager.getCamera();
    }

    @Override
    public void stop() {
        mCamera = null;
    }


    public Observable<String> takePicture(final String path) {
        return getCaptureObservable().flatMap(new Func1<byte[], Observable<String>>() {
            @Override
            public Observable<String> call(byte[] data) {
                return getPictureSaveObservable(data, path);
            }
        });
    }


    private Observable<byte[]> getCaptureObservable() {
        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(final Subscriber<? super byte[]> subscriber) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }

                subscriber.onStart();

                if (mCamera == null) {
                    subscriber.onError(new IOException("Camera not ready."));
                    return;
                }

                if (mCapturePreviewFrame) {
                    mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(final byte[] data, Camera camera) {
                            Timber.d("onPreviewFrame");
                            subscriber.onNext(data);
                            subscriber.onCompleted();
                        }
                    });
                } else {
                    mCamera.takePicture(new Camera.ShutterCallback() {
                        @Override
                        public void onShutter() {

                        }
                    }, null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            Timber.d("onPictureTaken");
                            subscriber.onNext(data);
                            subscriber.onCompleted();
                        }
                    });
                }
            }
        });
    }

    private Observable<String> getPictureSaveObservable(final byte[] data, final String path) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }

                subscriber.onStart();

                if (mCamera == null) {
                    subscriber.onError(new IOException("Camera not ready."));
                    return;
                }

                if (data == null) {
                    subscriber.onError(new IOException("Failed to capture image. Please try again."));
                    return;
                }

                Camera.Parameters parameters = mCamera.getParameters();
                int previewFormat = parameters.getPreviewFormat();
                Camera.Size previewSize = parameters.getPreviewSize();
                Timber.d("Capture Preview Size: %d x %d", previewSize.width, previewSize.height);

                try {
                    String outFile;

                    if (mCapturePreviewFrame) {
                        outFile = saveRawData(data, previewSize, previewFormat,
                                mMaxSide, mAdjustOrientation, mJpegQuality, path);
                    } else {
                        outFile = saveJpegData(data,
                                mMaxSide, mAdjustOrientation, mJpegQuality, path);
                    }

                    subscriber.onNext(outFile);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    Timber.e(e, TAG);
                    subscriber.onError(e);
                }

            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    private static String saveRawData(byte[] yuv, Camera.Size previewSize, int previewFormat,
                                      int maxSide, int adjustOrientation,
                                      int jpegQuality, String path) throws IOException {

        int width = previewSize.width;
        int height = previewSize.height;

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(yuv, previewFormat, width, height, null);
        Rect rect = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());

        boolean success;
        success = yuvImage.compressToJpeg(rect, jpegQuality, os);

        if (!success) {
            throw new IOException("Failed to save image.");
        }

        return saveJpegData(os.toByteArray(), maxSide, adjustOrientation, jpegQuality, path);
    }


    private static String saveJpegData(byte[] data, int maxSide, int adjustOrientation,
                                       int jpegQuality, String path) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        Timber.d("Before: %d x %d", bitmap.getWidth(), bitmap.getHeight());

        Bitmap rotBitmap = newScaledBitmap(bitmap, maxSide, adjustOrientation);
        Timber.d("After: %d x %d", rotBitmap.getWidth(), rotBitmap.getHeight());
        bitmap.recycle();

        File f = new File(path);
        FileOutputStream fos = new FileOutputStream(f);
        boolean success = rotBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, fos);
        fos.close();
        rotBitmap.recycle();

        if (!success) {
            throw new IOException("Failed to save image.");
        }

        return f.getAbsolutePath();
    }

    private static Bitmap newScaledBitmap(Bitmap bm, float maxSide, int adjustOrientation) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = (maxSide / (float) width);
        float scaleHeight = (maxSide / (float) height);
        float scale = Math.max(scaleWidth, scaleHeight);

        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scale, scale);
        matrix.postRotate(adjustOrientation);
        Timber.d("Scale: %f | Rotate: %d", scale, adjustOrientation);

        // "RECREATE" THE NEW BITMAP
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
    }

}
