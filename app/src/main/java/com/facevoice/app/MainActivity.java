
package com.facevoice.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

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
                                                                                                                subtitle.setText("Explore creative effects");
                                                                                                                        subtitle.setTextSize(16);
                                                                                                                                subtitle.setTextColor(Color.LTGRAY);
                                                                                                                                        subtitle.setGravity(Gravity.CENTER);

                                                                                                                                                Button faceButton = new Button(this);
                                                                                                                                                        faceButton.setText("Face Effects");

                                                                                                                                                                Button voiceButton = new Button(this);
                                                                                                                                                                        voiceButton.setText("Voice Effects");

                                                                                                                                                                                layout.addView(title);
                                                                                                                                                                                        layout.addView(subtitle);
                                                                                                                                                                                                layout.addView(faceButton);
                                                                                                                                                                                                        layout.addView(voiceButton);

                                                                                                                                                                                                                setContentView(layout);
                                                                                                                                                                                                                    }
                                                                                                                                                                                                                    }
                                                                                                                                                                                                                    