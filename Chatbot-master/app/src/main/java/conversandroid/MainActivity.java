/*
 *  Copyright 2016 Zoraida Callejas, Michael McTear and David Griol
 *
 *  This is AN UPDATE of the Conversandroid Toolkit, from the book:
 *  The Conversational Interface, Michael McTear, Zoraida Callejas and David Griol
 *  Springer 2016 <https://github.com/zoraidacallejas/>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package conversandroid;

/**
 * Example activity with speech input and output that connects to
 * a DialogFlow chatbot previously created.
 *
 * Note: it will not be functional until you do not insert your own
 * access token (see line 72)
 *
 * @author Zoraida Callejas, Michael McTear, David Griol
 * @version 4.0, 04/06/18
 */

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Locale;

//Check the dependencies necessary to make these imports in
//the build.gradle file
//See tutorial here: https://github.com/dialogflow/dialogflow-android-client
import ai.api.android.AIConfiguration; //<< be careful to use ai.api.android.AI... and not ai.api.AI...
import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import conversandroid.chatbot.R;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static conversandroid.ErrorCodeClient.*;

public class MainActivity extends VoiceActivity implements SensorEventListener{

    // Variables gestión Chatbot
    private static final String LOGTAG = "CHATBOT";
    private static final Integer ID_PROMPT_QUERY = 0;
    private static final Integer ID_PROMPT_INFO = 1;
    private static final String DEBUG_TAG = "DEBUG";

    // Constante tiempo max bloqueado
    private static final int TIME_DELAYED = 10000;
    // Pulsaciones detectadas
    private ArrayList<PointF> touchPoints = null;
    // Error del cliente
    private ErrorCodeClient errorCodeClient = ErrorCodeClient.DEFAULT;

    // Variables sensores
    private ZXingScannerView scannerView;
    Button initQRScannerBt;
    ImageButton stopChatBotBtn;
    ImageButton helpButton;
    int tap = 0;

    // Idioma del sistena
    private static final String languages = Locale.getDefault().toString();

    // Variable para bloquear el cambio de habitaciones
    private static boolean activeRoom = false;
    // Variables para el uso de habitaciones
    private static int roomSelected = 0;
    private static boolean changeRoom = false;
    private static int prev_ant = 0;

    // Variable que indica si la conversación ha terminado
    private static boolean endConver = true;

    int GLOBAL_TOUCH_POSITION_X = 0;
    int GLOBAL_TOUCH_CURRENT_POSITION_X = 0;


    private long startListeningTime = 0; // To skip errors (see processAsrError method)

    //Connection to DialogFlow
    private AIDataService aiDataService = null;
    private final String ACCESS_TOKEN = "";   //TODO: INSERT YOUR ACCESS TOKEN
    // https://dialogflow.com/docs/reference/agent/#obtaining_access_tokens)

    // Variables para trabajar los sensores
    private SensorManager mSensorManager;
    private Sensor accelerSensor;
    private Sensor lightSensor;

    // Variables para trabajar el multitouch
    private static final int MIN_DESPLACEMENT = 200;
    private static float last_x = 0, last_y = 0, last_z = 0;
    private static long lastUpdate = 0;

    private static final int SHAKE_THRESHOLD = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set layout
        setContentView(R.layout.activity_main);
        //Set the initial help reference
        Intent intent = getIntent();
        if((Boolean) intent.getBooleanExtra("HELPSHOW",true)){
            helpButton();
        }

