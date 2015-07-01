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

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * Custom SurfaceView utilized by the Camera to draw on.
 * We delegate much of the state management to {@link rx.Observable}s
 * in {@link CameraPreviewTracker} that do nothing more that notifying subscribers
 * on changes in state.
 */
public class CameraPreview extends SurfaceView {

    private CameraPreviewTracker mCameraPreviewTracker;

    public CameraPreview(Context context) {
        super(context);
        init();
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mCameraPreviewTracker = new CameraPreviewTracker(this);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        mCameraPreviewTracker.notifyTouchEvent(event);
        return super.onTouchEvent(event);
    }

    /**
     * This method *must* be called when the {@link android.app.Activity} is resumed.
     * Tapping into the Activity lifecycle is much more reliable than tracking
     * the lifecycle of this view.
     */
    public void notifyUiResume() {
        toggleSurfaceCallback(true);
        setVisibility(View.VISIBLE);
        mCameraPreviewTracker.notifyUiResume();
    }


    /**
     * This method *must* be called when the {@link android.app.Activity} is paused.
     * Tapping into the Activity lifecycle is much more reliable than tracking
     * the lifecycle of this view.
     */
    public void notifyUiPause() {
        mCameraPreviewTracker.notifyUiPause();
        toggleSurfaceCallback(false);
        setVisibility(View.GONE);
    }

    /**
     * Expose the {@link CameraPreviewTracker} for the external world to access
     * it's {@link rx.Observable}s and subscribe to them.
     *
     * @return CameraPreviewTracker
     */
    public CameraPreviewTracker getCameraPreviewTracker() {
        return mCameraPreviewTracker;
    }


    private void toggleSurfaceCallback(boolean register) {
        SurfaceHolder surfaceHolder = getHolder();
        if (surfaceHolder != null) {
            if (register) {
                surfaceHolder.addCallback(mCameraPreviewTracker);
            } else {
                surfaceHolder.removeCallback(mCameraPreviewTracker);
            }
        }
    }

}
