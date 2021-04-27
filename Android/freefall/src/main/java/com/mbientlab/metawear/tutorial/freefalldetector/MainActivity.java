/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 */

package com.mbientlab.metawear.tutorial.freefalldetector;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Logging;

import bolts.Continuation;
import bolts.Task;

public class MainActivity extends Activity implements ServiceConnection {

    private static final String LOG_TAG = "freefall";
    private MetaWearBoard mwBoard;
    private Accelerometer accelerometer;
    private Debug debug;
    private Logging logging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getApplicationContext().bindService(new Intent(this, BtleService.class), this, Context.BIND_AUTO_CREATE);

        findViewById(R.id.start_accel).setOnClickListener(v -> {
            logging.start(false);
            accelerometer.acceleration().start();
            accelerometer.start();
        });
        findViewById(R.id.stop_accel).setOnClickListener(v -> {
            accelerometer.stop();
            accelerometer.acceleration().stop();

            logging.stop();
            logging.downloadAsync().continueWith(task -> {
                Log.i(LOG_TAG, "Log download complete");
                return null;
            });
        });
        findViewById(R.id.reset_board).setOnClickListener(v -> debug.resetAsync());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getApplicationContext().unbindService(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        BtleService.LocalBinder serviceBinder = (BtleService.LocalBinder) service;

        String mwMacAddress= "D5:7B:B9:7D:CE:0E";   ///< Put your board's MAC address here
        BluetoothManager btManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothDevice btDevice= btManager.getAdapter().getRemoteDevice(mwMacAddress);

        mwBoard= serviceBinder.getMetaWearBoard(btDevice);
        mwBoard.connectAsync().onSuccessTask(task -> {
            accelerometer = mwBoard.getModule(Accelerometer.class);
            accelerometer.configure()
                    .odr(50f)
                    .commit();
            return accelerometer.acceleration().addRouteAsync(source ->
                source.map(Function1.RSS).lowpass((byte) 4).filter(ThresholdOutput.BINARY, 0.5f)
                    .multicast()
                        .to().filter(Comparison.EQ, -1).log((data, env) -> Log.i(LOG_TAG, data.formattedTimestamp() + ": Entered Free Fall"))
                        .to().filter(Comparison.EQ, 1).log((data, env) -> Log.i(LOG_TAG, data.formattedTimestamp() + ": Left Free Fall"))
                .end());
        }).continueWith((Continuation<Route, Void>) task -> {
            if (task.isFaulted()) {
                Log.e(LOG_TAG, mwBoard.isConnected() ? "Error setting up route" : "Error connecting", task.getError());
            } else {
                Log.i(LOG_TAG, "Connected");
                debug = mwBoard.getModule(Debug.class);
                logging= mwBoard.getModule(Logging.class);
            }

            return null;
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
