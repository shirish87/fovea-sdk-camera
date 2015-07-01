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

import java.util.List;

import io.e55.fovea.android.sdk.camera.lib.CameraConfig;
import io.e55.fovea.android.sdk.camera.lib.CameraManager;


/**
 * This module manages the Flash light for the Camera.
 */
public class CameraFlashModule extends CameraModule {

    private Camera mCamera;

    CameraFlashModule(Context context, CameraConfig config) {
        super(context, config);
    }

    @Override
    public void start(CameraManager cameraManager) {
        mCamera = cameraManager.getCamera();
    }

    @Override
    public void stop() {
        setFlash(false);
        mCamera = null;
    }

    public boolean isFlashOn() {
        if (mCamera == null) {
            return false;
        }

        String flashMode = mCamera.getParameters().getFlashMode();
        if (flashMode != null) {
            for (String mode : CameraConfig.FLASH_ON_MODES) {
                if (flashMode.equals(mode)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean setFlash(boolean on) {
        if (mCamera == null) {
            return false;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        String flashMode = null;

        if (supportedFlashModes != null) {
            String[] values = on ? CameraConfig.FLASH_ON_MODES : CameraConfig.FLASH_OFF_MODES;

            for (String s : values) {
                if (supportedFlashModes.contains(s)) {
                    flashMode = s;
                    break;
                }
            }
        }

        if (flashMode == null || flashMode.equals(parameters.getFlashMode())) {
            return false;
        }

        parameters.setFlashMode(flashMode);
        mCamera.setParameters(parameters);
        return true;
    }
}
