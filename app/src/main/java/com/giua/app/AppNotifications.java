/*
 * Giua App
 * Android app to view data from the giua@school workbook
 * Copyright (C) 2021 - 2022 Hiem, Franck1421 and contributors
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

package com.giua.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.webkit.CookieManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.giua.objects.Alert;
import com.giua.objects.Newsletter;
import com.giua.objects.Vote;
import com.giua.webscraper.GiuaScraper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

//FIXME ?: Non supporta url diversi da quello del Giua per il login con Gsuite
public class AppNotifications extends BroadcastReceiver {
    private Context context;
    private NotificationManagerCompat notificationManager;
    private LoggerManager loggerManager;
    private GiuaScraper gS;
    private OfflineDBController offlineDB;
    private NotificationsDBController notificationsDBController;

    private String activeUsername = "";

    private boolean isDebugMode = false;
    private boolean canSendNotifications = false;
    private boolean canSendNotificationsVotes = false;
    private boolean canSendNotificationsHomeworks = false;
    private boolean canSendNotificationsTests = false;
    private boolean canSendNotificationsAlerts = false;
    private boolean canSendNotificationsNewsletters = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        loggerManager = new LoggerManager("AppNotifications", context);
        activeUsername = AppData.getActiveUsername(context);
        canSendNotifications = SettingsData.getSettingBoolean(context, SettingKey.NOTIFICATION);

        if (activeUsername.equals("") || !canSendNotifications) return;

        canSendNotificationsVotes = SettingsData.getSettingBoolean(context, SettingKey.VOTES_NOTIFICATION);
        canSendNotificationsHomeworks = SettingsData.getSettingBoolean(context, SettingKey.HOMEWORKS_NOTIFICATION);
        canSendNotificationsTests = SettingsData.getSettingBoolean(context, SettingKey.TESTS_NOTIFICATION);
        canSendNotificationsAlerts = SettingsData.getSettingBoolean(context, SettingKey.ALERTS_NOTIFICATION);
        canSendNotificationsNewsletters = SettingsData.getSettingBoolean(context, SettingKey.NEWSLETTERS_NOTIFICATION);
        isDebugMode = SettingsData.getSettingBoolean(context, SettingKey.DEBUG_MODE);

        notificationManager = NotificationManagerCompat.from(context);
        offlineDB = new OfflineDBController(context);
        notificationsDBController = new NotificationsDBController(context);

        new Thread(this::checkAndSendNotifications).start();
    }

    /**
     * Controlla e invia le notifiche in base ai dati che riceve.<br>
     * NON è Thread-Safe
     */
    private void checkAndSendNotifications() {
        if (activeUsername.equals("gsuite")) makeGsuiteLogin();
        else makeLogin();

        if (!gS.isSessionValid(gS.getCookie())) {
            loggerManager.e("Il login non ha funzionato");
            return;
        }

        //region Controllo notifiche

        if (canSendNotificationsVotes) checkNewsForVotes();
        if (canSendNotificationsAlerts) checkNewsForAlerts();
        if (canSendNotificationsNewsletters) checkNewsForNewsletters();
        if (canSendNotificationsHomeworks | canSendNotificationsTests) checkNewsForAgenda();

        //endregion

        offlineDB.close();
        notificationsDBController.close();

        Random r = new Random(SystemClock.elapsedRealtime());
        long interval = AlarmManager.INTERVAL_HOUR + r.nextInt(3_600_000);
        //long interval = 60_000; //DEBUG
        resetAlarm(interval);
    }

    private void checkNewsForAgenda() {
        List<Alert> allNewTestsHomeworks;   //Ci sono sia gli avvisi dei compiti sia delle verifiche

        try {
            allNewTestsHomeworks = gS.getAlertsPage(false).getAllAlertsWithFilters(false, "per la materia");
        } catch (Exception e) {
            loggerManager.w("Controllo delle VERIFICHE e dei COMPITI in background non riuscito: " + e + " " + e.getMessage());
            return;
        }

        NotificationsDBController notificationsDBController = new NotificationsDBController(context);

        List<Alert> allNewTests = new Vector<>(allNewTestsHomeworks.size());
        List<Alert> allNewHomeworks = new Vector<>(allNewTestsHomeworks.size());

        List<Alert> allOldTests = notificationsDBController.readAlertsTests();
        List<Alert> allOldHomeworks = notificationsDBController.readAlertsHomeworks();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ITALY);
        Date today = new Date();

        for (Alert alert : allNewTestsHomeworks) {
            try {
                //Se il compito o la verifica era per un giorno prima di oggi non notificarla
                if (today.after(simpleDateFormat.parse(alert.date))) continue;
            } catch (ParseException e) {
                loggerManager.w("Controllando gli avvisi per i compiti e le verifiche ho trovato una data che non capisco: " + alert);
                continue;
            }

            if (alert.object.startsWith("C"))
                allNewHomeworks.add(alert);
            else if (alert.object.startsWith("V")) {
                allNewTests.add(alert);
            } else
                loggerManager.w("Durante il controllo dei nuovi avvisi per i compiti e le verifiche ho trovato un avviso che non è nessuno dei due: " + alert);
        }

        notificationsDBController.replaceAlertsTests(allNewTests);
        notificationsDBController.replaceAlertsHomeworks(allNewHomeworks);

        //region Compiti

        if (canSendNotificationsHomeworks && allOldHomeworks.size() != 0) {
            allNewHomeworks.removeAll(allOldHomeworks);

            if (allNewHomeworks.size() > 0)
                notificationManager.notify(AppNotificationsParams.HOMEWORKS_NOTIFICATION_ID, createHomeworkNotification(allNewHomeworks));
        }

        //endregion

        //region Verifiche

        if (canSendNotificationsTests && allOldTests.size() == 0) {
            allNewTests.removeAll(allOldTests);

            if (allNewTests.size() > 0)
                notificationManager.notify(AppNotificationsParams.TESTS_NOTIFICATION_ID, createTestsNotification(allNewTests));
        }

        //endregion
    }

    private Notification createTestsNotification(List<Alert> allNewTests) {
        String title;
        String text = "Clicca per andare all'agenda";
        String bigText = "";
        int nNewTests = allNewTests.size();

        for (Alert alert : allNewTests) {
            String[] rawSubject = alert.object.split(" per la materia ");

            if (rawSubject.length > 1) {
                String subject = rawSubject[1];
                bigText += subject + " - " + alert.date;
            } else
                loggerManager.w("Non ho trovato la materia nell'avviso delle verifiche: " + alert);
        }

        if (nNewTests > 1)
            title = "Nuova verifica";
        else
            title = nNewTests + " nuove verifiche";

        return createNotificationWithBigText(title, text, bigText, AppNotificationsParams.AGENDA_NOTIFICATION_GOTO);
    }

    private Notification createHomeworkNotification(List<Alert> allNewHomeworks) {
        String title;
        String text = "Clicca per andare all'agenda";
        String bigText = "";
        int nNewHomeworks = allNewHomeworks.size();

        for (Alert alert : allNewHomeworks) {
            String[] rawSubject = alert.object.split(" per la materia ");

            if (rawSubject.length > 1) {
                String subject = rawSubject[1];
                bigText += subject + " - " + alert.date;
            } else
                loggerManager.w("Non o trovato la materia nell'avviso dei compiti: " + alert);
        }

        if (nNewHomeworks > 1)
            title = "Nuovo compito";
        else
            title = nNewHomeworks + " nuovi compiti";

        return createNotificationWithBigText(title, text, bigText, AppNotificationsParams.AGENDA_NOTIFICATION_GOTO);
    }

    //region Newsletters

    private void checkNewsForNewsletters() {
        List<Newsletter> allNewNewsletters;

        try {
            allNewNewsletters = gS.getNewslettersPage(false).getAllNewsletters(1);
        } catch (Exception e) {
            loggerManager.w("Controllo delle CIRCOLARI in background non riuscito: " + e + " " + e.getMessage());
            return;
        }

        List<Newsletter> allOldNewsletters = notificationsDBController.readNewsletters();
        notificationsDBController.replaceNewsletters(allNewNewsletters);

        allNewNewsletters.removeAll(allOldNewsletters);   //Rimuovo gli avvisi vecchi da quelli nuovi

        if (allNewNewsletters.size() == 0 || allOldNewsletters.size() == 0) return;

        notificationManager.notify(AppNotificationsParams.NEWSLETTERS_NOTIFICATION_ID, createNewslettersNotification(allNewNewsletters));
    }

    private Notification createNewslettersNotification(List<Newsletter> allNewNewsletters) {
        String notificationTitle = "";

        int nNewslettersToNotify = allNewNewsletters.size();

        if (nNewslettersToNotify > 1)
            notificationTitle = nNewslettersToNotify + " nuove circolari";
        else
            notificationTitle = "Nuova circolare";

        return createNotification(notificationTitle, AppNotificationsParams.NEWSLETTERS_NOTIFICATION_GOTO, AppNotificationsParams.NEWSLETTERS_NOTIFICATION_REQUEST_CODE);
    }

    //endregion
    //region Alerts

    private void checkNewsForAlerts() {
        List<Alert> allNewAlerts;

        try {
            allNewAlerts = gS.getAlertsPage(false).getAllAlerts(1);
        } catch (Exception e) {
            loggerManager.w("Controllo degli AVVISI in background non riuscito: " + e + " " + e.getMessage());
            return;
        }

        List<Alert> allOldAlerts = notificationsDBController.readAlerts();
        notificationsDBController.replaceAlerts(allNewAlerts);

        allNewAlerts.removeAll(allOldAlerts);   //Rimuovo gli avvisi vecchi da quelli nuovi

        if (allNewAlerts.size() == 0 || allOldAlerts.size() == 0) return;

        notificationManager.notify(AppNotificationsParams.ALERTS_NOTIFICATION_ID, createAlertsNotification(allNewAlerts));
    }

    private Notification createAlertsNotification(List<Alert> alertsToNotify) {
        String notificationTitle = "";

        int nAlertsToNotify = alertsToNotify.size();

        if (nAlertsToNotify > 1)
            notificationTitle = nAlertsToNotify + " nuovi avvisi";
        else
            notificationTitle = "Nuovo avviso";

        return createNotification(notificationTitle, AppNotificationsParams.ALERTS_NOTIFICATION_GOTO, AppNotificationsParams.ALERTS_NOTIFICATION_REQUEST_CODE);
    }

    //endregion
    //region Votes

    private void checkNewsForVotes() {
        Map<String, List<Vote>> allNewVotes;

        List<String> notifiedSubjects = new Vector<>(10);
        int notifiedVotesCounter = 0;

        try {
            allNewVotes = gS.getVotesPage(false).getAllVotes();
        } catch (Exception e) {
            loggerManager.w("Controllo dei VOTI in background non riuscito: " + e + " " + e.getMessage());
            return;
        }

        Map<String, List<Vote>> allOldVotes = notificationsDBController.readVotes();

        notificationsDBController.replaceVotes(allNewVotes);

        for (String subject : allNewVotes.keySet()) {
            if (allOldVotes.get(subject) == null) continue;

            List<Vote> _newVotes = allNewVotes.get(subject);

            if (_newVotes == null) continue;

            //Rimuovo tutti i voti vecchi da quelli nuovi cosi che rimangano solo quelli nuovi se ce ne sono.
            _newVotes.removeAll(allOldVotes.get(subject));

            if (_newVotes.size() == 0) continue;

            notifiedVotesCounter += _newVotes.size();
            notifiedSubjects.add(subject);
        }

        if (notifiedVotesCounter == 0 || notifiedSubjects.size() == 0 || allOldVotes.size() == 0) return;

        Notification notification = createVotesNotification(notifiedSubjects, notifiedVotesCounter);
        notificationManager.notify(AppNotificationsParams.VOTES_NOTIFICATION_ID, notification);
    }

    private Notification createVotesNotification(List<String> notifiedSubjects, int notifiedVotesCounter) {
        String notificationTitle;
        String notificationText = "";
        int notifiedSubjectsLength = notifiedSubjects.size();

        if (notifiedVotesCounter > 1)
            notificationTitle = "Sono stati pubblicati nuovi voti";
        else
            notificationTitle = "È stato pubblicato un nuovo voto in " + notifiedSubjects.get(0);

        if (notifiedSubjectsLength == 1)
            notificationText = "Materia: " + notifiedSubjects.get(0);
        else if (notifiedSubjectsLength > 1) {
            notificationText = "Materie: ";

            for (int i = 0; i < notifiedSubjectsLength; i++) {
                notificationText += notifiedSubjects.get(i);

                if (i != notifiedSubjectsLength - 1)
                    notificationText += ", ";
            }
        }

        return createNotification(notificationTitle, notificationText, AppNotificationsParams.VOTES_NOTIFICATION_GOTO, AppNotificationsParams.VOTES_NOTIFICATION_REQUEST_CODE);
    }

    //endregion

    private Notification createNotification(String title, String goTo, int requestCode) {
        Intent intent = new Intent(context, ActivityManager.class).putExtra("goTo", goTo).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return new NotificationCompat.Builder(context, "0")
                .setContentIntent(PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setSmallIcon(R.drawable.ic_giuaschool_black)
                .setContentTitle(title)
                .setContentText("Clicca per avere più informazioni")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    private Notification createNotification(String title, String content, String goTo, int requestCode) {
        Intent intent = new Intent(context, ActivityManager.class).putExtra("goTo", goTo).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return new NotificationCompat.Builder(context, "0")
                .setContentIntent(PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setSmallIcon(R.drawable.ic_giuaschool_black)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    private Notification createNotificationWithBigText(String title, String contentText, String bigText, String goTo) {
        Intent intent = new Intent(context, ActivityManager.class).putExtra("goTo", goTo).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return new NotificationCompat.Builder(context, "0")
                .setContentIntent(PendingIntent.getActivity(context, 5, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setSmallIcon(R.drawable.ic_giuaschool_black)
                .setContentTitle(title)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(bigText))
                .build();
    }

    private void makeLogin() {
        loggerManager.d("Faccio il login");

        String password = AccountData.getPassword(context, activeUsername);
        String cookie = AccountData.getCookie(context, activeUsername);
        String siteUrl = AccountData.getSiteUrl(context, activeUsername);
        String defaultUrl = SettingsData.getSettingString(context, SettingKey.DEFAULT_URL);

        if (siteUrl.equals("")) GiuaScraper.setSiteURL(defaultUrl);  //siteUrl non impostato quindi usa defaultUrl
        else GiuaScraper.setSiteURL(siteUrl);

        gS = new GiuaScraper(activeUsername, password, cookie, true, loggerManager);

        try {
            gS.login();
        } catch (Exception e) {
            logErrorInLoggerManager(e);
            sendErrorNotification(e.toString());
        }
    }

    private void sendErrorNotification(String error) {
        if (!isDebugMode) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "0")
                .setSmallIcon(R.drawable.ic_giuaschool_black)
                .setContentTitle("Si è verificato un errore")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(error))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(12, builder.build());
    }

    private void makeGsuiteLogin() {
        String cookie = getCookieForGsuiteLogin();

        if (cookie.equals("")) return;   //Cè stato un errore e dovrebbe già essere stato segnalato

        gS = new GiuaScraper("gsuite", "gsuite", cookie, true, loggerManager);
    }

    private String getCookieForGsuiteLogin() {
        loggerManager.d("Eseguo login con cookie di google");

        Connection session = Jsoup.newSession()
                .followRedirects(true)
                .userAgent("Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36");

        //Inserisco tutti i cookie di Google presenti nella webview in session
        String[] allCookiesRaw = CookieManager.getInstance().getCookie("https://accounts.google.com").split("; ");
        for (String cookie : allCookiesRaw) {
            session.cookie(cookie.split("=")[0], cookie.split("=")[1]);
        }

        try {
            session.newRequest().url("https://registro.giua.edu.it/login/gsuite").get();

            return session.cookieStore().get(new URI("https://registro.giua.edu.it")).toString().split("=")[1].replace("]", "");
        } catch (IOException | URISyntaxException e) {
            loggerManager.e("Cookie google e cookie giua non più validi, impossibile continuare");
        } catch (Exception e) {
            logErrorInLoggerManager(e);
            sendErrorNotification(e.toString());
        }

        return "";
    }

    private void resetAlarm(long interval) {
        Intent iAppNotifications = new Intent(context, AppNotifications.class);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, iAppNotifications, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + interval,
                pendingIntent);
    }

    private void logErrorInLoggerManager(Exception e) {
        loggerManager.e("Messaggio: " + e.getMessage() + " Errore: " + e);
    }
}