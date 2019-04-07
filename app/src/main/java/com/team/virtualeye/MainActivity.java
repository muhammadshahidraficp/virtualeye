package com.team.virtualeye;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "bluetooth2";
    private final int REQ_CODE_SPEECH_INPUT = 100;

    TextView txtArduino;
    Handler h;
    Button startButton, stopButton, clearButton;
    TextView distText, navText, calibText;
    Vibrator var;
    Context context;
    Boolean calibStat;
    TextToSpeech speech;
    ImageView swipeImage;
    MediaPlayer mPlayer;

    final int RECIEVE_MESSAGE = 1;        // Status  for Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)
    private static String address = "FC:A8:9A:00:6E:E3";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);


        startButton = (Button) findViewById(R.id.start_service);
        clearButton = (Button) findViewById(R.id.clear_dist);
        stopButton = (Button) findViewById(R.id.stop_service);
        distText = (TextView) findViewById(R.id.distance_text_view);
        navText = (TextView) findViewById(R.id.nav_text);
        calibText = (TextView) findViewById(R.id.calibrate_text);
        swipeImage = (ImageView) findViewById(R.id.swipe_image);
        mPlayer = MediaPlayer.create(MainActivity.this, R.raw.start_instruction);

        readInstructions();

        //Speech initialization

        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1234);

        speech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    speech.setLanguage(Locale.UK);
                }
            }
        });

        enableUi(false);
        context = getApplicationContext();
        var = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);

        final GestureDetector.SimpleOnGestureListener listener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                promptSpeechInput();
                return true;
            }
        };

        final GestureDetector detector = new GestureDetector(context, listener);

        detector.setOnDoubleTapListener(listener);
        detector.setIsLongpressEnabled(true);

        getWindow().getDecorView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        });

        swipeImage.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeTop() {
                launchGesture();
            }

            public void onSwipeRight() {
                launchGesture();
            }

            public void onSwipeLeft() {
                readInstructions();
            }

            public void onSwipeBottom() {
                launchGesture();
            }

        });


        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
                        sb.append(strIncom);                                                // append string
                        int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                        if (endOfLineIndex > 0) {                                            // if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);               // extract string
                            sb.delete(0, sb.length());                                      // and clear
                            txtArduino.setText("Data from Arduino: " + sbprint);            // update TextView
                            startButton.setEnabled(true);
                            stopButton.setEnabled(true);
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            }

            ;
        };


        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startButton.setEnabled(false);
                mConnectedThread.write("A");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "Start service", Toast.LENGTH_SHORT).show();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopButton.setEnabled(false);
                mConnectedThread.write("a");    // Send "0" via Bluetooth
                Toast.makeText(getBaseContext(), "Stop service", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void readInstructions(){
        if(mPlayer.isPlaying()){
            mPlayer.pause();
        }
        mPlayer.start();
    }

    public void launchGesture(){
        if(mPlayer.isLooping()){
            mPlayer.pause();
        }
        mPlayer.pause();
        Thread logoTimer = new Thread() {
            public void run() {
                try {
                    sleep(500);
                    String speakText = getResources().getString(R.string.gesture);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        speech.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(MainActivity.this, GestureActivity.class);
                startActivity(intent);
            }
        };
        logoTimer.start();
    }

    public void enableUi(boolean flag) {
        if (!flag) {
            startButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.GONE);
        } else {
            startButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);
        }
        distText.setEnabled(flag);
    }

    public void onClickInstructions(View view){ onStartInstructions();}

    public void onStartInstructions(){
        readInstructions();
    }

    public void onClickStart(View view) {
        onStartRead();
    }

    public void onStartRead(){
        String speakText = getResources().getString(R.string.sensor_start);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            speech.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void onClickStop(View view) {
        enableUi(false);
        String speakText = getResources().getString(R.string.sensor_stop);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            speech.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null);
        }

    }

    public void onClickClear(View view) {
        distText.setText("");
        navText.setText("");
    }

    public void onClickSetting(View view) {
        Intent settingIntent = new Intent(this, Settings.class);
        startActivity(settingIntent);
    }




    // Function to activate speech input
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "....Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }

    /**
     * Receiving speech input
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    // txtSpeechInput.setText(result.get(0));
                    String s = result.get(0);
                    String a[] = s.split(" ");
                    String destination = " ";
                    for (String anA : a) {
                        destination = destination + anA + "+";
                    }
                    destination = destination.substring(0, destination.length() - 1);
                    navText.setText(destination);
                    destination = "google.navigation:q=" + destination + "&mode=walking";
                    try {
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(destination));
                        startActivity(intent);
                    } catch (ActivityNotFoundException noMaps) {
                        String speakText = getResources().getString(R.string.no_map);
                        Toast.makeText(this, speakText, Toast.LENGTH_LONG).show();
                        speech.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
                break;
            }
        }
    }


}