package com.example.bookapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.Constants;
import com.example.bookapp.databinding.ActivityPdfViewBinding;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class PdfViewActivity extends AppCompatActivity {

    //view binding
    private ActivityPdfViewBinding binding;
    private String bookId;
    private static final String TAG = "PDF_VIEW_TAG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //obt bookId din intent
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");
        Log.d(TAG, "onCreate: BookId " + bookId);

        loadBookDetails();

        //Facem click, go back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void loadBookDetails() {
        //1. obt book url folosind bookId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //obt book url
                        String pdfUrl = "" + snapshot.child("url").getValue();

                        //2. load pdf folosind acel url
                        loadBookFromUrl(pdfUrl);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadBookFromUrl(String pdfUrl) {
        // Ensure the URL is properly formatted and valid
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Toast.makeText(PdfViewActivity.this, "Invalid PDF URL", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        try {
            Uri uri = Uri.parse(pdfUrl);
            StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
            reference.getBytes(Constants.MAX_BYTES_PDF)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            // Load PDF using bytes
                            binding.pdfView.fromBytes(bytes)
                                    .swipeHorizontal(false)
                                    .onPageChange(new OnPageChangeListener() {
                                        @Override
                                        public void onPageChanged(int page, int pageCount) {
                                            // Set current and total pages in toolbar subtitle
                                            int currentPage = (page + 1);
                                            binding.toolbarSubtitleTv.setText(currentPage + "/" + pageCount);
                                        }
                                    })
                                    .onError(new OnErrorListener() {
                                        @Override
                                        public void onError(Throwable t) {
                                            Toast.makeText(PdfViewActivity.this, "Error loading PDF: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                            binding.progressBar.setVisibility(View.GONE);
                                        }
                                    })
                                    .onPageError(new OnPageErrorListener() {
                                        @Override
                                        public void onPageError(int page, Throwable t) {
                                            Toast.makeText(PdfViewActivity.this, "Error on page " + page + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                            binding.progressBar.setVisibility(View.GONE);
                                        }
                                    })
                                    .load();

                            binding.progressBar.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to load book
                            Toast.makeText(PdfViewActivity.this, "Failed to load PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    });
        } catch (IllegalArgumentException e) {
            Toast.makeText(PdfViewActivity.this, "Invalid PDF URL format", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
        }
    }



}