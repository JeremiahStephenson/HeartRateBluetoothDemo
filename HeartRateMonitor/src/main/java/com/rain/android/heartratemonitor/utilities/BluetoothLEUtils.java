package com.rain.android.heartratemonitor.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import com.rain.android.heartratemonitor.R;

/**
 * Created by jeremiahstephenson on 8/30/13.
 */
public class BluetoothLEUtils {

    public static boolean hasBluetoothLE(Context context) {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
                context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    }
}
