package conversandroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.net.URL;

import conversandroid.chatbot.R;

//Activity para reproducir vídeo obtenido por el QR o alguno que indiquemos
public class YoutubePlayer extends AppCompatActivity {

    String urlVideoYt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_player);

        //Parseamos la cadena para obtener el id del video
        urlVideoYt = "https://www.youtube.com/embed/" + getIdVideoYt(this.getIntent().getStringExtra("ID_YT_VIDEO"));//Falta el id del video
        WebView webView = (WebView) findViewById(R.id.webViewYT);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(urlVideoYt);//Pasamos la cadena con el id parseado para cargar el video
    }

    //Función para parsear url de youtube y obtener el id del vídeo
    private String getIdVideoYt(String url){
        return url.split("v=")[1].substring(0,11);
    }

    //Función para establecer varias variables para emplear en diversas funcionalidades al presionar
    //el botón de retroceso como mostrar la ayuda o realizar la animación.
    @Override
    public void onBackPressed() {
        Intent setIntent = new Intent(YoutubePlayer.this,MainActivity.class);
        setIntent.putExtra("PASOM", false);
        setIntent.putExtra("HELPSHOW", false);
        startActivity(setIntent);
    }
}
