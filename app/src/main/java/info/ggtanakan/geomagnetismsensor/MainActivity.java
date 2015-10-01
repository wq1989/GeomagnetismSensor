package info.ggtanakan.geomagnetismsensor;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.*;

public class MainActivity extends Activity implements SensorEventListener{
    final private int COUNT_MAX = 1000;
    private SensorManager sensorManager;
    private TextView textView;
    private ProgressBar progressBar;
    private int counter = 0;
    private Button startButton;
    private EditText editText;
    private File file;
    private FileOutputStream fileOutputStream;
    private OutputStreamWriter outputStreamWriter;
    private long startTime;
    private long endTime;
    private float[] magnetic = new float[3];
    private float[] gravity = new float[3] ;
    private float[] globalValue = new float[3];
    StringBuilder stringBuilder;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);

        textView = (TextView)findViewById(R.id.textview);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        startButton = (Button)findViewById(R.id.startButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButtonAction();
            }
        });

        editText = (EditText)findViewById(R.id.editText);
    }

    @Override
    protected void onResume() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);

        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float alpha = (float) 0.8;
        stringBuilder = new StringBuilder();
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone();
            // Isolate the force of gravity with the low-pass filter.
            //gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            //gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            //gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
            float[] inR = new float[9];
            float[] outR = new float[9];
            float[] I = new float[9];
            SensorManager.getRotationMatrix(inR, I, gravity, magnetic);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);

            float [] A_D = event.values.clone();
            float [] A_W = new float[3];
            A_W[0] = outR[0] * A_D[0] + outR[1] * A_D[1] + outR[2] * A_D[2];
            A_W[1] = outR[3] * A_D[0] + outR[4] * A_D[1] + outR[5] * A_D[2];
            A_W[2] = outR[6] * A_D[0] + outR[7] * A_D[1] + outR[8] * A_D[2];

            inR = new float[16];
            outR = new float[16];
            I = new float[9];
            SensorManager.getRotationMatrix(inR, I, gravity, magnetic);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
            float[] relativeMag = new float[4];
            float[] earthMag = new float[4];
            float[] inv = new float[16];
            relativeMag[0] = gravity[0];
            relativeMag[1] = gravity[1];
            relativeMag[2] = gravity[2];
            relativeMag[3] = 0;
            android.opengl.Matrix.invertM(inv, 0, outR, 0);
            android.opengl.Matrix.multiplyMV(earthMag, 0, inv, 0, relativeMag, 0);

            stringBuilder.append("x(device): " + A_D[0]).append("\n");
            stringBuilder.append("y(device): " + A_D[1]).append("\n");
            stringBuilder.append("z(device): " + A_D[2]).append("\n");
            stringBuilder.append("\n");

            stringBuilder.append("x(global_east): " + A_W[0]).append("\n");
            stringBuilder.append("y(global_north): " + A_W[1]).append("\n");
            stringBuilder.append("z(global): " + A_W[2]).append("\n");
            stringBuilder.append("\n");

            stringBuilder.append("x(OpenGL_global): " + earthMag[0]).append("\n");
            stringBuilder.append("y(OpenGL_global): " + earthMag[1]).append("\n");
            stringBuilder.append("z(OpenGL_global): " + earthMag[2]).append("\n");
            textView.setText(stringBuilder.toString());
            sensorManager.flush(this);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetic[0] = event.values[0];
            magnetic[1] = event.values[1];
            magnetic[2] = event.values[2];
            /*
            float[] inR = new float[9];
            float[] outR = new float[9];
            float[] I = new float[9];
            SensorManager.getRotationMatrix(inR, I, gravity, magnetic);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);

            float [] A_D = event.values.clone();
            float [] A_W = new float[3];
            A_W[0] = outR[0] * A_D[0] + outR[1] * A_D[1] + outR[2] * A_D[2];
            A_W[1] = outR[3] * A_D[0] + outR[4] * A_D[1] + outR[5] * A_D[2];
            A_W[2] = outR[6] * A_D[0] + outR[7] * A_D[1] + outR[8] * A_D[2];

            inR = new float[16];
            outR = new float[16];
            I = new float[9];
            SensorManager.getRotationMatrix(inR, I, gravity, magnetic);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
            float[] relativeMag = new float[4];
            float[] earthMag = new float[4];
            float[] inv = new float[16];
            relativeMag[0] = magnetic[0];
            relativeMag[1] = magnetic[1];
            relativeMag[2] = magnetic[2];
            relativeMag[3] = 0;
            android.opengl.Matrix.invertM(inv, 0, outR, 0);
            android.opengl.Matrix.multiplyMV(earthMag, 0, inv, 0, relativeMag, 0);

            stringBuilder.append("x(device): " + A_D[0]).append("\n");
            stringBuilder.append("y(device): " + A_D[1]).append("\n");
            stringBuilder.append("z(device): " + A_D[2]).append("\n");
            stringBuilder.append("\n");

            stringBuilder.append("x(global_east): " + A_W[0]).append("\n");
            stringBuilder.append("y(global_north): " + A_W[1]).append("\n");
            stringBuilder.append("z(global): " + A_W[2]).append("\n");
            stringBuilder.append("\n");

            stringBuilder.append("x(OpenGL_global): " + earthMag[0]).append("\n");
            stringBuilder.append("y(OpenGL_global): " + earthMag[1]).append("\n");
            stringBuilder.append("z(OpenGL_global): " + earthMag[2]).append("\n");
            textView.setText(stringBuilder.toString());
            sensorManager.flush(this);
            */
        }

        /*
        float[] rotate = new float[16];
        float[] outR = new float[16];
        float[] I = new float[16];
        float alpha = (float) 0.8;
        float[] inv = new float[16];
        float[] linear_acceleration = new float[3];
        stringBuilder = new StringBuilder();
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = event.values.clone();
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];
                sensorManager.getRotationMatrix(rotate, I, gravity, magnetic);
                SensorManager.remapCoordinateSystem(rotate, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);

                float[] relativacc = new float[4];
                float[] earthAcc = new float[4];
                relativacc[0] = linear_acceleration[0];
                relativacc[1] = linear_acceleration[1];
                relativacc[2] = linear_acceleration[2];
                relativacc[3] = 0;
                android.opengl.Matrix.invertM(inv, 0, outR, 0);
                android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, relativacc, 0);
                System.out.println("x: " + earthAcc[0]);
                System.out.println("y: " + earthAcc[1]);
                System.out.println("z: " + earthAcc[2]);
                stringBuilder.append("x: " + earthAcc[0]).append("\n");
                stringBuilder.append("y: " + earthAcc[1]).append("\n");
                stringBuilder.append("z: " + earthAcc[2]).append("\n");
                textView.setText(stringBuilder.toString());
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetic = event.values.clone();
                sensorManager.getRotationMatrix(rotate, I, gravity, magnetic);
                //SensorManager.remapCoordinateSystem(rotate, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
                float[] relativeMag = new float[4];
                float[] earthMag = new float[4];
                relativeMag[0] = magnetic[0];
                relativeMag[1] = magnetic[1];
                relativeMag[2] = magnetic[2];
                relativeMag[3] = 0;
                android.opengl.Matrix.invertM(inv, 0, rotate, 0);
                android.opengl.Matrix.multiplyMV(earthMag, 0, inv, 0, relativeMag, 0);
                System.out.println("x: " + earthMag[0]);
                System.out.println("y: " + earthMag[1]);
                System.out.println("z: " + earthMag[2]);
                stringBuilder.append("x: " + earthMag[0]).append("\n");
                stringBuilder.append("y: " + earthMag[1]).append("\n");
                stringBuilder.append("z: " + earthMag[2]).append("\n");
                textView.setText(stringBuilder.toString());
                break;
        }


        if(gravity != null && magnetic != null){




            //stringBuilder.append("測定時間: " + (endTime - startTime) * 1.0e-9);
            //textView.setText(stringBuilder.toString());

        }
        */
            /*
            SensorManager.getRotationMatrix(inR, null, gravity, magnetic);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
            float[] orientation = new float[3];
            SensorManager.getOrientation(outR, orientation);
            stringBuilder.append("azimuth: " + orientation[0]).append("\n");
            stringBuilder.append("pitch: " + orientation[1]).append("\n");
            stringBuilder.append("roll : " + orientation[2]).append("\n");
            stringBuilder.append("x: " + gravity[0]).append("\n");
            stringBuilder.append("y: " + gravity[1]).append("\n");
            stringBuilder.append("z: " + gravity[2]).append("\n");
            gravityConvertGlobal(orientation);
            magneticConvertGlobal(orientation);
            textView.setText(stringBuilder.toString());
            */

            /*
            sensorManager.getRotationMatrix(Rotate, I, gravity, magnetic);
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];

            monSensorManager.getRotationMatrix(Rotate, I, gravity_values, mag_values);
            earthAcc[0] = Rotate[0] * acc_values[0] + Rotate[1] * acc_values[1] + Rotate[2] * acc_values[2];
            earthAcc[1] = Rotate[3] * acc_values[0] + Rotate[4] * acc_values[1] + Rotate[5] * acc_values[2];
            earthAcc[2] = Rotate[6] * acc_values[0] + Rotate[7] * acc_values[1] + Rotate[8] * acc_values[2];
            */
    }

    public void createCsv(float[] globalValue){
        if(outputStreamWriter != null) {
            counter++;
            try {
                outputStreamWriter.write(String.valueOf(globalValue[0]));
                outputStreamWriter.write(",");
                outputStreamWriter.write(String.valueOf(globalValue[1]));
                outputStreamWriter.write(",");
                outputStreamWriter.write(String.valueOf(globalValue[2]));
                outputStreamWriter.write("\n");

                progressBar.setMax(COUNT_MAX);
                progressBar.setProgress(counter);
                if (counter == COUNT_MAX) {
                    endTime = System.nanoTime();
                    counter = 0;
                    outputStreamWriter.close();
                    outputStreamWriter = null;
                    editText.getText().clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void startButtonAction(){
        startTime = System.nanoTime();
        Time time = new Time("Asia/Tokyo");
        time.setToNow();
        String name = editText.getText().toString() + "_" + (time.month + 1) + "_" + time.monthDay + ".csv";
        file = new File(getExternalFilesDir(null), name);
        try {
            fileOutputStream = new FileOutputStream(file, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void gravityConvertGlobal(float[] orientation){
        // ピッチ・ロール
        //double nPitchRad = Math.toRadians(-rotationVector[1]); // n means negative
        double nPitchRad = -orientation[1]; // n means negative -> already radian
        double sinNPitch = Math.sin(nPitchRad);
        double cosNPitch = Math.cos(nPitchRad);

        //double nRollRad = Math.toRadians(-orientation[2]);
        double nRollRad = -orientation[2];
        double sinNRoll = Math.sin(nRollRad);
        double cosNRoll = Math.cos(nRollRad);
        float[] rotatedValues = new float[3];

        double bx, by; // 一時退避
        bx = gravity[0] * cosNRoll + gravity[2] * sinNRoll;
        by = gravity[0] * sinNPitch * sinNRoll + gravity[1] * cosNPitch - gravity[2] * sinNPitch * cosNRoll;
        rotatedValues[2] = (float) (-gravity[0] * cosNPitch * sinNRoll + gravity[1] * sinNPitch * cosNRoll + gravity[2] * cosNPitch * cosNRoll);

        // 方位
        //double nAzimuthRad = Math.toRadians(-rotationVector[0]);
        double nAzimuthRad = -orientation[0];
        double sinNAzimuth = Math.sin(nAzimuthRad);
        double cosNAzimuth = Math.cos(nAzimuthRad);

        // 回転
        rotatedValues[0] = (float) (bx * cosNAzimuth - by * sinNAzimuth);
        rotatedValues[1] = (float) (bx * sinNAzimuth + by * cosNAzimuth);

        stringBuilder.append("x(global): " + rotatedValues[0]).append("\n");
        stringBuilder.append("y(global): " + rotatedValues[1]).append("\n");
        stringBuilder.append("z(global): " + rotatedValues[2]).append("\n");
    }

    public void magneticConvertGlobal(float[] orientation){
        float[] rotatedValues = new float[3];

        // ピッチ・ロール
        //double nPitchRad = Math.toRadians(-rotationVector[1]); // n means negative
        double nPitchRad = -orientation[1]; // n means negative -> already radian
        double sinNPitch = Math.sin(nPitchRad);
        double cosNPitch = Math.cos(nPitchRad);

        //double nRollRad = Math.toRadians(-orientation[2]);
        double nRollRad = -orientation[2];
        double sinNRoll = Math.sin(nRollRad);
        double cosNRoll = Math.cos(nRollRad);

        double bx, by; // 一時退避
        bx = magnetic[0] * cosNRoll + magnetic[2] * sinNRoll;
        by = magnetic[0] * sinNPitch * sinNRoll + magnetic[1] * cosNPitch - magnetic[2] * sinNPitch * cosNRoll;
        rotatedValues[2] = (float) (-magnetic[0] * cosNPitch * sinNRoll + magnetic[1] * sinNPitch * cosNRoll + magnetic[2] * cosNPitch * cosNRoll);

        // 方位
        //double nAzimuthRad = Math.toRadians(-rotationVector[0]);
        double nAzimuthRad = -orientation[0];
        double sinNAzimuth = Math.sin(nAzimuthRad);
        double cosNAzimuth = Math.cos(nAzimuthRad);

        // 回転
        rotatedValues[0] = (float) (bx * cosNAzimuth - by * sinNAzimuth);
        rotatedValues[1] = (float) (bx * sinNAzimuth + by * cosNAzimuth);
        stringBuilder.append("地磁気センサー").append("\n");
        stringBuilder.append("x: " + rotatedValues[0]).append("\n");
        stringBuilder.append("y: " + rotatedValues[1]).append("\n");
        stringBuilder.append("z: " + rotatedValues[2]).append("\n");
    }



}