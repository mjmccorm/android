package com.example.mjmccorm.flightsensors;

import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.TextView;

import org.json.JSONObject;
import org.json.JSONException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class ActivityCollection extends Activity implements SensorEventListener {
    private SensorManager mSensorManager;
    private SensorEventListener mSensorListener;
    //For GUI
    TextView txtPressure, txtAmbientTemperature, txtRelativeHumidity, txtLight, txtProximity;
    TextView txtAccelerometerX, txtAccelerometerY, txtAccelerometerZ;
    TextView txtGyroscopeX, txtGyroscopeY, txtGyroscopeZ;
    TextView txtMagneticFieldX, txtMagneticFieldY, txtMagneticFieldZ;

    //For storing sensor data
    private double[] mAccBuffer 		= new double[3];
    private double mAccTime = 0;
    private static final double NS2S = 1.0f / 1000000000.0f; // nanosec to sec

    //for sending data
    StringBuilder mStrBuilder = new StringBuilder(256);
    private String mSensordata;
    public static DatagramSocket mSocket = null;
    public static DatagramPacket mPacket = null;
    private String mIP_Address;
    private String mPort;


    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        //Not sure what this does
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //manually setup UDP header info
        //need to implement dynamic ip address/port settings
        mIP_Address = "192.168.1.130";
        mPort = "50000";

        //assign TextViews
        txtPressure = (TextView)findViewById(R.id.txtValPressure);
        txtAmbientTemperature = (TextView)findViewById(R.id.txtValAmbientTemperature);
        txtRelativeHumidity = (TextView)findViewById(R.id.txtValRelativeHumidity);
        txtLight = (TextView)findViewById(R.id.txtValLight);
        txtProximity = (TextView)findViewById(R.id.txtValProximity);
        txtAccelerometerX = (TextView)findViewById(R.id.txtValAccelerometerX);
        txtAccelerometerY = (TextView)findViewById(R.id.txtValAccelerometerY);
        txtAccelerometerZ = (TextView)findViewById(R.id.txtValAccelerometerZ);
        txtGyroscopeX = (TextView)findViewById(R.id.txtValGyroscopeX);
        txtGyroscopeY = (TextView)findViewById(R.id.txtValGyroscopeY);
        txtGyroscopeZ = (TextView)findViewById(R.id.txtValGyroscopeZ);
        txtMagneticFieldX = (TextView)findViewById(R.id.txtValMagneticFieldX);
        txtMagneticFieldY = (TextView)findViewById(R.id.txtValMagneticFieldY);
        txtMagneticFieldZ = (TextView)findViewById(R.id.txtValMagneticFieldZ);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //UDP
        start_UDP_Stream();
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        Sensor sensor = event.sensor;
        //Set timestamp
        double timestamp_sec = event.timestamp * NS2S;

        if(sensor.getType() == Sensor.TYPE_PRESSURE) {
            float millibars_of_pressure = event.values[0];
            // Do something with this sensor data.
            txtPressure.setText(String.format("%6.1f", millibars_of_pressure));
        }else if(sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
            float c_ambient_temperature = event.values[0];
            txtAmbientTemperature.setText(String.format("%6.1f",c_ambient_temperature));
        }else if(sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY){
            float relative_humidity = event.values[0];
            txtRelativeHumidity.setText(String.format("%6.1f",relative_humidity));
        }else if(sensor.getType() == Sensor.TYPE_LIGHT){
            float val_light = event.values[0];
            txtLight.setText("" + val_light);
        }else if(sensor.getType() == Sensor.TYPE_PROXIMITY){
            float val_proximity = event.values[0];
            txtProximity.setText(String.format("%6.1f",val_proximity));
        }else if(sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float accelerometer_x = event.values[0];
            float accelerometer_y = event.values[1];
            float accelerometer_z = event.values[2];
            mAccBuffer[0] = event.values[0];
            mAccBuffer[1] = event.values[1];
            mAccBuffer[2] = event.values[2];
            mAccTime = timestamp_sec;

            txtAccelerometerX.setText(String.format("%6.3f",accelerometer_x));
            txtAccelerometerY.setText(String.format("%6.3f",accelerometer_y));
            txtAccelerometerZ.setText(String.format("%6.3f",accelerometer_z));
        }else if(sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float gyroscope_x = event.values[0];
            float gyroscope_y = event.values[1];
            float gyroscope_z = event.values[2];
            txtGyroscopeX.setText(String.format("%6.3f",gyroscope_x));
            txtGyroscopeY.setText(String.format("%6.3f",gyroscope_y));
            txtGyroscopeZ.setText(String.format("%6.3f",gyroscope_z));
        }else if(sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            float magnetic_field_x = event.values[0];
            float magnetic_field_y = event.values[1];
            float magnetic_field_z = event.values[2];
            txtMagneticFieldX.setText(String.format("%6.3f",magnetic_field_x));
            txtMagneticFieldY.setText(String.format("%6.3f", magnetic_field_y));
            txtMagneticFieldZ.setText(String.format("%6.3f",magnetic_field_z));
        }

        //Need some sort of method to update an array of sensor data
        //ie
        //sensors[0] = timestamp
        //sensors[1] = pressure
        //sensors[2] = temp
        //se
        //The sensor data above should be appended into a string
        //Also should send a timestamp with each

        try {
            JSONObject jsonAdd = new JSONObject(); // we need another object to store the address
            jsonAdd.put("timestamp", mAccTime);
            jsonAdd.put("AccX", String.format("%6.3f", mAccBuffer[0]));
            jsonAdd.put("AccY", String.format("%6.3f", mAccBuffer[1]));
            jsonAdd.put("AccZ", String.format("%6.3f", mAccBuffer[2]));
            mSensordata = jsonAdd.toString();

        }catch(JSONException e) {
            System.out.println("JSON Exception");
        }
        //mStrBuilder.setLength(0);
        //mStrBuilder.append(mAccTime);
        //mStrBuilder.append("X" + );
        //mStrBuilder.append("Y" + );
        //mStrBuilder.append("Z" +);
        //mSensordata = mStrBuilder.toString();
        new UDPThread(mSensordata).send();

        /*

        if(mUDP_SD_Stream.isChecked())
        {
            new UDPThread(mSensordata).send();
            if (SD_Card_Setup.getmBufferedwriter() != null)
            {
                SD_Card_Setup.write(mSensordata);
            }
        }
        else if(mUDP_Stream.isChecked())
        {
            new UDPThread(mSensordata).send();
        }
        else if(mSD_Card_Stream.isChecked())
        {
            if (SD_Card_Setup.getmBufferedwriter() != null)
            {
                SD_Card_Setup.write(mSensordata);
            }
        }
        */
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    private boolean start_UDP_Stream()
    {
        System.out.println("Starting UDP Stream");
        InetAddress client_address = null;
        try {
            client_address = InetAddress.getByName(mIP_Address);
        } catch (UnknownHostException e) {
            showDialog(R.string.error_invalidaddr);
            System.out.println("Invalid Address");
            return false;
        }
        try {
            mSocket = new DatagramSocket();
            mSocket.setReuseAddress(true);
        } catch (SocketException e) {
            mSocket = null;
            showDialog(R.string.error_neterror);
            System.out.println("Invalid Socket");
            return false;}

        byte[] buf = new byte[256];
        int port;
        try {
            port = Integer.parseInt(mPort);
            mPacket = new DatagramPacket(buf, buf.length, client_address, port);
        } catch (Exception e) {
            mSocket.close();
            mSocket = null;
            showDialog(R.string.error_neterror);
            System.out.println("Invalid Packet");
            return false;
        }

        return true;
    }

    private void stop_UDP_Stream()
    {
        if (mSocket != null)
            mSocket.close();
        mSocket = null;
        mPacket = null;

    }
}