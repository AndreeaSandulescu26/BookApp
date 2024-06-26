package com.example.bookapp.adapters;

import static com.example.bookapp.Constants.MAX_BYTES_PDF;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.MyApplication;
import com.example.bookapp.activities.PdfDetailActivity;
import com.example.bookapp.activities.PdfEditActivity;
import com.example.bookapp.databinding.RowPdfAdminBinding;
import com.example.bookapp.filters.FilterPdfAdmin;
import com.example.bookapp.models.ModelPdf;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;


public class AdapterPdfAdmin extends RecyclerView.Adapter<AdapterPdfAdmin.HolderPdfAdmin> implements Filterable {

    //context
    private Context context;
    //arraylist pt lista de date ale tipului de ModelPdf
    public ArrayList<ModelPdf> pdfArrayList, filterList;

    //view binding row_pdf_admin.xml
    private RowPdfAdminBinding binding;

    private FilterPdfAdmin filter;
    private static final String TAG = "PDF_ADAPTER_TAG";

    //progress
    private ProgressDialog progressDialog;

    //constructor
    public AdapterPdfAdmin(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
        this.filterList = pdfArrayList;

        //init progress dialog
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please wait!");
        progressDialog.setCanceledOnTouchOutside(false);
    }

    @NonNull
    @Override
    public HolderPdfAdmin onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //bind layout cu view binding
        binding = RowPdfAdminBinding.inflate(LayoutInflater.from(context), parent, false);
        return new HolderPdfAdmin(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterPdfAdmin.HolderPdfAdmin holder, int position) {
        //get data, set data, facem click-uri etc

        //get data
        ModelPdf model = pdfArrayList.get(position);
        String pdfId = model.getId();
        String categoryId = model.getCategoryId();
        String title = model.getTitle();
        String description = model.getDescription();
        String pdfUrl = model.getUrl();
        long timestamp = model.getTimestamp();

        //convertim timestamp la formatul dd/mm/yyyy
        String formattedDate = MyApplication.formatTimestamp(timestamp);

        //set data
        holder.titleTv.setText(title);
        holder.descriptionTv.setText(description);
        holder.dateTv.setText(formattedDate);

        //incarcam urm detalii cum ar fi categoria, pdf din url, marime pdf
        MyApplication.loadCategory("" + categoryId, holder.categoryTv);
        MyApplication.loadPdfFromUrlSinglePage("" + pdfUrl, "" + title, holder.pdfView, holder.progressBar);
        MyApplication.loadPdfSize("" + pdfUrl, "" + title, holder.sizeTv);

        //facem click pe cele 3 puncte - moreBtn
        holder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreOptionsDialog(model, holder);
            }
        });

        //facem click pe carte/pdf, deschidem pagina cu detaliile cartii
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PdfDetailActivity.class);
                intent.putExtra("bookId", pdfId);
                context.startActivity(intent);
            }
        });

    }

    private void moreOptionsDialog(ModelPdf model, HolderPdfAdmin holder) {

        String bookId = model.getId();
        String bookUrl = model.getUrl();
        String bookTitle = model.getTitle();

        //optiuni de aratat cand apasam pe moreBtn
        String[] options = {"Edit", "Delete"};

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose Option")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0){
                            //click pe edit => deschidem activitatea PdfEditActivity ca sa editam
                            Intent intent = new Intent(context, PdfEditActivity.class);
                            intent.putExtra("bookId", bookId);
                            context.startActivity(intent);
                        } else if (which == 1) {
                            //click pe delete
                            MyApplication.deleteBook(context, "" + bookId, "" + bookUrl, ""+bookTitle);
                            //deleteBook(model, holder);
                        }
                    }
                })
                .show();
    }

    //loadPdfFromUrl
    private void loadPdfFromUrl(ModelPdf model, HolderPdfAdmin holder) {
        // Get url
        String pdfUrl = model.getUrl();
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Log.e(TAG, "loadPdfFromUrl: Invalid pdfUrl, it is null or empty");
            holder.progressBar.setVisibility(View.GONE);
            return;
        }

        // Validate and parse the URL
        try {
            Uri uri = Uri.parse(pdfUrl);
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);

            ref.getBytes(MAX_BYTES_PDF)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Log.d(TAG, "onSuccess: successfully got the file");

                            // Set to pdfview
                            holder.pdfView.fromBytes(bytes)
                                    .pages(0) // show only first page
                                    .spacing(0)
                                    .enableSwipe(true) // Enable swipe to navigate pages
                                    .swipeHorizontal(false) // Set to false for vertical scrolling
                                    .enableDoubletap(true) // Enable double-tap to zoom
                                    .defaultPage(0) // Set default page to the first page
                                    .onError(new OnErrorListener() {
                                        @Override
                                        public void onError(Throwable t) {
                                            holder.progressBar.setVisibility(View.INVISIBLE);
                                            Log.d(TAG, "onError: " + t.getMessage());
                                        }
                                    })
                                    .onPageError(new OnPageErrorListener() {
                                        @Override
                                        public void onPageError(int page, Throwable t) {
                                            holder.progressBar.setVisibility(View.INVISIBLE);
                                            Log.d(TAG, "onPageError: " + t.getMessage());
                                        }
                                    })
                                    .onLoad(new OnLoadCompleteListener() {
                                        @Override
                                        public void loadComplete(int nbPages) {
                                            holder.progressBar.setVisibility(View.INVISIBLE);
                                            Log.d(TAG, "loadComplete: pdf loaded");
                                        }
                                    })
                                    .load();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            holder.progressBar.setVisibility(View.INVISIBLE);
                            Log.d(TAG, "onFailure: failed getting file from url due to " + e.getMessage());
                        }
                    });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "loadPdfFromUrl: Invalid pdfUrl format: " + pdfUrl, e);
            holder.progressBar.setVisibility(View.GONE);
        }
    }

//    private void loadCategory(ModelPdf model, HolderPdfAdmin holder) {
//        // obt categoria folosind categoryId
//        String categoryId = model.getCategoryId();
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
//        ref.child(categoryId)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        //obt categoria
//                        String category = "" + snapshot.child("category").getValue();
//
//                        //setam la category text view
//                        holder.categoryTv.setText(category);
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//
//                    }
//                });
//    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size(); //returneaza number of records | list size
    }

    @Override
    public Filter getFilter() {
        if(filter == null){
            filter = new FilterPdfAdmin(filterList, this);
        }
        return filter;
    }

    //vedem holder class pt row_pdf_admin.xml
    class HolderPdfAdmin extends RecyclerView.ViewHolder{

        //UI views of row_pdf_admin.xml
        PDFView pdfView;
        ProgressBar progressBar;
        TextView titleTv, descriptionTv, categoryTv, sizeTv, dateTv;
        ImageButton moreBtn;

        public HolderPdfAdmin(@NonNull View itemView) {
            super(itemView);

            // init ui views
            pdfView = binding.pdfView;
            progressBar = binding.progressBar;
            titleTv = binding.titleTv;
            descriptionTv = binding.descriptionTv;
            categoryTv = binding.categoryTv;
            sizeTv = binding.sizeTv;
            dateTv = binding.dateTv;
            moreBtn = binding.moreBtn;


        }
    }

}