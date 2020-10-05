package com.example.peta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;

import com.example.peta.manual.GuideAddChild;
import com.example.peta.manual.GuideAddTracked;
import com.example.peta.manual.GuideAddTrackerUser;
import com.example.peta.manual.GuideChatWithTracked;
import com.example.peta.manual.GuideChatWithTracker;
import com.example.peta.manual.GuideHistory;
import com.example.peta.manual.GuideInputName;
import com.example.peta.manual.GuideInputStatus;
import com.example.peta.manual.GuideInputStatusTracked;
import com.example.peta.manual.GuideListChild;
import com.example.peta.manual.GuideLogin;
import com.example.peta.manual.GuideMenuOrtu;
import com.example.peta.manual.GuideParentProfile;
import com.example.peta.manual.GuideSettingsProfileTracked;
import com.example.peta.manual.GuideTrackedMenu;
import com.example.peta.manual.GuideUserTrackerProfile;
import com.example.peta.manual.GuideVerifCode;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;

public class GuideTracked extends AppCompatActivity {

    SpringDotsIndicator dotsIndicator;
    ViewPager mPager;
    GuideLogin guideLogin;
    GuideVerifCode guideVerifCode;
    GuideInputStatusTracked guideInputStatusTracked;
    GuideAddTrackerUser guideAddTrackerUser;
    GuideTrackedMenu guideTrackedMenu;
    GuideSettingsProfileTracked guideSettingsProfileTracked;
    GuideParentProfile guideParentProfile;
    GuideChatWithTracker guideChatWithTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_tracked);

        dotsIndicator = (SpringDotsIndicator) findViewById(R.id.spring_dots_indicator_2);
        mPager = (ViewPager) findViewById(R.id.pager2);
        setupViewPager(mPager);
        dotsIndicator.setViewPager(mPager);
        dotsIndicator.setDotsClickable(true);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        guideLogin = new GuideLogin();
        guideVerifCode = new GuideVerifCode();
        guideInputStatusTracked = new GuideInputStatusTracked();
        guideAddTrackerUser = new GuideAddTrackerUser();
        guideTrackedMenu = new GuideTrackedMenu();
        guideSettingsProfileTracked = new GuideSettingsProfileTracked();
        guideParentProfile = new GuideParentProfile();
        guideChatWithTracker = new GuideChatWithTracker();
        viewPagerAdapter.addFragment(guideLogin);
        viewPagerAdapter.addFragment(guideVerifCode);
        viewPagerAdapter.addFragment(guideInputStatusTracked);
        viewPagerAdapter.addFragment(guideAddTrackerUser);
        viewPagerAdapter.addFragment(guideTrackedMenu);
        viewPagerAdapter.addFragment(guideSettingsProfileTracked);
        viewPagerAdapter.addFragment(guideParentProfile);
        viewPagerAdapter.addFragment(guideChatWithTracker);
        viewPager.setAdapter(viewPagerAdapter);
    }
}