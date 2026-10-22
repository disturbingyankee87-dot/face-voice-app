package com.facevoice.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ScrollView;
import java.io.InputStream;

public class MainActivity extends Activity {

    private static final int REQUEST_GALLERY = 100;
    private static final int REQUEST_CAMERA = 101;

    private ImageView photoPreview;
    private Bitmap originalBitmap;
    private Bitmap currentBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private LinearLayout createLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(Color.rgb(18, 20, 32));
        return layout;
    }

    private TextView makeText(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, 8, 0, 8);
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
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = createLayout();
        layout.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

        layout.addView(makeText("Face Effects Studio", 25));
        layout.addView(makeText(
            "Choose a photo, then apply an effect.", 16));

        Button gallery = makeButton("Choose from Gallery");
        Button camera = makeButton("Take a Photo");

        photoPreview = new ImageView(this);
        photoPreview.setAdjustViewBounds(true);
        photoPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photoPreview.setMinimumHeight(300);
        photoPreview.setBackgroundColor(Color.rgb(35, 38, 52));

        gallery.setOnClickListener(v -> openGallery());
        camera.setOnClickListener(v -> openCamera());

        layout.addView(gallery);
        layout.addView(camera);
        layout.addView(photoPreview,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400));

        layout.addView(makeText("BASIC FILTERS", 20));

        addFilterButton(layout, "Original", "original");
        addFilterButton(layout, "Black & White", "bw");
        addFilterButton(layout, "Sepia", "sepia");
        addFilterButton(layout, "Brighten", "brighten");
        addFilterButton(layout, "Vintage", "vintage");

        layout.addView(makeText("BEAUTY FILTERS", 20));

        addFilterButton(layout, "Beauty Smooth", "smooth");
        addFilterButton(layout, "Beauty Brighten", "beauty");

        layout.addView(makeText("FUNNY FACE EFFECTS", 20));

        addFilterButton(layout, "Big Head", "bighead");
        addFilterButton(layout, "Big Eyes", "bigeyes");
        addFilterButton(layout, "Funny Face", "funny");

        Button save = makeButton("Save Photo");
        save.setOnClickListener(v -> savePhoto());
        layout.addView(save);

        Button back = makeButton("Back to Home");
        back.setOnClickListener(v -> showHome());
        layout.addView(back);

        scroll.addView(layout);
        setContentView(scroll);

        if (currentBitmap != null) {
            photoPreview.setImageBitmap(currentBitmap);
        }
    }

    private void addFilterButton(
        LinearLayout layout, String label, String filter) {

        Button button = makeButton(label);
        button.setOnClickListener(v -> applyFilter(filter));
        layout.addView(button);
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
            MediaStore.ACTION_IMAGE_CAPTURE);

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

        try {
            Bitmap bitmap = null;

            if (requestCode == REQUEST_GALLERY) {
                Uri imageUri = data.getData();

                if (imageUri != null) {
                    InputStream stream =
                        getContentResolver().openInputStream(imageUri);

                    if (stream != null) {
                        bitmap = BitmapFactory.decodeStream(stream);
                        stream.close();
                    }
                }
            } else if (requestCode == REQUEST_CAMERA) {
                Object result = data.getExtras() == null
                    ? null : data.getExtras().get("data");

                if (result instanceof Bitmap) {
                    bitmap = (Bitmap) result;
                }
            }

            if (bitmap != null) {
                originalBitmap = bitmap.copy(
                    Bitmap.Config.ARGB_8888, false);
                currentBitmap = originalBitmap;
                showFaceScreen();
            } else {
                Toast.makeText(this, "Could not load that photo",
                    Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Unable to load photo",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void applyFilter(String filter) {
        if (originalBitmap == null) {
            Toast.makeText(this, "Choose a photo first",
                Toast.LENGTH_SHORT).show();
            return;
        }

        if (filter.equals("original")) {
            currentBitmap = originalBitmap;
            photoPreview.setImageBitmap(currentBitmap);
            return;
        }

        if (filter.equals("smooth")) {
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();

            Bitmap small = Bitmap.createScaledBitmap(
                originalBitmap,
                Math.max(1, width / 5),
                Math.max(1, height / 5),
                true);

            currentBitmap = Bitmap.createScaledBitmap(
                small, width, height, true);

            if (small != currentBitmap) {
                small.recycle();
            }

            photoPreview.setImageBitmap(currentBitmap);
            return;
        }

        if (filter.equals("bighead")
                || filter.equals("bigeyes")
                || filter.equals("funny")) {
            applyFunnyEffect(filter);
            return;
        }

        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(
            width, height, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = originalBitmap.getPixel(x, y);

                int a = Color.alpha(pixel);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                if (filter.equals("bw")) {
                    int gray = (int) (0.299 * r
                        + 0.587 * g + 0.114 * b);
                    r = g = b = gray;

                } else if (filter.equals("sepia")) {
                    int nr = (int) (0.393 * r
                        + 0.769 * g + 0.189 * b);
                    int ng = (int) (0.349 * r
                        + 0.686 * g + 0.168 * b);
                    int nb = (int) (0.272 * r
                        + 0.534 * g + 0.131 * b);

                    r = Math.min(255, nr);
                    g = Math.min(255, ng);
                    b = Math.min(255, nb);

                } else if (filter.equals("brighten")) {
                    r = Math.min(255, r + 35);
                    g = Math.min(255, g + 35);
                    b = Math.min(255, b + 35);

                } else if (filter.equals("vintage")) {
                    r = Math.min(255, (int) (0.90 * r + 20));
                    g = Math.min(255, (int) (0.80 * g + 15));
                    b = Math.min(255, (int) (0.70 * b + 10));

                } else if (filter.equals("beauty")) {
                    r = Math.min(255, (int) (r * 1.10 + 12));
                    g = Math.min(255, (int) (g * 1.10 + 12));
                    b = Math.min(255, (int) (b * 1.10 + 12));
                }

                result.setPixel(x, y, Color.argb(a, r, g, b));
            }
        }

        currentBitmap = result;
        photoPreview.setImageBitmap(currentBitmap);
    }

    private void applyFunnyEffect(String effect) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(
            width, height, Bitmap.Config.ARGB_8888);

        float minSide = Math.min(width, height);

        float headX = width * 0.50f;
        float headY = height * 0.43f;
        float headRadius = minSide * 0.43f;

        float leftEyeX = width * 0.38f;
        float rightEyeX = width * 0.62f;
        float eyeY = height * 0.39f;
        float eyeRadius = minSide * 0.15f;

        float mouthX = width * 0.50f;
        float mouthY = height * 0.63f;
        float mouthRadius = minSide * 0.13f;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sx = x;
                float sy = y;

                if (effect.equals("bighead")
                        || effect.equals("funny")) {
                    float[] point = bulge(
                        sx, sy, headX, headY,
                        headRadius, 0.75f);
                    sx = point[0];
                    sy = point[1];
                }

                if (effect.equals("bigeyes")
                        || effect.equals("funny")) {
                    float[] left = bulge(
                        sx, sy, leftEyeX, eyeY,
                        eyeRadius, 0.90f);
                    sx = left[0];
                    sy = left[1];

                    float[] right = bulge(
                        sx, sy, rightEyeX, eyeY,
                        eyeRadius, 0.90f);
                    sx = right[0];
                    sy = right[1];
                }

                if (effect.equals("funny")) {
                    float[] mouth = bulge(
                        sx, sy, mouthX, mouthY,
                        mouthRadius, 0.65f);
                    sx = mouth[0];
                    sy = mouth[1];
                }

                int sourceX = Math.max(0,
                    Math.min(width - 1, Math.round(sx)));
                int sourceY = Math.max(0,
                    Math.min(height - 1, Math.round(sy)));

                result.setPixel(
                    x, y, originalBitmap.getPixel(sourceX, sourceY));
            }
        }

        currentBitmap = result;
        photoPreview.setImageBitmap(currentBitmap);
    }

    private float[] bulge(
        float x, float y,
        float centerX, float centerY,
        float radius, float strength) {

        float dx = x - centerX;
        float dy = y - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance >= radius || radius <= 0) {
            return new float[] { x, y };
        }

        float amount = 1.0f - distance / radius;
        float scale = 1.0f + strength * amount * amount;

        float sourceX = centerX + dx / scale;
        float sourceY = centerY + dy / scale;

        return new float[] { sourceX, sourceY };
    }

    private void savePhoto() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Choose a photo first",
                Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String saved = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                currentBitmap,
                "FaceVoice_" + System.currentTimeMillis(),
                "Edited with Face Voice Studio");

            if (saved != null) {
                Toast.makeText(this, "Photo saved to your gallery!",
                    Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Could not save photo",
                    Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not save photo",
                Toast.LENGTH_LONG).show();
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
