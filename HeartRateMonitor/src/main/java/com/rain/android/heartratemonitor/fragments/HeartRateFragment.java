package com.rain.android.heartratemonitor.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.rain.android.heartratemonitor.R;
import com.rain.android.heartratemonitor.monitor.BasicHeartRateProvider;
import com.rain.android.heartratemonitor.utilities.BluetoothLEUtils;
import com.rain.android.heartratemonitor.views.BpmGraphView;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by jeremiahstephenson on 10/30/13.
 */
public class HeartRateFragment extends Fragment {

    private MenuItem mScan;
    private BasicHeartRateProvider mHeartRateProvider;
    private Timer mTimer;
    private TextView mBpm;
    private boolean mScanning = false;
    private OnHeartRateListener mListener;
    private BpmGraphView mChart;
    private GraphViewSeries mSeries;
    protected Double mMin = null;
    protected Double mMax = null;
    private AlertDialog mGpsDialog;
    private static final double MAX_Y = 90;
    private AlertDialog mGPSAlert;

    public interface OnHeartRateListener {
        public void onListening();
    }

    public static HeartRateFragment newInstance(OnHeartRateListener listener) {
        HeartRateFragment fragment = new HeartRateFragment(listener);
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public HeartRateFragment(OnHeartRateListener listener) {
        mListener = listener;
    }

    public HeartRateFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mHeartRateProvider = new BasicHeartRateProvider();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mChart = new BpmGraphView(getActivity(), "", createGraphStyle(), true, false);
        mChart.setDrawBackground(true);
        mChart.setScalable(false);
        mChart.setScrollable(true);
        mChart.setDisableTouch(true);
        mChart.setShowVerticalGridLines(false);

        ((ViewGroup)getView().findViewById(R.id.container_chart)).addView(mChart);

        mChart.setManualYAxisBounds(MAX_Y, 0);
        mChart.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX, int length, int index) {
                return String.valueOf(Math.round(value)) + " " +
                        (index > 0 && length > 0 && (index == (length - 1)) ? getString(R.string.beats_per_minute_short) : "");
            }
        });

        if (BluetoothLEUtils.hasBluetoothLE(getActivity())) {
            final BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();

            if (!bt.isEnabled()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setCancelable(true)
                        .setTitle(R.string.needs_bluetooth_title)
                        .setMessage(getString(R.string.needs_bluetooth))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (bt != null) {
                                    bt.enable();
                                }
                            }
                        });

                builder.show();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mBpm = (TextView) rootView.findViewById(R.id.lbl_bpm);
        mBpm.setText(getString(R.string.looking_for_monitor));

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHeartRateProvider != null && mHeartRateProvider.isConnected()) {
            mHeartRateProvider.stop();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.heart_rate, menu);
        mScan = menu.findItem(R.id.action_sync);
        if (!mScanning) {
            mScan.setTitle(R.string.action_scan);
        } else {
            mScan.setTitle(R.string.action_stop);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sync:

                if (!mScanning) {
                    scanForHeartRate();
                    item.setTitle(R.string.action_stop);
                    mScanning = true;
                } else {
                    mScanning = false;
                    item.setTitle(R.string.action_scan);
                    stopScanForHearRate();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void scanForHeartRate() {

        if (mChart != null) {
            mChart.removeAllSeries();
            mSeries = null;
        }

        if (mListener != null) {
            mListener.onListening();
        }

        mScan.setTitle(R.string.action_stop);
        mBpm.setVisibility(View.VISIBLE);
        getActivity().setProgressBarIndeterminateVisibility(true);

        mHeartRateProvider.start(getActivity());

        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateHeartRate();
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopScanForHearRate() {

        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }

        if (mHeartRateProvider != null) {
            mHeartRateProvider.stop();
            mBpm.setText(String.valueOf(0));
            mBpm.setVisibility(View.GONE);
            getActivity().setProgressBarIndeterminateVisibility(false);
        }
    }

    private void updateHeartRate() {
        if (mHeartRateProvider != null) {
            final int bpm = mHeartRateProvider.getBpm();
            if (bpm > -1) {
                getActivity().setProgressBarIndeterminateVisibility(false);
                mBpm.setText(String.valueOf(bpm));

                if (bpm > 0) {

                    GraphView.GraphViewData data = new GraphView.GraphViewData(Calendar.getInstance().getTimeInMillis(), bpm);

                    setMinAndMax(data);
                    if (mMax != null && mMin != null) {
                        mChart.setManualYAxisBounds(mMax > MAX_Y ? mMax : MAX_Y, mMin);
                    }

                    if (mChart != null) {

                        if (mSeries == null || mSeries.getLastDataItem() == null) {
                            mChart.setViewPort(Calendar.getInstance().getTimeInMillis(), 3 * 60 * 1000);
                        }

                        if (mSeries == null) {
                            mSeries = new GraphViewSeries(new GraphView.GraphViewData[]{data});
                            mSeries.getStyle().thickness = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
                            mChart.removeAllSeries();
                            mChart.addSeries(mSeries);
                        } else if (mSeries.getLastDataItem() != null) {
                            mSeries.appendData(data, true, 500, true);
                        }
                    }
                }

            } else {
                getActivity().setProgressBarIndeterminateVisibility(true);
                final BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
                mBpm.setText(getResources().getString((!bt.isEnabled()) ? R.string.needs_bluetooth_title : R.string.looking_for_monitor));

            }
        }
    }

    private GraphViewStyle createGraphStyle() {

        final GraphViewStyle style = new GraphViewStyle();
        style.setNumVerticalLabels(6);
        style.setNumHorizontalLabels(5);
        style.setVerticalLabelsColor(getResources().getColor(R.color.really_dark_grey));
        style.setShowBottomLineAndLabels(false);

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics);

        final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics);
        style.setVerticalLabelsMargins((int)px, 0);
        style.setVerticalImagesMargins(0, (int)px);

        style.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, metrics));

        return style;
    }

    private void setMinAndMax(GraphViewDataInterface data) {

        if (data != null) {

            if (mMin == null || data.getY() < mMin) {
                mMin = data.getY();
            }

            if (mMax == null || data.getY() > mMax) {
                mMax = data.getY();
            }
        }
    }
}
