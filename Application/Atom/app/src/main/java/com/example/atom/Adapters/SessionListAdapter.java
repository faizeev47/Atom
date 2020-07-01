package com.example.atom.Adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.method.Touch;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.atom.R;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.example.atom.Utilities.FirebaseUtils.*;
import static com.example.atom.Utilities.Utils.getFormattedTime;
import static com.example.atom.Utilities.Utils.getTimeOfDayDrawableIndex;

public class SessionListAdapter extends RecyclerView.Adapter<SessionListAdapter.SessionViewHolder> {
    private static Typeface PRODUCT_SANS_TYPEFACE;

    private final GregorianCalendar mCalender = new GregorianCalendar(TimeZone.getDefault());

    private List<Long> mEntries;
    private TypedArray mPartIcons;
    private RecyclerView mAdapterPartner;
    private TextView mSessionHeading;
    private LinearLayout mSessionDataContainer;
    private ScrollView mMainScrollView;

    public SessionListAdapter(RecyclerView adapterPartner, TypedArray partIcons,
                              TextView sessionHeading, LinearLayout sessionDataContainer,
                              ScrollView mainScrollView) {
        // Get the RecyclerView to disable touch on onClick call
        mAdapterPartner = adapterPartner;
        mPartIcons = partIcons;
        mSessionHeading = sessionHeading;
        mSessionDataContainer = sessionDataContainer;
        mMainScrollView = mainScrollView;

        PRODUCT_SANS_TYPEFACE = ResourcesCompat.getFont(mMainScrollView.getContext(), R.font.product_sans_regular);
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context parentContext = parent.getContext();
        View viewItem = LayoutInflater.from(parentContext)
                .inflate(R.layout.recyclerview_calendar, parent, false);
       SessionViewHolder viewHolder = new SessionViewHolder(viewItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        // Set calendar from current timestamp
        mCalender.setTime(new Date(mEntries.get(position)));

        // Set Date text for the session entry in the card
        holder.sessionEntry.setText(String.format(Locale.ENGLISH, "%s %d %s %s",
                mCalender.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH),
                mCalender.get(Calendar.DATE),
                mCalender.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH),
                mCalender.get(Calendar.YEAR)));
        // Set Time text for the session time in the card
        holder.sessionTimestamp.setText(String.format(Locale.ENGLISH, "%02d:%02d %s",
                mCalender.get(Calendar.HOUR),
                mCalender.get(Calendar.MINUTE),
                mCalender.getDisplayName(Calendar.AM_PM, Calendar.LONG, Locale.ENGLISH)));
        // Set the part of the day
        holder.sessionDayTime.setImageDrawable(
                mPartIcons.getDrawable(
                        getTimeOfDayDrawableIndex(mCalender.get(Calendar.HOUR_OF_DAY))));
    }

    @Override
    public int getItemCount() {
        return mEntries != null ? mEntries.size() : 0;
    }

    public void setEntries(List<Long> entries) {
        mEntries = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0; i--) {
            mEntries.add(entries.get(i));
        }
        notifyDataSetChanged();
    }

    static class TouchDisabler extends RecyclerView.SimpleOnItemTouchListener {
        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            return true;
        }
    }

    class SessionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Get firebase reference from the database for timestamp click handler
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mDatabaseReference = mFirebaseDatabase.getReference();

        private final TextView sessionEntry;
        private final TextView sessionTimestamp;
        private final ImageView sessionDayTime;

        public SessionViewHolder(View itemView) {
            super(itemView);
            sessionEntry = itemView.findViewById(R.id._session_entry);
            sessionTimestamp = itemView.findViewById(R.id._session_timestamp);
            sessionDayTime = itemView.findViewById(R.id._session_daytime);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // Disable touch events for the recycler view until execution
            TouchDisabler touchDisabler = new TouchDisabler();
            mAdapterPartner.addOnItemTouchListener(touchDisabler);

            // For the selected timestamp, query firebase for session data
            mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d("Logging", "onDataChange\n" + mAdapterPartner.getId());
                    // Get session data for referenced timestamp
                    DataSnapshot sessionData = dataSnapshot
                            .child(USER_REPORTS)
                            .child(mFirebaseAuth.getCurrentUser().getUid())
                            .child(SESSIONS)
                            .child(Long.toString(mEntries.get(getLayoutPosition())));

                    // Display session data by setting data into referenced views from the parent LinearLayout container
                    ((TextView)mSessionDataContainer
                            .findViewById(R.id.progress_selected_session_book))
                            .setText(sessionData.child(BOOK_NAME).getValue().toString());
                    ((TextView)mSessionDataContainer
                            .findViewById(R.id.progress_selected_session_score))
                            .setText(String.format(
                                    Locale.ENGLISH,
                                    "%s%%",
                                    sessionData.child(SESSION_SCORE).getValue().toString()));
                    ((TextView)mSessionDataContainer
                            .findViewById(R.id.progress_selected_session_time))
                            .setText(
                                    getFormattedTime(
                                            Long.parseLong(
                                                sessionData
                                                        .child(SESSION_TIME)
                                                        .getValue().toString())));
                    mSessionHeading.setText(R.string.session_selected_heading);

                    // Parse the cart data from the session data
                    List<Entry> entries = new ArrayList<>();
                    for (DataSnapshot entry : sessionData.child(ATTENTION_TIME).getChildren()) {
                        entries.add(new Entry(Integer.parseInt(entry.getKey()), Float.parseFloat(entry.getValue().toString())));
                    }

                    // Format the visual appearance of the chart
                    final LineChart chart = mSessionDataContainer.findViewById(R.id.progress_selected_session_plot);
                    Context chartContext = chart.getContext();
                    LineDataSet dataSet = new LineDataSet(entries, "sessions");
                    dataSet.setValueTextSize(10f);
                    dataSet.setColor(Color.DKGRAY);
                    dataSet.setLineWidth(3f);
                    dataSet.setCircleRadius(2f);
                    dataSet.setCircleColor(chartContext.getColor(R.color.colorAccent));
                    dataSet.setColor(chartContext.getColor(R.color.success));
                    dataSet.setFillDrawable(chartContext.getDrawable(R.drawable.gradient_success));
                    dataSet.setDrawFilled(true);
                    dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                    XAxis xAxis = chart.getXAxis();
                    xAxis.setEnabled(true);
                    xAxis.setTextColor(chartContext.getColor(R.color.colorTertiary));
                    xAxis.setTypeface(PRODUCT_SANS_TYPEFACE);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                    YAxis yAxis = chart.getAxisLeft();
                    yAxis.setTypeface(PRODUCT_SANS_TYPEFACE);
                    yAxis.setAxisMinimum(0f);
                    yAxis.setAxisMaximum(100f);

                    chart.getAxisRight().setEnabled(false);
                    chart.getLegend().setEnabled(false);

                    final LineData lineData =  new LineData(dataSet);
                    chart.getDescription().setEnabled(false);

                    // Make view elements visible
                    mSessionDataContainer.setAlpha(0);
                    mSessionDataContainer.setVisibility(View.VISIBLE);
                    chart.setData(lineData);

                    mSessionDataContainer
                            .animate()
                            .alpha(1f)
                            .setDuration(1000);

                    chart.invalidate();
                    chart.animateY(1400, Easing.EaseInOutQuad);

                    mAdapterPartner.removeOnItemTouchListener(touchDisabler);

                    mMainScrollView.smoothScrollTo(0, mMainScrollView.getBottom() + 1800);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    mAdapterPartner.removeOnItemTouchListener(touchDisabler);
                }
            });
        }
    }

}
