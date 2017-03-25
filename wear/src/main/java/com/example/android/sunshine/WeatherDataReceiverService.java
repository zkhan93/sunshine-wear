package com.example.android.sunshine;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by zeeshan on 3/23/2017.
 */

public class WeatherDataReceiverService extends WearableListenerService {
    public static final String TAG = WeatherDataReceiverService.class.getSimpleName();

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "wear:on Data Changes");
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences
                (getApplicationContext());
        for (DataEvent de : dataEventBuffer) {
            if (de.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(de.getDataItem()).getDataMap();
                String path = de.getDataItem().getUri().getPath();
                if (path.equals("/sunshine-weather")) {
                    int minWeather = dataMap.getInt("minWeather", -1);
                    int maxWeather = dataMap.getInt("maxWeather", -1);
                    int icon = dataMap.getInt("iconType", -1);
                    spf.edit().putInt("minWeather", minWeather)
                            .putInt("maxWeather", maxWeather).putInt("iconType", icon).apply();
                    break;
                }
            }
        }
    }
}
