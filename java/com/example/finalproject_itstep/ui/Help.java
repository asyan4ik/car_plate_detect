package com.example.finalproject_itstep.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.example.finalproject_itstep.R;

public class Help extends Activity {
    private static final String TAG         = "HelpActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        setTitle("О программе");
    }
}
