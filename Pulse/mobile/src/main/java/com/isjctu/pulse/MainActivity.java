package com.isjctu.pulse;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, DataApi.DataListener, MessageApi.MessageListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST = 1;

    private static final String PATH = "/hr-data";

    private static final String KEY_TIMESTAMP = "com.isjctu.pulse.data.timestamp";
    private static final String KEY_HEART_RATE = "com.isjctu.pulse.data.heart_rate";
    private static final String KEY_ACCURACY = "com.isjctu.pulse.data.accuracy";

    public static final String EXTRA_TIMESTAMPS = "com.isjctu.pulse.extra.TIMESTAMP";
    public static final String EXTRA_HEART_RATE = "com.isjctu.pulse.extra.HEART_RATE";
    public static final String EXTRA_ACCURACY = "com.isjctu.pulse.extra.ACCURACY";
    public static final String EXTRA_LATITUDE = "com.isjctu.pulse.extra.LAT";
    public static final String EXTRA_LONGITUDE = "com.isjctu.pulse.extra.LON";

    private GoogleApiClient apiClient;
    private Location currentLocation;

    private ServerBroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, ">>>>onCreate(Bundle)");

        broadcastReceiver = new ServerBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(ServerBroadcastReceiver.ACTION_PUSH_TO_SERVER);
        registerReceiver(broadcastReceiver, intentFilter);

        apiClient = new GoogleApiClient.Builder(this, this, this).addApiIfAvailable(LocationServices.API).addApiIfAvailable(Wearable.API).build();

        apiClient.connect();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSION_REQUEST);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, ">>>>onConnected(...)");
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(apiClient);

        Wearable.DataApi.addListener(apiClient, this);
        Wearable.MessageApi.addListener(apiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, ">>>>onSuspended(...)");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, ">>>>onConnectionFailed(...)");
    }

    @Override
    protected void onDestroy() {
        apiClient.disconnect();
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
        Wearable.DataApi.removeListener(apiClient, this);
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    apiClient.connect();
                } else {
                    Toast.makeText(this, "YOU NEED TO LET ME KNOW WHERE YOU ARE!!!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.i(TAG, ">>>>onDataChanged(...)");
        for (DataEvent event : dataEventBuffer) {
            Log.i("[[[[MAIN_ACTIVITY]]]]", "List: " + dataEventBuffer.toString());
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(PATH) == 0) {
                    DataMap map = DataMapItem.fromDataItem(item).getDataMap();
                    long[] timestamps  = map.getLongArray(KEY_TIMESTAMP);
                    float[] heartRates = map.getFloatArray(KEY_HEART_RATE);
                    ArrayList<Integer> accuracies = map.getIntegerArrayList(KEY_ACCURACY);

                    Intent intent = new Intent();
                    intent.setAction(ServerBroadcastReceiver.ACTION_PUSH_TO_SERVER);
                    intent.putExtra(EXTRA_TIMESTAMPS, timestamps);
                    intent.putExtra(EXTRA_HEART_RATE, heartRates);
                    intent.putExtra(EXTRA_ACCURACY, accuracies);
                    intent.putExtra(EXTRA_LATITUDE, currentLocation.getLatitude());
                    intent.putExtra(EXTRA_LONGITUDE, currentLocation.getLongitude());
                    sendBroadcast(intent);
            }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, ">>>>onMessageReceived(...)");
        byte[] bytes = messageEvent.getData();
        Log.i(TAG, "" + bytes[0]);

//        Toast.makeText(this, "MESSAGE RECEIVED", Toast.LENGTH_SHORT).show();
    }
}
