package com.example.atom.Library;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;

@Entity(tableName = "library")
public class Book {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "uri")
    private String mUri;

    @NonNull
    @ColumnInfo(name = "name")
    private String mName;

    @NonNull
    @ColumnInfo(name = "pageNumber")
    private int mPageNumber;

    @NonNull
    @ColumnInfo(name = "lastOpened")
    private Date mLastOpened;

    public Book(@NonNull String uri, @NonNull String name, @NonNull int pageNumber) {
        this.mUri = uri;
        this.mName = name;
        this.mPageNumber = pageNumber;
        this.mLastOpened = new Date();
    }

    public String getUri() { return this.mUri; }
    public String getName() { return this.mName; }
    public int getPageNumber() { return this.mPageNumber; }
    public Date getLastOpened() { return this.mLastOpened; }
    public void setUri(String uri) { this.mUri = uri; }
    public void setName(String name) { this.mName = name; }
    public void setPageNumber(int pageNumber) { this.mPageNumber = pageNumber; }
    public void setLastOpened(Date lastOpened) { this.mLastOpened = lastOpened; }
}
