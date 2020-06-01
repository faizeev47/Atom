package com.example.atom;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.atom.Headset.HeadsetState;
import com.example.atom.Headset.IntentFilterFactory;
import com.example.atom.Headset.ServiceAction;
import com.example.atom.Headset.SocketService;
import com.example.atom.Headset.StandardHeadsetReceiver;
import com.example.atom.Library.Book;
import com.example.atom.Library.BookViewModel;
import com.example.atom.Library.FetchBookInfo;
import com.example.atom.Utilities.Utils;
import com.example.atom.Utilities.NotificationUtils;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;

import static com.example.atom.Utilities.FirebaseUtils.BOOK_NAME;
import static com.example.atom.Utilities.FirebaseUtils.BOOK_REPERTOIRE;
import static com.example.atom.Utilities.FirebaseUtils.HIGHEST_ACTIVITY;
import static com.example.atom.Utilities.FirebaseUtils.LAST_READ;
import static com.example.atom.Utilities.FirebaseUtils.MEDIAN_ACTIVITY;
import static com.example.atom.Utilities.FirebaseUtils.SESSION_TIME;
import static com.example.atom.Utilities.NotificationUtils.notifyNotReading;
import static com.example.atom.Utilities.Utils.connectionActive;
import static com.example.atom.Utilities.Utils.getClockDifference;

public class ReaderActivity extends AppCompatActivity {
    private static final String LOG_TAG = ReaderActivity.class.getSimpleName() + "LOGGING";

    private static final int ANSWER_REQUEST = 1;

    private static final String EXTRA_BUNDLE_ = BuildConfig.APPLICATION_ID + "EXTRA_BUNDLE_";
    private static final String EXTRA_QUESTION = BuildConfig.APPLICATION_ID + "EXTRA_QUESTION";
    private static final String EXTRA_OPTIONS = BuildConfig.APPLICATION_ID + "EXTRA_OPTIONS";
    private static final String EXTRA_ANSWER = BuildConfig.APPLICATION_ID + "EXTRA_ANSWER";
    private static final String EXTRA_IS_FINAL = BuildConfig.APPLICATION_ID + "EXTRA_IS_FINAL";

    private FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mDatabaseReference = mFirebaseDatabase.getReference();
    private DatabaseReference mUserReference;
    private DatabaseReference mGraphReference;
    private DatabaseReference mSessionReference;

    private View mMainView;

    private String mUserId;
    private HeadsetState mHeadsetState;
    private int mAccumulatedAttention;
    private int mMaximumAttention;
    private int mTotalSamples;
    private Date mSessionStartClock;
    private Date mSessionPausedClock = null;
    private long mReadingTimeSeconds;
    private long mPauseTimeSeconds;

    private GregorianCalendar mCalender;
    private int mReadingHour;
    private int[] mReadingDistribution = new int[24];

    private int mBookReadingHistory;

    private Bundle mQuestionAnswers;
    private LinkedList<Integer> mQuestionsToAsk;

    private final Book mCurrentBook = new Book("" , "", 0);
    private BookViewModel mBookViewModel;

    private NotificationManager mNotifyManager;
    private LocalBroadcastManager mLocalBroadcastManager;

