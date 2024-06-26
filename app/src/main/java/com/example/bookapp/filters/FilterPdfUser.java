package com.example.bookapp.filters;

import android.widget.Filter;

import com.example.bookapp.adapters.AdapterPdfUser;
import com.example.bookapp.models.ModelPdf;

import java.util.ArrayList;

public class FilterPdfUser extends Filter {

    //arraylist in care vrem sa cautam
    ArrayList<ModelPdf> filterList;
    //adapter in care este implementat filter
    AdapterPdfUser adapterPdfUser;

    public FilterPdfUser(ArrayList<ModelPdf> filterList, AdapterPdfUser adapterPdfUser) {
        this.filterList = filterList;
        this.adapterPdfUser = adapterPdfUser;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        //val cautata dif de 0
        if(constraint!=null || constraint.length() > 0){
            constraint = constraint.toString().toUpperCase();
            ArrayList<ModelPdf> filteredModels = new ArrayList<>();

            for (int i = 0; i < filterList.size(); i++){
                //validam
                if (filterList.get(i).getTitle().toUpperCase().contains(constraint)){
                    filteredModels.add(filterList.get(i));
                }
            }
            results.count = filteredModels.size();
            results.values = filteredModels;
        }
        else{
            //empty sau null
            results.count = filterList.size();
            results.values = filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        adapterPdfUser.pdfArrayList = (ArrayList<ModelPdf>)results.values;

        adapterPdfUser.notifyDataSetChanged();
    }
}
