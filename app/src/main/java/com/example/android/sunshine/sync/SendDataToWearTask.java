package com.example.android.sunshine.sync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContentResolverCompat;
import android.util.Log;

import com.example.android.sunshine.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;

/**
 * Created by zeeshan on 3/13/2017.
 */

public class SendDataToWearTask extends AsyncTask<Void, Void, Void> implements GoogleApiClient
        .OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    public static final String TAG = SendDataToWearTask.class.getSimpleName();
    public static int dummyWeather = 0;
    WeakReference<Context> contextWeakReference;
    private GoogleApiClient mGoogleApiClient;

    {
        Log.d(TAG, "created");
    }

    public SendDataToWearTask(Context context) {
        contextWeakReference = new WeakReference<>(context);
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        mGoogleApiClient.blockingConnect();

        Uri uri = WeatherContract.WeatherEntry.buildWeatherUriWithDate(System.currentTimeMillis());
        Cursor cursor = ContentResolverCompat.query(contextWeakReference.get().getContentResolver(),
                uri, new String[]{
                        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID},
                null, null, null, null);
        int minWeather = 16, maxWeather = 16, iconType = 0;
        if (cursor.moveToFirst()) {
            minWeather = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry
                    .COLUMN_MIN_TEMP));
            maxWeather = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry
                    .COLUMN_MAX_TEMP));
            iconType = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry
                    .COLUMN_WEATHER_ID));
        }
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/sunshine-weather");
        putDataMapRequest.getDataMap().putInt("minWeather", minWeather);
        putDataMapRequest.getDataMap().putInt("maxWeather", maxWeather);
        putDataMapRequest.getDataMap().putInt("iconType", iconType);
        putDataMapRequest.getDataMap().putInt("dummyWeather", dummyWeather++);

        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        if (dataItemResult.getStatus().isSuccess()) {
                            Log.d(TAG, "minWeather value saved");
                        } else {
                            Log.d(TAG, "minWeather save failed");
                        }
                    }
                });
        return null;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected " + connectionHint);
        // Now you can use the Data Layer API

    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result);
    }
}
