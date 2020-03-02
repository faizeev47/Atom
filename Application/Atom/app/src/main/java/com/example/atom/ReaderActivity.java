package com.example.atom;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.atom.Library.Book;
import com.example.atom.Library.BookViewModel;
import com.example.atom.Library.FetchBookInfo;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;

import static com.example.atom.Utils.connectionActive;
import static com.example.atom.Utils.getIdFromEmail;

public class ReaderActivity extends AppCompatActivity {
    private static final String PRIMARY_CHANNEL_ID = "primary_notification_channel";
    private static final int NOTIFICATION_ID = 1;

    private static final String LOG_TAG = ReaderActivity.class.getSimpleName() + "LOGGING";

    private final Book mCurrentBook = new Book("" , "", 0);
    private BookViewModel mBookViewModel;

    private DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    SocketReceiver mSocketReceiver = new SocketReceiver();

    private boolean mConnected;

    private LocalBroadcastManager mLocalBroadcastManager;

    private NotificationManager mNotifyManager;

    private int readingSamples;
    private int totalSamples;
    private Date date;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = database.getReference();

    private String mUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        readingSamples = 0;
        totalSamples = 0;
        date = new Date();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mUserEmail = currentUser.getEmail();
        }
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (googleAccount != null) {
            mUserEmail = googleAccount.getEmail();
        }

        createNotificationChannel();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SocketService.ACTION_CONNECTED);
        intentFilter.addAction(SocketService.ACTION_DISCONNECTED);
        intentFilter.addAction(SocketService.ACTION_USER_READING);
        intentFilter.addAction(SocketService.ACTION_USER_NOT_READING);
        mLocalBroadcastManager.registerReceiver(mSocketReceiver, intentFilter);
        mLocalBroadcastManager.sendBroadcast(new Intent(SocketService.ACTION_GET_CONNECTION_STATUS));

        mBookViewModel = ViewModelProviders.of(this).get(BookViewModel.class);
        Intent intent = getIntent();
        Uri bookUri = Uri.parse(intent.getStringExtra(BookActivity.EXTRA_BOOK_URI));
        String bookName = intent.getStringExtra(BookActivity.EXTRA_BOOK_NAME);
        final int bookPageNumber = intent.getIntExtra(BookActivity.EXTRA_BOOK_PAGE_NUMBER, 0);

        mCurrentBook.setUri(bookUri.toString());
        mCurrentBook.setName(bookName);
        mCurrentBook.setPageNumber(bookPageNumber);
        mCurrentBook.setLastOpened(new Date());
        mBookViewModel.updateBook(mCurrentBook);

        Log.d(LOG_TAG, "Page " + bookPageNumber + " of " + bookName + " opened on " + new Date().toString());

        final PDFView pdfView = findViewById(R.id.pdfView);
        pdfView.fromUri(bookUri)
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
                .defaultPage(bookPageNumber)
                .load();

        if (intent.hasExtra(BookActivity.EXTRA_BOOK_OPEN_NAME_SELECTION)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            new FetchBookInfo(builder, mBookViewModel, mCurrentBook).execute(bookName);
        }

        if (!intent.hasExtra(ConnectionActivity.EXTRA_CONNECTION_STATUS)) {
            Notification notification2 = getNotificationBuilder(
                    "Headset not connected!",
                    "Please connect the headset to enable monitoring!").build();

            mNotifyManager.notify(3, notification2);
        }
    }

    public void navigateBack(View view) {
        mBookViewModel.updateBook(mCurrentBook);
        mNotifyManager.cancel(3);
        mNotifyManager.cancel(2);
        sessionParameters();
        finish();
    }

    @Override
    protected void onDestroy() {
        sessionParameters();
        super.onDestroy();
    }

    public void sessionParameters() {
        float sessionPercentage = 0.0f;
        if (totalSamples > 0) {
            sessionPercentage = ((float)readingSamples / (float)totalSamples) * 100f;
            Log.d(LOG_TAG, "RS: " + readingSamples + "\nTS: " + totalSamples + "\nR%" + sessionPercentage);
            String userId = getIdFromEmail(mUserEmail);
            Log.d(LOG_TAG, "userId: " + userId);

            if (connectionActive(this)) {
                databaseReference.child("userReports").child(userId).child(Long.toString(date.getTime())).setValue(sessionPercentage);
            } else {
                Toast.makeText(this, "Sorry! Session not recorded. No Internet.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "You did not read, so session was not recorded.", Toast.LENGTH_SHORT).show();
        }
    }

    public class SocketReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction != null) {
                switch (intentAction) {
                    case SocketService.ACTION_USER_READING:
                        mNotifyManager.cancel(2);
                        readingSamples++;
                        totalSamples++;
                        break;
                    case SocketService.ACTION_USER_NOT_READING:
                        totalSamples++;
                        NotificationCompat.Builder notifyBuilder =
                                getNotificationBuilder(
                                        "You're not reading!",
                                        "You're taken your eyes of the book or you're not reading it anymore!");
                        Notification notification = notifyBuilder.build();

                        mNotifyManager.notify(2, notification);
                        break;
                    case SocketService.ACTION_CONNECTED:
                        mConnected = true;
                        break;
                    case SocketService.ACTION_DISCONNECTED:
                        mConnected = false;
                        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
                        break;
                    default:
                        break;
                }

            }
        }
    }


    private void createNotificationChannel() {
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(PRIMARY_CHANNEL_ID,
                    "Socket notification",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription("Notification from Socket Service");
            mNotifyManager.createNotificationChannel(notificationChannel);
        }
    }

    private NotificationCompat.Builder getNotificationBuilder(String title, String message) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(this,
                NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.splash_image)
                .setContentIntent(notificationPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setPriority(NotificationCompat.DEFAULT_ALL);
    }
}
