package conversandroid;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;

import conversandroid.chatbot.R;

//Activity inicial de nuestra aplicación
public class Welcome extends AppCompatActivity {

    //Delaración de variables
    ImageView claqueta;
    ImageButton startB;
    private ArrayList<PointF> touchPoints = null;
    Handler dHandler = new Handler();
    private RelativeLayout wLayout = null;
    public Animation animRotate = null;
    public boolean PASO;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        //Cargamos la animación definida para la claqueta en la carpeta anim.
        animRotate = AnimationUtils.loadAnimation(this, R.anim.claquetaanimation);
        claqueta = (ImageView) findViewById(R.id.idClaquetaS);
        startB = (ImageButton) findViewById(R.id.idRotateB);
        //Inicializamos el vector de puntos
        initialize();
        PASO = false;
        //Definimos onClick para que se llame al main activity al pulsar el botón definido
        //para ello
        startB.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                claqueta.startAnimation(animRotate);
                dHandler.postDelayed(startActivityUser, 500);

            }
        });
    }

    // METODO IMPORTANTE
    // Control de la multipulsacion de la pantalla
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        // Define que accion se esta realizando en la pantalla
        // getAction(): clase de acción que se está ejecutando.
        // ACTION_MASK: máscara de bits de partes del código de acción.
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            // Pulsamos
            case MotionEvent.ACTION_DOWN: {
                setPoints(event);// Fija puntos
                Log.i("INFO", "Presión:" + event.getPressure());
                Log.i("INFO", "Tamaño:" + event.getSize());
                break;
            }
            // Movemos
            case MotionEvent.ACTION_MOVE: {
                setPoints(event);// Fija puntos nuevos
                Intent intent = getIntent();
                Boolean mainPASO =(Boolean) intent.getBooleanExtra("PASOM",false);
                if (touchPoints.size() == 2 && (!PASO || mainPASO)){ //Movemos con 2 dedos detectados

                    Toast.makeText(getApplicationContext(), R.string.startWelcome, Toast.LENGTH_SHORT).show();
                    claqueta.startAnimation(animRotate);//Iniciamos la animación
                    dHandler.postDelayed(startActivityUser, 500);   //Lanzamos main tras 500ms
                    PASO = true;
                }
                break;
            }
            // Levantamos
            case MotionEvent.ACTION_UP: {
                initialize(); // Borra la pantalla
                break;
            }

            // Pulsamos con mas de un dedo
            case MotionEvent.ACTION_POINTER_DOWN: {
                setPoints(event); //
                break;
            }
            // Levantamos
            case MotionEvent.ACTION_POINTER_UP: {
                initialize(); // Borra la pantalla
                break;
            }

        }
        return true;
    }

    //Inicializa el vector de puntos
    private void initialize() {
        touchPoints = new ArrayList<PointF>();
    }

    // Fija los puntos detectados
    public void setPoints(MotionEvent event) {
        touchPoints.clear(); // Elimina la lista anterior
        for (int index = 0; index < event.getPointerCount(); ++index) {
            // Obtiene la lista de puntos pulsados del MotionEvent
            touchPoints.add(new PointF(event.getX(index), event.getY(index)));
        }
    }

    //Ejecutamos el main en una nueva hebra con retardo tras ejecutar la animación
    private Runnable startActivityUser = new Runnable() {
        @Override
        public void run() {
            Intent MainActivity = new Intent(Welcome.this, conversandroid.MainActivity.class);
            startActivity(MainActivity);
        }
    };
}


