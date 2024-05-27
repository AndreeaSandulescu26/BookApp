package com.example.bookapp.filters;

import android.widget.Filter;

import com.example.bookapp.adapters.AdapterCategory;
import com.example.bookapp.adapters.AdapterPdfAdmin;
import com.example.bookapp.models.ModelCategory;
import com.example.bookapp.models.ModelPdf;

import java.util.ArrayList;

public class FilterPdfAdmin extends Filter {

    // arraylist in care vrem sa cautam
    ArrayList<ModelPdf> filterList;
    // adapter in care filtrul trb sa fie implementat
    AdapterPdfAdmin adapterPdfAdmin;

    //constructor


    public FilterPdfAdmin(ArrayList<ModelPdf> filterList, AdapterPdfAdmin adapterPdfAdmin) {
        this.filterList = filterList;
        this.adapterPdfAdmin = adapterPdfAdmin;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        // valoarea nu trb sa fie null sau empty
        if (constraint != null && constraint.length() > 0){

            //schimbam in  upper case sau lower case sa evitam case sensitivity
            constraint = constraint.toString().toUpperCase();
            ArrayList<ModelPdf> filteredModels = new ArrayList<>();

            for (int i = 0; i < filterList.size(); i++){
                //validam
                if (filterList.get(i).getTitle().toUpperCase().contains(constraint)){
                    // adaugam la lista filtrata
                    filteredModels.add(filterList.get(i));
                }
            }
            results.count = filteredModels.size();
            results.values = filteredModels;
        }
        else{
            results.count = filterList.size();
            results.values = filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        //aplicam schimbarile de filtru
        adapterPdfAdmin.pdfArrayList = (ArrayList<ModelPdf>)results.values;

        //notificam schimbarile
        adapterPdfAdmin.notifyDataSetChanged();
    }
}
