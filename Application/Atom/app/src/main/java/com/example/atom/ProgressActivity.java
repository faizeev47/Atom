package com.example.atom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Typeface;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.example.atom.Utils.connectionActive;
import static com.example.atom.Utils.getIdFromEmail;

public class ProgressActivity extends AppCompatActivity {

    private static final String LOG_TAG = ProgressActivity.class.getSimpleName() + "Logging ";
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = firebaseDatabase.getReference();

    private TextView mNoRecordsHeading;
    private TextView mNoRecordsSubHeading;
    private TextView mChartHeading;
    private LineChart mProgressChart;
    private RelativeLayout mChartContainer;
    private ProgressBar mLoadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        mNoRecordsHeading = findViewById(R.id.progress_NoRecordsHeading);
        mNoRecordsSubHeading = findViewById(R.id.progress_NoRecordsSubHeading);
        mChartHeading = findViewById(R.id.progress_chartHeading);
        mProgressChart = findViewById(R.id.progress_chart);
        mLoadingBar = findViewById(R.id.progress_loadingBar);
        mChartContainer = findViewById(R.id.progress_chartContainer);

        if (!connectionActive(this)) {
            Toast.makeText(this, "Please connect to the internet to get progress!", Toast.LENGTH_SHORT).show();
            mNoRecordsHeading.setVisibility(View.VISIBLE);
            mNoRecordsSubHeading.setVisibility(View.VISIBLE);
            mLoadingBar.setVisibility(View.GONE);
            return;
        }

        String mUserEmail = "";
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mUserEmail = currentUser.getEmail();
        }
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (googleAccount != null) {
            mUserEmail = googleAccount.getEmail();
        }

        final String userId = getIdFromEmail(mUserEmail);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean noUserData = true;
                if (dataSnapshot.child("userReports").child(userId).exists()) {
                    mNoRecordsHeading.setVisibility(View.GONE);
                    mNoRecordsSubHeading.setVisibility(View.GONE);
                    DataSnapshot userData = dataSnapshot.child("userReports").child(userId);
                    if (userData.hasChildren()) {
                        mNoRecordsHeading.setVisibility(View.GONE);
                        mNoRecordsSubHeading.setVisibility(View.GONE);

                        List<Entry> chartEntries = new ArrayList<>();
                        noUserData = false;
                        for (DataSnapshot report: userData.getChildren()) {
                            Date reportDate = new Date(Long.parseLong(report.getKey()));
                            float sessionScore = Float.parseFloat(report.getValue().toString());
                            chartEntries.add(new Entry(reportDate.getTime(), sessionScore));
                            Log.d(LOG_TAG, "Date: " + reportDate.toString() + " : Score:" + sessionScore);
                        }
                        LineDataSet dataSet = new LineDataSet(chartEntries, "Reading Progress");
                        LineData lineData = new LineData(dataSet);
                        lineData.setValueTextSize(18f);
                        lineData.setValueTypeface(getResources().getFont(R.font.product_sans_regular));
                        lineData.setValueTextColor(getColor(R.color.colorTertiary));

                        mProgressChart.setData(lineData);
                        Description description = new Description();
                        description.setText("");
                        mProgressChart.setDescription(description);

                        XAxis xAxis = mProgressChart.getXAxis();
                        xAxis.setLabelRotationAngle(45f);
                        xAxis.setTextSize(12f);
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        xAxis.setValueFormatter(new MyXAxisFormatter());
                        xAxis.setTypeface(getResources().getFont(R.font.product_sans_regular));
                        xAxis.setTextColor(getColor(R.color.backLight));

                        YAxis rightAxis = mProgressChart.getAxisRight();
                        rightAxis.setEnabled(false);
                        rightAxis.setDrawAxisLine(false);

                        YAxis leftAxis = mProgressChart.getAxisLeft();
                        leftAxis.setTypeface(getResources().getFont(R.font.product_sans_regular));
                        leftAxis.setTextSize(16f);

                    }
                }
                if (noUserData) {
                    mNoRecordsHeading.setVisibility(View.VISIBLE);
                    mNoRecordsSubHeading.setVisibility(View.VISIBLE);
                    mChartHeading.setVisibility(View.GONE);
                    mChartContainer.setVisibility(View.GONE);
                }
                else {
                    mChartHeading.setVisibility(View.VISIBLE);
                    mChartContainer.setVisibility(View.VISIBLE);
                    mNoRecordsHeading.setVisibility(View.GONE);
                    mNoRecordsSubHeading.setVisibility(View.GONE);
                }
                mLoadingBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mLoadingBar.setVisibility(View.GONE);
            }
        });
    }

    public void navigateBack(View view) {
        finish();
    }

    private class MyXAxisFormatter extends ValueFormatter {
        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            SimpleDateFormat curFormater = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat timeFormater = new SimpleDateFormat("hh:mm:ss");
            Date date = new Date((long)value);
            return curFormater.format(date) + "\n\r" + timeFormater.format(date);
        }
    }
}
