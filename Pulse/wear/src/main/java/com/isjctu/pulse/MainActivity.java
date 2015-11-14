package com.isjctu.pulse;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends WearableActivity implements SensorEventListener2, View.OnClickListener {

    private TextView textView;
    private ImageButton imageButtonClear;
    private ImageButton imageButtonOk;

    private Sensor hrSensor;
    private SensorManager sensorManager;

    private double hr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        textView = (TextView) findViewById(R.id.text);
        imageButtonClear = (ImageButton) findViewById(R.id.image_button_clear);
        imageButtonOk = (ImageButton) findViewById(R.id.image_button_ok);

        imageButtonClear.setOnClickListener(this);
        imageButtonOk.setOnClickListener(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        imageButtonClear.setVisibility(View.GONE);
        imageButtonOk.setVisibility(View.GONE);
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
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.image_button_clear) {
            if (sensorManager != null) {
                sensorManager.registerListener(this, hrSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } else if (view.getId() == R.id.image_button_ok) {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        }
    }
}
