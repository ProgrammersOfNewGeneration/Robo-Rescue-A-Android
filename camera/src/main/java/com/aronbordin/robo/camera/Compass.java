package com.aronbordin.robo.camera;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * @author Aron Bordin <aron.bordin@gmail.com>
 * Android compass helper
 */
public class Compass implements SensorEventListener{
    private float mDegree;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private static Compass __compass = null;
    private static  MainActivity parent = null;
    private float[] mGravity;
    private float[] mGeomagnetic;
    private float[] mOrientation = new float[3];

    public static Compass getInstance(MainActivity parent){
        return __compass == null ? new Compass(parent) : __compass;
    }
    private Compass(MainActivity parent){
        this.parent = parent;
        mSensorManager = (SensorManager)parent.getSystemService(parent.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        __compass = this;
    }

    public float getDirection(){
        float azimuthInDegress = (float)Math.toDegrees(mOrientation[0]);
        if (azimuthInDegress < 0.0f) {
            azimuthInDegress += 180.0f;
        }
        return azimuthInDegress;

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = sensorEvent.values;
        if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = sensorEvent.values;

        if(mGravity != null && mGeomagnetic != null){
            float r[] = new float[9];
            float i[] = new float[9];

            boolean ok = SensorManager.getRotationMatrix(r, i, mGravity, mGeomagnetic);
            if(ok){
                mOrientation = new float[3];
                SensorManager.getOrientation(r, mOrientation);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {


    }
}
