package com.facevoice.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_GALLERY = 100;
    private static final int REQUEST_CAMERA = 101;
    private ImageView photoPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private LinearLayout createLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.rgb(18, 20, 32));
        return layout;
    }

    private TextView makeText(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    private void showHome() {
        LinearLayout layout = createLayout();

        layout.addView(makeText("FACE & VOICE STUDIO", 26));
        layout.addView(makeText("Choose your creative effect", 16));

        Button face = makeButton("Face Effects");
        Button voice = makeButton("Voice Effects");

        face.setOnClickListener(v -> showFaceScreen());
        voice.setOnClickListener(v -> showVoiceScreen());

        layout.addView(face);
        layout.addView(voice);

        setContentView(layout);
    }

    private void showFaceScreen() {
        LinearLayout layout = createLayout();

        layout.addView(makeText("Face Effects", 26));
        layout.addView(makeText(
            "Choose a photo or take a new one.", 16));

        Button gallery = makeButton("Choose from Gallery");
        Button camera = makeButton("Take a Photo");

        photoPreview = new ImageView(this);
        photoPreview.setAdjustViewBounds(true);
        photoPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photoPreview.setMinimumHeight(300);
        photoPreview.setBackgroundColor(Color.rgb(35, 38, 52));

        gallery.setOnClickListener(v -> openGallery());
        camera.setOnClickListener(v -> openCamera());

        Button back = makeButton("Back to Home");
        back.setOnClickListener(v -> showHome());

        layout.addView(gallery);
        layout.addView(camera);
        layout.addView(photoPreview,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        layout.addView(back);
        setContentView(layout);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        try {
            startActivityForResult(
                Intent.createChooser(intent, "Choose a photo"),
                REQUEST_GALLERY);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open gallery",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(
            android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, "No camera app is available",
                Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        if (requestCode == REQUEST_GALLERY) {
            Uri imageUri = data.getData();

            if (imageUri != null && photoPreview != null) {
                photoPreview.setImageURI(imageUri);
            }

        } else if (requestCode == REQUEST_CAMERA) {
            Object result = data.getExtras() == null
                ? null : data.getExtras().get("data");

            if (result instanceof Bitmap && photoPreview != null) {
                photoPreview.setImageBitmap((Bitmap) result);
            } else {
                Toast.makeText(this,
                    "The camera did not return a preview photo",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showVoiceScreen() {
        LinearLayout layout = createLayout();

        layout.addView(makeText("Voice Effects", 26));
        layout.addView(makeText(
            "Voice recording and effects will be added next.", 17));

        Button back = makeButton("Back to Home");
        back.setOnClickListener(v -> showHome());

        layout.addView(back);
        setContentView(layout);
    }
}
