package com.example.bookapp;

import android.app.Application;

import android.text.format.DateFormat;
import java.util.Calendar;
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

}
