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

import io.e55.fovea.android.sdk.camera.lib.CameraConfig;

/**
 * Factory that instantiates the specified CameraModule, if available.
 */
public class CameraModuleFactory {

    private final Context mContext;
    private final CameraConfig mConfig;

    public CameraModuleFactory(Context context, CameraConfig config) {
        mContext = context;
        mConfig = config;
    }

    public CameraModule get(String comp) {
        switch (comp) {
            case CameraModules.LIGHT_SENSOR:
                return new CameraSensorModule(mContext, mConfig);

            case CameraModules.AUTO_FOCUS:
                return new CameraFocusModule(mContext, mConfig);

            case CameraModules.FLASH:
                return new CameraFlashModule(mContext, mConfig);

            case CameraModules.CAPTURE:
                return new CameraCaptureModule(mContext, mConfig);

            case CameraModules.SHADE:
                return new CameraShadeModule(mContext, mConfig);
        }

        return null;
    }


    public interface CameraModules {
        String LIGHT_SENSOR = "LIGHT_SENSOR";
        String AUTO_FOCUS = "AUTO_FOCUS";
        String FLASH = "FLASH";
        String CAPTURE = "CAPTURE";
        String SHADE = "SHADE";
    }

}
