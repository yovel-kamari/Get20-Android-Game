package com.example.get20;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private TextView scoreText;
    private Bitmap[] images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        loadImages();

        scoreText = findViewById(R.id.scoreText);
        gameView = findViewById(R.id.gameView);

        gameView.setImages(images);

        gameView.setScoreListener(score ->
                scoreText.setText("Score: " + score)
        );
    }

    private void loadImages() {

        images = new Bitmap[40];

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        for (int i = 1; i <= 20; i++) {

            int normalResId = getResources().getIdentifier(
                    "p" + i,
                    "drawable",
                    getPackageName()
            );

            int selectedResId = getResources().getIdentifier(
                    "p" + i + "a",
                    "drawable",
                    getPackageName()
            );

            images[i - 1] = BitmapFactory.decodeResource(getResources(), normalResId, options);
            images[i - 1 + 20] = BitmapFactory.decodeResource(getResources(), selectedResId, options);
        }
    }
}