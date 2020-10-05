package com.example.peta;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private GoogleMap mMap;
    private String kecamatan;

    boolean isFirstTime = true;

    int i=0;
    int j=0;

    TextView tv_kecamatan,name_popup,online_status_popup;
    CircleImageView profile_image_popup;
    Geocoder geocoder;
    Marker marker;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    FusedLocationProviderClient mFusedLocationClient;

    private DatabaseReference ChildRef, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUserID="";

    BottomSheetBehavior bottomSheetBehavior;

    ImageView profileImage1, profileImage2, profileImage3, profileImage4, addChild;

    TextView rvtv1, rvtv2, rvtv3, rvtv4;

    ImageView callPopup, smsPopup;

    LinearLayout llBottomSheet;

    RelativeLayout relativeLayout1, relativeLayout2, relativeLayout3,relativeLayout4;

    Map<String, Marker> mNamedMarkers = new HashMap<String,Marker>();

    public MapFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        tv_kecamatan = v.findViewById(R.id.geocoder);
        profileImage1 = v.findViewById(R.id.profile_image_frag_1);
        profileImage2 = v.findViewById(R.id.profile_image_frag_2);
        profileImage3 = v.findViewById(R.id.profile_image_frag_3);
        profileImage4 = v.findViewById(R.id.profile_image_frag_4);
        relativeLayout1 = v.findViewById(R.id.relative1);
        relativeLayout2 = v.findViewById(R.id.relative2);
        relativeLayout3 = v.findViewById(R.id.relative3);
        relativeLayout4 = v.findViewById(R.id.relative4);
        rvtv1 = v.findViewById(R.id.tv_child_1_map);
        rvtv2 = v.findViewById(R.id.tv_child_2_map);
        rvtv3 = v.findViewById(R.id.tv_child_3_map);
        rvtv4 = v.findViewById(R.id.tv_child_4_map);

        // get the bottom sheet view
        llBottomSheet = (LinearLayout) v.findViewById(R.id.bottom_sheet);

        // init the bottom sheet behavior
        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);

        name_popup = llBottomSheet.findViewById(R.id.name_popup);
        online_status_popup=llBottomSheet.findViewById(R.id.online_status_popup);
        profile_image_popup=llBottomSheet.findViewById(R.id.profile_image_popup);
        callPopup= llBottomSheet.findViewById(R.id.phone_call_popup);
        smsPopup = llBottomSheet.findViewById(R.id.chat_popup);

        addChild = v.findViewById(R.id.add_child);
        addChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addChildIntent = new Intent(getContext(), AddKidOrParentsREG.class);
                startActivity(addChildIntent);
            }
        });
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        geocoder = new Geocoder(getContext(), Locale.getDefault());
        //maps
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChildRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000); // two minute interval
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }


        final ArrayList<Contact> listContact = new ArrayList<>();

        ChildRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                    for (DataSnapshot s : dataSnapshot.getChildren()){
                        i++;
                        final String childKey = s.getKey();
                        Log.d("countchild",childKey);

                        UsersRef.child(childKey).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if (dataSnapshot.exists())
                                {
                                    if (dataSnapshot.hasChild("name") && dataSnapshot.hasChild("uid")){
                                        j++;
                                        String retName = dataSnapshot.child("name").getValue().toString();
                                        String retChildId = dataSnapshot.child("uid").getValue().toString();

                                        String retImage = "default_image";

                                        if (dataSnapshot.hasChild("image"))
                                        {
                                            retImage = dataSnapshot.child("image").getValue().toString();
                                        }

                                        Contact contact = new Contact();
                                        contact.setUid(retChildId);
                                        contact.setPhoto(retImage);
                                        contact.setName(retName);
                                        listContact.add(contact);

                                        if (dataSnapshot.hasChild("location"))
                                        {
                                            String time = dataSnapshot.child("location").child("time").getValue().toString();
                                            String tanggal = dataSnapshot.child("location").child("tanggal").getValue().toString();
                                            String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                            String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                            Double nLat = Double.parseDouble(latitude);
                                            Double nLong = Double.parseDouble(longitude);

                                            LatLng location = new LatLng(nLat,nLong);

                                            Marker marker2 = mNamedMarkers.get(childKey);

                                            if (marker2 == null) {
                                                marker2 = mMap.addMarker(new MarkerOptions().position(location).title(retName).snippet(retChildId));
                                                marker2.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                                                mNamedMarkers.put(childKey, marker2);
                                            } else {
                                                // This marker-already-exists section should never be called in this listener's normal use, but is here to handle edge cases quietly.
                                                // TODO: Confirm if marker title/snippet needs updating.
                                                marker2.setPosition(location);
                                            }

                                        }

                                        if (i==1){
                                            relativeLayout1.setVisibility(View.VISIBLE);
                                            Log.d("childsnapshotimage", listContact.get(0).getUid());
                                            if (!listContact.get(0).getPhoto().equals("default_image")){
                                                Picasso.get().load(listContact.get(0).getPhoto()).fit().centerInside().into(profileImage1);
                                            }
                                            rvtv1.setText(listContact.get(0).getName());
                                            profileImage1.setVisibility(View.VISIBLE);
                                            profileImage1.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Contact contact1 = listContact.get(i-1);
                                                    UsersRef.child(contact1.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild("location")){
                                                                String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                                                String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                                                Double nLat = Double.parseDouble(latitude);
                                                                Double nLong = Double.parseDouble(longitude);

                                                                LatLng latLng = new LatLng(nLat, nLong);
                                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });

                                                }
                                            });
                                        }
                                        if (i==2){
                                            relativeLayout1.setVisibility(View.VISIBLE);
                                            relativeLayout2.setVisibility(View.VISIBLE);
                                            if (j>=2){
                                                Log.d("bangke", listContact.get(0).getUid());
                                                Log.d("bangke", listContact.get(1).getUid());
                                                rvtv1.setText(listContact.get(0).getName());
                                                rvtv2.setText(listContact.get(1).getName());

                                                if (!listContact.get(1).getPhoto().equals("default_image")){
                                                    Picasso.get().load(listContact.get(1).getPhoto()).fit().centerInside().into(profileImage2);
                                                }
                                                if (!listContact.get(0).getPhoto().equals("default_image")){
                                                    Picasso.get().load(listContact.get(0).getPhoto()).fit().centerInside().into(profileImage1);
                                                }
                                            }

                                            profileImage1.setVisibility(View.VISIBLE);
                                            profileImage1.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Contact contact1 = listContact.get(i-2);
                                                    UsersRef.child(contact1.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild("location")){
                                                                String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                                                String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                                                Double nLat = Double.parseDouble(latitude);
                                                                Double nLong = Double.parseDouble(longitude);

                                                                LatLng latLng = new LatLng(nLat, nLong);
                                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });
                                            profileImage2.setVisibility(View.VISIBLE);
                                            profileImage2.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Contact contact1 = listContact.get(i-1);
                                                    UsersRef.child(contact1.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild("location")){
                                                                String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                                                String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                                                Double nLat = Double.parseDouble(latitude);
                                                                Double nLong = Double.parseDouble(longitude);

                                                                LatLng latLng = new LatLng(nLat, nLong);
                                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });
                                        }
                                        if (i==3){
                                            relativeLayout1.setVisibility(View.VISIBLE);
                                            relativeLayout2.setVisibility(View.VISIBLE);
                                            relativeLayout3.setVisibility(View.VISIBLE);

                                            if (j>=3){
                                                Log.d("bangke", listContact.get(0).getUid());
                                                Log.d("bangke", listContact.get(1).getUid());
                                                Log.d("bangke", listContact.get(2).getUid());
                                                rvtv1.setText(listContact.get(0).getName());
                                                rvtv2.setText(listContact.get(1).getName());
                                                rvtv3.setText(listContact.get(2).getName());

                                                if (!listContact.get(2).getPhoto().equals("default_image")){
                                                    Picasso.get().load(listContact.get(2).getPhoto()).fit().centerInside().into(profileImage3);
                                                }

                                                if (!listContact.get(1).getPhoto().equals("default_image")){
                                                    Picasso.get().load(listContact.get(1).getPhoto()).fit().centerInside().into(profileImage2);
                                                }
                                                if (!listContact.get(0).getPhoto().equals("default_image")){
                                                    Picasso.get().load(listContact.get(0).getPhoto()).fit().centerInside().into(profileImage1);
                                                }
                                            }
                                            profileImage1.setVisibility(View.VISIBLE);
                                            profileImage1.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Contact contact1 = listContact.get(i-3);
                                                    UsersRef.child(contact1.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild("location")){
                                                                String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                                                String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                                                Double nLat = Double.parseDouble(latitude);
                                                                Double nLong = Double.parseDouble(longitude);

                                                                LatLng latLng = new LatLng(nLat, nLong);
                                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });

                                            profileImage2.setVisibility(View.VISIBLE);
                                            profileImage2.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Contact contact1 = listContact.get(i-2);
                                                    UsersRef.child(contact1.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild("location")){
                                                                String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                                                String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                                                Double nLat = Double.parseDouble(latitude);
                                                                Double nLong = Double.parseDouble(longitude);

                                                                LatLng latLng = new LatLng(nLat, nLong);
                                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });

                                            profileImage3.setVisibility(View.VISIBLE);
                                            profileImage3.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Contact contact1 = listContact.get(i-1);
                                                    UsersRef.child(contact1.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild("location")){
                                                                String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                                                String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                                                Double nLat = Double.parseDouble(latitude);
                                                                Double nLong = Double.parseDouble(longitude);

                                                                LatLng latLng = new LatLng(nLat, nLong);
                                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });
                                        }
                                        if (i==4){
                                            relativeLayout1.setVisibility(View.VISIBLE);
                                            relativeLayout2.setVisibility(View.VISIBLE);
                                            relativeLayout3.setVisibility(View.VISIBLE);
                                            relativeLayout4.setVisibility(View.VISIBLE);

                                            if (j>=3){
                                                Log.d("bangke", listContact.get(0).getUid());
                                                Log.d("bangke", listContact.get(1).getUid());
                                                Log.d("bangke", listContact.get(2).getUid());

                                                rvtv1.setText(listContact.get(0).getName());
                                                rvtv2.setText(listContact.get(1).getName());
                                                rvtv3.setText(listContact.get(2).getName());
                                                rvtv3.setText(listContact.get(3).getName());

                                                if (!listContact.get(3).getPhoto().equals("default_image")){
                                                    Picasso.get().load(listContact.get(3).getPhoto()).fit().centerInside().into(profileImage4);
                                                }

                                                if (!listContact.get(2).getPhoto().equals("default_image")){
                                                    Picasso.get().load(listContact.get(2).getPhoto()).fit().centerInside().into(profileImage3);
                                                }

                                                if (!listContact.get(1).getPhoto().equals("default_image")){
                                                    Picasso.get().load(listContact.get(1).getPhoto()).fit().centerInside().into(profileImage2);
                                                }
                                                if (!listContact.get(0).getPhoto().equals("default_image")){
                                                    Picasso.get().load(listContact.get(0).getPhoto()).fit().centerInside().into(profileImage1);
                                                }
                                            }

                                            profileImage1.setVisibility(View.VISIBLE);
                                            profileImage1.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Contact contact1 = listContact.get(i-4);
                                                    UsersRef.child(contact1.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild("location")){
                                                                String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                                                String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                                                Double nLat = Double.parseDouble(latitude);
                                                                Double nLong = Double.parseDouble(longitude);

                                                                LatLng latLng = new LatLng(nLat, nLong);
                                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });

                                            profileImage2.setVisibility(View.VISIBLE);
                                            profileImage2.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Contact contact1 = listContact.get(i-3);
                                                    UsersRef.child(contact1.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild("location")){
                                                                String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                                                String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                                                Double nLat = Double.parseDouble(latitude);
                                                                Double nLong = Double.parseDouble(longitude);

                                                                LatLng latLng = new LatLng(nLat, nLong);
                                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });

                                            profileImage3.setVisibility(View.VISIBLE);
                                            profileImage3.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Contact contact1 = listContact.get(i-2);
                                                    UsersRef.child(contact1.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild("location")){
                                                                String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                                                String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                                                Double nLat = Double.parseDouble(latitude);
                                                                Double nLong = Double.parseDouble(longitude);

                                                                LatLng latLng = new LatLng(nLat, nLong);
                                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });

                                            profileImage4.setVisibility(View.VISIBLE);
                                            profileImage4.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Contact contact1 = listContact.get(i-1);
                                                    UsersRef.child(contact1.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.hasChild("location")){
                                                                String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                                                String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                                                Double nLat = Double.parseDouble(latitude);
                                                                Double nLong = Double.parseDouble(longitude);

                                                                LatLng latLng = new LatLng(nLat, nLong);
                                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });

                                        }
                                    }

                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                if (!marker.getTitle().equals("Current Position")){
                    UsersRef.child(marker.getSnippet()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()){
                                if (dataSnapshot.hasChild("name") && dataSnapshot.hasChild("uid") &&
                                        dataSnapshot.hasChild("phone")){
                                    final String retName = dataSnapshot.child("name").getValue().toString();
                                    final String retPhone = dataSnapshot.child("phone").getValue().toString();
                                    final String retChildId = dataSnapshot.child("uid").getValue().toString();
                                    final String[] retImage = {"default_image"};

                                    if (dataSnapshot.child("userState").hasChild("state"))
                                    {
                                        String retOnlineStatus = dataSnapshot.child("userState").child("state").getValue().toString();
                                        online_status_popup.setText("Status :" +retOnlineStatus);
                                    }

                                    if (dataSnapshot.hasChild("image"))
                                    {
                                        retImage[0] = dataSnapshot.child("image").getValue().toString();
                                        Picasso.get().load(retImage[0]).fit().centerInside().into(profile_image_popup);
                                    }
                                    else {
                                        profile_image_popup.setImageResource(R.drawable.ic_person_account);
                                    }

                                    name_popup.setText(retName);
                                    callPopup.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                                            callIntent.setData(Uri.parse("tel:"+"+62"+retPhone));
                                            startActivity(callIntent);
                                        }
                                    });

                                    smsPopup.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            chatIntent.putExtra("visit_user_id", retChildId);
                                            chatIntent.putExtra("visit_user_name", retName);
                                            chatIntent.putExtra("visit_image", retImage[0]);
                                            startActivity(chatIntent);
                                        }
                                    });

                                    if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                                    } else {
                                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                                    }
                                }

                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                return false;
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        mMap.setOnCameraIdleListener(this);


    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude() + " | " + location);
                mLastLocation = location;
                if (marker != null) {
                    marker.remove();
                }

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current Position");
                marker = mMap.addMarker(markerOptions);

                if (isFirstTime){

                    //move map camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
                    isFirstTime = false;
                }

            }
        }
    };

    @Override
    public void onCameraIdle() {
        CameraPosition position=mMap.getCameraPosition();
        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(position.target.latitude, position.target.longitude, 1);

            if (addresses != null && !addresses.isEmpty()){
                kecamatan = addresses.get(0).getLocality();
                if (kecamatan != null){
                    if (kecamatan.length() > 10 ){
                        if (kecamatan.substring(0,10).equals("Kecamatan ")){
                            kecamatan = kecamatan.replace("Kecamatan ","");
                            tv_kecamatan.setText(kecamatan);
                        }
                    }
                    else {
                        tv_kecamatan.setText(kecamatan);
                    }


                    Log.i("KECAMATAN", kecamatan);
                }
                else {
                    tv_kecamatan.setText("");
                }

            }

        } catch (IOException e1) {
            e1.printStackTrace();
        }

        Log.d("onCameraIdle",
                String.format("lat: %f, lon: %f, zoom: %f, tilt: %f",
                        position.target.latitude,
                        position.target.longitude, position.zoom,
                        position.tilt));
    }
}
