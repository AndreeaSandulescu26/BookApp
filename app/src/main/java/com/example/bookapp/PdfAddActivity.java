package com.example.bookapp;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.databinding.ActivityPdfAddBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfAddActivity extends AppCompatActivity {

    //setup view binding
    private ActivityPdfAddBinding binding;

    //uri of picked pdf
    private Uri pdfUri = null;

    //firebase auth
    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    //arraylist pentru pdf categories
    private ArrayList<String> categoryTitleArrayList, categoryIdArraylist;

    private static final int PDF_PICK_CODE = 1000;

    //Tag pentru debugging
    private static final String TAG = "ADD_PDF_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        loadPdfCategories();

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait..");
        progressDialog.setCanceledOnTouchOutside(false);

        // facem click, go back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // facem click, adaugam pdf
        binding.attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pdfPickIntent();
            }
        });

        // facem click, alegem categorie
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoryPickDialog();
            }
        });

        // facem click, incarcam pdf
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //validate data
                validateData();
            }
        });

    }

    private String title = "", description = "";
    private void validateData() {
        // Pasul 1: Validam datele
        Log.d(TAG, "validateData: validating data..");

        // obtinem datele
        title = binding.titleEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();

        // le validam
        if (TextUtils.isEmpty(title)){
            Toast.makeText(this, "Enter Title..", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Enter description..", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(selectedCategoryTitle)){
            Toast.makeText(this, "Pick category..", Toast.LENGTH_SHORT).show();
        }
        else if(pdfUri == null){
            Toast.makeText(this, "Pick PDF..", Toast.LENGTH_SHORT).show();
        }
        else {
            // toate datele sunt validate deci putem incarca
            uploadPdfToStorage();
        }
    }

    private void uploadPdfToStorage() {
        // Pasul 2: incarcam PDF in firebase storage
        Log.d(TAG, "uploadPdfToStorage: uploading to storage..");

        //show progress
        progressDialog.setMessage("Uploading PDF..");
        progressDialog.show();

        // timestamp
        long timestamp = System.currentTimeMillis();

        // path of PDF in firebase storage
        String filePathAndName = "Books/" + timestamp;
        // storage reference
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(pdfUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "onSuccess: PDF uploaded to storage..");
                        Log.d(TAG, "onSuccess: getting PDF url..");

                        // obtinem pdf url
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while(!uriTask.isSuccessful());
                        String uploadedPdfUrl = " " + uriTask.getResult();

                        // incarcam in firebase bd
                        uploadPdfInfoToDb(uploadedPdfUrl, timestamp);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: PDF upload failed due to " + e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "PDF upload failed due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadPdfInfoToDb(String uploadedPdfUrl, long timestamp) {
        // Pasul 3: incarcam PDF info in firebase bd
        Log.d(TAG, "uploadPdfToStorage: uploading PDF info to firebase db..");

        progressDialog.setMessage("Uploading PDF info..");

        String uid = firebaseAuth.getUid();

        //setup data ca sa incarcam
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", "" + uid);
        hashMap.put("id", "" + timestamp);
        hashMap.put("title", "" + title);
        hashMap.put("description", "" + description);
        hashMap.put("categoryId", "" + selectedCategoryId);
        hashMap.put("url", "" + uploadedPdfUrl);
        hashMap.put("timestamp", "" + timestamp);

        //db reference: DB > Books
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child("" + timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onSuccess: Successfully uploaded!");
                        Toast.makeText(PdfAddActivity.this, "Successfully uploaded!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: Failed to upload to db due to: " + e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Failed to upload to db due to: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


    }

    private void loadPdfCategories() {
        Log.d(TAG, "loadPdfCategories: Loading pdf categories..");
        categoryTitleArrayList = new ArrayList<>();
        categoryIdArraylist = new ArrayList<>();

        //db reference ca sa incarcam categoriile.. db > Categories
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryTitleArrayList.clear(); //clear before adding data
                categoryIdArraylist.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    //luam id-ul si titlul categoriei
                    String categoryId = "" + ds.child("id").getValue();
                    String categoryTitle = "" + ds.child("category").getValue();

                    //adaugam la arraylist-ul respectiv
                    categoryTitleArrayList.add(categoryTitle);
                    categoryIdArraylist.add(categoryId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //id category si title category selectate
    private String selectedCategoryId, selectedCategoryTitle;
    private void categoryPickDialog() {
        Log.d(TAG, "categoryPickDialog: showing category pick dialog");

        // obtinem string array de categories din arraylist
        String[] categoriesArray = new String[categoryTitleArrayList.size()];
        for(int i = 0; i < categoryTitleArrayList.size(); i++){
            categoriesArray[i] = categoryTitleArrayList.get(i);
        }

        //alert dialog
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle("Pick Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // facem click pe item
                        // luam item-ul din list
                        selectedCategoryTitle = categoryTitleArrayList.get(which);
                        selectedCategoryId = categoryIdArraylist.get(which);
                        //set la category textview
                        binding.categoryTv.setText(selectedCategoryTitle);

                        Log.d(TAG, "onClick: Selected Category " + selectedCategoryId + " " + selectedCategoryTitle);
                    }
                })
                .show();
    }

    private void pdfPickIntent() {
        Log.d(TAG, "pdfPickIntent: starting pdf pick intent");

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select PDF"),PDF_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            if (requestCode == PDF_PICK_CODE){
                Log.d(TAG, "onActivityResult: PDF picked");

                pdfUri = data.getData();

                Log.d(TAG, "onActivityResult: URI: "+ pdfUri);

            }
        }
        else{
            Log.d(TAG, "onActivityResult: cancelled picking pdf");
            Toast.makeText(this, "cancelled picking pdf", Toast.LENGTH_SHORT).show();
        }
    }
}