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

package io.e55.fovea.android.sdk.camera.ui.view;

import android.graphics.Point;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import rx.Subscriber;
import timber.log.Timber;

/**
 * Internal class that tracks the lifecycle and changes of the {@link SurfaceView}
 * and associated {@link SurfaceHolder}.
 */
class SurfaceViewTracker implements SurfaceHolder.Callback {

    private Subscriber<? super SurfaceHolder> mSurfaceHolderSubscriber;
    private Subscriber<? super Point> mSurfaceChangeSubscriber;

    private boolean mIsActive;

    private final SurfaceView mSurfaceView;

    SurfaceViewTracker(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Timber.d("surfaceCreated");

        if (holder == null) {
            Timber.w("*** WARNING *** surfaceCreated() gave us a null surface!");
        }

        if (mSurfaceHolderSubscriber != null && !mSurfaceHolderSubscriber.isUnsubscribed()) {
            Timber.d("notifySurfaceCreated");
            mSurfaceHolderSubscriber.onNext(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Timber.d("surfaceChanged: %d x %d", width, height);

        if (holder == null || holder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        if (mSurfaceChangeSubscriber != null && !mSurfaceChangeSubscriber.isUnsubscribed()) {
            Timber.d("notifySurfaceChanged");
            mSurfaceChangeSubscriber.onNext(new Point(width, height));
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Timber.d("surfaceDestroyed");
        mIsActive = false;
    }


    /**
     * This method *must* be called (through {@link CameraPreview})
     * when the {@link android.app.Activity} is paused.
     *
     * We notify all interested subscribers here that the
     * {@link SurfaceHolder} is no longer available.
     *
     * Tapping into the Activity lifecycle is much more reliable than tracking
     * the lifecycle of this view.
     */
    public void notifyUiPause() {
        if (mSurfaceHolderSubscriber != null && !mSurfaceHolderSubscriber.isUnsubscribed()) {
            Timber.d("notifyActivityPaused");
            mSurfaceHolderSubscriber.onNext(null);
        }
    }

    /**
     * This method *must* be called (through {@link CameraPreview})
     * when the {@link android.app.Activity} is resumed.
     *
     * Tapping into the Activity lifecycle is much more reliable than tracking
     * the lifecycle of this view.
     */
    public void notifyUiResume() {

    }

    public boolean isActive() {
        return mIsActive;
    }

    public void setSurfaceHolderSubscriber(Subscriber<? super SurfaceHolder> subscriber) {
        mSurfaceHolderSubscriber = subscriber;
    }

    public void setSurfaceChangeSubscriber(Subscriber<? super Point> subscriber) {
        mSurfaceChangeSubscriber = subscriber;
    }
}
