package com.example.atom.Library;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.atom.BookActivity;
import com.example.atom.ConnectionActivity;
import com.example.atom.R;
import com.example.atom.ReaderActivity;

import java.util.List;

public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.BookViewHolder> {
    private static final String LOG_TAG = BookActivity.class.getSimpleName() + "LOGGING";

    private Context mContext;
    private boolean mConnected;
    private final LayoutInflater mInflater;
    private List<Book> mBooks;

    public BookListAdapter(Context context, boolean headsetConnected) {
        mConnected = headsetConnected;
        mContext = context;
        mInflater = LayoutInflater.from(context); }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.recyclerview_book, parent, false);
        return new BookViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        final Book current = mBooks.get(position);
        holder.nameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(mContext, ReaderActivity.class);
                intent.putExtra(BookActivity.EXTRA_BOOK_URI, current.getUri());
                intent.putExtra(BookActivity.EXTRA_BOOK_NAME, current.getName());
                intent.putExtra(BookActivity.EXTRA_BOOK_PAGE_NUMBER, current.getPageNumber());

                if (mConnected) {
                    intent.putExtra(ConnectionActivity.EXTRA_CONNECTION_STATUS, "Connected");
                }
                Log.d(LOG_TAG, current.getName() + ": @ " + current.getPageNumber());
                mContext.startActivity(intent);
            }
        });
        holder.nameView.setText(mBooks != null ? current.getName() : "No book!");
    }

    public void setBooks(List<Book> books) {
        mBooks = books;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mBooks != null ? mBooks.size() : 0;
    }

    public Book getBookAtPosition(int position) {
        return mBooks.get(position);
    }

    class BookViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;

        public BookViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id._book_name);
        }
    }
}
