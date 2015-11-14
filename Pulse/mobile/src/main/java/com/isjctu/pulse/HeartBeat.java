package com.isjctu.pulse;

/**
 * Created by Drew on 11/14/2015.
 */
public class HeartBeat {

    float rate;
    long time;
    double lat;
    double lon;

    public HeartBeat(float rateIn, long timeIn, double latIn, double lonIn) {
        rate = rateIn;
        time = timeIn;
        lat = latIn;
        lon = lonIn;
    }
}
