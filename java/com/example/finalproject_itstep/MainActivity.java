package com.example.finalproject_itstep;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.finalproject_itstep.ui.CamActivity;
import com.example.finalproject_itstep.ui.Help;

public class MainActivity extends AppCompatActivity {


    //Permissions
    static final Integer WRITE_EXSTERNAL_STORAGE = 101;
    static final Integer READ_EXSTERNAL_STORAGE = 102;
    static final Integer CAMERA = 103;

    //Buttons
    Button btnCamStart;
    Button btnHelpStart;
    Button exitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        request();

        //Buttons
        btnHelpStart = findViewById(R.id.startHelp);
        btnHelpStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent helpIntent = new Intent(MainActivity.this , Help.class);
                startActivity(helpIntent);

            }
        });

        btnCamStart = findViewById(R.id.cam_start);
        btnCamStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (request()) {
                    Intent camActivityIntent = new Intent(MainActivity.this, CamActivity.class);
                    startActivity(camActivityIntent);
                }
            }
        });

        exitBtn = findViewById(R.id.exitBtn);
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Выход")
                        .setMessage("Действительно хотите выйти?")
                        .setPositiveButton("Да", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }

                        })
                        .setNegativeButton("Нет", null)
                        .show();
            }
        });
    }
//user Permissions
    private boolean requestPermission(String permission , Integer codeRequest){
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, codeRequest);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, codeRequest);
            }
            return false;
        } else {
            return true;
        }
    }
    public boolean request() {
        boolean answer = false;
        if (requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXSTERNAL_STORAGE)) {
            if (requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXSTERNAL_STORAGE)) {
                if (requestPermission(Manifest.permission.CAMERA, CAMERA)) {
                    answer = true;
                }
            }
        } else {
            answer = false;
        }
        return answer;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
