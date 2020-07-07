package com.example.atom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.atom.Adapters.SessionListAdapter;
import com.example.atom.Headset.IntentFilterFactory;
import com.example.atom.Headset.StandardHeadsetReceiver;
import com.example.atom.Utilities.Utils;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.example.atom.Utilities.FirebaseUtils.BOOK_NAME;
import static com.example.atom.Utilities.FirebaseUtils.HIGHEST_ACTIVITY;
import static com.example.atom.Utilities.FirebaseUtils.LAST_READ;
import static com.example.atom.Utilities.FirebaseUtils.MEDIAN_ACTIVITY;
import static com.example.atom.Utilities.FirebaseUtils.MOST_READ_BOOK;
import static com.example.atom.Utilities.FirebaseUtils.READING_HOUR_DISTRIBUTION;
import static com.example.atom.Utilities.FirebaseUtils.SESSIONS;
import static com.example.atom.Utilities.FirebaseUtils.SESSION_SCORE;
import static com.example.atom.Utilities.FirebaseUtils.SESSION_TIME;
import static com.example.atom.Utilities.FirebaseUtils.USER_REPORTS;
import static com.example.atom.Utilities.Utils.connectionActive;
import static com.example.atom.Utilities.Utils.fadeAppearViewObject;
import static com.example.atom.Utilities.Utils.getFormattedTime;
import static com.example.atom.Utilities.Utils.getTimeOfDayDrawableIndex;
import static com.example.atom.Utilities.Utils.getTimeWithPeriod;

public class ProgressActivity extends AppCompatActivity {

    private static final String LOG_TAG = ProgressActivity.class.getSimpleName() + "Logging ";

    private FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mDatabaseReference = mFirebaseDatabase.getReference();

    private RecyclerView mSessionsList;

    private static Typeface PRODUCT_SANS_TYPEFACE;
    private static Typeface PRODUCT_SANS_BOLD_TYPEFACE;
    private GregorianCalendar mCalender;
    private String mUserId;
    final List<Long> timestamps = new ArrayList<>();

