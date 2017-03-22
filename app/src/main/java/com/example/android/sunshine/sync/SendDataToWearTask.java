package com.example.android.sunshine.sync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.sunshine.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;

/**
 * Created by zeeshan on 3/13/2017.
 */

public class SendDataToWearTask extends AsyncTask<Void, Void, Void> {
    public static final String TAG = SendDataToWearTask.class.getSimpleName();
    WeakReference<Context> contextWeakReference;
    private GoogleApiClient mGoogleApiClient;
    {
        Log.d(TAG,"created");
    }
    public SendDataToWearTask(Context context) {
        contextWeakReference = new WeakReference<Context>(context);
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes
                (mGoogleApiClient).await();
        Uri uri = WeatherContract.WeatherEntry.buildWeatherUriWithDate(System.currentTimeMillis());
        String data="16,29,1";
        for (Node node : nodes.getNodes()) {
            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "weather", data.getBytes())
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.d(TAG, "failed to send message with status code : " +
                                        "" + sendMessageResult.getStatus());
                            }else{
                                Log.d(TAG, "data message sent to wear");
                            }
                        }
                    });
        }
        return null;
    }
}
