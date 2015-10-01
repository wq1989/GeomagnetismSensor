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
    private float[] gravity = new float[3];
    private float[] acc = new float[3] ;

    private float[] globalValue = new float[3];
    StringBuilder stringBuilder;




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
        //sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        stringBuilder = new StringBuilder();
        int sensor = event.sensor.getType();
        switch (sensor){
            //case Sensor.TYPE_LINEAR_ACCELERATION:
            case Sensor.TYPE_ACCELEROMETER:
            acc = event.values.clone();
                if(acc != null && gravity != null && magnetic != null){
                    System.out.println("test");
                    float[] inR = new float[16];
                    float[] outR = new float[16];
                    SensorManager.getRotationMatrix(inR, null, gravity, magnetic);
                    SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
                    float[] relativeAcc = new float[4];
                    float[] globalAcc = new float[4];
                    float[] inv = new float[16];
                    relativeAcc[0] = acc[0];
                    relativeAcc[1] = acc[1];
                    relativeAcc[2] = acc[2];
                    relativeAcc[3] = 0;
                    android.opengl.Matrix.invertM(inv, 0, outR, 0);
                    android.opengl.Matrix.multiplyMV(globalAcc, 0, inv, 0, relativeAcc, 0);
                    stringBuilder.append("x(global): " + globalAcc[0]).append("\n");
                    stringBuilder.append("y(global): " + globalAcc[1]).append("\n");
                    stringBuilder.append("z(global): " + globalAcc[2]).append("\n");
                    textView.setText(stringBuilder.toString());
                }
                break;
            case Sensor.TYPE_GRAVITY:
                gravity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetic = event.values.clone();
                break;
        }
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
}