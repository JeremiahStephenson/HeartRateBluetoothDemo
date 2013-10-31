package com.rain.android.heartratemonitor.monitor;

/**
 * Created by jeremiahstephenson on 9/3/13.
 */
public class HeartRateEvent {

    private String mMessage = "";

    public String getMessage() {
        return mMessage;
    }

    public HeartRateEvent(String message) {
        mMessage = message;
    }

}
