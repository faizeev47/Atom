package com.example.atom.Library;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class BookViewModel extends AndroidViewModel {
    private BookRepository mRepository;
    private LiveData<List<Book>> mAllBooks;

    public BookViewModel(@NonNull Application application) {
        super(application);
        mRepository = new BookRepository(application);
        mAllBooks = mRepository.getAllBooks();
    }

    public LiveData<List<Book>> getAllBooks() {
        return mAllBooks;
    }

    public void insert(Book book) { mRepository.insert(book); }

    public void deleteAll() { mRepository.deleteAll(); }

    public void deleteBook(Book book) { mRepository.deleteBook(book); }

    public void updateBook(Book book) { mRepository.updateBook(book); }
}
