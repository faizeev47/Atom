package com.example.atom.Library;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;

public class BookRepository {
    private BookDao mBookDao;
    private LiveData<List<Book>> mAllBooks;

    BookRepository(Application application) {
        BookRoomDatabase db = BookRoomDatabase.getDatabase(application);
        mBookDao = db.bookDao();
        mAllBooks = mBookDao.getAllBooks();
    }

    LiveData<List<Book>> getAllBooks() {
        return mAllBooks;
    }

    public void insert (Book book) {
        new insertAsyncTask(mBookDao).execute(book);
    }

    public void deleteAll () { new deleteAllBooksAsyncTask(mBookDao).execute(); }

    public void deleteBook(Book book) { new deleteOneBookAsyncTask(mBookDao).execute(book); }

    public void updateBook(Book book) { new updateBookAsyncTask(mBookDao).execute(book); }

    private static class insertAsyncTask extends AsyncTask<Book, Void, Void> {
        private BookDao mAsyncBookDao;

        insertAsyncTask(BookDao dao) { this.mAsyncBookDao = dao;}

        @Override
        protected Void doInBackground(Book... books) {
            mAsyncBookDao.insert(books[0]);
            return null;
        }
    }

    private static class deleteAllBooksAsyncTask extends AsyncTask<Void, Void, Void> {
        private BookDao mAsyncBookDao;

        deleteAllBooksAsyncTask(BookDao dao) { mAsyncBookDao = dao; }


        @Override
        protected Void doInBackground(Void... voids) {
            mAsyncBookDao.deleteAll();
            return null;
        }
    }

    private static class deleteOneBookAsyncTask extends AsyncTask<Book, Void, Void> {
        private BookDao mAsyncBookDao;

        deleteOneBookAsyncTask(BookDao dao) { mAsyncBookDao = dao; }


        @Override
        protected Void doInBackground(Book... books) {
            mAsyncBookDao.deleteBook(books[0]);
            return null;
        }
    }

    private static class updateBookAsyncTask extends AsyncTask<Book, Void, Void> {
        private BookDao mAsyncBookDao;

        updateBookAsyncTask(BookDao dao) { mAsyncBookDao = dao; }


        @Override
        protected Void doInBackground(Book... books) {
            mAsyncBookDao.updateBook(books[0]);
            return null;
        }
    }
}
