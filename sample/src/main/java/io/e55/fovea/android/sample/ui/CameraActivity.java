package io.e55.fovea.android.sample.ui;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import io.e55.fovea.android.sample.R;
import io.e55.fovea.android.sdk.camera.ui.FoveaCameraActivity;

public class CameraActivity extends FoveaCameraActivity {

    @Override
    public void onFragmentViewCreated(View view) {
        // attach event listeners, etc.
    }

    @Override
    public int provideLayout() {
        return R.layout.fragment_camera;
    }

    @Override
    public void onPictureTaken(String path) {
        Toast.makeText(this, "Saved at: " + path, Toast.LENGTH_LONG).show();
        ImageActivity.start(this, path);
        finish();
    }

    @Override
    public void onError(Throwable e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    }


    public static void start(Context context) {
        Intent intent = new Intent(context, CameraActivity.class);
        context.startActivity(intent);
    }

}
