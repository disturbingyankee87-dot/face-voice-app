package com.facevoice.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_GALLERY = 100;
    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_MICROPHONE = 102;

    private ImageView photoPreview;
    private Bitmap originalBitmap;
    private Bitmap currentBitmap;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private File recordingFile;
    private boolean isRecording = false;

    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private Voice selectedVoice;

    private TextView recordingStatus;
    private EditText speechInput;
    private TextView selectedVoiceLabel;

    private String selectedTtsStyle = "Default";

    private float ttsPitch = 1.0f;
    private float ttsRate = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showHome();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true;

                int result = textToSpeech.setLanguage(Locale.getDefault());

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech.setLanguage(Locale.US);
                }

                applyTtsSettings();
            } else {
                toast("Text-to-speech is unavailable on this device");
            }
        });
    }

    private LinearLayout createLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 20, 24, 24);
        layout.setBackgroundColor(Color.rgb(18, 20, 32));
        return layout;
    }

    private TextView makeText(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setPadding(4, 8, 4, 8);
        return view;
    }

    private Button makeButton(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void showHome() {
        LinearLayout layout = createLayout();
        layout.setGravity(Gravity.CENTER);

        TextView title = makeText("FACE & VOICE STUDIO", 27);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = makeText(
                "Create funny photos and transform your voice", 16);
        subtitle.setGravity(Gravity.CENTER);

        layout.addView(title);
        layout.addView(subtitle);

        layout.addView(makeButton("Face Effects", this::showFaceScreen));
        layout.addView(makeButton("Voice Effects", this::showVoiceScreen));

        // Open the video calling screen.
        layout.addView(makeButton("Video Call", () ->
                startActivity(new Intent(
                        MainActivity.this, CallActivity.class))));

        setContentView(layout);
    }

    private void showFaceScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = createLayout();

        layout.addView(makeButton("← Back to Home", this::showHome));
        layout.addView(makeText("FACE EFFECTS", 24));

        photoPreview = new ImageView(this);
        photoPreview.setAdjustViewBounds(true);
        photoPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photoPreview.setMinimumHeight(240);
        layout.addView(photoPreview);

        layout.addView(makeButton("Choose from Gallery", this::openGallery));
        layout.addView(makeButton("Take a Photo", this::openCamera));

        layout.addView(makeText("Photo Filters", 18));
        addFilterButton(layout, "Original", "original");
        addFilterButton(layout, "Black & White", "bw");
        addFilterButton(layout, "Sepia", "sepia");
        addFilterButton(layout, "Brighten", "brighten");
        addFilterButton(layout, "Vintage", "vintage");

        layout.addView(makeText("Beauty Effects", 18));
        addFilterButton(layout, "Beauty Smooth", "smooth");
        addFilterButton(layout, "Beauty Brighten", "beautybright");

        layout.addView(makeText("Funny Face Effects", 18));
        addFilterButton(layout, "Big Head", "bighead");
        addFilterButton(layout, "Big Eyes", "bigeyes");
        addFilterButton(layout, "Funny Face", "funny");

        layout.addView(makeText("More Face Effects", 18));
        addFilterButton(layout, "Skinny Face", "skinny");
        addFilterButton(layout, "Wide Face", "wide");
        addFilterButton(layout, "Tiny Mouth", "tiny");
        addFilterButton(layout, "Long Nose", "nose");

        layout.addView(makeButton("Save Photo", this::savePhoto));

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void addFilterButton(
            LinearLayout layout, String label, String filter) {
        layout.addView(makeButton(label, () -> applyFilter(filter)));
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            toast("No camera app is available");
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
                Uri uri = data.getData();
                if (uri != null) {
                    try (InputStream input =
                                 getContentResolver().openInputStream(uri)) {
                        bitmap = BitmapFactory.decodeStream(input);
                    }
                }
            } else if (requestCode == REQUEST_CAMERA) {
                Object extra = data.getExtras() == null
                        ? null : data.getExtras().get("data");
                if (extra instanceof Bitmap) {
                    bitmap = (Bitmap) extra;
                }
            }

            if (bitmap != null) {
                originalBitmap = bitmap;
                currentBitmap = bitmap;
                if (photoPreview != null) {
                    photoPreview.setImageBitmap(bitmap);
                }
                toast("Photo loaded");
            } else {
                toast("Could not load the photo");
            }
        } catch (Exception e) {
            toast("Error loading photo");
        }
    }

    private void applyFilter(String filter) {
        if (originalBitmap == null) {
            toast("Choose a photo first");
            return;
        }

        if (filter.equals("bighead")
                || filter.equals("bigeyes")
                || filter.equals("funny")) {
            applyFunnyEffect(filter);
            return;
        }

        if (filter.equals("skinny")
                || filter.equals("wide")
                || filter.equals("tiny")
                || filter.equals("nose")) {
            applyExtraFaceEffect(filter);
            return;
        }

        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888);

        int[] pixels = new int[width * height];
        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            int a = Color.alpha(color);
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);

            switch (filter) {
                case "bw":
                    int gray = (r + g + b) / 3;
                    r = g = b = gray;
                    break;

                case "sepia":
                    int sr = (int) (0.393 * r + 0.769 * g + 0.189 * b);
                    int sg = (int) (0.349 * r + 0.686 * g + 0.168 * b);
                    int sb = (int) (0.272 * r + 0.534 * g + 0.131 * b);
                    r = Math.min(255, sr);
                    g = Math.min(255, sg);
                    b = Math.min(255, sb);
                    break;

                case "brighten":
                case "beautybright":
                    r = Math.min(255, r + 30);
                    g = Math.min(255, g + 30);
                    b = Math.min(255, b + 30);
                    break;

                case "vintage":
                    r = Math.min(255, (int) (r * 1.08 + 12));
                    g = Math.min(255, (int) (g * 0.94 + 6));
                    b = (int) (b * 0.80);
                    break;

                case "smooth":
                    int avg = (r + g + b) / 3;
                    r = (r + avg) / 2;
                    g = (g + avg) / 2;
                    b = (b + avg) / 2;
                    break;

                case "original":
                default:
                    break;
            }

            pixels[i] = Color.argb(a, r, g, b);
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height);
        currentBitmap = result;
        photoPreview.setImageBitmap(currentBitmap);
    }

    private void applyFunnyEffect(String effect) {
        if (originalBitmap == null) {
            toast("Choose a photo first");
            return;
        }

        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        float scale = Math.min(1f, 900f /
                Math.max(originalWidth, originalHeight));

        int width = Math.max(1, Math.round(originalWidth * scale));
        int height = Math.max(1, Math.round(originalHeight * scale));

        Bitmap small = Bitmap.createScaledBitmap(
                originalBitmap, width, height, true);

        int[] input = new int[width * height];
        int[] output = new int[width * height];
        small.getPixels(input, 0, width, 0, 0, width, height);

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

                if (effect.equals("bighead") || effect.equals("funny")) {
                    float[] p = bulge(
                            sx, sy, headX, headY, headRadius, 0.75f);
                    sx = p[0];
                    sy = p[1];
                }

                if (effect.equals("bigeyes") || effect.equals("funny")) {
                    float[] p1 = bulge(
                            sx, sy, leftEyeX, eyeY, eyeRadius, 0.90f);
                    float[] p2 = bulge(
                            p1[0], p1[1], rightEyeX, eyeY,
                            eyeRadius, 0.90f);
                    sx = p2[0];
                    sy = p2[1];
                }

                if (effect.equals("funny")) {
                    float[] p = bulge(
                            sx, sy, mouthX, mouthY, mouthRadius, 0.65f);
                    sx = p[0];
                    sy = p[1];
                }

                int sourceX = Math.max(0,
                        Math.min(width - 1, Math.round(sx)));
                int sourceY = Math.max(0,
                        Math.min(height - 1, Math.round(sy)));

                output[y * width + x] =
                        input[sourceY * width + sourceX];
            }
        }

        Bitmap result = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(output, 0, width, 0, 0, width, height);

        if (small != originalBitmap && !small.isRecycled()) {
            small.recycle();
        }

        currentBitmap = result;
        photoPreview.setImageBitmap(currentBitmap);
    }

    private void applyExtraFaceEffect(String effect) {
        if (originalBitmap == null) {
            toast("Choose a photo first");
            return;
        }

        int ow = originalBitmap.getWidth();
        int oh = originalBitmap.getHeight();
        float scale = Math.min(1f, 900f / Math.max(ow, oh));

        int width = Math.max(1, Math.round(ow * scale));
        int height = Math.max(1, Math.round(oh * scale));

        Bitmap small = Bitmap.createScaledBitmap(
                originalBitmap, width, height, true);

        int[] input = new int[width * height];
        int[] output = new int[width * height];
        small.getPixels(input, 0, width, 0, 0, width, height);

        float minSide = Math.min(width, height);
        float faceX = width * 0.50f;
        float faceY = height * 0.43f;
        float faceRadius = minSide * 0.24f;
        float mouthX = width * 0.50f;
        float mouthY = height * 0.63f;
        float mouthRadius = minSide * 0.075f;
        float noseX = width * 0.50f;
        float noseY = height * 0.49f;
        float noseRadius = minSide * 0.085f;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sx = x;
                float sy = y;

                if (effect.equals("skinny") || effect.equals("wide")) {
                    float dx = x - faceX;
                    float dy = y - faceY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    if (distance < faceRadius) {
                        float t = 1f - distance / faceRadius;
                        float strength = effect.equals("skinny")
                                ? 0.28f : -0.28f;
                        sx = faceX + dx * (1f + strength * t * t);
                    }
                } else if (effect.equals("tiny")) {
                    float dx = x - mouthX;
                    float dy = y - mouthY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    if (distance < mouthRadius) {
                        float t = 1f - distance / mouthRadius;
                        float factor = 1f + 0.65f * t * t;
                        sx = mouthX + dx * factor;
                        sy = mouthY + dy * factor;
                    }
                } else if (effect.equals("nose")) {
                    float dx = x - noseX;
                    float dy = y - noseY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    if (distance < noseRadius) {
                        float t = 1f - distance / noseRadius;
                        sy = noseY + dy * (1f + 0.90f * t * t);
                    }
                }

                int sourceX = Math.max(0,
                        Math.min(width - 1, Math.round(sx)));
                int sourceY = Math.max(0,
                        Math.min(height - 1, Math.round(sy)));

                output[y * width + x] =
                        input[sourceY * width + sourceX];
            }
        }

        Bitmap result = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(output, 0, width, 0, 0, width, height);

        if (small != originalBitmap && !small.isRecycled()) {
            small.recycle();
        }

        currentBitmap = result;
        photoPreview.setImageBitmap(currentBitmap);
    }

    private float[] bulge(
            float x, float y, float centerX, float centerY,
            float radius, float strength) {

        float dx = x - centerX;
        float dy = y - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance >= radius || radius <= 0) {
            return new float[]{x, y};
        }

        float amount = 1f - distance / radius;
        float scale = 1f + strength * amount * amount;

        return new float[]{
                centerX + dx / scale,
                centerY + dy / scale
        };
    }

    private void savePhoto() {
        if (currentBitmap == null) {
            toast("Choose a photo first");
            return;
        }

        try {
            String saved = MediaStore.Images.Media.insertImage(
                    getContentResolver(), currentBitmap,
                    "FaceVoice_" + System.currentTimeMillis(),
                    "Edited with Face & Voice Studio");

            toast(saved != null
                    ? "Photo saved to your gallery"
                    : "Could not save photo");
        } catch (Exception e) {
            toast("Could not save photo");
        }
    }

    private void showVoiceScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = createLayout();

        layout.addView(makeButton("← Back to Home", this::showHome));
        layout.addView(makeText("VOICE STUDIO", 24));
        layout.addView(makeText(
                "Record your voice, change playback, or speak text", 15));

        recordingStatus = makeText("Status: Ready", 16);
        layout.addView(recordingStatus);

        layout.addView(makeButton("Start Recording", this::startRecording));
        layout.addView(makeButton("Stop Recording", this::stopRecording));
        layout.addView(makeButton("Play Recording", this::playRecording));

        layout.addView(makeText("Recording Playback Effects", 18));
        layout.addView(makeButton("Normal Voice",
                () -> playWithEffect("Normal")));
        layout.addView(makeButton("Deep Voice",
                () -> playWithEffect("Deep Voice")));
        layout.addView(makeButton("High Voice",
                () -> playWithEffect("High Voice")));
        layout.addView(makeButton("Robot Style",
                () -> playWithEffect("Robot Style")));
        layout.addView(makeButton("Funny / Fast Voice",
                () -> playWithEffect("Fast Voice")));
        layout.addView(makeButton("Slow Voice",
                () -> playWithEffect("Slow Voice")));
        layout.addView(makeButton("Stop Playback", this::stopPlayback));

        layout.addView(makeText("TEXT-TO-SPEECH VOICE GENERATOR", 20));

        speechInput = new EditText(this);
        speechInput.setTextColor(Color.WHITE);
        speechInput.setHintTextColor(Color.LTGRAY);
        speechInput.setHint("Type something for the voice to speak");
        speechInput.setMinLines(3);
        speechInput.setGravity(Gravity.TOP);
        layout.addView(speechInput);

        selectedVoiceLabel = makeText(
                "Selected style: " + selectedTtsStyle, 15);
        layout.addView(selectedVoiceLabel);

        layout.addView(makeText("Choose a Voice Style", 18));
        layout.addView(makeButton("Default Voice",
                () -> chooseTtsStyle("Default")));
        layout.addView(makeButton("Male Voice Style",
                () -> chooseTtsStyle("Male")));
        layout.addView(makeButton("Female Voice Style",
                () -> chooseTtsStyle("Female")));
        layout.addView(makeButton("Young Adult Voice",
                () -> chooseTtsStyle("Young Adult")));
        layout.addView(makeButton("Older Man Voice",
                () -> chooseTtsStyle("Older Man")));
        layout.addView(makeButton("Older Woman Voice",
                () -> chooseTtsStyle("Older Woman")));
        layout.addView(makeButton("Child Voice",
                () -> chooseTtsStyle("Child")));
        layout.addView(makeButton("Baby Voice",
                () -> chooseTtsStyle("Baby")));
        layout.addView(makeButton("Narrator Voice",
                () -> chooseTtsStyle("Narrator")));
        layout.addView(makeButton("Robot Voice",
                () -> chooseTtsStyle("Robot")));
        layout.addView(makeButton("Fast Voice",
                () -> chooseTtsStyle("Fast")));
        layout.addView(makeButton("Slow Voice",
                () -> chooseTtsStyle("Slow")));

        layout.addView(makeText("More Natural Voices", 18));
        layout.addView(makeButton("Choose Installed Phone Voice",
                this::chooseInstalledVoice));

        layout.addView(makeButton("Speak Text", this::speakText));
        layout.addView(makeButton("Stop Speaking", () -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
            }
        }));

        layout.addView(makeText(
                "Voice styles adjust pitch and speed. The actual sound "
                        + "depends on your phone's speech engine. For more "
                        + "natural male, female, child or accent voices, install "
                        + "additional voices in your phone's Text-to-speech "
                        + "settings and select them above. Online AI voices "
                        + "require a separate service and are not connected yet.",
                13));

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void chooseTtsStyle(String style) {
        selectedTtsStyle = style;

        switch (style) {
            case "Male":
                ttsPitch = 0.82f;
                ttsRate = 0.96f;
                break;

            case "Female":
                ttsPitch = 1.22f;
                ttsRate = 1.00f;
                break;

            case "Young Adult":
                ttsPitch = 1.12f;
                ttsRate = 1.05f;
                break;

            case "Older Man":
                ttsPitch = 0.72f;
                ttsRate = 0.82f;
                break;

            case "Older Woman":
                ttsPitch = 1.08f;
                ttsRate = 0.82f;
                break;

            case "Child":
                ttsPitch = 1.42f;
                ttsRate = 0.94f;
                break;

            case "Baby":
                ttsPitch = 1.75f;
                ttsRate = 0.78f;
                break;

            case "Narrator":
                ttsPitch = 0.94f;
                ttsRate = 0.90f;
                break;

            case "Robot":
                ttsPitch = 0.70f;
                ttsRate = 1.00f;
                break;

            case "Fast":
                ttsPitch = 1.00f;
                ttsRate = 1.35f;
                break;

            case "Slow":
                ttsPitch = 1.00f;
                ttsRate = 0.72f;
                break;

            case "Default":
            default:
                ttsPitch = 1.00f;
                ttsRate = 1.00f;
                break;
        }

        applyTtsSettings();

        if (selectedVoiceLabel != null) {
            String voiceName = selectedVoice == null
                    ? "Phone default voice"
                    : selectedVoice.getName();

            selectedVoiceLabel.setText(
                    "Selected style: " + selectedTtsStyle
                            + "\nInstalled voice: " + voiceName);
        }

        toast(style + " voice style selected");
    }

    private void applyTtsSettings() {
        if (!ttsReady || textToSpeech == null) {
            return;
        }

        textToSpeech.setPitch(ttsPitch);
        textToSpeech.setSpeechRate(ttsRate);
    }

    private void startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE);
            return;
        }

        if (isRecording) {
            toast("Recording is already in progress");
            return;
        }

        try {
            File directory = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (directory == null) {
                toast("Recording storage is unavailable");
                return;
            }

            if (!directory.exists()) {
                directory.mkdirs();
            }

            recordingFile = new File(directory,
                    "voice_" + System.currentTimeMillis() + ".m4a");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder = new MediaRecorder(this);
            } else {
                recorder = new MediaRecorder();
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setOutputFile(recordingFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            isRecording = true;
            updateRecordingStatus("Recording...");
            toast("Recording started");
        } catch (Exception e) {
            releaseRecorder();
            isRecording = false;
            updateRecordingStatus("Could not start recording");
            toast("Recording failed. Check microphone permission.");
        }
    }

    private void stopRecording() {
        if (!isRecording || recorder == null) {
            toast("There is no active recording");
            return;
        }

        try {
            recorder.stop();
            updateRecordingStatus("Saved: " + recordingFile.getName());
            toast("Recording saved inside the app");
        } catch (Exception e) {
            toast("Recording was too short or could not be saved");
        } finally {
            releaseRecorder();
            isRecording = false;
        }
    }

    private void playRecording() {
        playWithEffect("Normal");
    }

    private void playWithEffect(String effect) {
        if (recordingFile == null || !recordingFile.exists()) {
            toast("Record your voice first");
            return;
        }

        if (isRecording) {
            toast("Stop recording before playback");
            return;
        }

        stopPlayback();

        try {
            player = new MediaPlayer();
            player.setDataSource(recordingFile.getAbsolutePath());

            player.setOnPreparedListener(mp -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PlaybackParams params = new PlaybackParams();
                        params.setSpeed(1f);
                        params.setPitch(1f);

                        switch (effect) {
                            case "Deep Voice":
                                params.setPitch(0.75f);
                                break;

                            case "High Voice":
                                params.setPitch(1.30f);
                                break;

                            case "Robot Style":
                                params.setPitch(0.70f);
                                break;

                            case "Fast Voice":
                                params.setSpeed(1.35f);
                                params.setPitch(1.10f);
                                break;

                            case "Slow Voice":
                                params.setSpeed(0.75f);
                                break;

                            default:
                                break;
                        }

                        mp.setPlaybackParams(params);
                    } else if (!effect.equals("Normal")) {
                        toast("Playback effects require Android 6 or newer");
                    }

                    mp.start();
                    updateRecordingStatus("Playing: " + effect);
                } catch (Exception e) {
                    toast("Could not apply this playback effect");
                    stopPlayback();
                }
            });

            player.setOnCompletionListener(mp -> {
                updateRecordingStatus("Playback finished");
                stopPlayback();
            });

            player.setOnErrorListener((mp, what, extra) -> {
                toast("Playback failed");
                stopPlayback();
                return true;
            });

            player.prepareAsync();
        } catch (Exception e) {
            stopPlayback();
            toast("Could not play the recording");
        }
    }

    private void stopPlayback() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception ignored) {
            }

            try {
                player.reset();
                player.release();
            } catch (Exception ignored) {
            }

            player = null;
        }
    }

    private void chooseInstalledVoice() {
        if (!ttsReady || textToSpeech == null) {
            toast("Text-to-speech is still starting");
            return;
        }

        try {
            List<Voice> voices = new ArrayList<>(
                    textToSpeech.getVoices());

            if (voices.isEmpty()) {
                toast("No installed voices were found");
                return;
            }

            List<Voice> usableVoices = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            for (Voice voice : voices) {
                if (voice == null) {
                    continue;
                }

                usableVoices.add(voice);

                Locale locale = voice.getLocale();
                String language = locale == null
                        ? "Unknown language"
                        : locale.getDisplayName();

                String quality = voice.getQuality() >= Voice.QUALITY_HIGH
                        ? "High quality" : "Standard quality";

                labels.add(language + " — " + voice.getName()
                        + " (" + quality + ")");
            }

            String[] options = labels.toArray(new String[0]);

            new AlertDialog.Builder(this)
                    .setTitle("Choose an Installed Voice")
                    .setItems(options, (dialog, which) -> {
                        Voice voice = usableVoices.get(which);
                        int result = textToSpeech.setVoice(voice);

                        if (result == TextToSpeech.SUCCESS) {
                            selectedVoice = voice;

                            if (selectedVoiceLabel != null) {
                                selectedVoiceLabel.setText(
                                        "Selected style: " + selectedTtsStyle
                                                + "\nInstalled voice: "
                                                + voice.getName()
                                                + " — "
                                                + voice.getLocale()
                                                        .getDisplayName());
                            }

                            applyTtsSettings();
                            toast("Installed voice selected");
                        } else {
                            toast("Could not select that voice");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        } catch (Exception e) {
            toast("Could not load installed voices");
        }
    }

    private void speakText() {
        if (!ttsReady || textToSpeech == null) {
            toast("Text-to-speech is not ready yet");
            return;
        }

        if (speechInput == null) {
            return;
        }

        String text = speechInput.getText().toString().trim();

        if (text.isEmpty()) {
            toast("Type some text first");
            return;
        }

        try {
            applyTtsSettings();

            textToSpeech.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "face_voice_speech");
        } catch (Exception e) {
            toast("Could not speak the text");
        }
    }

    private void updateRecordingStatus(String text) {
        if (recordingStatus != null) {
            recordingStatus.setText("Status: " + text);
        }
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.reset();
            } catch (Exception ignored) {
            }

            try {
                recorder.release();
            } catch (Exception ignored) {
            }

            recorder = null;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                toast("Microphone permission is needed to record");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (isRecording && recorder != null) {
            try {
                recorder.stop();
            } catch (Exception ignored) {
            }
        }

        isRecording = false;
        releaseRecorder();
        stopPlayback();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        super.onDestroy();
    }
}
