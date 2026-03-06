package com.leethub.lifesaver;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class AboutActivity extends AppCompatActivity {

    private MaterialButton btnContactMe;
    private TextView tvEmailAbout;
    private ImageView ivGithub, ivLinkedIn, ivInstagram;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        btnContactMe = findViewById(R.id.btnContactMe);
        tvEmailAbout = findViewById(R.id.tvEmailAbout);
        ivGithub = findViewById(R.id.ivGithubAbout);
        ivLinkedIn = findViewById(R.id.ivLinkedinAbout);
        ivInstagram = findViewById(R.id.ivInstagramAbout);

        View.OnClickListener emailAction = v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:leethub.official@gmail.com"));
            startActivity(intent);
        };

        btnContactMe.setOnClickListener(emailAction);
        tvEmailAbout.setOnClickListener(emailAction);

        ivGithub.setOnClickListener(v -> openUrl("https://github.com/Leet-Hub"));
        ivLinkedIn.setOnClickListener(v -> openUrl("https://www.linkedin.com/in/sudarshan-leet-lanjile"));
        ivInstagram.setOnClickListener(v -> openUrl("https://www.instagram.com/yourleet"));
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
