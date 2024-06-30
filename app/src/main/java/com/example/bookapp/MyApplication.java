package com.example.bookapp;

import static com.example.bookapp.Constants.MAX_BYTES_PDF;

import android.app.Application;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.bookapp.adapters.AdapterPdfAdmin;
import com.example.bookapp.models.ModelPdf;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

//cls asta ruleaza inainte de launcher activity
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    //metoda statica pt a converti timestamp la un format mai familiar si pt a o putea folosi peste tot in proiect
    public static final String formatTimestamp(long timestamp){
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        //formatam timestamp la dd/mm/yyy
        String date = DateFormat.format("dd/MM/yyy", cal).toString();

        return date;
    }

    public static void deleteBook(Context context, String bookId, String bookUrl, String bookTitle) {
        String TAG = "DELETE_BOOK_TAG";


        Log.d(TAG, "deleteBook: Deleting..");
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please wait..");
        progressDialog.setMessage("Deleting " + bookTitle + " ...");
        progressDialog.show();

        Log.d(TAG, "deleteBook: Deleting from storage..");
        Uri uri = Uri.parse(bookUrl);
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Deleting from storage");
                        Log.d(TAG, "onSuccess: Now deleting info from db..");

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                        reference.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Deleted from db too.");
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "Book Deleted Successfully!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: Failed to delete from db due to " + e.getMessage());
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: Failed to delete from storage due to " + e.getMessage());
                        progressDialog.dismiss();
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
    public static void loadPdfSize(String pdfUrl, String pdfTitle, TextView sizeTv) {
        String TAG = "PDF_SIZE_TAG";

        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Log.e(TAG, "loadPdfSize: Invalid URL format, it is null or empty");
            return;
        }

        // Extract the timestamp from the URL
        try {
            Uri uri = Uri.parse(pdfUrl);
            String path = uri.getPath();
            String[] segments = path.split("/");
            String timestampString = segments[segments.length - 1].split("\\?")[0]; // Get the last segment before query params

            // Try to parse the timestamp
            long pdfTimestamp = Long.parseLong(timestampString);

            // Get reference to the correct storage location
            StorageReference ref = FirebaseStorage.getInstance().getReference("Books").child(timestampString);
            ref.getMetadata()
                    .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                        @Override
                        public void onSuccess(StorageMetadata storageMetadata) {
                            // Get file size in bytes
                            double bytes = storageMetadata.getSizeBytes();
                            Log.d(TAG, "onSuccess: " + pdfTitle + " " + bytes);

                            // Convert bytes to KB, MB
                            double kb = bytes / 1024;
                            double mb = kb / 1024;

                            if (mb >= 1) {
                                sizeTv.setText(String.format("%.2f", mb) + " MB");
                            } else if (kb >= 1) {
                                sizeTv.setText(String.format("%.2f", kb) + " KB");
                            } else {
                                sizeTv.setText(String.format("%.2f", bytes) + " bytes");
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to get metadata
                            Log.e(TAG, "onFailure: " + e.getMessage());
                        }
                    });
        } catch (NumberFormatException e) {
            Log.e(TAG, "loadPdfSize: Invalid URL format, timestamp could not be parsed: " + pdfUrl, e);
        }
    }

    public static void loadPdfFromUrlSinglePage(String pdfUrl, String pdfTitle, PDFView pdfView, ProgressBar progressBar, TextView pagesTv) {
        String TAG = "PDF_LOAD_SINGLE_TAG";
//         Get url
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Log.e(TAG, "loadPdfFromUrl: Invalid pdfUrl, it is null or empty");
            progressBar.setVisibility(View.GONE);
            return;
        }

