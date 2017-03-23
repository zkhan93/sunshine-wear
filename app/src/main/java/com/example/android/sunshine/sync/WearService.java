package com.example.android.sunshine.sync;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 *
 * Created by zeeshan on 3/23/2017.
 */

public class WearService extends WearableListenerService {
    public static final String TAG = WearService.class.getSimpleName();

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "some Data changes:");
        for (DataEvent de : dataEventBuffer) {
            if (de.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(de.getDataItem()).getDataMap();
                String path = de.getDataItem().getUri().getPath();
                if (path.equals("/sunshine-weather")) {
                    boolean sendWeatherData = dataMap.getBoolean("sendData", false);
                    Log.d(TAG, "sendData:" + sendWeatherData);
                    if (sendWeatherData)
                        new SendDataToWearTask(this).execute();
                    break;
                }
            }
        }
    }
}
