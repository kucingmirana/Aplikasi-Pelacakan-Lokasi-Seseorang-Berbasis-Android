package com.example.peta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class GuideActivity extends AppCompatActivity {

    ImageView guideTracked,guideTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        guideTracked = findViewById(R.id.imgGuideTracked);
        guideTracker = findViewById(R.id.imgGuideTracker);

        guideTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent guideIntent = new Intent(GuideActivity.this, GuideTracker.class);
                startActivity(guideIntent);
            }
        });

        guideTracked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent guideIntent = new Intent(GuideActivity.this, GuideTracked.class);
                startActivity(guideIntent);
            }
        });

    }

}