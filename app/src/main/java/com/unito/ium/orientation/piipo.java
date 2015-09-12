package com.unito.ium.orientation;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class piipo extends Activity implements SensorEventListener {

    public static Float azimut;  // View to draw a compass
    public static Float pitch;
    public static Float roll;
    public static  SensorManager mSensorManager;
    public static  Sensor accelerometer;
    public static  Sensor magnetometer;

    private static final int FROM_RADS_TO_DEGS = -57;

    private static final int SENSOR_DELAY = 500 * 1000; // 500ms


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            Log.d("Sensors", "" + sensor.getName());
            mSensorManager.registerListener(this, sensor, SENSOR_DELAY);
        }
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //    mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        //   mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d("onCreate", "fine");
    }

    protected void onResume() {
        super.onResume();
        Log.d("RESUME", " "+ mSensorManager + " "+ accelerometer);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);;
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    private List<float[]> mRotHist = new ArrayList<float[]>();
    private int mRotHistIndex;
    // Change the value so that the azimuth is stable and fit your requirement
    private int mHistoryMaxLength = 10;
    float[] mGravity;
    float[] mMagnetic;
    float[] mRotationMatrix = new float[9];
    // the direction of the back camera, only valid if the device is tilted up by
    // at least 25 degrees.
    public static float mFacing = Float.NaN;
    public static float inclination = Float.NaN;

    public static final float TWENTY_FIVE_DEGREE_IN_RADIAN = 0.436332313f;
    public static final float ONE_FIFTY_FIVE_DEGREE_IN_RADIAN = 2.7052603f;

    public void onSensorChanged(SensorEvent event) {
        Log.d("onSensorChanged"," "+ event.sensor.getName());
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)//TYPE_GRAVITY)
        {
            mGravity = event.values.clone();
        }
        else
        {
            mMagnetic = event.values.clone();
        }

        if (mGravity != null && mMagnetic != null)
        {
            if (SensorManager.getRotationMatrix(mRotationMatrix, null, mGravity, mMagnetic))
            {
                // inclination is the degree of tilt by the device independent of orientation (portrait or landscape)
                // if less than 25 or more than 155 degrees the device is considered lying flat
                inclination = (float) Math.acos(mRotationMatrix[8]);
                if (inclination < TWENTY_FIVE_DEGREE_IN_RADIAN
                        || inclination > ONE_FIFTY_FIVE_DEGREE_IN_RADIAN)
                {
                    // mFacing is undefined, so we need to clear the history
                    clearRotHist();
                    mFacing = Float.NaN;
                }
                else
                {
                    setRotHist();
                    // mFacing = azimuth is in radian
                    mFacing = findFacing();
                }
                // ALTRO
                float orientation[] = new float[3];
                SensorManager.getOrientation(mRotationMatrix, orientation); // orientation contains: azimut, pitch and roll
                azimut = orientation[0];
                pitch = orientation[1];
                roll = orientation[2];

                ((TextView)findViewById(R.id.pitch)).setText("Pitch: "+(int)(FROM_RADS_TO_DEGS*pitch));
                ((TextView)findViewById(R.id.azimut)).setText("Azimut: "+(int)(FROM_RADS_TO_DEGS*azimut));
                ((TextView)findViewById(R.id.roll)).setText("Roll: "+(int)(FROM_RADS_TO_DEGS*roll));
            }
        }
    }

    private void update(float[] vectors) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
        int worldAxisX = SensorManager.AXIS_X;
        int worldAxisZ = SensorManager.AXIS_Z;
        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
        float pitch = orientation[1] * FROM_RADS_TO_DEGS;
        float roll = orientation[2] * FROM_RADS_TO_DEGS;
        Log.d("UPDATE", "pitch"+pitch+ " roll "+ roll);

        ((TextView)findViewById(R.id.pitch)).setText("Pitch: "+(int)(57*pitch));
        ((TextView)findViewById(R.id.roll)).setText("Roll: "+(int)(57*roll));
    }

    private void clearRotHist()
    {
        //   if (DEBUG) {Log.d(TAG, "clearRotHist()");}
        mRotHist.clear();
        mRotHistIndex = 0;
    }

    private void setRotHist()
    {
        //   if (DEBUG) {Log.d(TAG, "setRotHist()");}
        float[] hist = mRotationMatrix.clone();
        if (mRotHist.size() == mHistoryMaxLength)
        {
            mRotHist.remove(mRotHistIndex);
        }
        mRotHist.add(mRotHistIndex++, hist);
        mRotHistIndex %= mHistoryMaxLength;
    }

    private float findFacing()
    {
        //  if (DEBUG) {Log.d(TAG, "findFacing()");}
        float[] averageRotHist = average(mRotHist);
        return (float) Math.atan2(-averageRotHist[2], -averageRotHist[5]);
    }

    public float[] average(List<float[]> values)
    {
        float[] result = new float[9];
        for (float[] value : values)
        {
            for (int i = 0; i < 9; i++)
            {
                result[i] += value[i];
            }
        }

        for (int i = 0; i < 9; i++)
        {
            result[i] = result[i] / values.size();
        }

        return result;
    }

}
/*
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;

    private static final int SENSOR_DELAY = 500 * 1000; // 500ms
    private static final int FROM_RADS_TO_DEGS = -57;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY);
        } catch (Exception e) {
            Toast.makeText(this, "Hardware compatibility issue", Toast.LENGTH_LONG).show();
        }
        Log.d("onCreate", "ciao");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("onSensorChanged", event.sensor.toString());
        if (event.sensor == mRotationSensor) {
            if (event.values.length > 4) {
                float[] truncatedRotationVector = new float[4];
                System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                update(truncatedRotationVector);
            } else {
                update(event.values);
            }
        }
    }

    private void update(float[] vectors) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
        int worldAxisX = SensorManager.AXIS_X;
        int worldAxisZ = SensorManager.AXIS_Z;
        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
        float pitch = orientation[1] * FROM_RADS_TO_DEGS;
        float roll = orientation[2] * FROM_RADS_TO_DEGS;
        ((TextView)findViewById(R.id.pitch)).setText("Pitch: "+pitch);
        ((TextView)findViewById(R.id.roll)).setText("Roll: "+roll);
    }

}
*/