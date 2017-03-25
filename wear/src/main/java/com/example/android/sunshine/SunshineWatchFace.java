/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    public static final String TAG = SunshineWatchFace.class.getSimpleName();
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create("sans-serif-light", Typeface.NORMAL);
    private static final Typeface BOLD_TYPEFACE =
            Typeface.create("sans-serif-light", Typeface.BOLD);
    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint textLargeBoldPaint, textLargePaint, textSmallPaint, textSmallFadedPaint,
                textSmallerFadedPaint;
        boolean mAmbient;
        Calendar mCalendar;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        float mXOffset, mYOffset, timeYOffset, dateYOffset, barYOffset, weatherYOffset, barLength;
        String timeHour, timeMin, date, maxWeatherString, minWeatherString;
        int iconType, minWeather, maxWeather;
        float textSmallSize, textLargeSize, barHeight, textGap;
        Bitmap icon;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        SimpleDateFormat dateFormat;
        Rect textBounds = new Rect();

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            dateFormat = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.ENGLISH);
            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SunshineWatchFace.this.getResources();

            barLength = resources.getDimension(R.dimen.digital_bar_lenght);
            barHeight = resources.getDimension(R.dimen.digital_bar_height);
            textGap = resources.getDimension(R.dimen.digital_text_gap);

            mCalendar = Calendar.getInstance();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            textLargeBoldPaint = createTextPaint(resources.getColor(R.color.text_white));
            textLargeBoldPaint.setTypeface(BOLD_TYPEFACE);

            textLargePaint = createTextPaint(resources.getColor(R.color.text_white));

            textSmallPaint = createTextPaint(resources.getColor(R.color.text_white));

            textSmallFadedPaint = createTextPaint(resources.getColor(R.color.text_lightblue1));

            textSmallerFadedPaint = createTextPaint(resources.getColor(R.color.text_lightblue1));


            SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences
                    (getApplicationContext());
            minWeather = spf.getInt("minWeather", -1);
            maxWeather = spf.getInt("maxWeather", -1);
            iconType = spf.getInt("iconType", -1);
            icon = getIcon();
            spf.registerOnSharedPreferenceChangeListener(Engine.this);
        }

        private Bitmap getIcon() {
            switch (iconType) {
                case 1:
                    return BitmapFactory.decodeResource(getResources(), R.drawable.ic_clear);
                default:
                    return BitmapFactory.decodeResource(getResources(), R.drawable.ic_cloudy);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            switch (s) {
                case "minWeather":
                    minWeather = sharedPreferences.getInt(s, -1);
                    break;
                case "maxWeather":
                    maxWeather = sharedPreferences.getInt(s, -1);
                    break;
                case "iconType":
                    iconType = sharedPreferences.getInt(s, -1);
                    icon = getIcon();
                    break;
            }
            Log.d(TAG, String.format(Locale.ENGLISH, "%d %d %d", maxWeatherString, minWeatherString, iconType));
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();

            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            mYOffset = resources.getDimension(isRound
                    ? R.dimen.digital_y_offset_round : R.dimen.digital_y_offset);

            textLargeSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_large_size_round : R.dimen.digital_text_large_size);
            float textSmallerSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_smaller_size_round : R.dimen.digital_text_smaller_size);
            textSmallSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            barHeight = resources.getDimension(R.dimen.digital_bar_height);
            textLargeBoldPaint.setTextSize(textLargeSize);
            textLargePaint.setTextSize(textLargeSize);
            textSmallPaint.setTextSize(textSmallSize);
            textSmallFadedPaint.setTextSize(textSmallSize);
            textSmallerFadedPaint.setTextSize(textSmallerSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    textLargeBoldPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            invalidate();
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            timeYOffset = mYOffset;


            // Draw HH:MM in ambient mode or HH:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            timeHour = String.format(Locale.ENGLISH, "%02d", mCalendar.get(Calendar.HOUR));
            timeMin = String.format(Locale.ENGLISH, "%02d", mCalendar.get(Calendar.MINUTE));
            float colonXOffset = textLargePaint.measureText(":");
            //bold hours
            canvas.drawText(":", bounds.centerX() - colonXOffset / 2, timeYOffset, textLargePaint);

            canvas.drawText(timeHour, bounds.centerX() - textLargeBoldPaint.measureText(timeHour)
                            - colonXOffset / 2,
                    timeYOffset, textLargeBoldPaint);

            //normal colon and minute
            canvas.drawText(timeMin, bounds.centerX() + colonXOffset / 2, timeYOffset,
                    textLargePaint);

            if (!isInAmbientMode()) {
                textSmallerFadedPaint.getTextBounds("A", 0, 1, textBounds);
                dateYOffset = timeYOffset + textGap + textBounds.height();

                barYOffset = dateYOffset + 1.3f * textGap;

                textSmallFadedPaint.getTextBounds("A", 0, 1, textBounds);

                weatherYOffset = barYOffset + barHeight + 1.3f * textGap + textBounds.height();

//                Log.d(TAG, String.format("%f, %f, %f, %f, %f", timeYOffset, dateYOffset, barYOffset,
//                        weatherYOffset, textGap));

                date = dateFormat.format(mCalendar.getTime());
                maxWeatherString = getString(R.string.weather_string, maxWeather);
                minWeatherString = getString(R.string.weather_string, minWeather);

                canvas.drawText(date.toUpperCase(), bounds.centerX() - textSmallerFadedPaint.measureText(date)
                                / 2,
                        dateYOffset, textSmallerFadedPaint);

                canvas.drawLine(bounds.centerX() - barLength / 2, barYOffset, bounds.centerX() +
                                barLength / 2, barYOffset,
                        textSmallFadedPaint);

                canvas.drawText(maxWeatherString, bounds.centerX() - textSmallPaint.measureText
                                (maxWeatherString) / 2,
                        weatherYOffset,
                        textSmallPaint);
                canvas.drawText(minWeatherString, bounds.centerX() + textSmallPaint.measureText
                                (maxWeatherString) / 2 + 0.5f * textGap,
                        weatherYOffset,
                        textSmallFadedPaint);

                canvas.drawBitmap(icon, bounds.centerX() - textSmallPaint.measureText
                        (maxWeatherString) / 2 - 0.5f * textGap - icon.getWidth(), barYOffset +
                        barHeight + 0.5f * textGap, textSmallPaint);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
