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

package io.e55.fovea.android.sdk.camera.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

import io.e55.fovea.android.sdk.camera.R;
import io.e55.fovea.android.sdk.camera.lib.CameraConfig;
import io.e55.fovea.android.sdk.camera.ui.frag.FoveaCameraFragment;
import io.e55.fovea.android.sdk.camera.ui.frag.FoveaCameraFragment.CameraActivityListener;
import rx.Subscriber;
import timber.log.Timber;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;


/**
 * Extend this class for your activity to start the camera and receive callbacks
 * on successful capture or error.
 * Use `camera.xml` to configure options for the camera.
 *
 * <pre><code>
 * public class CameraActivity extends FoveaCameraActivity {
 *     {@literal @}Override
 *     public void onPictureTaken(String path) {
 *       // do something with the picture stored at `path`
 *     }
 *
 *      {@literal @}Override
 *      public void onError(Throwable e) {
 *        // handle the error `e`
 *      }
 * }
 * </code></pre>
 */
public abstract class FoveaCameraActivity extends Activity implements CameraActivityListener {

    private static final String TAG = FoveaCameraActivity.class.getSimpleName();

    private FoveaCameraFragment mCameraFragment;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lockOrientation();
        setContentView(R.layout.activity_frame);

        FragmentManager fm = getFragmentManager();
        if (savedInstanceState == null) {
            mCameraFragment = FoveaCameraFragment.newInstance(provideLayout());
            fm.beginTransaction()
                    .replace(R.id.cameraContainer, mCameraFragment, FoveaCameraFragment.TAG)
                    .commit();
        } else {
            mCameraFragment = (FoveaCameraFragment) fm.findFragmentByTag(FoveaCameraFragment.TAG);
        }
    }

    protected void lockOrientation() {
        boolean lockInLandscape = CameraConfig.getInstance(getApplicationContext())
                .isCaptureOrientationLandscape();

        setRequestedOrientation(lockInLandscape ?
                SCREEN_ORIENTATION_LANDSCAPE : SCREEN_ORIENTATION_PORTRAIT);
    }


    @Override
    public int provideLayout() {
        return R.layout.fragment_camera;
    }

    @Override
    public String provideOutputFilePath() {
        return CameraConfig.getOutputFilePath();
    }

    @Override
    public Subscriber<String> providePictureTakenSubscriber() {
        return new Subscriber<String>() {
            @Override
            public void onCompleted() {
                Timber.d("Done");
            }

            @Override
            public void onError(Throwable e) {
                Timber.w(e, TAG);
                FoveaCameraActivity.this.onError(e);
            }

            @Override
            public void onNext(String s) {
                Timber.d("Out: %s", s);
                FoveaCameraActivity.this.onPictureTaken(s);
            }
        };
    }

    /**
     * Invoked after the captured frame is successfully persisted.
     *
     * @param path Path where the JPG file is stored
     */
    public abstract void onPictureTaken(String path);

    /**
     * Invoked if an error has occurred.
     *
     * @param e Error to be handled by the implementation.
     */
    public abstract void onError(Throwable e);
}
