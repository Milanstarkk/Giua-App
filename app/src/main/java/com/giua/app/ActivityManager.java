package com.giua.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;

import androidx.appcompat.app.AppCompatActivity;

import com.giua.webscraper.GiuaScraper;

public class ActivityManager extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO: da togliere in futuro
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //GiuaScraper.setSiteURL("http://hiemvault.ddns.net:9090");       //Usami solo per DEBUG per non andare continuamente nelle impostazioni
        //GiuaScraper.setDebugMode(true);

        final String defaultUrl = SettingsData.getSettingString(this, "defaultUrl");

        if (defaultUrl != null)
            GiuaScraper.setSiteURL(defaultUrl);

        if (LoginData.getUser(this).equals("")) {
            Intent intent = new Intent(ActivityManager.this, MainLogin.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(ActivityManager.this, LoadingScreenLogin.class);
            startActivity(intent);
        }
    }

    /**
     * Esci dall'applicazione simulando la pressione del tasto home
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onRestart() {
        onRestoreInstanceState(new Bundle());
        releaseInstance();
        super.onRestart();
    }

    @Override
    protected void onResume() {
        onRestoreInstanceState(new Bundle());
        releaseInstance();
        super.onResume();
    }

    @Override
    protected void onPause() {
        onSaveInstanceState(new Bundle());
        super.onPause();
    }

    @Override
    protected void onStop() {
        onSaveInstanceState(new Bundle());
        super.onStop();
    }
}