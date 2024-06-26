package com.example.bookapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    //view binding
    private ActivityLoginBinding binding;

    //firebase auth
    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        // facem click, ne redirectioneaza pe ecranul de register
        binding.noAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        // facem click, ne logam
        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();

            }
        });
    }

    private String email = "", password = "";

    /*Inainte sa ne autentificam, verificam datele*/
    private void validateData() {

        //luam datele
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString().trim();

        //validam datele
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //email edit text is empty sau nu e valid => nu poti continua in cazul asta
            Toast.makeText(this, "Invalid email pattern..!", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(password)){
            //password edit text is empty => introdu parola
            Toast.makeText(this, "Enter password..", Toast.LENGTH_SHORT).show();
        }
        else {
            //datele sunt validate => putem sa ne logam
            loginUser();
        }
    }

    private void loginUser() {
        progressDialog.setMessage("Logging in..");
        progressDialog.show();

        //autentificare user
        firebaseAuth.signInWithEmailAndPassword(email,password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //login success, verif daca utiliz este user sau admin
                        checkUser();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //login failed
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUser() {
        progressDialog.setMessage("Checking User..");
        // verif daca utiliz este user sau admin din realtime database
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        // verifi in baza de date
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        progressDialog.dismiss();
                        // get user type
                        String userType = "" + snapshot.child("userType").getValue();
                        //check user type
                        if(userType.equals("user")){
                            startActivity(new Intent(LoginActivity.this, DashboardUserActivity.class));
                            finish();
                        }
                        else if (userType.equals("admin")){
                            startActivity(new Intent(LoginActivity.this, DashboardAdminActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                    }
                });
    }
}