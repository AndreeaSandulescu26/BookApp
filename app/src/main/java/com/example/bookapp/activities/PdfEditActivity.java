package com.example.bookapp.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.databinding.ActivityPdfEditBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfEditActivity extends AppCompatActivity {


    //view binding
    private ActivityPdfEditBinding binding;

    //book id luat din intentul inceput din AdapterPdfAdmin
    private String bookId;

    //progress dialog
    private ProgressDialog progressDialog;

    private ArrayList<String> categoryTitleArraylist, categoryIdArraylist;

    private static final String TAG = "BOOK_EDIT_TAG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookId = getIntent().getStringExtra("bookId");

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait!");
        progressDialog.setCanceledOnTouchOutside(false);

        loadCategories();
        loadBookInfo();

        //facem click, alegem categorie
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoryDialog();
            }
        });

        //facem click, ne intoarcem la ecranul anterior
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        //facem click pt actualizare
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private void loadBookInfo() {
        Log.d(TAG, "loadBookInfo: Loading book info..");

        DatabaseReference refBooks = FirebaseDatabase.getInstance().getReference("Books");
        refBooks.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //obt info despre carti
                        selectedCategoryId = "" + snapshot.child("categoryId").getValue();
                        String description = "" + snapshot.child("description").getValue();
                        String title = "" + snapshot.child("title").getValue();

                        //set to views
                        binding.titleEt.setText(title);
                        binding.descriptionEt.setText(description);

                        Log.d(TAG, "onDataChange: Loading Book Category Info..");
                        DatabaseReference refBookCategory = FirebaseDatabase.getInstance().getReference("Categories");
                        refBookCategory.child(selectedCategoryId)
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        //obt categoria
                                        String category = "" + snapshot.child("category").getValue();
                                        //set to category
                                        binding.categoryTv.setText(category);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    String title = "", description = "";
    private void validateData(){
        //obt data
        title = binding.titleEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();

        //validam datele
        if (TextUtils.isEmpty(title)){
            Toast.makeText(this, "Enter title..", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(description)){
            Toast.makeText(this, "Enter description..", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(selectedCategoryId)){
            Toast.makeText(this, "Pick category..", Toast.LENGTH_SHORT).show();
        }
        else {
            updatePdf();
        }
    }

    private void updatePdf() {
        Log.d(TAG, "updatePdf: Starting updating pdf info to db..");

        //show progress
        progressDialog.setMessage("Updating book info..");
        progressDialog.show();

        //setup data pt actualiz la bd
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("title", "" + title);
        hashMap.put("description", "" + description);
        hashMap.put("categoryId", "" + selectedCategoryId);

        //start update
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Book updated..");
                        progressDialog.dismiss();
                        Toast.makeText(PdfEditActivity.this, "Book Info Updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: failed to update due to " + e.getMessage());
                        progressDialog.dismiss();
                        Toast.makeText(PdfEditActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String selectedCategoryId="", selectedCategoryTitle="";

    private void categoryDialog(){
        String[] categoriesArray = new String[categoryTitleArraylist.size()];
        for (int i = 0; i < categoryTitleArraylist.size(); i ++){
            categoriesArray[i] =categoryTitleArraylist.get(i);
        }

        //Alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedCategoryId = categoryIdArraylist.get(which);
                        selectedCategoryTitle = categoryTitleArraylist.get(which);

                        //set to textview
                        binding.categoryTv.setText(selectedCategoryTitle);
                    }
                })
                .show();
    }

    private void loadCategories(){
        Log.d(TAG, "loadCategories: Loading categories..");

        categoryIdArraylist = new ArrayList<>();
        categoryTitleArraylist = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryIdArraylist.clear();
                categoryTitleArraylist.clear();
                for(DataSnapshot ds : snapshot.getChildren()){
                    String id = "" + ds.child("id").getValue();
                    String category = "" + ds.child("category").getValue();
                    categoryIdArraylist.add(id);
                    categoryTitleArraylist.add(category);

                    Log.d(TAG, "onDataChange: ID: " + id);
                    Log.d(TAG, "onDataChange: Category: " + category);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}