package com.example.atom;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.example.atom.Library.Book;
import com.example.atom.Library.BookViewModel;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;

import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

public class ReaderActivity extends AppCompatActivity {
    private static final String LOG_TAG = ReaderActivity.class.getSimpleName() + "LOGGING";

    private final Book mCurrentBook = new Book("" , "", 0);
    private BookViewModel mBookViewModel;

    private DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        mBookViewModel = ViewModelProviders.of(this).get(BookViewModel.class);
        Intent intent = getIntent();
        Uri uri = Uri.parse(intent.getStringExtra(BookActivity.EXTRA_BOOK_URI));
        String name = intent.getStringExtra(BookActivity.EXTRA_BOOK_NAME);
        final int pageNumber = intent.getIntExtra(BookActivity.EXTRA_BOOK_PAGE_NUMBER, 0);

        mCurrentBook.setUri(uri.toString());
        mCurrentBook.setName(name);
        mCurrentBook.setPageNumber(pageNumber);
        mCurrentBook.setLastOpened(new Date());
        mBookViewModel.updateBook(mCurrentBook);

        Log.d(LOG_TAG, "Page " + pageNumber + " of " + name + " opened on " + new Date().toString());

        final PDFView pdfView = findViewById(R.id.pdfView);
        pdfView.fromUri(uri)
                .enableSwipe(true)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(false)
                .password(null)
                .onPageChange(new OnPageChangeListener() {
                    @Override
                    public void onPageChanged(int page, int pageCount) {
                        mCurrentBook.setPageNumber(page);
                    }
                })
                .scrollHandle(new DefaultScrollHandle(this, true))
                .defaultPage(pageNumber)
                .load();
    }

    public void navigateBack(View view) {
        mBookViewModel.updateBook(mCurrentBook);
        finish();
    }


}
