package com.example.bookapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    //firebase auth
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();

        //start main screen after 2 sec
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //start main screen
                checkUser();
            }
            },2000); //2000 means 2 sec
        }

    private void checkUser() {
        //preluam user-ul curent daca este logat
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if(firebaseUser == null){
            // nu este logat
            //start main screen
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
        else{
            // este logat
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            // get user type
                            String userType = "" + snapshot.child("userType").getValue();
                            //check user type
                            if(userType.equals("user")){
                                startActivity(new Intent(SplashActivity.this, DashboardUserActivity.class));
                                finish();
                            }
                            else if (userType.equals("admin")){
                                startActivity(new Intent(SplashActivity.this, DashboardAdminActivity.class));
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {

                        }
                    });
        }
    }
}