    private LocalBroadcastManager mLocalBroadcastManager;
    private StandardHeadsetReceiver mStandardReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        // Require user to be logged in
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Otherwise redirect to login
            Utils.redirectToLogin(this, this);
        } else {
            mUserId = currentUser.getUid();
        }
        mCalender = new GregorianCalendar(TimeZone.getDefault());
        PRODUCT_SANS_TYPEFACE = ResourcesCompat.getFont(getApplicationContext(),
                R.font.product_sans_regular);
        PRODUCT_SANS_BOLD_TYPEFACE = ResourcesCompat.getFont(getApplicationContext(),
                R.font.product_sans_bold);

        // Initialize UI components
        final ScrollView mMainView = findViewById(R.id.progress_main_view);
        final ProgressBar loadingBar = findViewById(R.id.progress_loadingBar);
        final RelativeLayout progressChartParent = findViewById(R.id.progress_cumulative_chart_parent);
        final LinearLayout noRecordsContainer = findViewById(R.id.progress_no_records_container);
        final LinearLayout recordsContainer = findViewById(R.id.progress_records_container);
        mSessionsList = findViewById(R.id.progress_sessions_list);

        // Indicate user about no session selected
        TextView selectedSessionHeading = findViewById(R.id.progress_selected_session_heading);
        selectedSessionHeading
                .setText(getResources().getString(R.string.no_session_selected_heading));
        LinearLayout selectedSessionDataContainer =
                findViewById(R.id.progress_selected_session_container);
        selectedSessionDataContainer.setVisibility(View.GONE);

        // Session list formatting and data acquisition and handling
        TypedArray partsImageResources = getResources().obtainTypedArray(R.array.parts_of_day);
        final SessionListAdapter sessionListAdapter = new SessionListAdapter(mSessionsList,
                partsImageResources,
                selectedSessionHeading,
                selectedSessionDataContainer,
                mMainView);
        mSessionsList.setAdapter(sessionListAdapter);
        GridLayoutManager manager = new GridLayoutManager(this, 2);
        manager.setSmoothScrollbarEnabled(true);
        mSessionsList.setLayoutManager(manager);


        loadingBar.setVisibility(View.VISIBLE);
        noRecordsContainer.setVisibility(View.GONE);
        recordsContainer.setVisibility(View.GONE);

        if (!connectionActive(this)) {
            noRecordsContainer.setAlpha(0f);
            ((TextView)findViewById(R.id.progress_NoRecordsHeading))
                    .setText(R.string.no_internet_heading);
            ((TextView)findViewById(R.id.progress_NoRecordsSubHeading))
                    .setText(R.string.no_internet_subheading);
            noRecordsContainer.setVisibility(View.VISIBLE);
            noRecordsContainer
                    .animate()
                    .alpha(1f)
                    .setDuration(1000);
            Snackbar.make(mMainView,
                    "Please connect to the internet to get your progress!",
                    Snackbar.LENGTH_LONG).show();
            loadingBar.setVisibility(View.GONE);
        } else {
            mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    boolean missingData = true;
                    if (dataSnapshot.exists()) {
                        DataSnapshot userReference = dataSnapshot
                                .child(USER_REPORTS).child(mUserId);
                        if (userReference.exists()) {

                            // Display the preferred reading period from user's median activity hour
                            if (userReference.child(MEDIAN_ACTIVITY).exists() &&
                                    userReference.child(MEDIAN_ACTIVITY).getValue() != null) {
                                int medianActivity = Integer.parseInt(
                                        userReference
                                                .child(MEDIAN_ACTIVITY).getValue().toString());
                                mCalender.set(Calendar.HOUR_OF_DAY, medianActivity);
                                mCalender.roll(Calendar.HOUR_OF_DAY, false);
                                int lowerBound = mCalender.get(Calendar.HOUR);
                                String periodLower = mCalender.getDisplayName(
                                        Calendar.AM_PM, Calendar.LONG, Locale.ENGLISH);
                                mCalender.roll(Calendar.HOUR_OF_DAY, 2);
                                int upperBound = mCalender.get(Calendar.HOUR);
                                String periodUpper = mCalender.getDisplayName(
                                        Calendar.AM_PM, Calendar.LONG, Locale.ENGLISH);
                                String text = String.format(Locale.ENGLISH,
                                        "%02d:00 %s - %02d:00 %s",
                                        lowerBound, periodLower, upperBound, periodUpper);
                                
                                Drawable partOfDayDrawable = partsImageResources
                                        .getDrawable(getTimeOfDayDrawableIndex(medianActivity));
                                ((TextView)findViewById(R.id.progress_preferred_reading_hours))
                                        .setText(text);
                                ((ImageView)
                                        findViewById(R.id.progress_preferred_reading_hours_icon))
                                        .setImageDrawable(partOfDayDrawable);
                                missingData = false;
                            }

                            // Display user's highest activity hour and book
                            // Get the view elements
                            TextView highestActivityHour =
                                    findViewById(R.id.progress_highest_activity_hour);
                            if (userReference.child(HIGHEST_ACTIVITY).exists() &&
                                    userReference.child(HIGHEST_ACTIVITY).getValue() != null) {
                                int highestActivity = Integer.parseInt(
                                        userReference
                                                .child(HIGHEST_ACTIVITY).getValue().toString());
                                mCalender.set(Calendar.HOUR_OF_DAY, highestActivity);
                                highestActivityHour
                                        .setText(String.format(Locale.ENGLISH
                                                , "%02d:00 %s",
                                                mCalender.get(Calendar.HOUR),
                                                mCalender.getDisplayName(Calendar.AM_PM,
                                                        Calendar.LONG,
                                                        Locale.ENGLISH)));
                                missingData = false;
                            }
                            if (userReference.child(MOST_READ_BOOK).exists() &&
                                    userReference.child(MOST_READ_BOOK).getValue() != null) {
                                String mostReadBook = userReference
                                        .child(MOST_READ_BOOK).getValue().toString();

                                if (!mostReadBook.isEmpty()) {
                                    ((TextView)findViewById(R.id.progress_highest_activity_book))
                                            .setText(mostReadBook);
                                }
                                missingData = false;
                            }

                            // Display user's most recent activity
                            if (userReference.child(LAST_READ).exists() &&
                                    userReference.child(LAST_READ)
                                            .child(BOOK_NAME).getValue() != null &&
                                    userReference.child(LAST_READ)
                                            .child(SESSION_TIME).getValue() != null) {
                                String lastReadBook = userReference
                                        .child(LAST_READ)
                                        .child(BOOK_NAME).getValue().toString();
                                if (!lastReadBook.isEmpty()) {
                                    ((TextView)findViewById(R.id.progress_last_read_book))
                                            .setText(lastReadBook);
                                }

                                String lastReadTime = getFormattedTime(
                                        Long.parseLong(userReference.child(LAST_READ)
                                                .child(SESSION_TIME).getValue().toString()));
                                if (!lastReadTime.isEmpty()) {
                                    ((TextView)findViewById(R.id.progress_last_read_time))
                                            .setText(lastReadTime);
                                    findViewById(R.id.progress_time_format)
                                            .setVisibility(View.VISIBLE);
                                }
                                missingData = false;
                            }

                            // Display the daily distribution pie chart
                            if (userReference.child(READING_HOUR_DISTRIBUTION).exists()) {
                                PieChart distributionChart = findViewById(R.id.progress_distribution_chart);

                                // Prepare and collect the dataset for the chart
                                List<PieEntry> hourEntries = new ArrayList<>();
                                int totalRegistered = 0;
                                for (DataSnapshot entry : userReference.child(READING_HOUR_DISTRIBUTION).getChildren()) {
                                    int hour = Integer.parseInt(entry.getKey());
                                    float registered = Float.parseFloat(entry.getValue().toString());
                                    if (registered > 0) {
                                        totalRegistered += registered;
                                        hourEntries.add(
                                                new PieEntry(
                                                        registered,
                                                        getTimeWithPeriod(hour)));
                                    }
                                }
                                if (totalRegistered > 0) {
                                    distributionChart.setVisibility(View.VISIBLE);
                                    formatPieChart(distributionChart, hourEntries, totalRegistered);
                                    distributionChart.invalidate();
                                    distributionChart.animateY(1400, Easing.EaseInOutQuad);
                                    ((TextView)findViewById(R.id.progress_distribution_missing))
                                            .setVisibility(View.GONE);
                                }
                            }

                            // Display the sessions chart and get the timestamps
                            if (userReference.child(SESSIONS).exists()) {
                                missingData = false;
                                if (userReference.child(SESSIONS).getChildrenCount() > 1) {
                                    final HashMap<Long, Pair<String, Long>> sessionData = new HashMap<>();
                                    List<Entry> entries = new ArrayList<>();
                                    int numEntries = 0;
                                    for (DataSnapshot session : userReference.child(SESSIONS).getChildren()) {
                                        if (session.getKey() != null &&
                                                session.child(BOOK_NAME).getValue() != null &&
                                                session.child(SESSION_TIME).getValue() != null &&
                                                session.child(SESSION_SCORE).getValue() != null) {
                                            long timestamp = Long.parseLong(session.getKey());
                                            timestamps.add(timestamp);
                                            sessionData.put(timestamp, new Pair<>(
                                                    session.child(BOOK_NAME).getValue().toString(),
                                                    Long.parseLong(session.child(SESSION_TIME).getValue().toString())));

                                            entries.add(
                                                    new Entry(
                                                            numEntries++,
                                                            Float.parseFloat(
                                                                    session.child(SESSION_SCORE).getValue().toString())));
                                        }
                                    }

                                    if (numEntries > 0) {
                                        // Make the container visible and change the subheading
                                        ((LinearLayout)
                                                findViewById(
                                                        R.id.progress_cumulative_chart_container))
                                                .setVisibility(View.VISIBLE);
                                        ((TextView)findViewById(R.id.progress_cumulative_chart_subheading))
                                                .setText(getString(R.string.cumulative_chart_subheading));
                                        ((LinearLayout)findViewById(R.id.progress_sessions_list_container))
                                                .setVisibility(View.VISIBLE);
                                        ((TextView)findViewById(R.id.progress_selected_session_heading))
                                                .setText(getString(R.string.no_session_selected_heading));

                                        // Adding timestamps to the calendar  list
                                        sessionListAdapter.setEntries(timestamps);

                                        final LineChart chart = findViewById(R.id.progress_cumulative_chart);
                                        formatAttentionChart(chart,
                                                entries, mMainView, progressChartParent);

                                        final SessionMarker markerView = new SessionMarker(chart.getContext(),
                                                R.layout.layout_marker,
                                                chart.getRootView().getWidth() - 50,
                                                chart.getLayoutParams().height);
                                        chart.setMarker(markerView);
                                        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                                            @Override
                                            public void onValueSelected(Entry e, Highlight h) {
                                                long timestamp = timestamps.get((int)e.getX() - 1);
                                                mCalender.setTime(new Date(timestamp));

                                                ((TextView)markerView.findViewById(R.id.marker_session_number))
                                                        .setText(String.format(Locale.ENGLISH, "Session #%d", (int)e.getX()));
                                                ((TextView)markerView.findViewById(R.id.marker_session_score))
                                                        .setText(String.format(Locale.ENGLISH, "%.3f%%", e.getY()));
                                                ((TextView)markerView.findViewById(R.id.marker_session_date))
                                                        .setText(String.format(Locale.ENGLISH, "%s %02d %s %s",
                                                                mCalender.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.ENGLISH),
                                                                mCalender.get(Calendar.DATE),
                                                                mCalender.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US),
                                                                Integer.toString(mCalender.get(Calendar.YEAR)).substring(2, 4)));
                                                ((TextView)markerView.findViewById(R.id.marker_session_timestamp))
                                                        .setText(String.format(Locale.ENGLISH, "%02d:%02d %s",mCalender.get(Calendar.HOUR)
                                                                ,mCalender.get(Calendar.MINUTE)
                                                                ,mCalender.getDisplayName(Calendar.AM_PM, Calendar.SHORT, Locale.US)));

                                                if (sessionData.get(timestamp) != null) {

                                                    ((TextView)markerView.findViewById(R.id.marker_session_book))
                                                            .setText(
                                                                    String.format(
                                                                            Locale.ENGLISH, "%s...", sessionData.get(timestamp).first.substring(0, 7)));
                                                    ((TextView)markerView.findViewById(R.id.marker_session_time))
                                                            .setText(getFormattedTime(sessionData.get(timestamp).second));
                                                }
                                            }

                                            @Override
                                            public void onNothingSelected() { }
                                        });
                                    }
                                }

                            }
                        }
                    }

                    loadingBar.setVisibility(View.GONE);
                    if (missingData) {
                        fadeAppearViewObject(noRecordsContainer, 1000);
                    } else {
                        fadeAppearViewObject(recordsContainer, 1000);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    loadingBar.setVisibility(View.GONE);
                    noRecordsContainer.setVisibility(View.VISIBLE);
                    recordsContainer.setVisibility(View.GONE);
                    TextView headingView = findViewById(R.id.progress_NoRecordsHeading);
                    headingView.setText(R.string.error_heading);
                    headingView.setTextColor(getColor(R.color.warning));
                    ((TextView)findViewById(R.id.progress_NoRecordsSubHeading))
                            .setText(R.string.fetch_data_error);

                    mDatabaseReference.removeEventListener(this);
                }
            });

        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mStandardReceiver = new StandardHeadsetReceiver(LOG_TAG, mMainView);
        mLocalBroadcastManager.registerReceiver(mStandardReceiver,
                IntentFilterFactory.createStandardFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mStandardReceiver);
    }

    public void navigateBack(View view) {
        finish();
    }

    public void formatPieChart(PieChart distributionChart,
                               List<PieEntry> hourEntries, int totalRegistered) {
        PieDataSet dataSet = new PieDataSet(hourEntries, "Reading Percentage");
        dataSet.setColors(getResources().getIntArray(R.array.pie_colors));
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ReadingPercentFormatter(totalRegistered));
        data.setValueTextSize(13f);
        data.setValueTextColor(getColor(R.color.backDark));
        data.setValueTypeface(PRODUCT_SANS_BOLD_TYPEFACE);

        distributionChart.setData(data);
        distributionChart.setUsePercentValues(false);
        distributionChart.getDescription().setEnabled(false);
        distributionChart.setExtraOffsets(5, 10, 5, 5);

        distributionChart.setDragDecelerationFrictionCoef(0.95f);

        distributionChart.setDrawHoleEnabled(true);
        distributionChart.setHoleColor(getColor(R.color.colorSecondaryDark));

        distributionChart.setTransparentCircleColor(Color.BLACK);
        distributionChart.setTransparentCircleAlpha(110);

        distributionChart.setHoleRadius(50f);
        distributionChart.setTransparentCircleRadius(61f);

        distributionChart.setDrawCenterText(true);

        distributionChart.setRotationAngle(0);

        distributionChart.setRotationEnabled(true);
        distributionChart.setHighlightPerTapEnabled(true);

        distributionChart.setAlpha(0.75f);

        Legend l = distributionChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(true);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);
        l.setTypeface(PRODUCT_SANS_TYPEFACE);

        distributionChart.setEntryLabelColor(Color.WHITE);
        distributionChart.setEntryLabelTypeface(PRODUCT_SANS_TYPEFACE);
        distributionChart.setEntryLabelTextSize(12f);
    }

    public void formatAttentionChart(LineChart chart, List<Entry> entries,
                                     ScrollView mMainView, RelativeLayout progressChartParent) {
        // Configure and enable chart settings
        chart.getDescription().setEnabled(true);
        chart.getDescription().setText("AttentionGraph");

        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(getColor(R.color.colorSecondary));

        // Prepare the empty data set
        LineDataSet dataSet = new LineDataSet(entries, "sessions");
        dataSet.setValueTextSize(10f);
        dataSet.setColor(Color.DKGRAY);
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setValueTypeface(PRODUCT_SANS_TYPEFACE);
        dataSet.setCircleColor(getColor(R.color.colorAccent));
        dataSet.setColor(getColor(R.color.colorPrimary));
        dataSet.setFillDrawable(getDrawable(R.drawable.gradient_accent));
        dataSet.setDrawFilled(true);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        chart.getLegend().setEnabled(false);

        // Configure the axes
        chart.getXAxis().setEnabled(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTypeface(PRODUCT_SANS_TYPEFACE);
        leftAxis.setTextColor(getColor(R.color.colorTertiary));
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        chart.getAxisRight().setEnabled(false);

        final LineData lineData =  new LineData(dataSet);
        chart.getDescription().setEnabled(false);
        mMainView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                Rect bounds = new Rect();
                progressChartParent.getHitRect(bounds);

                Rect scrollBounds = new Rect();
                mMainView.getDrawingRect(scrollBounds);

                int chartOrdinate = (bounds.top +
                        ((TextView)findViewById(R.id.progress_cumulative_chart_heading)).getHeight()
                        + (progressChartParent.getHeight() / 2));

                if (chartOrdinate <= scrollBounds.bottom) {
                    chart.setData(lineData);
                    chart.invalidate();
                    chart.animateY(1400, Easing.EaseInOutQuad);
                    mMainView.getViewTreeObserver().removeOnScrollChangedListener(this);
                }
            }
        });
    }

    private class ReadingPercentFormatter extends ValueFormatter {
        private int totalRegistered;

        public ReadingPercentFormatter(int totalRegistered) {
            super();
            this.totalRegistered = totalRegistered;
        }

        @Override
        public String getPieLabel(float value, PieEntry pieEntry) {
            return  String.format(Locale.ENGLISH, "%d%%", Math.round(100 * value/totalRegistered));
        }
    }

    private static class SessionMarker extends MarkerView {
        float chartWidth;
        float chartHeight;

        public SessionMarker(Context context, int layoutResource, float chartWidth, float chartHeight) {
            super(context, layoutResource);
            this.chartWidth = chartWidth;
            this.chartHeight = chartHeight;
        }
        @Override
        public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
            MPPointF currentOffset = super.getOffsetForDrawingAtPoint(posX, posY);
            if (posX + getWidth() >= chartWidth) {
                currentOffset.x -= getWidth();
            }
            if (posY + getHeight() >= chartHeight) {
                currentOffset.y -= getHeight();
            }
            return currentOffset;
        }
    }
}
