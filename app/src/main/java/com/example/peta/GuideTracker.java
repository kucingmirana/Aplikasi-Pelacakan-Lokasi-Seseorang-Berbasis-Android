package com.example.peta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;

import com.example.peta.manual.GuideAddChild;
import com.example.peta.manual.GuideAddTracked;
import com.example.peta.manual.GuideChatWithTracked;
import com.example.peta.manual.GuideHistory;
import com.example.peta.manual.GuideInputName;
import com.example.peta.manual.GuideInputStatus;
import com.example.peta.manual.GuideListChild;
import com.example.peta.manual.GuideLogin;
import com.example.peta.manual.GuideMenuOrtu;
import com.example.peta.manual.GuideUserTrackerProfile;
import com.example.peta.manual.GuideVerifCode;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;

public class GuideTracker extends AppCompatActivity {

    SpringDotsIndicator dotsIndicator;
    ViewPager mPager;
    GuideLogin guideLogin;
    GuideVerifCode guideVerifCode;
    GuideInputStatus guideInputStatus;
    GuideMenuOrtu guideMenuOrtu;
    GuideAddTracked guideAddTracked;
    GuideAddChild guideAddChild;
    GuideListChild guideListChild;
    GuideChatWithTracked guideChatWithTracked;
    GuideHistory guideHistory;
    GuideUserTrackerProfile guideUserTrackerProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_tracker);

        dotsIndicator = (SpringDotsIndicator) findViewById(R.id.spring_dots_indicator);
        mPager = (ViewPager) findViewById(R.id.pager);
        setupViewPager(mPager);
        dotsIndicator.setViewPager(mPager);
        dotsIndicator.setDotsClickable(true);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        guideLogin = new GuideLogin();
        guideVerifCode = new GuideVerifCode();
        guideInputStatus = new GuideInputStatus();
        guideMenuOrtu = new GuideMenuOrtu();
        guideAddTracked = new GuideAddTracked();
        guideAddChild = new GuideAddChild();
        guideListChild = new GuideListChild();
        guideChatWithTracked = new GuideChatWithTracked();
        guideHistory = new GuideHistory();
        guideUserTrackerProfile = new GuideUserTrackerProfile();
        viewPagerAdapter.addFragment(guideLogin);
        viewPagerAdapter.addFragment(guideVerifCode);
        viewPagerAdapter.addFragment(guideInputStatus);
        viewPagerAdapter.addFragment(guideMenuOrtu);
        viewPagerAdapter.addFragment(guideAddTracked);
        viewPagerAdapter.addFragment(guideAddChild);
        viewPagerAdapter.addFragment(guideListChild);
        viewPagerAdapter.addFragment(guideChatWithTracked);
        viewPagerAdapter.addFragment(guideHistory);
        viewPagerAdapter.addFragment(guideUserTrackerProfile);
        viewPager.setAdapter(viewPagerAdapter);
    }

}