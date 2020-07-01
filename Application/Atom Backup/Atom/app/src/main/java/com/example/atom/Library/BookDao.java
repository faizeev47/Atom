package com.example.atom.Library;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BookDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Book book);

    @Query("DELETE FROM library")
    void deleteAll();

    @Delete
    void deleteBook(Book book);

    @Query("SELECT * FROM library LIMIT 1")
    Book[] getAnyBook();

    @Query("SELECT * FROM library ORDER BY lastOpened DESC")
    LiveData<List<Book>> getAllBooks();

    @Update
    void updateBook(Book book);
}
