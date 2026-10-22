package com.facevoice.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.rgb(18, 20, 32));

        TextView title = new TextView(this);
        title.setText("FACE & VOICE STUDIO");
        title.setTextSize(26);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("Choose your creative effect");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);

        Button faceButton = new Button(this);
        faceButton.setText("Face Effects");

        Button voiceButton = new Button(this);
        voiceButton.setText("Voice Effects");

        faceButton.setOnClickListener(v ->
            showFeature("Face Effects",
                "Face effects screen is working. Next, we will add photo selection and face filters.")
        );

        voiceButton.setOnClickListener(v ->
            showFeature("Voice Effects",
                "Voice effects screen is working. Next, we will add audio recording and voice effects.")
        );

        layout.addView(title);
        layout.addView(subtitle);
        layout.addView(faceButton);
        layout.addView(voiceButton);

        setContentView(layout);
    }

    private void showFeature(String heading, String message) {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER);
        screen.setPadding(32, 32, 32, 32);
        screen.setBackgroundColor(Color.rgb(18, 20, 32));

        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextSize(26);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);

        TextView description = new TextView(this);
        description.setText(message);
        description.setTextSize(17);
        description.setTextColor(Color.LTGRAY);
        description.setGravity(Gravity.CENTER);

        Button backButton = new Button(this);
        backButton.setText("Back to Home");
        backButton.setOnClickListener(v -> onCreate(null));

        screen.addView(title);
        screen.addView(description);
        screen.addView(backButton);

        setContentView(screen);
    }
}
                                                                                                                                                         
