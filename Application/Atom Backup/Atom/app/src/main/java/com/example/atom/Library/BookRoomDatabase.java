package com.example.atom.Library;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Book.class}, version = 4, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class BookRoomDatabase extends RoomDatabase {

    public abstract BookDao bookDao();

    private static BookRoomDatabase INSTANCE;

    public static BookRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BookRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BookRoomDatabase.class,
                            "book_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback =
            new RoomDatabase.Callback() {

                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    super.onOpen(db);
                    new PopulateDbAsync(INSTANCE).execute();
                }
            };

    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {
        private final BookDao mDao;

        PopulateDbAsync(BookRoomDatabase db) { mDao = db.bookDao(); }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }
}
