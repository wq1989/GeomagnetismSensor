package info.ggtanakan.geomagnetismsensor;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity implements SensorEventListener, View.OnClickListener{
    private float[] magnetic = new float[3];
    private float[] gravity = new float[3];
    private float[] acc = new float[3] ;
    private float[] globalAccValues = new float[3];
    private float[] globalMagValues = new float[3];
    private final int COUNT_MAX = 1000;
    private AtomicInteger counter = new AtomicInteger(0);
    private boolean start = false;
    private SensorManager sensorManager;
    private TextView textView;
    private ProgressBar progressBar;
    private Button button;
    private EditText editText;
    private File file;
    private FileOutputStream fileOutputStream;
    private OutputStreamWriter outputStreamWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);

        textView = (TextView)findViewById(R.id.textview);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        button = (Button)findViewById(R.id.startButton);

        button.setOnClickListener(this);

        editText = (EditText)findViewById(R.id.editText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onClick(View v) {
        startButtonAction();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        StringBuilder stringBuilder = new StringBuilder();
        int sensor = event.sensor.getType();
        switch (sensor){
            //case Sensor.TYPE_LINEAR_ACCELERATION:
            case Sensor.TYPE_ACCELEROMETER:
                acc = event.values.clone();
                globalAccValues = convertGlobalValues(acc);
                break;
            case Sensor.TYPE_GRAVITY:
                gravity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetic = event.values.clone();
                globalMagValues = convertGlobalValues(magnetic);
                break;
        }

        stringBuilder.append("加速度").append("\n");
        stringBuilder.append("x: " + globalAccValues[0]).append("\n");
        stringBuilder.append("y: " + globalAccValues[1]).append("\n");
        stringBuilder.append("z: " + globalAccValues[2]).append("\n");

        stringBuilder.append("地磁気").append("\n");
        stringBuilder.append("x(east): " + globalMagValues[0]).append("\n");
        stringBuilder.append("y(north): " + globalMagValues[1]).append("\n");
        stringBuilder.append("z: " + globalMagValues[2]).append("\n");
        createCsv(globalMagValues);
        textView.setText(stringBuilder.toString());
    }

    public float[] convertGlobalValues(float[] deviceValues){
        float[] globalValues = new float[4];
        if(acc != null && gravity != null && magnetic != null){
            float[] inR = new float[16];
            float[] outR = new float[16];
            SensorManager.getRotationMatrix(inR, null, gravity, magnetic);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
            float[] temp = new float[4];
            float[] inv = new float[16];
            temp[0] = deviceValues[0];
            temp[1] = deviceValues[1];
            temp[2] = deviceValues[2];
            temp[3] = 0;
            android.opengl.Matrix.invertM(inv, 0, outR, 0);
            android.opengl.Matrix.multiplyMV(globalValues, 0, inv, 0, temp, 0);
        }
        return globalValues;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void startButtonAction(){
        start = true;
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

    public void createCsv(float[] globalValue){
        if(start) {
            counter.incrementAndGet();
            try {
                outputStreamWriter.write(String.valueOf(globalValue[0]));
                outputStreamWriter.write(",");
                outputStreamWriter.write(String.valueOf(globalValue[1]));
                outputStreamWriter.write(",");
                outputStreamWriter.write(String.valueOf(globalValue[2]));
                outputStreamWriter.write("\n");
                progressBar.setMax(COUNT_MAX);
                progressBar.setProgress(counter.get());
                if (counter.get() == COUNT_MAX) {
                    counter.set(0);
                    outputStreamWriter.close();
                    outputStreamWriter = null;
                    editText.getText().clear();
                    start = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}