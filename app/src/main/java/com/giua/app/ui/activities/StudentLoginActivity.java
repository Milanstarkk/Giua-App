/*
 * Giua App
 * Android app to view data from the giua@school workbook 
 * Copyright (C) 2021 - 2021 Hiem, Franck1421 and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package com.giua.app.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.giua.app.AppData;
import com.giua.app.GlobalVariables;
import com.giua.app.LoggerManager;
import com.giua.app.LoginData;
import com.giua.app.R;
import com.giua.app.ui.fragments.ObscureLayoutView;
import com.giua.webscraper.GiuaScraper;
import com.google.android.material.snackbar.Snackbar;

public class StudentLoginActivity extends AppCompatActivity {

    WebView webView;
    ObscureLayoutView obscureLayoutView;
    final String userAgent = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36";
    String cookie = "";
    LoggerManager loggerManager;
    ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);
        loggerManager = new LoggerManager("StudentLoginActivity", this);
        loggerManager.d("onCreate chiamato");

        webView = findViewById(R.id.studentWebView);
        obscureLayoutView = findViewById(R.id.studentObscureLayoutView);
        progressBar = findViewById(R.id.login_google_progress);

        obscureLayoutView.setVisibility(View.GONE);

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
                loggerManager.d("Richiesto caricamento dell'url " + request.getUrl().toString());
                String requestedUrl = request.getUrl().toString();
                if (requestedUrl.equals("https://registro.giua.edu.it/") || requestedUrl.equals("https://registro.giua.edu.it/#")) {
                    loggerManager.d("Ottengo cookie del registro...");
                    String rawCookie = CookieManager.getInstance().getCookie("https://registro.giua.edu.it");
                    if (rawCookie != null) {
                        cookie = rawCookie.split("=")[1];
                        onStoppedWebView();
                        return true;
                    }
                    loggerManager.e("Errore, cookie ottenuto è null. Impossibile continuare");
                    new Thread(() -> AppData.increaseVisitCount("WebView cookie error")).start();
                    Snackbar.make(findViewById(android.R.id.content), "Login studente fallito, contatta gli sviluppatori", Snackbar.LENGTH_LONG).show();
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loggerManager.d("Caricamento pagina completato");
                webView.setVisibility(View.VISIBLE);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        webView.getSettings().setUserAgentString(userAgent);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.loadUrl("https://registro.giua.edu.it/login/gsuite");
    }


    private void onStoppedWebView() {
        loggerManager.d("onStoppedWebView chiamato");
        new Thread(() -> AppData.increaseVisitCount("Login OK (Studente/Google)")).start();
        webView.setVisibility(View.INVISIBLE);
        obscureLayoutView.setVisibility(View.VISIBLE);

        loggerManager.d("Creazione credenziali con cookie ottenuto da google");
        GlobalVariables.gS = new GiuaScraper("gsuite", "gsuite", cookie, true);
        LoginData.setCredentials(this, "gsuite", "gsuite", cookie);
        obscureLayoutView.setVisibility(View.GONE);
        loggerManager.d("Avvio DrawerActivity");
        Intent intent = new Intent(StudentLoginActivity.this, DrawerActivity.class);
        startActivity(intent);
    }
}