package com.example.atom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.atom.Headset.HeadsetState;
import com.example.atom.Headset.IntentFilterFactory;
import com.example.atom.Headset.SocketService;
import com.example.atom.Headset.StandardHeadsetReceiver;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

import static com.example.atom.Headset.ServiceAction.*;
import static com.example.atom.Utilities.FirebaseUtils.*;
import static com.example.atom.Utilities.Utils.fadeAppearTextView;
import static com.example.atom.Utilities.Utils.fadeAppearViewObject;
import static com.example.atom.Utilities.Utils.toFirstCaps;

public class ConnectionActivity extends AppCompatActivity {
    private static final String LOG_TAG = ConnectionActivity.class.getSimpleName() + " LOGGING ";
    private static Typeface PRODUCT_SANS_TYPEFACE;

    private View mMainView;
    private Button mConnectButton;
    private Button mDisconnectButton;
    private TextView mConnectionState;
    private TextView mConnectionStatus;
    private ProgressBar mLoadingBar;
    private LinearLayout mNotConnectedContainer;
    private LinearLayout mConnectedContainer;
    private LineChart mLiveChart;

    private boolean viewFeedChecked;

    private HeadsetState mHeadsetState;

    private LocalBroadcastManager mLocalBroadcastManager;

    private StandardHeadsetReceiver mStandardReceiver;
    private SocketReceiver mSocketReceiver = new SocketReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference();

        PRODUCT_SANS_TYPEFACE = ResourcesCompat.getFont(getApplicationContext(), R.font.product_sans_regular);

