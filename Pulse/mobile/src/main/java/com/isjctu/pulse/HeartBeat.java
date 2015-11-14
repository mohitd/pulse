package com.isjctu.pulse;

/**
 * Created by Drew on 11/14/2015.
 */
public class HeartBeat {

    float rate;
    long time;
    //String id;
    int accuracy;

    public HeartBeat(float rateIn, long timeIn, int accuracyIn) {
        rate = rateIn;
        time = timeIn;
        //id = idIn;
        accuracy = accuracyIn;
    }
}
