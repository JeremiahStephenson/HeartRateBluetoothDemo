package com.rain.android.heartratemonitor.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;

import com.rain.android.heartratemonitor.R;
import com.rain.android.heartratemonitor.monitor.HeartRateEvent;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

/**
 * Created by jeremiahstephenson on 10/30/13.
 */
public class LogFragment extends ListFragment {

    private ArrayAdapter<String> mAdapter;
    private ArrayList<String> mData;

    public static LogFragment newInstance() {
        LogFragment fragment = new LogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public LogFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = super.onCreateView(inflater, container, savedInstanceState);

        view.setBackgroundColor(getResources().getColor(R.color.grey_background));

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mData = new ArrayList<String>();
        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);

        getListView().setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        setListAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(HeartRateEvent event) {

        if (mAdapter != null) {
            Log.d("LE example", event.getMessage());
            mData.add(event.getMessage());

            mAdapter.clear();
            for (String data : mData) {
                mAdapter.add(data);
            }

            mAdapter.notifyDataSetChanged();
        }
    }

    public void clearLog() {
        mData.clear();
        mAdapter.clear();
    }
}
