package com.example.loginpage;

import android.app.Application;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;

public class MyApplication extends Application {


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);


    }
}