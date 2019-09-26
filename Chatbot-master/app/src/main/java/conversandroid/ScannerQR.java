package conversandroid;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.zxing.Result;

import conversandroid.chatbot.R;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

//Activity para el uso del QR
public class ScannerQR extends AppCompatActivity implements ZXingScannerView.ResultHandler {


    private ZXingScannerView scannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_qr);
        //Procedemos a lanzar la cámara para la lectura del QR
        ScannerQR(this.findViewById(android.R.id.content).getRootView());
    }

    //Comprobamos que disponemos del permiso de la cámara y en caso de no tenerlo
    //procedemos a solicitarlo
    private void checkCameraPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.i("Mensaje", "No se tiene permiso para la camara!.");
            //Solicitamos el permiso cuando no lo tiene
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 225);
        } else {
            Log.i("Mensaje", "Tienes permiso para usar la camara.");
        }
    }

    //Activamos la cámara para lectura de QR
    public void ScannerQR(View view){
        checkCameraPermission(); //Comprobamos permisos de la cámara
        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);
        scannerView.setResultHandler(this);
        scannerView.startCamera();//Lanzamos la cámara
    }

    @Override
    protected void onPause() {
        super.onPause();
        scannerView.stopCamera(); //Detenemos la cámara cuando pausemos la app
    }

    //Procesamos la respuesta tras detectar el QR con la cámara
    @Override
    public void handleResult(Result result) {
        scannerView.resumeCameraPreview(this); //Pausamos la cámara
        //Creamos una instancia del activity del reproductor
        Intent YTPlayer =new Intent(this,YoutubePlayer.class);
        //Obtenemos el id del vídeo obtenido del QR y lo mandamos al reproductor
        YTPlayer.putExtra("ID_YT_VIDEO", result.getText());
        startActivity(YTPlayer);//Lanzamos el reproductor
    }
}
