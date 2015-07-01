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
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;

/**
 * Use this class to record the lifecycle changes and events for the specified {@link SurfaceView}.
 * This class creates and exposes {@link Observable}s for external components across the app
 * to subscribe to.
 */
public class CameraPreviewTracker extends SurfaceViewTracker {

    /**
     * Tracks the availability of the {@link SurfaceHolder}
     */
    public final Observable<SurfaceHolder> surfaceHolderObservable;

    /**
     * Tracks changes in the {@link SurfaceView}
     */
    public final Observable<Point> surfaceChangeObservable;

    /**
     * Tracks touch events on the {@link SurfaceView}
     */
    public final Observable<MotionEvent> touchEventObservable;


    private Subscriber<? super MotionEvent> mTouchEventSubscriber;

    public CameraPreviewTracker(final SurfaceView surfaceView) {
        super(surfaceView);

        surfaceHolderObservable = Observable.create(new Observable.OnSubscribe<SurfaceHolder>() {
            @Override
            public void call(Subscriber<? super SurfaceHolder> subscriber) {
                setSurfaceHolderSubscriber(subscriber);

                if (!subscriber.isUnsubscribed() && isActive()) {
                    subscriber.onNext(surfaceView.getHolder());
                }
            }
        }).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                setSurfaceHolderSubscriber(null);
            }
        }).share();

        surfaceChangeObservable = Observable.create(new Observable.OnSubscribe<Point>() {
            @Override
            public void call(Subscriber<? super Point> subscriber) {
                setSurfaceChangeSubscriber(subscriber);
            }
        }).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                setSurfaceChangeSubscriber(null);
            }
        }).share();

        touchEventObservable = Observable.create(new Observable.OnSubscribe<MotionEvent>() {
            @Override
            public void call(Subscriber<? super MotionEvent> subscriber) {
                mTouchEventSubscriber = subscriber;
            }
        }).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                mTouchEventSubscriber = null;
            }
        }).share();
    }


    /**
     * This method must be called in the `onTouchEvent` of the {@link SurfaceView}
     * so that interested subscribers for the related {@link Observable}
     * can receive the corresponding {@link MotionEvent}.
     *
     * @param event Event that occurred when the user tapped the {@link SurfaceView}
     */
    public void notifyTouchEvent(MotionEvent event) {
        if (mTouchEventSubscriber != null && !mTouchEventSubscriber.isUnsubscribed()) {
            mTouchEventSubscriber.onNext(event);
        }
    }
}
