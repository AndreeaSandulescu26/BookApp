package com.example.bookapp;

import android.widget.Filter;

import java.util.ArrayList;

public class FilterCategory extends Filter {

    // arraylist in care vrem sa cautam
    ArrayList<ModelCategory> filterList;
    // adapter in care filtrul trb sa fie implementat
    AdapterCategory adapterCategory;

    //constructor


    public FilterCategory(ArrayList<ModelCategory> filterList, AdapterCategory adapterCategory) {
        this.filterList = filterList;
        this.adapterCategory = adapterCategory;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        // valoarea nu trb sa fie null sa empty
        if (constraint != null && constraint.length() > 0){

            //schimbam in  upper case sau lower case sa evitam case sensitivity
            constraint = constraint.toString().toUpperCase();
            ArrayList<ModelCategory> filteredModels = new ArrayList<>();

            for (int i = 0; i < filterList.size(); i++){
                //validam
                if (filterList.get(i).getCategory().toUpperCase().contains(constraint)){
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
        adapterCategory.categoryArrayList = (ArrayList<ModelCategory>)results.values;

        //notificam schimbarile
        adapterCategory.notifyDataSetChanged();
    }
}
