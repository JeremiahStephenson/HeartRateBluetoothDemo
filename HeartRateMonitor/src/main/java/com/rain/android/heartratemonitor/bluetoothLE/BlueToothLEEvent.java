package com.rain.android.heartratemonitor.bluetoothLE;

/**
 * Created by jeremiahstephenson on 9/3/13.
 */
public class BlueToothLEEvent {

    private int mBpm = 0;

    public int getBpm() {
        return mBpm;
    }

    public BlueToothLEEvent(int bpm) {
        mBpm = bpm;
    }

}
