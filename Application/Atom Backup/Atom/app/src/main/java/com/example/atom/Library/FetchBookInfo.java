package com.example.atom.Library;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.example.atom.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class FetchBookInfo extends AsyncTask<String, String, String> {
    private static final String LOG_TAG = FetchBookInfo.class.getSimpleName() + " Logging: ";

    private WeakReference<BookViewModel> mBookViewModel;
    private WeakReference<AlertDialog.Builder> mDialogBuilder;
    private WeakReference<Book> mBook;

    public FetchBookInfo(AlertDialog.Builder dialogBuilder, BookViewModel bookViewModel, Book book) {
        mDialogBuilder = new WeakReference<>(dialogBuilder);
        mBookViewModel = new WeakReference<>(bookViewModel);
        mBook = new WeakReference<>(book);
    }

    @Override
    protected String doInBackground(String... strings) {
        return BooksAPIUtils.getBookInfo(strings[0]);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        try {
            JSONObject jsonObject = new JSONObject(s);
            JSONArray itemsArray = jsonObject.getJSONArray("items");
            final ArrayList<CharSequence> titlesArray = new ArrayList<>();
            for (int i = 0, l = itemsArray.length(); i < l; i++) {
                String title = null, authors = null;
                JSONObject volumeInfo = itemsArray.getJSONObject(i).getJSONObject("volumeInfo");
                try {
                    title = volumeInfo.getString("title");
//                    authors = volumeInfo.getString("authors");
                    if (title != null && !title.isEmpty() && !titlesArray.contains(title)) {
                        titlesArray.add(title);
                    }
                } catch (Exception e) {
                }

            }
            if (mDialogBuilder.get() != null) {
                mDialogBuilder.get().setTitle(R.string.dialog_heading)
                        .setNegativeButton(R.string.action_keep_filename_as_bookname, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setItems(titlesArray.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(LOG_TAG, new Book(mBook.get().getUri(), titlesArray.get(which).toString(), mBook.get().getPageNumber()).toString());
                                mBookViewModel.get().updateBook(new Book(mBook.get().getUri(), titlesArray.get(which).toString(), mBook.get().getPageNumber()));
                                mBook.get().setName(titlesArray.get(which).toString());
                            }
                        });
                mDialogBuilder.get().show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
