package com.isjctu.pulse;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class MainActivity extends WearableActivity implements SensorEventListener2, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "Wear." + MainActivity.class.getSimpleName();
    private static final String PATH = "/hr-data";

    private static final String KEY_TIMESTAMP = "com.isjctu.pulse.data.timestamp";
    private static final String KEY_HEART_RATE = "com.isjctu.pulse.data.heart_rate";
    private static final String KEY_ACCURACY = "com.isjctu.pulse.data.accuracy";
    private static final int MAX_SIZE = 100;

    private TextView textView;
    private ImageButton imageButtonClear;
    private ImageButton imageButtonOk;

    private Sensor hrSensor;
    private SensorManager sensorManager;

    private double hr;

    private Node node;

    private GoogleApiClient apiClient;

    private ArrayList<Float> heartRates;
    private ArrayList<Long> timeStamps;
    private ArrayList<Integer> accuracies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        heartRates = new ArrayList<>();
        timeStamps = new ArrayList<>();
        accuracies = new ArrayList<>();

        textView = (TextView) findViewById(R.id.text);
        imageButtonClear = (ImageButton) findViewById(R.id.image_button_clear);
        imageButtonOk = (ImageButton) findViewById(R.id.image_button_ok);

        imageButtonClear.setOnClickListener(this);
        imageButtonOk.setOnClickListener(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        apiClient = new GoogleApiClient.Builder(this, this, this).addApi(Wearable.API).build();
        apiClient.connect();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        imageButtonClear.setVisibility(View.GONE);
        imageButtonOk.setVisibility(View.GONE);
        textView.setTextSize(32);
        textView.getPaint().setAntiAlias(false);
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        textView.setText(String.valueOf(hr));
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        imageButtonClear.setVisibility(View.VISIBLE);
        imageButtonOk.setVisibility(View.VISIBLE);
        textView.setTextSize(22);
        textView.getPaint().setAntiAlias(true);
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE && sensorEvent.values[0] != 0 && !isAmbient()) {
            textView.setText(String.valueOf(sensorEvent.values[0]));
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE && sensorEvent.values[0] != 0) {
            hr = sensorEvent.values[0];
        }

        if (heartRates.size() > MAX_SIZE) {
            sendData();
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE && sensorEvent.values[0] != 0) {
            timeStamps.add(System.currentTimeMillis());
            heartRates.add(sensorEvent.values[0]);
            accuracies.add(sensorEvent.accuracy);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.image_button_clear) {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        } else if (view.getId() == R.id.image_button_ok) {
            textView.setText("Getting HR...");
            if (sensorManager != null) {
                sensorManager.registerListener(this, hrSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        apiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "C22onnected!");
        Wearable.NodeApi.getConnectedNodes(apiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for (Node aNode : getConnectedNodesResult.getNodes()) {
                    node = aNode;
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "Suspended...");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection Failed!");
    }

    private void sendData() {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH);

        long[] timeStampArray = new long[timeStamps.size()];
        for (int i = 0; i < timeStampArray.length; i++) {
            timeStampArray[i] = timeStamps.get(i);
        }

        float[] heartRateArray = new float[heartRates.size()];
        for (int i = 0; i < heartRateArray.length; i++) {
            heartRateArray[i] = heartRates.get(i);
        }

        putDataMapRequest.getDataMap().putLong("ts", System.currentTimeMillis());
        putDataMapRequest.getDataMap().putLongArray(KEY_TIMESTAMP, timeStampArray);
        putDataMapRequest.getDataMap().putFloatArray(KEY_HEART_RATE, heartRateArray);
        putDataMapRequest.getDataMap().putIntegerArrayList(KEY_ACCURACY, accuracies);

        byte[] bytes = new byte[] { 1 };
        Wearable.MessageApi.sendMessage(apiClient, node.getId(), PATH, bytes).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                if (sendMessageResult.getStatus().isSuccess()) Log.i(TAG, "SUCCESS MESSAGE!");
            }
        });

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(apiClient, putDataRequest);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if (dataItemResult.getStatus().isSuccess()) Log.i(TAG, "SUCCESS!");
            }
        });

        timeStamps.clear();
        heartRates.clear();
        accuracies.clear();
    }
}
