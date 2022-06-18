package com.daily_step_counter;

import static android.content.Context.MODE_PRIVATE;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.content.SharedPreferences;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;



public class PedometerImpl implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor stepCounter;
    private ReactApplicationContext reactContext;

    private static final String STEP_COUNT_PREFERENCE_KEY = "step-count";
    private static final String LAST_UPDATED_AT_KEY = "last-updated-at";
    private static final String CORRECTION_KEY = "correction";
    private static final String DATE_FORMAT = "yyyy-MM-dd";


    private String lastUpdatedDate;
    private int correction = 0;
    private boolean listening = false;
    private String listeningFromDate;
    private int listeningFromValue = 0;

    public PedometerImpl(ReactApplicationContext reactContext) {
        sensorManager = (SensorManager) reactContext.getSystemService(reactContext.SENSOR_SERVICE);
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        this.reactContext = reactContext;
        String currentDateString = getDate(Calendar.getInstance(Locale.KOREA).getTimeInMillis());
        this.lastUpdatedDate = getStepCount(LAST_UPDATED_AT_KEY) == null ? currentDateString : getStepCount(LAST_UPDATED_AT_KEY);
        String todayStepCount = getStepCount(currentDateString);
        String queriedCorrection = getStepCount(CORRECTION_KEY);
        this.correction = (queriedCorrection == null ? (todayStepCount == null ? 0 : -Integer.parseInt(todayStepCount)) : Integer.parseInt(queriedCorrection));
    }

    public void start() {
        if ((stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)) != null) {
            sensorManager.unregisterListener(this); // 앱 껐다 켰을때를 고려해서...?
            this.listeningFromValue = 0;
            this.listeningFromDate = null;
            this.listening = false;
            sensorManager.registerListener(this, this.stepCounter, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    public void start(long date) {
        if ((stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)) != null) {
            sensorManager.unregisterListener(this); // 앱 껐다 켰을때를 고려해서...?
            Calendar cal = Calendar.getInstance(Locale.KOREA);
            cal.add(Calendar.DAY_OF_YEAR, -1);
            List<Date> list = getDatesBetweenUsingJava7(dateFromTimestamp(date), cal.getTime());

            Iterator<Date> it = list.iterator();

            int sum = 0;

            while (it.hasNext()) {
                String queryResult = getStepCount(getDate(it.next().getTime()));
                int count = queryResult == null ? 0 : Integer.parseInt(queryResult);
                sum += count;
            }
            this.listeningFromValue = sum;
            this.listeningFromDate = getDate(dateFromTimestamp(date).getTime());
            this.listening = sensorManager.registerListener(this, this.stepCounter, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    public void stop() {
        if (this.listening) {
            sensorManager.unregisterListener(this);
            this.listening = false;
            this.listeningFromValue = 0;
            this.listeningFromDate = null;
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;

        int currentStepCount = (int) event.values[0];
        String currentDate = getDate(getTimestampFromBootTimeStamp(event.timestamp));

        if (!currentDate.equals(this.lastUpdatedDate)) {
            this.lastUpdatedDate = currentDate;
            setStepCount(LAST_UPDATED_AT_KEY, currentDate);
            this.correction = currentStepCount;
            setStepCount(CORRECTION_KEY, String.valueOf(currentStepCount));
        }

        int todayStepCount = currentStepCount - this.correction;

        setStepCount(currentDate, String.valueOf(todayStepCount));

        if (listening) {
            WritableMap params = Arguments.createMap();
            params.putString("startDate", this.listeningFromDate);
            params.putString("endDate", currentDate);
            params.putInt("numberOfSteps", todayStepCount + this.listeningFromValue);
            params.putString("date", currentDate);
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("stepCountChanged", params);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void queryPedometerDataFromDate(Long from, Long end, Callback callback) {
        try {
            Date startDate = dateFromTimestamp(from);
            Date endDate = dateFromTimestamp(end);
            List<Date> list = getDatesBetweenUsingJava7(startDate, endDate);

            Iterator<Date> it = list.iterator();
            int sum = 0;

            while (it.hasNext()) {
                String queryResult = getStepCount(getDate(it.next().getTime()));
                int count = queryResult == null ? 0 : Integer.parseInt(queryResult);
                sum += count;
            }


            WritableMap result = Arguments.createMap();

            result.putString("startDate", getDate(startDate.getTime()));
            result.putString("endDate", getDate(endDate.getTime()));
            result.putString("date", getDate(endDate.getTime()));
            result.putInt("numberOfSteps", sum);
            callback.invoke(null, result);

        } catch (Exception e) {
            callback.invoke(e);
        }

    }


    public String getStepCount(String key) {
        return reactContext.getSharedPreferences(STEP_COUNT_PREFERENCE_KEY, MODE_PRIVATE).getString(key, null);
    }

    public void setStepCount(String key, String value) {
        SharedPreferences preferences = reactContext.getSharedPreferences(STEP_COUNT_PREFERENCE_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private String getDate(long time) {
        String date = DateFormat.format(DATE_FORMAT, dateFromTimestamp(time)).toString();
        return date;
    }

    private long getTimestampFromBootTimeStamp(long bootTimeStamp) {
        return System.currentTimeMillis() + ((bootTimeStamp - SystemClock.elapsedRealtimeNanos()) / 1000000L);
    }

    private static List getDatesBetweenUsingJava7(Date startDate, Date endDate) {
        List datesInRange = new ArrayList<>();
        Calendar calendar = getCalendarWithoutTime(startDate);
        Calendar endCalendar = getCalendarWithoutTime(endDate);
        endCalendar.add(Calendar.DAY_OF_YEAR, 1);
        while (calendar.before(endCalendar)) {
            Date result = calendar.getTime();
            datesInRange.add(result);
            calendar.add(Calendar.DATE, 1);
        }

        return datesInRange;
    }

    private static Calendar getCalendarWithoutTime(Date date) {
        Calendar calendar = new GregorianCalendar(Locale.KOREA);
        calendar.setTime(date);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private Date dateFromTimestamp(long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.KOREA);
        cal.setTimeInMillis(timestamp);
        return cal.getTime();
    }

}


