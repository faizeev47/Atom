package com.example.fypversion2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;

public class bookshelf extends AppCompatActivity implements View.OnClickListener {
    Button btnOpen;
    TextView filePath;
    com.github.barteksc.pdfviewer.PDFView pdfview;
    PDFView.Configurator pdfconfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookshelf);
        btnOpen = findViewById(R.id.btnOpen);
        pdfview = findViewById(R.id.pdfView);
        btnOpen.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Choose Picture"), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            // action cancelled
        }
        if (resultCode == RESULT_OK) {
            Uri selectedFile = data.getData();
            pdfconfig = pdfview.fromUri(selectedFile);
            pdfconfig.defaultPage(0);
            pdfconfig.load();


        }
    }
}
