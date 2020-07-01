package com.example.atom.Adapters;

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
import com.example.atom.Library.Book;
import com.example.atom.R;
import com.example.atom.ReaderActivity;

import java.util.List;

public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.BookViewHolder> {

    private List<Book> mBooks;

    public BookListAdapter() {}

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context parentContext = parent.getContext();
        View viewItem = LayoutInflater.from(parentContext)
                .inflate(R.layout.recyclerview_book, parent, false);
        BookViewHolder viewHolder = new BookViewHolder(viewItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        final Book current = mBooks.get(position);
        holder.nameView.setText(current.getName());
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

    class BookViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        private final TextView nameView;

        public BookViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id._book_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Context context = v.getContext();
            Book itemBook = mBooks.get(getLayoutPosition());
            Intent intent = new Intent(context, ReaderActivity.class);
            intent.putExtra(BookActivity.EXTRA_BOOK_URI, itemBook.getUri());
            intent.putExtra(BookActivity.EXTRA_BOOK_NAME, itemBook.getName());
            intent.putExtra(BookActivity.EXTRA_BOOK_PAGE_NUMBER, itemBook.getPageNumber());
            context.startActivity(intent);
            Log.d("Clicking", "clicking");
        }
    }
}
