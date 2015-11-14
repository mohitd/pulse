package com.isjctu.pulse;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Drew on 11/14/2015.
 */
public class SyncService extends Service {

    private static class PushToServerTask extends AsyncTask<ArrayList<HeartBeat>, Void, Integer> {

        Socket mySocket;

        @Override
        protected Integer doInBackground(ArrayList<HeartBeat>... params) {
            try {

                mySocket = new Socket("54.152.69.195", 6969);
                Log.e("Created Socket", "");
                String result = "false";
                if(mySocket.isConnected()) {
                    result = "true";
                }
                Log.e("Connection: ", result);
                OutputStream out = mySocket.getOutputStream();
                InputStream in = mySocket.getInputStream();
                ByteBuffer myBuffer = ByteBuffer.allocate(10000);



                JSONArray jArray = new JSONArray();
                for(HeartBeat obj : params[0]) {
                    JSONObject curr = new JSONObject();
                    curr.put("rate", obj.rate);
                    curr.put("time", obj.time);
                    curr.put("accuracy", obj.accuracy);
                    jArray.put(curr);
                }
                JSONObject topObj = new JSONObject();

                topObj.put("requestType", "Send");
                topObj.put("dataSet", jArray);


                myBuffer.put(topObj.toString().getBytes("utf-8" ));
                out.write(myBuffer.array());
                out.flush();
                Log.e("Success", "made it");
                byte[] feedback = new byte[100];
                in.read(feedback);
                ByteBuffer inBuffer = ByteBuffer.allocate(100);
                inBuffer.put(feedback);
                Log.e("Feedback: ", new String(inBuffer.array()));
                out.close();
                in.close();

            } catch (IOException e) {
                Log.e("Error", "Did not connect to server: " + e.getClass().getCanonicalName());
            } catch (JSONException e) {

            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            try {
                mySocket.close();
            } catch (Exception e) {

            }
            super.onPostExecute(integer);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final int size = 100;
        ArrayList<HeartBeat> myList = new ArrayList<>();

        //Get stuff in
        //Expect 3 arrays and a string
        float[] rateArr = new float[size]; //something in
        long[] timeArr = new long[size]; //something in
        ArrayList<Integer> accAL = new ArrayList<>();
        //String id = ""; //something in
        ArrayList<Float> rateAL = new ArrayList<>();
        for(float obj : rateArr) {
            rateAL.add(obj);
        }
        ArrayList<Long> timeAL = new ArrayList<>();
        for(long obj : timeArr) {
            timeAL.add(obj);
        }

        //call method in main activity to populate arraylists

        HeartBeat curr;
        for(int i = 0; i < size; i++) {
            //curr = new HeartBeat(rateAL.get(i), timeAL.get(i), accAL.get(i));
            curr = new HeartBeat(0, 0, 0);
            myList.add(curr);
        }
        PushToServerTask myTask = new PushToServerTask();
        myTask.execute(myList);
        return START_NOT_STICKY;
    }

}
