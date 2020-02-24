package com.example.atom;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.atom.Library.Book;
import com.example.atom.Library.BookListAdapter;
import com.example.atom.Library.BookViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import static com.example.atom.Helpers.formatFileName;

public class BookActivity extends AppCompatActivity {
    public static final String EXTRA_BOOK_URI = BookActivity.class.getSimpleName() + "EXTRA_BOOK_URI";
    public static final String EXTRA_BOOK_NAME = BookActivity.class.getSimpleName() + "EXTRA_BOOK_NAME";
    public static final String EXTRA_BOOK_PAGE_NUMBER = BookActivity.class.getSimpleName() + "EXTRA_BOOK_PAGE_NUMBER";

    private static final String LOG_TAG = BookActivity.class.getSimpleName() + "LOGGING";
    private static final int PICK_BOOK_FILE = 0;

    private BookViewModel mBookViewModel;
    private View mainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        mainView = findViewById(R.id.book_view);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/pdf");
                startActivityForResult(intent, PICK_BOOK_FILE);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.books_recyclerview);
        final BookListAdapter adapter = new BookListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mBookViewModel = ViewModelProviders.of(this).get(BookViewModel.class);
        mBookViewModel.getAllBooks().observe(this, new Observer<List<Book>>() {
            @Override
            public void onChanged(List<Book> books) {
                adapter.setBooks(books);
            }
        });

        ItemTouchHelper touchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        final Book bookToDel = adapter.getBookAtPosition(position);
                        mBookViewModel.deleteBook(bookToDel);
                        Snackbar undoSnackbar = Snackbar.make(mainView, "Cleared " + bookToDel.getName() + " from your library!", Snackbar.LENGTH_LONG);
                        undoSnackbar.setAction("Undo", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mBookViewModel.insert(bookToDel);
                            }
                        });
                        undoSnackbar.show();
                    }
                }
        );

        touchHelper.attachToRecyclerView(recyclerView);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_BOOK_FILE && resultCode == RESULT_OK) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();

                Cursor returnCursor =
                        getContentResolver().query(uri, null, null, null, null);

                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();

                String name = formatFileName(returnCursor.getString(nameIndex));

                Log.d(LOG_TAG,  "Uri received: " + uri.getPath());
                if (isValidDocument(uri)) {
                    RequestQueue queue = Volley.newRequestQueue(this);
                    String url = "https://sandbox.iexapis.com/stable/stock/twtr/quote?token=Tsk_da37fe8dceae4116957b77665b32daf1";
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(LOG_TAG, response.toString());
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(LOG_TAG, error.getMessage());
                        }
                    });
                    queue.add(jsonObjectRequest);
                    mBookViewModel.insert(new Book(uri.toString(), name, 0));
                    Intent intent = new Intent(this, ReaderActivity.class);
                    intent.putExtra(EXTRA_BOOK_URI, uri.toString());
                    intent.putExtra(EXTRA_BOOK_NAME, name);
                    intent.putExtra(EXTRA_BOOK_PAGE_NUMBER, 0);
                    startActivity(intent);
                }
            }
        }
    }

    public void navigateBack(View view) {
        finish();
    }

    public boolean isValidDocument(Uri docUri) {
        if (!DocumentsContract.isDocumentUri(this, docUri)) {
            Toast.makeText(this, "File is not a document file!", Toast.LENGTH_SHORT).show();
            return false;
        }
        Cursor cursor = getContentResolver().query(
                docUri,
                new String[] { DocumentsContract.Document.COLUMN_FLAGS },
                null, null, null
        );
        int flags = 0;
        if (cursor.moveToFirst()) {
            flags = cursor.getInt(0);
        }
        cursor.close();

        if ((flags & DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT) != 0) {
            Toast.makeText(this, "File is a virtual file!", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    public void clearAll(View view) {
        mBookViewModel.deleteAll();
        final List<Book> books = mBookViewModel.getAllBooks().getValue();
        Snackbar undoSnackbar = Snackbar.make(mainView, "Cleared your library!", Snackbar.LENGTH_LONG);
        undoSnackbar.setAction("Undo", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Book deletedBook : books) {
                    mBookViewModel.insert(deletedBook);
                }
            }
        });
        undoSnackbar.show();
    }
}
