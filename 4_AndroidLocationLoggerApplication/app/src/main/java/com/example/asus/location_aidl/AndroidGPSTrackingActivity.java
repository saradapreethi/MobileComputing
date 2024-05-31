package com.example.LocationLoggerProj.location_aidl;

/**
 * Created by ASUS on 2016/6/2.
 */

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AndroidGPSTrackingActivity extends Activity {

    public TextView view_latitude, view_longitude,view_distance,view_speed;
   // public final int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;
    //private static final int PERMISSION_FINE_LOCATION = 0;
    //private static final int PERMISSION_INTERNET = 1;
    private Button startBtn;
    private Button stopBtn;
    private Button updateBtn;
    private Button exitBtn;

    IGPSTrackerService ServiceProxy = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    double preLatitude = 0.0;
    double preLongitude = 0.0;
    boolean flag;
    float[] dis = null;
    float totalDistance = 0;
    int locationNum;
    float totalSpeed;
    float avgSpeed;
    long interval;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_management);
        flag = false;
        view_latitude = (TextView) findViewById(R.id.latitude);
        view_longitude = (TextView) findViewById(R.id.longitude);
        view_distance = (TextView) findViewById(R.id.distance);
        view_speed = (TextView) findViewById(R.id.speed);

        startBtn = (Button) findViewById(R.id.start);
        stopBtn = (Button) findViewById(R.id.stop);
        updateBtn = (Button) findViewById(R.id.update);
        exitBtn = (Button) findViewById(R.id.exit);
        startBtn.setOnClickListener(listener);
        stopBtn.setOnClickListener(listener);
        updateBtn.setOnClickListener(listener);
        exitBtn.setOnClickListener(listener);

    }
    private OnClickListener listener = new OnClickListener(){
            // show location button click event

                // @Override

                // create class object
                    public void onClick(View v) {

                        switch (v.getId()) {
                            case R.id.start:
                                bindToService();
                                break;
                            case R.id.stop:
                                unBind();
                                break;
                            case R.id.update:
                                update_view();
                                break;
                            case R.id.exit:
                                break;
                            default:
                                break;
                        }
                    }
    };

    private void bindToService(){
        Intent intent = new Intent(AndroidGPSTrackingActivity.this,GPSTracker.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unBind(){
        if(flag == true){
            unbindService(mServiceConnection);
            flag = false;
        }
    }



    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            ServiceProxy = IGPSTrackerService.Stub.asInterface(service);
            Toast.makeText(getApplicationContext(), "Service Connected", Toast.LENGTH_LONG).show();
            flag = true;

        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Toast.makeText(getApplicationContext(), "Service Connection Lost", Toast.LENGTH_LONG).show();
            ServiceProxy = null;
        }
    };

    private void update_view(){
        try {
            if(flag) {
                double latitude = ServiceProxy.getLatitude();
                view_latitude.setText(String.valueOf(latitude));
                double longitude = ServiceProxy.getLongitude();
                view_longitude.setText(String.valueOf(longitude));
                float newDis = ServiceProxy.getDistance();
                totalDistance += newDis;
                view_distance.setText(String.valueOf(newDis));
                avgSpeed = ServiceProxy.getSpeed();
                view_speed.setText(String.valueOf(avgSpeed));
            }
        }catch(RemoteException e){
            e.printStackTrace();
        }

    }

}
