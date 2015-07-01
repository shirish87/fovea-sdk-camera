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

package io.e55.fovea.android.sdk.camera.ui.frag;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.e55.fovea.android.sdk.camera.R;
import io.e55.fovea.android.sdk.camera.lib.CameraManager;
import io.e55.fovea.android.sdk.camera.ui.view.CameraPreview;
import rx.Subscriber;

/**
 * Fragment that contains the {@link io.e55.fovea.android.sdk.camera.ui.view.CameraPreview}
 * and other UI components such as the Flash toggle and Capture buttons.
 * This fragment accepts the layout to be used from the activity, which much contain all
 * of the mentioned components.
 *
 * Any activity creating and using this Fragment *must* implement the
 * {@link io.e55.fovea.android.sdk.camera.ui.frag.FoveaCameraFragment.CameraActivityListener},
 * or this fragment will throw an exception.
 */
public class FoveaCameraFragment extends Fragment {
    public static final String TAG = FoveaCameraFragment.class.getSimpleName();

    private int mLayoutId;

    private CameraManager mCameraManager;
    private CameraPreview mCameraPreview;

    private CameraActivityListener mActivityListener;

    public interface Args {
        String LAYOUT_ID = "LAYOUT_ID";
    }

    public static FoveaCameraFragment newInstance(int layoutId) {
        FoveaCameraFragment fragment = new FoveaCameraFragment();
        fragment.setRetainInstance(true);

        Bundle args = new Bundle();
        args.putInt(Args.LAYOUT_ID, layoutId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(mLayoutId, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Could have used Butterknife, but trying to limit dependencies
        mCameraPreview = (CameraPreview) view.findViewById(R.id.cameraSurfaceView);
        if (mCameraPreview != null) {
            mCameraManager.setCameraPreviewTracker(mCameraPreview.getCameraPreviewTracker());
        }

        View cameraShadeView = view.findViewById(R.id.cameraShade);
        if (cameraShadeView != null) {
            mCameraManager.setCameraShadeView(cameraShadeView);
        }

        View flashView = view.findViewById(R.id.cameraFlashToggle);
        if (flashView != null) {
            flashView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCameraManager.setFlash(!mCameraManager.isFlashOn());
                }
            });
        }

        View captureView = view.findViewById(R.id.cameraCapture);
        if (captureView != null) {
            captureView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mActivityListener != null) {
                        // enable click just once
                        v.setOnClickListener(null);

                        mCameraManager.takePicture(
                                mActivityListener.provideOutputFilePath(),
                                mActivityListener.providePictureTakenSubscriber()
                        );
                    }
                }
            });
        }

        if (mActivityListener != null) {
            mActivityListener.onFragmentViewCreated(view);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof CameraActivityListener) {
            mActivityListener = (CameraActivityListener) activity;
        } else {
            throw new IllegalStateException("Attached activity must implement CameraActivityListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mActivityListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null && args.containsKey(Args.LAYOUT_ID)) {
            mLayoutId = args.getInt(Args.LAYOUT_ID, R.layout.fragment_camera);
        }

        mCameraManager = new CameraManager(getActivity().getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCameraPreview != null) {
            mCameraPreview.notifyUiResume();
        }
    }

    @Override
    public void onPause() {
        if (mCameraPreview != null) {
            mCameraPreview.notifyUiPause();
        }

        super.onPause();
    }

    /**
     * Activities that instantiate and use the {@link FoveaCameraFragment} must implement
     * this interface. This interface is used to collect configuration from the attaching Activity.
     */
    public interface CameraActivityListener {

        /**
         * Returns the inflated view from `provideLayout()` for the FoveaCameraFragment so that
         * the other components can attach event listeners.
         * @param view View for the Fragment
         */
        void onFragmentViewCreated(View view);

        /**
         * Used to collect the layout to be used by this fragment
         * @return R.layout.* id
         */
        int provideLayout();

        /**
         * Used to supply the path where the camera's output is to be stored.
         * @return Writable path to store the image file.
         */
        String provideOutputFilePath();

        /**
         * RxAndroid {@link rx.Subscriber} to be used to get capture status or errors.
         * @return Subscriber to be used for feedback.
         */
        Subscriber<String> providePictureTakenSubscriber();
    }
}
