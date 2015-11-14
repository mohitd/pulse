package com.isjctu.pulse;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

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

/**
 * Created by Drew on 11/14/2015.
 */
public class SyncService extends Service {

    private static class PushToServerTask extends AsyncTask<ArrayList<?>, Void, Integer> {

        Socket mySocket;

        @Override
        protected Integer doInBackground(ArrayList<?>... params) {
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
                ByteBuffer myBuffer = ByteBuffer.allocate(100);
                myBuffer.putChar('z');
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
        //String id = ""; //something in
        int[] accuracyArr = new int[size]; //something in

        HeartBeat curr;
        for(int i = 0; i < size; i++) {
            curr = new HeartBeat(rateArr[i], timeArr[i], accuracyArr[i]);
            myList.add(curr);
        }
        PushToServerTask myTask = new PushToServerTask();
        myTask.execute();
        return START_NOT_STICKY;
    }

}
