package com.example.LocationLoggerProj.location_aidl;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GPSTracker extends Service implements LocationListener {

    private final Context mContext;
    // flag for GPS status
    private boolean isGPSEnabled = false;

    // flag for network status
    private boolean isNetworkEnabled = false;

    // flag for GPS status
    private boolean canGetLocation = false;
    private Location preLocation;
    private Location location; // location
    private double latitude; // latitude
    private double longitude; // longitude
    private long initialTime;
    private float totalDistance = 0;

    //List for the location
    private List<Location> locList;


    IGPSTrackerService ServiceProxy;
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 2; // 2 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 1; // 1 second

    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    //string for requestPermission()
    private static final int PERMISSION_FINE_LOCATION = 0;

    private static final int PERMISSION_INTERNET = 1;

    // Declaring a Location Manager
    protected LocationManager locationManager;
//    private PackageManager pm;
    AndroidGPSTrackingActivity ata;

    public GPSTracker() {
        this.mContext = null;
    }


    public GPSTracker(Context context) {
        this.mContext = context;
        getLocation();
    }

    public Location getLocation() {
        try {
            locationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;

                // First get location from Network Provider
                if (isNetworkEnabled) {

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }

                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {

                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    @Override
    public IBinder onBind(Intent intent) {
        locList = new ArrayList<Location>();
        getLocation();
        preLocation = location;
        initialTime = location.getTime();
        return myBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(getApplicationContext(), "Service Stop", Toast.LENGTH_LONG).show();
        writeToFile();
        return super.onUnbind(intent);
    }

    private void writeToFile(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date now = new Date();
        String strDate = format.format(now);
        File sdDir = Environment.getExternalStorageDirectory();
        File file = new File(sdDir, strDate + ".gpx");
        try {
            file.createNewFile();
            FileOutputStream fout = openFileOutput("GPSTracker.gpx",MODE_WORLD_WRITEABLE);
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();

            // root element
            Document doc = builder.newDocument();
            Element gpx = doc.createElement("gpx");
            doc.appendChild(gpx);

            //trk element
            Element trk = doc.createElement("trk");
            gpx.appendChild(trk);

            //trkseg
            Element trkseg = doc.createElement("trkseg");
            trk.appendChild(trkseg);

            //add location
            for(int i=0; i<locList.size(); i++){
                //set attributes
                Element trkpt = doc.createElement("trkpt");
                Element time = doc.createElement("time");
                trkpt.setAttribute("lat", String.valueOf(locList.get(i).getLatitude()));
                trkpt.setAttribute("lon", String.valueOf(locList.get(i).getLongitude()));

                //set time
                Date date = new Date(locList.get(i).getTime());
                time.setTextContent(format.format(date));

                //append children
                trkseg.appendChild(trkpt);
                trkpt.appendChild(time);
            }
            //write to the gpx file
            // Use a Transformer for output
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);

            fout.flush();
            fout.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private final IGPSTrackerService.Stub myBinder = new IGPSTrackerService.Stub() {
        public double getLatitude(){
            getLocation();
            double latitude = getLocationLatitude();
            return latitude;
        }
        public double getLongitude(){
            getLocation();
            double longitude = getLocationLongitude();
            return longitude;
        }
        public float getDistance(){
            float dis = 0;
            getLocation();
            if(preLocation != null ) {
                dis = location.distanceTo(preLocation);
            }
            totalDistance += dis;
            preLocation = location;
            return totalDistance;
        }
        public float getSpeed(){
            float speed = 0;
            getLocation();
            long interval = location.getTime() - initialTime;
            if (interval != 0) {
                speed = (totalDistance * 1000) / interval;
            }
            return speed;

        }
    };

    private double getLocationLatitude(){
            if(location != null){
                latitude = location.getLatitude();
            }
            // return latitude
            return latitude;
        }

    private double getLocationLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }

        // return latitude
        return longitude;
    }

    @Override
    public void onLocationChanged(Location location) {
        locList.add(location);
        getLocation();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }



}
