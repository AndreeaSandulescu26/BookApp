package com.example.bookapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.bookapp.adapters.AdapterPdfUser;
import com.example.bookapp.databinding.FragmentBooksUserBinding;
import com.example.bookapp.models.ModelPdf;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BooksUserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BooksUserFragment extends Fragment {

    private String categoryId;
    private String category;
    private String uid;
    private ArrayList<ModelPdf> pdfArrayList;
    private AdapterPdfUser adapterPdfUser;

    //view binding
    private FragmentBooksUserBinding binding;

    public BooksUserFragment() {
        // Required empty public constructor
    }


    public static BooksUserFragment newInstance(String categoryId, String category, String uid) {
        BooksUserFragment fragment = new BooksUserFragment();
        Bundle args = new Bundle();
        args.putString("categoryId", categoryId);
        args.putString("category", category);
        args.putString("uid", uid);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString("categoryId");
            category = getArguments().getString("category");
            uid = getArguments().getString("uid");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentBooksUserBinding.inflate(LayoutInflater.from(getContext()), container, false);
        if(category.equals("All")){
            //incarcam cartile
            loadAllBooks();
        }
        else if (category.equals("Most Viewed")) {
            //incarcam cele mai vizualizate
            loadMostViewedDownloadedBooks("viewsCount");
        }
        else if (category.equals("Most Downloaded")){
            //incarcam cele mai descarcate
            loadMostViewedDownloadedBooks("downloadsCount");
        }
        else{
            //incarcam categ selectate cu carti
            loadCategorizedBooks();
        }

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    adapterPdfUser.getFilter().filter(s);
                }
                catch (Exception e){

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return binding.getRoot();
    }

    private void loadCategorizedBooks() {
        pdfArrayList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.orderByChild("categoryId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        pdfArrayList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()){
                            ModelPdf model = ds.getValue(ModelPdf.class);
                            pdfArrayList.add(model);
                        }
                        adapterPdfUser = new AdapterPdfUser(getContext(), pdfArrayList);
                        binding.bookRv.setAdapter(adapterPdfUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadMostViewedDownloadedBooks(String orderBy) {
        pdfArrayList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.orderByChild(orderBy).limitToLast(10) //incarcam 10 cele mai viz sau desc carti
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pdfArrayList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPdf model = ds.getValue(ModelPdf.class);
                    pdfArrayList.add(model);
                }
                adapterPdfUser = new AdapterPdfUser(getContext(), pdfArrayList);
                binding.bookRv.setAdapter(adapterPdfUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadAllBooks() {
        pdfArrayList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pdfArrayList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPdf model = ds.getValue(ModelPdf.class);
                    pdfArrayList.add(model);
                }
                adapterPdfUser = new AdapterPdfUser(getContext(), pdfArrayList);
                binding.bookRv.setAdapter(adapterPdfUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}