//         Validate and parse the URL
        try {
          Uri uri = Uri.parse(pdfUrl);
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
          //  StorageReference ref = FirebaseStorage.getInstance().getReference("Books").child(String.valueOf(uri));
              ref.getBytes(MAX_BYTES_PDF)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Log.d(TAG, "onSuccess: " + pdfTitle + "successfully got the file");

                            // Set to pdfview
                            pdfView.fromBytes(bytes)
                                    .pages(0) // show only first page
                                    .spacing(0)
                                    .enableSwipe(true) // Enable swipe to navigate pages
                                    .swipeHorizontal(false) // Set to false for vertical scrolling
                                    .enableDoubletap(true) // Enable double-tap to zoom
                                    .defaultPage(0) // Set default page to the first page
                                    .onError(new OnErrorListener() {
                                        @Override
                                        public void onError(Throwable t) {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            Log.d(TAG, "onError: " + t.getMessage());
                                        }
                                    })
                                    .onPageError(new OnPageErrorListener() {
                                        @Override
                                        public void onPageError(int page, Throwable t) {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            Log.d(TAG, "onPageError: " + t.getMessage());
                                        }
                                    })
                                    .onLoad(new OnLoadCompleteListener() {
                                        @Override
                                        public void loadComplete(int nbPages) {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            Log.d(TAG, "loadComplete: pdf loaded");

                                            // daca param pagesTv nu e null, atunci setam nr pag
                                            if (pagesTv != null){
                                                pagesTv.setText("" + nbPages);
                                            }
                                        }
                                    })
                                    .load();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Log.d(TAG, "onFailure: failed getting file from url due to " + e.getMessage());
                        }
                    });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "loadPdfFromUrl: Invalid pdfUrl format: " + pdfUrl, e);
            progressBar.setVisibility(View.GONE);
        }
    }
    public static void loadCategory(String categoryId, TextView categoryTv) {
        // obt categoria folosind categoryId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.child(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //obt categoria
                        String category = "" + snapshot.child("category").getValue();

                        //setam la category text view
                        categoryTv.setText(category);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public static void downloadBook(Context context, String bookId, String bookTitle, String bookUrl){
        String nameWithExtension = bookTitle + ".pdf";

        //progress dialog
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please wait..");
        progressDialog.setMessage("Downloading " + nameWithExtension + "...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        //download from firebase storage using url
        try {
            Uri uri = Uri.parse(bookUrl);
            StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
            reference.getBytes(Constants.MAX_BYTES_PDF)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            saveDownloadedBook(context, progressDialog, bytes, nameWithExtension, bookId);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(context, "Failed to download PDF due to: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IllegalArgumentException e) {
            Toast.makeText(context, "Invalid PDF URL format", Toast.LENGTH_SHORT).show();
        }
    }

    private static void saveDownloadedBook(Context context, ProgressDialog progressDialog, byte[] bytes, String nameWithExtension, String bookId) {
        try{
            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            downloadsFolder.mkdirs();

            String filePath = downloadsFolder.getPath() + "/" + nameWithExtension;

            FileOutputStream out = new FileOutputStream(filePath);
            out.write(bytes);
            out.close();

            Toast.makeText(context, "Saved to Download Folder", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();

            incrementBookDownloadCount(bookId);
        }
        catch(Exception e) {
            Toast.makeText(context, "Failed saving to Download Folder due to : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }

    private static void incrementBookDownloadCount(String bookId) {
        //Step 1: luam nr de descarcari
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String downloadsCount = "" + snapshot.child("downloadsCount").getValue();

                        if(downloadsCount.equals("") || downloadsCount.equals("null")) {
                            downloadsCount = "0";
                        }

                        // convertim la long si incrementam cu 1
                        long newDownloadsCount = Long.parseLong(downloadsCount) + 1;

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("downloadsCount", newDownloadsCount);

                        //Step 2 : actualizam descarcarile incrementate ls bd
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                        reference.child(bookId).updateChildren(hashMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public static void incrementBookViewCount(String bookId){
        // 1. obt book views count
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //obt views count
                        String viewsCount = "" + snapshot.child("viewsCount").getValue();
                        //in cazul null, inlocuim cu 0
                        if (viewsCount.equals("") || viewsCount.equals("null")){
                            viewsCount = "0";
                        }

                        // 2. incrementam views count
                        long newViewsCount = Long.parseLong(viewsCount) + 1;

                        HashMap<String, Object> hashMap= new HashMap<>();
                        hashMap.put("viewsCount", newViewsCount);

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                        reference.child(bookId)
                                .updateChildren(hashMap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

//    public static void loadPdfPageCount(Context context, String pdfUrl, TextView pagesTv){
//        //incarcam fisierul pdf din firebase storage folosind url
//        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
//        storageReference
//                .getBytes(MAX_BYTES_PDF)
//                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
//                    @Override
//                    public void onSuccess(byte[] bytes) {
//                        //fisier primit
//
//                        //incarcam pag pdf-ului utiliz libraria PdfView
//                        PDFView pdfView = new PDFView(context, null);
//                        pdfView.fromBytes(bytes)
//                                .onLoad(new OnLoadCompleteListener() {
//                                    @Override
//                                    public void loadComplete(int nbPages) {
//                                        pagesTv.setText("" + nbPages);
//                                    }
//                                });
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        //fisier neprimit
//                    }
//                });
//    }



    public static void addToFavorite(Context context, String bookId){
        //putem adauga doar daca suntem logati
        // 1. Verif ca user e logat
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            //nelogat
            Toast.makeText(context, "You're not logged in..", Toast.LENGTH_SHORT).show();
        }
        else{
            long timestamp = System.currentTimeMillis();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("bookId", "" + bookId);
            hashMap.put("timestamp", "" + timestamp);

            //salvam la bd
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                    .setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(context, "Added to your favorites list!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Failed to add to favorites due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public static void removeFromFavorite(Context context, String bookId){
        //putem sterge doar daca suntem logati
        // 1. Verif ca user e logat
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            //nelogat
            Toast.makeText(context, "You're not logged in..", Toast.LENGTH_SHORT).show();
        }
        else{


            //stergem din bd
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                    .removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(context, "Removed from your favorites list!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Failed to remove from favorites due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

}


