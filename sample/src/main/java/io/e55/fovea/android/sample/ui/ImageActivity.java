package io.e55.fovea.android.sample.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;

import io.e55.fovea.android.sample.R;

public class ImageActivity extends AppCompatActivity {

    public interface Args {
        String IMAGE_PATH = "imagePath";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);

        if (imageView != null) {
            boolean loadedImage = loadImage(this, getIntent().getExtras(), imageView);

            if (!loadedImage) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void start(Context context, String image) {
        Intent intent = new Intent(context, ImageActivity.class);
        intent.putExtra(Args.IMAGE_PATH, image);
        context.startActivity(intent);
    }


    private static boolean loadImage(final Context context,
                                  final Bundle extras,
                                  final ImageView imageView) {
        
        if (extras != null) {
            String imagePath = extras.getString(Args.IMAGE_PATH);
            if (imagePath != null) {
                File imageFile = new File(imagePath);

                if (imageFile.exists()) {
                    Glide.with(context)
                            .load(imageFile)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(imageView);
                    return true;
                }
            }
        }

        return false;
    }
}