        helpButton = (ImageButton) findViewById(R.id.idHelpButton);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helpButton();
            }
        });
        // Set Touch Listener
        setOnTouchListener();

        //Initialize the speech recognizer and synthesizer
        initSpeechInputOutput(this);

        //Set rooms buttons
        setRoomsButtons();

        //Set up the speech button
        setSpeakButton();

        // Initialize cancel button
        setCancelButton();

        // Initialize touchedPoints
        initialize();

        // Initialize QR-Scanner button
        setQRScanerButton();

        // Shake sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerSensor = getSensorAcceleration();
        lightSensor = getSensorLight();

        //Dialogflow configuration parameters
        final AIConfiguration config = new AIConfiguration(ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.Spanish,
                AIConfiguration.RecognitionEngine.System);

        aiDataService = new AIDataService(config);
    }

    /**
     * Iniciar el Scanner
     */
    private void setQRScanerButton() {
        initQRScannerBt = (Button) findViewById(R.id.idScannerQRBt);
        initQRScannerBt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent ScannerQRActivity = new Intent(MainActivity.this, ScannerQR.class);
                startActivity(ScannerQRActivity);
            }
        });
    }

    /**
     * Iniciar el botón de ayuda
     */
    private void helpButton() {
        // Estructura del mensaje
        TextView title = new TextView(this);
        title.setText(R.string.help);
        title.setPadding(10, 10, 10, 10);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(getResources().getColor(R.color.colorDialogText));
        title.setTextSize(23);
        TextView msg = new TextView(this);
        msg.setText(R.string.help_description);
        msg.setPadding(10, 10, 10, 10);
        msg.setGravity(Gravity.FILL);
        msg.setTextSize(18);

        // Lanzar mensaje
        DialogInterface.OnClickListener onClick = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
            }
        };

        //Declaramos el builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Establecemos el título y mensajes declarados
        builder.setCustomTitle(title);
        builder.setView(msg);
        //Creamos un botón para indicar que se han entendido las instrucciones
        builder.setPositiveButton(R.string.helpClick, onClick);
        //Lanzamos la alerta
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Definir botón de cancelar
     */
    private void setCancelButton(){
        stopChatBotBtn = (ImageButton) findViewById(R.id.stopChaplinBtn);
        stopChatBotBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopChatBot();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Iniciar Listener de gestos
        mSensorManager.registerListener(this, accelerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausar Listener de gestos
        mSensorManager.unregisterListener(this);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    /**
     * Obtener sensor de luz
     */
    private Sensor getSensorLight(){
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            return mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }else{
            Toast.makeText(getApplicationContext(),R.string.error_sensor,Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    /**
     * Obtener acelerómetro
     */

    private Sensor getSensorAcceleration(){
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null){
            return mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }else{
            if(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
                return mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
            else{
                Toast.makeText(getApplicationContext(),R.string.error_sensor,Toast.LENGTH_SHORT).show();
            }
        }
        return null;
    }

    /**
     * Detectar cambios en los sensores
     * @param event evento ocurrido
     */
    @Override
    public void onSensorChanged(SensorEvent event){
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER || event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            long curTime = System.currentTimeMillis();
            float x, y, z;
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                x = event.values[SensorManager.DATA_X];
                y = event.values[SensorManager.DATA_Y];
                z = event.values[SensorManager.DATA_Z];

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    Log.d("sensor", "shake detected w/ speed: " + speed);
                    // Toast.makeText(this, "shake detected w/ speed: " + speed, Toast.LENGTH_SHORT).show();
                    stopChatBot();
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }

        if(event.sensor.getType() == Sensor.TYPE_LIGHT){
            if(event.values[0] == 0 && roomSelected == 1){
                proyectar("https://www.youtube.com/watch?v=p64WxNjY3hE");
            }
            //Toast.makeText(getApplicationContext(),"luz",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *Función para lanzar el reproductor con una url dada.
     */
    private void proyectar(String idVideo){
        Intent YTPlayer =new Intent(this,YoutubePlayer.class);
        YTPlayer.putExtra("ID_YT_VIDEO", idVideo);
        startActivity(YTPlayer);
    }

    /**
     * Para el habla y la escucha
     */
    private void stopChatBot(){
        errorCodeClient = ErrorCodeClient.STOP;
        stop();
        stopListening();
    }

    /**
     * Definimos el desbloqueo /bloqueo de cambio de habitación
     * @param b activar / desactivar
     */
    private void setActiveRoom(boolean b){

        activeRoom = b;
    }

    /**
     * Initializes rooms Buttons
     */

    private void setRoomsButtons() {
        final Button roomA = (Button) findViewById(R.id.roomA);
        final Button roomB = (Button) findViewById(R.id.roomB);
        final Button roomC = (Button) findViewById(R.id.roomC);

        /**
         * Onclick Button roomA
         */

        final Handler roomHandler = new Handler();

        roomA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Ask the user to speak

                try {
                    if(!activeRoom) {
                        activeRoom = true;
                        TextView txt = (TextView) findViewById(R.id.youarein);
                        txt.setText("Estas en A");
                        roomSelected = 0;

                        // Modifico los botones
                        roomC.setBackgroundColor(Color.parseColor("#FFF9C4"));
                        roomB.setBackgroundColor(Color.parseColor("#FFF9C4"));
                        roomA.setBackgroundColor(Color.parseColor("#FDD835"));

                        endConver = true;
                        // Envio mensaje a chatbot
                        sendMsgToChatBot("Estoy en la sala A");

                        // Bloqueo los botones TIME_DELAYED ms
                        roomHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setActiveRoom(false);
                            }
                        },TIME_DELAYED);
                    }
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }
        });

        /**
         * Onclick Button roomB
         */
        roomB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Ask the user to speak
                try {
                    if(!activeRoom) {
                        activeRoom = true;
                        TextView txt = (TextView) findViewById(R.id.youarein);
                        txt.setText("Estas en B");
                        roomSelected = 1;
                        roomC.setBackgroundColor(Color.parseColor("#FFF9C4"));
                        roomB.setBackgroundColor(Color.parseColor("#FDD835"));
                        roomA.setBackgroundColor(Color.parseColor("#FFF9C4"));

                        endConver = true;
                        sendMsgToChatBot("Estoy en la sala B");

                        roomHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setActiveRoom(false);
                            }
                        },TIME_DELAYED);
                    }
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }
        });

        /**
         * Onclick Button roomA
         */

        roomC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Ask the user to speak
                try {
                    if(!activeRoom) {
                        activeRoom = true;
                        TextView txt = (TextView) findViewById(R.id.youarein);
                        txt.setText("Estas en C");
                        roomSelected = 2;
                        roomC.setBackgroundColor(Color.parseColor("#FDD835"));
                        roomB.setBackgroundColor(Color.parseColor("#FFF9C4"));
                        roomA.setBackgroundColor(Color.parseColor("#FFF9C4"));

                        endConver = true;
                        sendMsgToChatBot("Estoy en la sala C");

                        roomHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setActiveRoom(false);
                            }
                        },TIME_DELAYED);
                    }
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }
        });
    }

    /**
     * Initializes the search button and its listener. When the button is pressed, a feedback is shown to the user
     * and the recognition starts
     */

    private void setSpeakButton() {
        // gain reference to speak button
        ImageButton speak = findViewById(R.id.speech_btn);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Ask the user to speak
                try {
                    speak(getResources().getString(R.string.initial_prompt), languages, ID_PROMPT_QUERY);
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }
        });
    }

    private Runnable startActivityScanner = new Runnable() {
        @Override
        public void run() {
            Intent ScannerActivity = new Intent(MainActivity.this, conversandroid.ScannerQR.class);
            startActivity(ScannerActivity);
        }
    };


    /**
     * Explain to the user why we need their permission to record audio on the device
     * See the checkASRPermission in the VoiceActivity class
     */
    public void showRecordPermissionExplanation() {
        Toast.makeText(getApplicationContext(), R.string.asr_permission, Toast.LENGTH_SHORT).show();
    }

    /**
     * If the user does not grant permission to record audio on the device, a message is shown and the app finishes
     */
    public void onRecordAudioPermissionDenied() {
        Toast.makeText(getApplicationContext(), R.string.asr_permission_notgranted, Toast.LENGTH_SHORT).show();
        System.exit(0);
    }

    /**
     * Starts listening for any user input.
     * When it recognizes something, the <code>processAsrResult</code> method is invoked.
     * If there is any error, the <code>onAsrError</code> method is invoked.
     */
    private void startListening() {

        if (deviceConnectedToInternet()) {
            try {

                /*Start listening, with the following default parameters:
                 * Language = English
                 * Recognition model = Free form,
                 * Number of results = 1 (we will use the best result to perform the search)
                 */
                startListeningTime = System.currentTimeMillis();
                listen(new Locale("ES"), RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, 1); //Start listening
            } catch (Exception e) {
                this.runOnUiThread(new Runnable() {  //Toasts must be in the main thread
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.asr_notstarted, Toast.LENGTH_SHORT).show();
                        changeButtonAppearanceToDefault();
                    }
                });

                Log.e(LOGTAG, "ASR could not be started");
                try {
                    speak(getResources().getString(R.string.asr_notstarted), languages, ID_PROMPT_INFO);
                } catch (Exception ex) {
                    Log.e(LOGTAG, "TTS not accessible");
                }

            }
        } else {

            this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.check_internet_connection, Toast.LENGTH_SHORT).show();
                    changeButtonAppearanceToDefault();
                }
            });
            try {
                speak(getResources().getString(R.string.check_internet_connection), languages, ID_PROMPT_INFO);
            } catch (Exception ex) {
                Log.e(LOGTAG, "TTS not accessible");
            }
            Log.e(LOGTAG, "Device not connected to Internet");

        }
    }

    /**
     * Invoked when the ASR is ready to start listening. Provides feedback to the user to show that the app is listening:
     * * It changes the color and the message of the speech button
     */
    @Override
    public void processAsrReadyForSpeech() {
        changeButtonAppearanceToListening();
    }

    /**
     * Provides feedback to the user to show that the app is listening:
     * * It changes the color and the message of the speech button
     */
    private void changeButtonAppearanceToListening() {
        activeRoom = true;
        TextView textoChaplin = findViewById(R.id.textChaplin); //Obtains a reference to the button
        textoChaplin.setText(getResources().getString(R.string.speechbtn_listening)); //Changes the button's message to the text obtained from the resources folder
        //button.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.speechbtn_listening), PorterDuff.Mode.MULTIPLY);  //Changes the button's background to the color obtained from the resources folder
    }

    /**
     * Provides feedback to the user to show that the app is idle:
     * * It changes the color and the message of the speech button
     */
    private void changeButtonAppearanceToDefault() {
        activeRoom = false;
        TextView textoChaplin = findViewById(R.id.textChaplin); //Obtains a reference to the button
        textoChaplin.setText(getResources().getString(R.string.speechbtn_default)); //Changes the button's message to the text obtained from the resources folder
        //button.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.speechbtn_default), PorterDuff.Mode.MULTIPLY);    //Changes the button's background to the color obtained from the resources folder
    }

    /**
     * Provides feedback to the user (by means of a Toast and a synthesized message) when the ASR encounters an error
     */
    @Override
    public void processAsrError(int errorCode) {

        changeButtonAppearanceToDefault();

        //Possible bug in Android SpeechRecognizer: NO_MATCH errors even before the the ASR
        // has even tried to recognized. We have adopted the solution proposed in:
        // http://stackoverflow.com/questions/31071650/speechrecognizer-throws-onerror-on-the-first-listening
        long duration = System.currentTimeMillis() - startListeningTime;
        if (duration < 500 && errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
            Log.e(LOGTAG, "Doesn't seem like the system tried to listen at all. duration = " + duration + "ms. Going to ignore the error");
            stopListening();
        } else {
            int errorMsg;
            switch (errorCode) {

                case SpeechRecognizer.ERROR_AUDIO:
                    errorMsg = R.string.asr_error_audio;
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    switch(errorCodeClient) {
                        case STOP:
                            errorMsg = R.string.stopSpeak;
                            break;
                        default:
                            errorMsg = R.string.asr_error_client;
                            break;
                    }
                    errorCodeClient = ErrorCodeClient.DEFAULT;
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    errorMsg = R.string.asr_error_permissions;
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    errorMsg = R.string.asr_error_network;
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    errorMsg = R.string.asr_error_networktimeout;
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errorMsg = R.string.asr_error_nomatch;
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    errorMsg = R.string.asr_error_recognizerbusy;
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    errorMsg = R.string.asr_error_server;
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errorMsg = R.string.asr_error_speechtimeout;
                    break;
                default:
                    errorMsg = R.string.asr_error; //Another frequent error that is not really due to the ASR, we will ignore it
                    break;
            }
            String msg = getResources().getString(errorMsg);
            this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.asr_error, Toast.LENGTH_LONG).show();
                }
            });

            Log.e(LOGTAG, "Error when attempting to listen: " + msg);
            try {
                TextView text = (TextView) findViewById(R.id.conver); //Obtains a reference to the button
                text.setText(msg);
                speak(msg, languages, ID_PROMPT_INFO);

            } catch (Exception e) {
                Log.e(LOGTAG, "TTS not accessible");
            }
        }
    }

    /**
     * Synthesizes the best recognition result
     */
    @Override
    public void processAsrResults(ArrayList<String> nBestList, float[] nBestConfidences) {

        if (nBestList != null) {

            Log.d(LOGTAG, "ASR best result: " + nBestList.get(0));

            if (nBestList.size() > 0) {
                changeButtonAppearanceToDefault();
                sendMsgToChatBot(nBestList.get(0)); //Send the best recognition hypothesis to the chatbot
            }
        }
    }

    /**
     * Connects to DialogFlow sending the user input in text form
     *
     * @param userInput recognized utterance
     */
    private void sendMsgToChatBot(String userInput) {

        //final AIRequest aiRequest = new AIRequest();
        //aiRequest.setQuery(userInput);

        new AsyncTask<String, Void, AIResponse>() {

            /**
             * Connects to the DialogFlow service
             * @param strings Contains the user request
             * @return language understanding result from DialogFlow
             */
            @Override
            protected AIResponse doInBackground(String... strings) {
                final String request = strings[0];
                Log.d(LOGTAG, "Request: " + strings[0]);
                try {
                    final AIRequest aiRequest = new AIRequest(request);
                    final AIResponse response = aiDataService.request(aiRequest);
                    Log.d(LOGTAG, "Request: " + aiRequest);
                    Log.d(LOGTAG, "Response: " + response);


                    return response;
                } catch (AIServiceException e) {
                    try {
                        speak("Could not retrieve a response from DialogFlow", languages, ID_PROMPT_INFO);
                        Log.e(LOGTAG, "Problems retrieving a response");
                    } catch (Exception ex) {
                        Log.e(LOGTAG, "English not available for TTS, default language used instead");
                    }
                }
                return null;
            }

            /**
             * The semantic parsing is decomposed and the text corresponding to the chatbot
             * response is synthesized
             * @param response parsing corresponding to the output of DialogFlow
             */
            @Override
            protected void onPostExecute(AIResponse response) {
                if (response != null) {
                    // process aiResponse here
                    // Mmore info for a more detailed parsing on the response: https://github.com/dialogflow/dialogflow-android-client/blob/master/apiAISampleApp/src/main/java/ai/api/sample/AIDialogSampleActivity.java

                    final Result result = response.getResult();
                    Log.d(LOGTAG, "Result: " + result.getResolvedQuery());
                    Log.d(LOGTAG, "Action: " + result.getAction());

                    final String chatbotResponse = result.getFulfillment().getSpeech();
                    try {
                        TextView text = (TextView) findViewById(R.id.conver); //Obtains a reference to the button
                        text.setText(chatbotResponse);
                        if(!endConver)
                            speak(chatbotResponse, languages, ID_PROMPT_QUERY); //It always starts listening after talking, it is neccessary to include a special "last_exchange" intent in dialogflow and process it here
                            //so that the last system answer is synthesized using ID_PROMPT_INFO.
                        else
                            speak(chatbotResponse, languages, ID_PROMPT_INFO);
                        endConver = true;
                    } catch (Exception e) {
                        Log.e(LOGTAG, "TTS not accessible");
                    }

                }
            }
        }.execute(userInput);

    }

    /**
     * Checks whether the device is connected to Internet (returns true) or not (returns false)
     * From: http://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
     */
    public boolean deviceConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    /**
     * Shuts down the TTS engine when finished
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdown();
    }

    /**
     * Invoked when the TTS has finished synthesizing.
     * <p>
     * In this case, it starts recognizing if the message that has just been synthesized corresponds to a question (its id is ID_PROMPT_QUERY),
     * and does nothing otherwise.
     * <p>
     * According to the documentation the speech recognizer must be invoked from the main thread. onTTSDone callback from TTS engine and thus
     * is not in the main thread. To solve the problem, we use Androids native function for forcing running code on the UI thread
     * (runOnUiThread).
     *
     * @param uttId identifier of the prompt that has just been synthesized (the id is indicated in the speak method when the text is sent
     *              to the TTS engine)
     */

    @Override
    public void onTTSDone(String uttId) {
        if (uttId.equals(ID_PROMPT_QUERY.toString())) {
            runOnUiThread(new Runnable() {
                public void run() {
                    startListening();
                }
            });
        }
    }

    /**
     * Invoked when the TTS encounters an error.
     * <p>
     * In this case it just writes in the log.
     */
    @Override
    public void onTTSError(String uttId) {
        Log.e(LOGTAG, "TTS error");
    }

    /**
     * Invoked when the TTS starts synthesizing
     * <p>
     * In this case it just writes in the log.
     */
    @Override
    public void onTTSStart(String uttId) {
        Log.d(LOGTAG, "TTS starts speaking");
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            Log.d("CDA", "onKeyDown Called");
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onBackPressed() {
        Intent setIntent = new Intent(MainActivity.this,Welcome.class);
        setIntent.putExtra("PASOM", false);
        startActivity(setIntent);
    }

    /**
     * Set OnTouchListener
     */

    private void setOnTouchListener(){
        ConstraintLayout TextLoggerLayout = (ConstraintLayout) findViewById(R.id.main_activity);
        TextLoggerLayout.setOnTouchListener(
                new ConstraintLayout.OnTouchListener(){

                    @Override
                    public boolean onTouch(View v, MotionEvent m) {
                        handleTouch(m);
                        return true;
                    }

                });
    }

    /**
     * Gestiona el evento multitouch
     * @param m
     */

    void handleTouch(MotionEvent m){

        //Number of touches
        int pointerCount = m.getPointerCount();
        int action = m.getActionMasked();
        int actionIndex = m.getActionIndex();
        String actionString;

        // Si son tres dedos
        if(pointerCount == 3){
            switch (action)
            {
                case MotionEvent.ACTION_DOWN:
                    GLOBAL_TOUCH_POSITION_X = (int) m.getX(1);
                    break;
                case MotionEvent.ACTION_UP:
                    GLOBAL_TOUCH_CURRENT_POSITION_X = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    GLOBAL_TOUCH_CURRENT_POSITION_X = (int) m.getX(1);
                    int diff = GLOBAL_TOUCH_POSITION_X-GLOBAL_TOUCH_CURRENT_POSITION_X;

                    // Si se han levantado los dedos
                    if(prev_ant != GLOBAL_TOUCH_POSITION_X) {
                        prev_ant = GLOBAL_TOUCH_POSITION_X;
                        changeRoom = false;
                    }
                    // Si no se ha cambiado la habitacion, comprueba la diferencia
                    if(!changeRoom)
                        checkDiff(diff);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    GLOBAL_TOUCH_POSITION_X = (int) m.getX(1);
                    break;
                default:
                    actionString = "";
            }


        }
        // Si no han sido 3 dedos
        else {
            GLOBAL_TOUCH_POSITION_X = 0;
            GLOBAL_TOUCH_CURRENT_POSITION_X = 0;
            switch (action)
            {
                case MotionEvent.ACTION_POINTER_DOWN:
                    GLOBAL_TOUCH_POSITION_X = (int) m.getX(1);
                    setPoints(m); //

                    // Si son 2 dedos
                    if(pointerCount == 2){
                        // Comprueba si ocurre un doble tap en 300ms
                        tap++;
                        Handler doubleTap = new Handler();
                        doubleTap.postDelayed(new Runnable() {
                            // Lanza segunda hebra comprobadora
                            @Override
                            public void run() {
                                if(tap == 2){
                                    ImageButton speak = findViewById(R.id.speech_btn);
                                    Toast.makeText(getApplicationContext(),R.string.doubleTapStart,Toast.LENGTH_SHORT).show();
                                    speak.callOnClick();
                                }
                                tap = 0;
                            }
                        },300);
                    }

                    break;
                default:
                    actionString = "";
            }
        }
        pointerCount = 0;
    }


    /**
     * Calcula el deslizado con 3 dedos
     * @param diff
     */

    private void checkDiff(int diff) {

        boolean change = false;

        // Comprueba si supera el umbral

        // Slide Left
        if(diff < -MIN_DESPLACEMENT){

            roomSelected = (roomSelected+2) % 3;
            change = true;
            //Slide Right
        }else if(diff > MIN_DESPLACEMENT){
            roomSelected = (roomSelected+1) % 3;
            change = true;
        }

        // Si lo supera, actualiza habitación
        if(change) {
            changeRoom = true;
            Button room = null;

            switch (roomSelected){
                case 0:
                    room = (Button) findViewById(R.id.roomA);
                    break;
                case 1:
                    room = (Button) findViewById(R.id.roomB);
                    break;
                case 2:
                    room = (Button) findViewById(R.id.roomC);
                    break;
                default:
                    room = (Button) findViewById(R.id.roomA);

            }
            room.callOnClick();
        }
    }

    /**
     * Inicia el scanner QR
     */
    private void startScannerQR(){
        Intent ScannerQRActivity = new Intent(MainActivity.this, conversandroid.ScannerQR.class);
        startActivity(ScannerQRActivity);

    }

    private void initialize() {
        touchPoints = new ArrayList<PointF>();
    }

    /**
     * Fija los puntos a dibujar
     */
    public void setPoints(MotionEvent event) {
        touchPoints.clear(); // Elimina la lista anterior
        for (int index = 0; index < event.getPointerCount(); ++index) {
            // Obtiene la lista de puntos pulsados del MotionEvent
            touchPoints.add(new PointF(event.getX(index), event.getY(index)));
        }
    }
}