    private SocketReceiver mSocketReceiver = new SocketReceiver();
    private StandardHeadsetReceiver mStandardReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        logPausedTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        logPausedTime();
    }

    private void logPausedTime() {
        if (mSessionPausedClock != null) {
            long pauseStartTime = getClockDifference(mSessionStartClock, mSessionPausedClock, true);
            long pauseEndTime = getClockDifference(mSessionStartClock, new Date(), true);
            mPauseTimeSeconds += pauseEndTime - pauseStartTime;

            mGraphReference
                    .child(Long.toString(pauseStartTime))
                    .setValue(-1);
            mGraphReference
                    .child(Long.toString(pauseEndTime))
                    .setValue(-1);
            mSessionPausedClock = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);


        // Require user to be logged in
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Otherwise redirect to login
            Utils.redirectToLogin(ReaderActivity.this, this);
        } else {
            mUserId = currentUser.getUid();
            Log.d(LOG_TAG, mUserId);
        }

        // Initialize UI components
        mMainView = findViewById(R.id.reader_view);

        mHeadsetState = HeadsetState.DISCONNECTED;

        mMaximumAttention = -1;
        mAccumulatedAttention = 0;
        mTotalSamples = 0;
        mReadingTimeSeconds = 0;
        mPauseTimeSeconds = 0;

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

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mStandardReceiver = new StandardHeadsetReceiver(LOG_TAG, mMainView);

        mLocalBroadcastManager.registerReceiver(mStandardReceiver,
                IntentFilterFactory.createStandardFilter());
        mLocalBroadcastManager.registerReceiver(mSocketReceiver,
                IntentFilterFactory.createFilter(
                        ServiceAction.STATUS_UPDATE,
                        ServiceAction.ATTENTION_UPDATE));
        mLocalBroadcastManager.sendBroadcast(new Intent(SocketService.ACTION_GET_CONNECTION_STATUS));

        mNotifyManager = NotificationUtils.createNotificationChannel(this);
        if (!SocketService.SERVICE_RUNNING) {
            NotificationUtils.notifyNotConnected(this, mNotifyManager);
        } else if (!connectionActive(this)) {
            Snackbar.make(mMainView,
                    "Please connect to the internet to record this session.",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        sessionParameters();
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
        mLocalBroadcastManager.unregisterReceiver(mStandardReceiver);
        mBookViewModel.updateBook(mCurrentBook);
        mNotifyManager.cancelAll();
        sessionParameters();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mSessionPausedClock != null) {
            Date currentClock = new Date();
            if (currentClock.before(mSessionPausedClock)) {
                mSessionPausedClock = currentClock;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ANSWER_REQUEST) {
            if (resultCode == RESULT_OK) {
                mQuestionsToAsk.removeFirst();
                launchQuestionSequence();
            }
        }
    }

    public void navigateBack(View view) {
        finish();
    }


    private void prepareQuestions(int pageNumber) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getResources().getStringArray(R.array.questions)[pageNumber - 1]);
        String[] questionsStr = buffer.toString().split("#");

        mQuestionAnswers = new Bundle();
        mQuestionsToAsk = new LinkedList<>();

        int questionNo = 1;

        for(String questionPackage: questionsStr) {
            String[] parts = questionPackage.split(">");
            if (parts.length == 4) {
                Bundle newQuestion = new Bundle();
                newQuestion.putString(EXTRA_QUESTION, parts[1]);
                newQuestion.putInt(EXTRA_ANSWER, Integer.parseInt(parts[3].trim()) - 1 );
                newQuestion.putStringArray(EXTRA_OPTIONS, parts[2].split(":"));
                mQuestionAnswers.putBundle(EXTRA_BUNDLE_ + questionNo, newQuestion);
                mQuestionsToAsk.add(questionNo++);
            }
        }
    }

    private void launchQuestionSequence() {
        int remainingQuestions = mQuestionsToAsk.size();
        if (remainingQuestions > 0) {
            Intent intent = new Intent(this, QuestionActivity.class);
            intent.putExtras(mQuestionAnswers.getBundle(EXTRA_BUNDLE_ + mQuestionsToAsk.getFirst()));
            if (remainingQuestions == 1) {
                intent.putExtra(EXTRA_IS_FINAL, 1);
            }
            startActivityForResult(intent, ANSWER_REQUEST);
        }
    }

    private void initializeSchemaStructure () {
        if (mHeadsetState == HeadsetState.CONNECTED) {
            if (connectionActive(this)) {
                mCalender = new GregorianCalendar(TimeZone.getDefault());
                mSessionStartClock = new Date();
                mCalender.setTime(mSessionStartClock);
                mReadingHour = mCalender.get(Calendar.HOUR_OF_DAY);

                mBookReadingHistory = 0;
                for (int i = 0, l = mReadingDistribution.length; i < l; i++) {
                    mReadingDistribution[i] = 0;
                }

                mUserReference = mDatabaseReference
                        .child("userReports")
                        .child(mUserId);
                mSessionReference = mUserReference
                        .child("sessions")
                        .child(Long.toString(mSessionStartClock.getTime()));
                mSessionReference.child("bookName").setValue(mCurrentBook.getName());

                mUserReference.child("readingHourDistribution")
                        .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.d(LOG_TAG, "onDataChange: started!");
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot hour : dataSnapshot.getChildren()) {
                                if (hour != null
                                        && hour.getKey() != null
                                        && hour.getValue() != null) {
                                    mReadingDistribution[Integer.parseInt(hour.getKey())] =
                                            Integer.parseInt(hour.getValue().toString());

                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });

                mUserReference.child("bookRepertoire")
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    int maximumReadBookSamples = mBookReadingHistory;
                                    String maximumReadBookName = mCurrentBook.getName();
                                    for (DataSnapshot book : dataSnapshot.getChildren()) {
                                        if (book != null && book.getKey() != null && book.getValue() != null) {
                                            String bookName = book.getKey();
                                            int bookSamples = Integer.parseInt(book.getValue().toString());
                                            if (bookSamples > maximumReadBookSamples) {
                                                maximumReadBookSamples = bookSamples;
                                                maximumReadBookName = bookName;
                                            }
                                            if (bookName.equals(mCurrentBook.getName())) {
                                                mBookReadingHistory = bookSamples;
                                            }
                                        }
                                    }
                                    mUserReference
                                            .child("mostReadBook").setValue(maximumReadBookName);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) { }
                        });

                mGraphReference = mSessionReference.child("attentionTime");
                mGraphReference.child("0").setValue(0);
            } else {
                Snackbar.make(mMainView,
                        "Please connect to the internet to record this session.",
                        Snackbar.LENGTH_LONG).show();
            }
        } else {
            NotificationUtils.notifyNotConnected(this, mNotifyManager);
        }

    }

    private void appendSessionValue(int attentionValue) {
        if (connectionActive(this) && mHeadsetState == HeadsetState.CONNECTED) {
            mGraphReference
                    .child(Long.toString(getClockDifference(mSessionStartClock, new Date(), true)))
                    .setValue(attentionValue);
            mTotalSamples++;
            mAccumulatedAttention += attentionValue;
            if (attentionValue > mMaximumAttention) {
                mMaximumAttention = attentionValue;
            }

            mCalender.setTime(new Date());
            mReadingHour =  mCalender.get(Calendar.HOUR_OF_DAY);
            mReadingDistribution[mReadingHour]++;
        }
    }

    public void sessionParameters() {
        float sessionPercentage = 0.0f;
        if (mTotalSamples > 0) {
            sessionPercentage = ((float)mAccumulatedAttention / (float)(mTotalSamples * mMaximumAttention)) * 100.0f;
            if (connectionActive(this)) {
                mGraphReference.getParent()
                        .child("sessionScore")
                        .setValue(Float.toString(sessionPercentage));
                int highestHour = 0;
                int highestRecorded = 0;

                int totalCumulativeFrequency = 0;

                for (int i = 0, l = mReadingDistribution.length ; i < l; i++) {
                    if (mReadingDistribution[i] >= highestRecorded) {
                        highestRecorded = mReadingDistribution[i];
                        highestHour = i;
                    }

                    totalCumulativeFrequency += mReadingDistribution[i];
                    mUserReference
                            .child("readingHourDistribution")
                            .child(Integer.toString(i))
                            .setValue(mReadingDistribution[i]);
                }

                double medianSample = (double)totalCumulativeFrequency / 2;
                double medianActivityHour = -1;

                int tempCumulative = 0;
                for (int i = 0, l = mReadingDistribution.length; i < l; i++) {
                    tempCumulative += mReadingDistribution[i];
                    if (medianSample <= tempCumulative) {
                        medianActivityHour = i;
                        break;
                    }
                }

                mUserReference
                        .child(BOOK_REPERTOIRE)
                        .child(mCurrentBook.getName())
                        .setValue(mBookReadingHistory + mTotalSamples);

                mUserReference
                        .child(HIGHEST_ACTIVITY)
                        .setValue(highestHour);
                mUserReference
                        .child(MEDIAN_ACTIVITY)
                        .setValue(medianActivityHour);


                long sessionTime = getClockDifference(mSessionStartClock, new Date(), true) -
                        mPauseTimeSeconds;

                if (sessionTime >= 10) {

                    mUserReference
                            .child(LAST_READ)
                            .child(BOOK_NAME)
                            .setValue(mCurrentBook.getName());
                    mUserReference
                            .child(LAST_READ)
                            .child(SESSION_TIME)
                            .setValue(sessionTime);

                }


                mSessionReference
                        .child(SESSION_TIME)
                        .setValue(sessionTime);

            } else {
                Snackbar.make(mMainView,
                        "Sorry! Session not recorded. No Internet.",
                        Snackbar.LENGTH_LONG).show();
            }
        } else {
            Snackbar.make(mMainView,
                    "No samples recorded from teh headset!",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    public class SocketReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction != null) {
                switch (intentAction) {
                    case SocketService.ACTION_RETURN_CONNECTION_STATUS:
                        String status = intent.getStringExtra(SocketService.EXTRA_STATUS);
                        if (status.equals(SocketService.CONNECTED)) {
                            mHeadsetState = HeadsetState.CONNECTED;
                            initializeSchemaStructure();
                        } else {
                            mHeadsetState = HeadsetState.DISCONNECTED;
                            NotificationUtils.notifyNotConnected(ReaderActivity.this, mNotifyManager);
                        }
                        Log.d(LOG_TAG, "" + mHeadsetState);
                        break;
                    case SocketService.ACTION_RETURN_ATTENTION_VALUE:
                        int attention = intent.getIntExtra(SocketService.EXTRA_ATTENTION_VALUE, -1);
//                        Log.d(LOG_TAG,
//                                "attention: " + attention);
                        if (attention == 0) {
                            notifyNotReading(ReaderActivity.this, mNotifyManager);
                            prepareQuestions(mCurrentBook.getPageNumber() + 1);
                            launchQuestionSequence();
                        } else {
                            mNotifyManager.cancel(NotificationUtils.NOT_READING_NOTIFICATION);
                        }
                        appendSessionValue(attention);
                        break;

                }

            }
        }
    }
}
