package com.sourav.livebusgietu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class DevTeam extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_dev_team);
        Window window = getWindow();
        View decorView = window.getDecorView();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, decorView);
        insetsController.setAppearanceLightStatusBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        ImageView anshumanEmail = findViewById(R.id.anshumanemail);
        ImageView anshumanInsta = findViewById(R.id.anshumaninsta);
        ImageView anshumanGit = findViewById(R.id.anshumangithub);
        ImageView anshumanLinkedIn = findViewById(R.id.anshumanlinkdin);
        ImageView Devback =findViewById(R.id.devback);
        Devback.setOnClickListener(v-> finish());

        anshumanEmail.setOnClickListener(v -> openEmail("anshuman@example.com"));
        anshumanInsta.setOnClickListener(v -> openUrl("https://www.instagram.com/anshuman.br?igsh=MWwzMzRjamVxbm50cw=="));
        anshumanGit.setOnClickListener(v -> openUrl("https://github.com/AnshumanMahanta"));
        anshumanLinkedIn.setOnClickListener(v -> openUrl("https://www.linkedin.com/in/anshuman-mahanta-149054278?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app"));

        ImageView souravEmail = findViewById(R.id.souravemail);
        ImageView souravInsta = findViewById(R.id.souravinsta);
        ImageView souravGit = findViewById(R.id.souravgithub);
        ImageView souravLinkedIn = findViewById(R.id.souravlinkdin);

        souravEmail.setOnClickListener(v -> openEmail("souravpati451@gmail.com"));
        souravInsta.setOnClickListener(v -> openUrl("https://www.instagram.com/sourav.pati_?utm_source=ig_web_button_share_sheet&igsh=ZDNlZDc0MzIxNw=="));
        souravGit.setOnClickListener(v -> openUrl("https://github.com/Fpvsourav955"));
        souravLinkedIn.setOnClickListener(v -> openUrl("https://www.linkedin.com/in/sourav-kumar-pati-aa0833297?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app"));

        ImageView mukeshEmail = findViewById(R.id.mukeshemail);
        ImageView mukeshInsta = findViewById(R.id.mukeshinsta);
        ImageView mukeshGit = findViewById(R.id.mukeshgithub);
        ImageView mukeshLinkedIn = findViewById(R.id.mukeshlinkdin);

        mukeshEmail.setOnClickListener(v -> openEmail("mukesh@example.com"));
        mukeshInsta.setOnClickListener(v -> openUrl("https://www.instagram.com/mr_raj_mm_?igsh=Y2Q4eHVyNzBnY3Ex"));
        mukeshGit.setOnClickListener(v -> openUrl("https://github.com/mukeshbala143"));
        mukeshLinkedIn.setOnClickListener(v -> openUrl("https://www.linkedin.com/in/mukesh-bala-aa9743231?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app"));
    }
    

private void openUrl(String url) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(intent);
}

private void openEmail(String emailAddress) {
    Intent intent = new Intent(Intent.ACTION_SENDTO);
    intent.setData(Uri.parse("mailto:" + emailAddress));
    intent.putExtra(Intent.EXTRA_SUBJECT, "Hello from DevTeam App");
    startActivity(intent);
}
}