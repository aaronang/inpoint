package nl.tudelft.inpoint.localization;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import nl.tudelft.inpoint.Globals;
import nl.tudelft.inpoint.R;

public class ActivityListener implements SensorEventListener {

    private long lastUpdate = 0;
    private ArrayList<Float> values = new ArrayList<>();
    private ArrayList<Integer> directions = new ArrayList<>();
    private ArrayList<Boolean> walking = new ArrayList<>();
    private long startWalking = 0;
    private long endWalking = 0;
    private static final float CONFIDENCE_LEVEL = 0.75f;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 1) {
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];

                float eucl = x * x + y * y + z * z;

                float variance = 0f;

                if (values.size() == 75) {
                    variance = calculateVariance(values);
                    TextView varianceView = (TextView) Globals.VIEW.findViewById(R.id.variance);
                    varianceView.setText(variance + "");
                    values.remove(0);

                    isWalking(variance);
                }

                if (walking.size() == 310) {

                    if (Globals.RECORDING_MOTION) {
                        moveProbabilities(1);
                    }

                    if (!walking.isEmpty()) {
                        walking.remove(0);
                        directions.remove(0);
                    }
                }

                values.add(eucl);
                directions.add(Globals.getDirection());
                walking.add(Globals.WALKING);

                lastUpdate = curTime;
            }
        }
    }

    private boolean isWalking(float variance) {
        FloatingActionButton button = (FloatingActionButton) Globals.VIEW.findViewById(R.id.fabMotion);

        if (variance < 10) { // Standing
            button.setImageResource(R.drawable.ic_directions_walk);
            Globals.WALKING = false;
            return false;
        } else { // Walking
            button.setImageResource(R.drawable.ic_directions_run);
            Globals.WALKING = true;
            return true;
        }
    }

    private int direction() {
        int directionCount = 0;
        int direction = -1;
        for (int i = 0; i < 4; i++) {
            int count = Collections.frequency(directions, i);
            if (count > directionCount) {
                directionCount = count;
                direction = i;
            }
        }
        float confidence = ((float) directionCount) / directions.size();
        if (confidence < CONFIDENCE_LEVEL)
            return -1;
        return direction;
    }

    private boolean hasWalked() {
        int walkCount = Collections.frequency(walking, true);
        return (float) walkCount / (float) walking.size() > CONFIDENCE_LEVEL;
    }

    private void moveProbabilities( int numberOfRoomsMoved ) {
        int direction = direction();

        if (direction == -1 || !hasWalked()) {
            return;
        }
        
        for ( int x = 0; x < numberOfRoomsMoved; x++ ) {
            if (direction == 0) { // EAST
                for (int i = 1; i <= 3; i++) {
                    Globals.POSTERIOR[i] = 0;
                }

                for (int i = 17; i >= 5; i--) {
                    Globals.POSTERIOR[i] = Globals.POSTERIOR[i - 1];
                }
                Globals.POSTERIOR[4] = 0;

                for (int i = 18; i <= 21; i++) {
                    Globals.POSTERIOR[i] = 0;
                }
            } else if (direction == 1) { // SOUTH
                Globals.POSTERIOR[19] = Globals.POSTERIOR[18];

                Globals.POSTERIOR[18] = Globals.POSTERIOR[5];
                Globals.POSTERIOR[20] = Globals.POSTERIOR[12];
                Globals.POSTERIOR[21] = Globals.POSTERIOR[16];

                Globals.POSTERIOR[5] = Globals.POSTERIOR[1];
                Globals.POSTERIOR[12] = Globals.POSTERIOR[2];
                Globals.POSTERIOR[16] = Globals.POSTERIOR[3];

                Globals.POSTERIOR[1] = 0;
                Globals.POSTERIOR[2] = 0;
                Globals.POSTERIOR[3] = 0;
                Globals.POSTERIOR[4] = 0;
                Globals.POSTERIOR[6] = 0;
                Globals.POSTERIOR[7] = 0;
                Globals.POSTERIOR[8] = 0;
                Globals.POSTERIOR[9] = 0;
                Globals.POSTERIOR[10] = 0;
                Globals.POSTERIOR[11] = 0;
                Globals.POSTERIOR[13] = 0;
                Globals.POSTERIOR[14] = 0;
                Globals.POSTERIOR[15] = 0;
                Globals.POSTERIOR[17] = 0;
            } else if (direction == 2) { // WEST
                for (int i = 1; i <= 3; i++) {
                    Globals.POSTERIOR[i] = 0;
                }

                for (int i = 4; i <= 16; i++) {
                    Globals.POSTERIOR[i] = Globals.POSTERIOR[i + 1];
                }

                for (int i = 17; i <= 21; i++) {
                    Globals.POSTERIOR[i] = 0;
                }
            } else if (direction == 3) { // North
                Globals.POSTERIOR[1] = Globals.POSTERIOR[5];
                Globals.POSTERIOR[2] = Globals.POSTERIOR[12];
                Globals.POSTERIOR[3] = Globals.POSTERIOR[16];

                Globals.POSTERIOR[5] = Globals.POSTERIOR[18];
                Globals.POSTERIOR[12] = Globals.POSTERIOR[20];
                Globals.POSTERIOR[16] = Globals.POSTERIOR[21];

                Globals.POSTERIOR[18] = Globals.POSTERIOR[19];

                Globals.POSTERIOR[4] = 0;
                Globals.POSTERIOR[6] = 0;
                Globals.POSTERIOR[7] = 0;
                Globals.POSTERIOR[8] = 0;
                Globals.POSTERIOR[9] = 0;
                Globals.POSTERIOR[10] = 0;
                Globals.POSTERIOR[11] = 0;
                Globals.POSTERIOR[13] = 0;
                Globals.POSTERIOR[14] = 0;
                Globals.POSTERIOR[15] = 0;
                Globals.POSTERIOR[17] = 0;
                Globals.POSTERIOR[19] = 0;
                Globals.POSTERIOR[20] = 0;
                Globals.POSTERIOR[21] = 0;
            }

            float sum = 0f;
            for (int i = 1; i <= 21; i++) {
                sum += Globals.POSTERIOR[i];
            }

            for (int i = 1; i <= 21; i++) {
                Globals.POSTERIOR[i] /= sum;
            }
        }

        for (int i = 1; i <= Globals.NUMBER_OF_ROOMS; i++) {
            int id = Globals.RESOURCES.getIdentifier("room" + i, "id", Globals.PACKAGE_NAME);
            setRoom((TextView) Globals.VIEW.findViewById(id), Globals.POSTERIOR[i]);
        }

        directions.clear();
        walking.clear();
    }

    private void setRoom(TextView room, float probability) {
        int p = Math.round(probability * 100);
        room.setBackgroundColor(Globals.RESOURCES.getColor(Globals.getColor(p)));
        room.setText(p + "");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private float calculateVariance(ArrayList<Float> list) {
        float mean = calculateMean(list);
        float result = 0;
        for (float f : list) {
            result += (f - mean) * (f - mean);
        }
        return (float) Math.sqrt(result / list.size());
    }

    private float calculateMean(ArrayList<Float> list){
        float total = 0;

        for ( float f : list ){
            total += f;
        }

        return ( total /= (float) list.size() );
    }
}
