package com.example.peta;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.peta.Notification.Token;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class InputName extends AppCompatActivity {

    EditText et_name;
    Button btnName;
    String name;

    private FirebaseAuth mAuth;
    private String UserID,phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_name);
        et_name = findViewById(R.id.et_name);
        btnName = findViewById(R.id.btnname);

        mAuth = FirebaseAuth.getInstance();
        UserID = mAuth.getCurrentUser().getUid();

        phoneNumber = getIntent().getExtras().get("phone_number").toString();

        btnName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = et_name.getText().toString().trim();
                if(name.isEmpty()){
                    et_name.setError("Invalid Phone Number");
                }else {
                    Intent intent = new Intent(InputName.this,InputStatus.class);
                    intent.putExtra("name", name);
                    intent.putExtra("phone_number", phoneNumber);
                    startActivity(intent);

                }
            }
        });
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(InputName.this, new
                OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        updateToken(instanceIdResult.getToken());
                    }
                });

    }

    private void updateToken(String token){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 = new Token(token);
        reference.child(UserID).setValue(token1);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}