        // Updates the SERVER_URL used from firebase
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("hostOptions")) {
                    SocketService.SERVER_URL =  String.format(Locale.ENGLISH,
                            "ws://%s:%s",
                            dataSnapshot
                                    .child(HOST_OPTIONS)
                                    .child(HOSTNAME).getValue(),
                            dataSnapshot
                                    .child(HOST_OPTIONS)
                                    .child(PORT).getValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        // Initialize UI components
        mMainView = findViewById(R.id.connect_view);
        mConnectButton = findViewById(R.id.connect_connect);
        mDisconnectButton = findViewById(R.id.connect_disconnect);
        mConnectionState = findViewById(R.id.connect_status);
        mConnectionStatus = findViewById(R.id.connect_state);
        mLoadingBar = findViewById(R.id.connect_loadingBar);
        mNotConnectedContainer = findViewById(R.id.connect_not_connected_container);
        mConnectedContainer = findViewById(R.id.connect_connected_container);

        ((Switch)findViewById(R.id.connect_view_feed)).setChecked(false);

        // Reference and perform a preliminary chart
        mLiveChart = findViewById(R.id.connect_live_feed_graph);
        setupChart();

        mHeadsetState = HeadsetState.DISCONNECTED;

        viewFeedChecked = false;

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mStandardReceiver = new StandardHeadsetReceiver(LOG_TAG, mMainView);

        mLocalBroadcastManager.registerReceiver(mStandardReceiver,
                IntentFilterFactory.createStandardFilter());
        mLocalBroadcastManager.registerReceiver(mSocketReceiver,
                IntentFilterFactory.createFilter(
                        STATUS_UPDATE,
                        STATE_UPDATE,
                        CONNECTION_INITIATION,
                        CONNECTION_FAILED,
                        CONNECTION,
                        DISCONNECTION,
                        ATTENTION_UPDATE));
        mLocalBroadcastManager.sendBroadcast(new Intent(SocketService.ACTION_GET_CONNECTION_STATUS));

        updateUI();
    }

    @Override
    protected void onDestroy() {
        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
        mLocalBroadcastManager.unregisterReceiver(mStandardReceiver);
        if (mHeadsetState != HeadsetState.CONNECTED) {
            disconnect();
        }
        super.onDestroy();
    }

    public void setupChart() {
        // Configure and enable chart settings
        mLiveChart.getDescription().setEnabled(true);
        mLiveChart.getDescription().setText("Live attention feed");

        mLiveChart.setDragXEnabled(true);
        mLiveChart.setDragYEnabled(false);

        mLiveChart.setDefaultFocusHighlightEnabled(false);
        mLiveChart.setHighlightPerDragEnabled(false);
        mLiveChart.setHighlightPerTapEnabled(false);

        // Prepare the empty data set
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        mLiveChart.setData(data);

        // Get and configure the legend
        mLiveChart.getLegend().setEnabled(false);

        // Configure the axes
        XAxis xl = mLiveChart.getXAxis();
        xl.setTypeface(PRODUCT_SANS_TYPEFACE);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        xl.setLabelRotationAngle(90f);
        xl.setGranularityEnabled(true);
        xl.setGranularity(2f);

        YAxis leftAxis = mLiveChart.getAxisLeft();
        leftAxis.setTypeface(PRODUCT_SANS_TYPEFACE);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        mLiveChart.getAxisRight().setEnabled(false);
    }

    public void addEntry(int attention) {

        LineData data = mLiveChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), attention), 0);
            data.notifyDataChanged();

            // Set the minimum x-axis value
            mLiveChart.setVisibleXRange(0f, 25f);

            // let the chart know it's data has changed
            mLiveChart.notifyDataSetChanged();

            // limit the number of visible entries
            mLiveChart.setVisibleXRangeMaximum(120);
            // chart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mLiveChart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        return set;
    }

    public void navigateBack(View view) {
        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
        finish();
    }

    public void connect(View view) {
        if (!SocketService.SERVICE_RUNNING) {
            Intent intent = new Intent(this, SocketService.class);
            startService(intent);
            Log.d(LOG_TAG, "NOT RUNNING");
        }
        else {
            Log.d(LOG_TAG, "RUNNING");
        }
    }

    public void disconnect() {
        if (SocketService.SERVICE_RUNNING) {
            Intent intent = new Intent(this, SocketService.class);
            stopService(intent);
        }
    }

    public void disconnect(View view) {
        disconnect();
    }

    private void updateUI() {
        switch (mHeadsetState) {
            case DISCONNECTED:
                mConnectButton.setEnabled(true);
                mDisconnectButton.setEnabled(false);

                mConnectionState.setTextColor(getColor(R.color.warning));
                mConnectedContainer.setVisibility(View.GONE);

                fadeAppearTextView(mConnectionStatus,
                        getString(R.string.status_disconnected), 1000);
                fadeAppearTextView(mConnectionState,
                        getString(R.string.status_disconnected), 1000);
                fadeAppearViewObject(mNotConnectedContainer, 1000);
                break;
            case CONNECTING:
                mConnectButton.setEnabled(false);
                mDisconnectButton.setEnabled(false);

                mConnectedContainer.setVisibility(View.GONE);

                fadeAppearTextView(mConnectionStatus,
                        getString(R.string.state_attempting_connection), 1000);
                fadeAppearViewObject(mLoadingBar, 1000);
                fadeAppearViewObject(mNotConnectedContainer, 1000);
                break;
            case FAILED:
                mConnectButton.setEnabled(true);
                mDisconnectButton.setEnabled(false);

                mLoadingBar.setVisibility(View.INVISIBLE);
                mConnectedContainer.setVisibility(View.GONE);

                fadeAppearTextView(mConnectionStatus,
                        getString(R.string.state_failed_to_connect), 1000);
                fadeAppearViewObject(mNotConnectedContainer, 1000);
                break;
            case CONNECTED:
                mConnectButton.setEnabled(false);
                mDisconnectButton.setEnabled(true);

                mConnectionState.setTextColor(getColor(R.color.success));
                mLoadingBar.setVisibility(View.INVISIBLE);
                mNotConnectedContainer.setVisibility(View.GONE);

                mConnectionStatus.setText("");
                fadeAppearTextView(mConnectionState,
                        getString(R.string.status_connected), 1000);
                fadeAppearViewObject(mConnectedContainer, 1000);
                break;
            default:
                break;
        }
    }

    private void updateUI(String state) {
        updateUI();
        if (mHeadsetState == HeadsetState.CONNECTING) {
            mConnectionStatus.setText(toFirstCaps(state) + "…");
        }
    }


    public void viewFeed(View view) {
        viewFeedChecked = ((Switch)view).isChecked();

        // Set a new dataset to display a clean plot
        if (!viewFeedChecked) {
            mLiveChart.setData(new LineData());
        }
    }

    public class SocketReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            String state = null;
            if (intentAction != null) {
                switch (intentAction) {
                    case SocketService.ACTION_CONNECTION_INITIATED:
                        mHeadsetState = HeadsetState.CONNECTING;
                        state = "Attempting to connect";
                        updateUI(state);
                        break;
                    case SocketService.ACTION_UPDATE_CONNECTION_STATE:
                        mHeadsetState = HeadsetState.CONNECTING;
                        state = intent.getStringExtra(SocketService.EXTRA_STATE);
                        updateUI(state);
                        break;
                    case SocketService.ACTION_RESPOND_CONNECTED:
                        mHeadsetState = HeadsetState.CONNECTED;
                        updateUI();
                        break;
                    case SocketService.ACTION_FAILED:
                        mHeadsetState = HeadsetState.FAILED;
                        updateUI();
                        break;
                    case SocketService.ACTION_RESPOND_DISCONNECTED:
                        mHeadsetState = HeadsetState.DISCONNECTED;
                        updateUI();
                        break;
                    case SocketService.ACTION_RETURN_CONNECTION_STATUS:
                        String status = intent.getStringExtra(SocketService.EXTRA_STATUS);
                        if (status != null && status.equals("connected")) {
                            mHeadsetState = HeadsetState.CONNECTED;
                        } else {
                            mHeadsetState = HeadsetState.DISCONNECTED;
                        }
                        updateUI();
                        break;
                    case SocketService.ACTION_RETURN_ATTENTION_VALUE:
                        if (viewFeedChecked) {
                            int attention = intent.getIntExtra(SocketService.EXTRA_ATTENTION_VALUE, 0);
                            addEntry(attention);
                        }
                        break;
                }
            }
        }
    }


}
