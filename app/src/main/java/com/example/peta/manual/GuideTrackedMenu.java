package com.example.peta.manual;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.peta.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GuideTrackedMenu#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GuideTrackedMenu extends Fragment {

    public GuideTrackedMenu() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_guide_tracked_menu, container, false);
    }
}