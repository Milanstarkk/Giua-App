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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

public class AppData {

    /**
     * Controlla la memorizzazione dei dati dell'app
     */
    private static final String appDataPreferenceKey = "appData";

    //Chiavi per il salvataggio delle cose offline
    private static final String voteKey = "votes";
    private static final String newsletterKey = "newsletters";
    private static final String alertsKey = "alerts";

    //Chiavi per il salvataggio dei dati per le notifiche
    private static final String numberNewslettersKey = "number_newsletters";
    private static final String numberAlertsKey = "number_alerts";
    private static final String numberVotesKey = "number_votes";
    private static final String notificationErrorsCounter = "notification_errors";

    //Chiavi per il salvataggio delle versioni
    private static final String lastUpdateVersionKey = "last_update_version";
    private static final String nextUpdateReminderKey = "next_update_reminder";
    private static final String updatePresenceKey = "update_presence";
    private static final String logsStorageKey = "logs_storage";
    private static final String appVersionKey = "app_version";

    //Chiavi per il salvataggio delle configurazioni per più account
    private static final String allAccountNamesKey = "all_account_names";
    private static final String allAccountPasswordsKey = "all_account_passwords";
    private static final String allAccountCookiesKey = "all_account_cookies";

    private static final String introStatusKey = "intro_status";
    private static final String crashStatusKey = "crash_status";
    private static final String lastSentReportTimeKey = "last_sent_report_time";


    private static SharedPreferences getSharedPreferences(final Context context) {
        return context.getSharedPreferences(appDataPreferenceKey, Context.MODE_PRIVATE);
    }

    //region Dati per l'offline

    public static void saveVotesString(final Context context, final String jsonString) {
        getSharedPreferences(context).edit()
                .putString(voteKey, jsonString)
                .apply();
    }

    public static String getVotesString(final Context context) {
        return getSharedPreferences(context).getString(voteKey, null);
    }

    public static void saveNewslettersString(final Context context, final String jsonString) {
        getSharedPreferences(context).edit()
                .putString(newsletterKey, jsonString)
                .apply();
    }

    public static String getNewslettersString(final Context context) {
        return getSharedPreferences(context).getString(newsletterKey, null);
    }

    public static void saveAlertsString(final Context context, final String jsonString) {
        getSharedPreferences(context).edit()
                .putString(alertsKey, jsonString)
                .apply();
    }

    public static String getAlertsString(final Context context) {
        return getSharedPreferences(context).getString(alertsKey, null);
    }

    //endregion

    //region Dati per le notifiche

    public static void saveNumberNewslettersInt(final Context context, final int value) {
        getSharedPreferences(context).edit()
                .putInt(numberNewslettersKey, value)
                .apply();
    }

    public static int getNumberNewslettersInt(final Context context) {
        return getSharedPreferences(context).getInt(numberNewslettersKey, -1);
    }

    public static void saveNumberAlertsInt(final Context context, final int value) {
        getSharedPreferences(context).edit()
                .putInt(numberAlertsKey, value)
                .apply();
    }

    public static int getNumberAlertsInt(final Context context) {
        return getSharedPreferences(context).getInt(numberAlertsKey, -1);
    }

    public static void saveNumberVotesInt(final Context context, final int value) {
        getSharedPreferences(context).edit()
                .putInt(numberVotesKey, value)
                .apply();
    }

    public static int getNumberVotesInt(final Context context) {
        return getSharedPreferences(context).getInt(numberVotesKey, -1);
    }

    public static int getNumberNotificationErrors(final Context context) {
        return getSharedPreferences(context).getInt(notificationErrorsCounter, 0);
    }

    public static void saveNumberNotificationErrors(final Context context, final int value) {
        getSharedPreferences(context).edit()
                .putInt(notificationErrorsCounter, value)
                .apply();
    }

    //endregion

    //region Dati dell'utlimo aggiornamento trovato

    public static void saveLastUpdateVersionString(final Context context, final String value) {
        getSharedPreferences(context).edit()
                .putString(lastUpdateVersionKey, value)
                .apply();
    }

    public static String getLastUpdateVersionString(final Context context) {
        return getSharedPreferences(context).getString(lastUpdateVersionKey, "");
    }

    public static void saveAppVersion(final Context context, final String value) {
        getSharedPreferences(context).edit()
                .putString(appVersionKey, value)
                .apply();
    }

    public static String getAppVersion(final Context context) {
        return getSharedPreferences(context).getString(appVersionKey, "");
    }

    @SuppressLint("SimpleDateFormat")
    public static void saveLastUpdateReminderDate(final Context context, final Calendar date) {
        getSharedPreferences(context).edit()
                .putString(nextUpdateReminderKey, date.get(Calendar.DAY_OF_YEAR) + "#" + date.get(Calendar.YEAR))
                .apply();
    }

    @SuppressLint("SimpleDateFormat")
    public static String getLastUpdateReminderDate(final Context context) {
        return getSharedPreferences(context).getString(nextUpdateReminderKey, "");
    }

    //endregion

    //region Dati per più account

    public static void addAccountCredentials(final Context context, final String username, final String password) {
        getSharedPreferences(context).edit()
                .putString(allAccountNamesKey, getAllAccountNames(context) + username + ";")
                .putString(allAccountPasswordsKey, getAllAccountPasswords(context) + password + ";")
                .apply();
    }

    public static void removeAccountCredentialsOfIndex(final Context context, final int index) {
        String[] usernames = getAllAccountNames(context).split(";");
        String[] passwords = getAllAccountPasswords(context).split(";");

        if (index >= usernames.length) return;
        usernames[index] = "";
        passwords[index] = "";

        StringBuilder usernameAppDataString = new StringBuilder();
        for (String username : usernames) {
            if (!username.equals(""))
                usernameAppDataString.append(username).append(";");
        }
        StringBuilder passwordAppDataString = new StringBuilder();
        for (String password : passwords) {
            if (!password.equals(""))
                passwordAppDataString.append(password).append(";");
        }
        getSharedPreferences(context).edit()
                .putString(allAccountNamesKey, usernameAppDataString.toString())
                .putString(allAccountPasswordsKey, passwordAppDataString.toString())
                .apply();

        //usernames
    }

    public static void saveAllAccountNames(final Context context, final String value) {
        getSharedPreferences(context).edit()
                .putString(allAccountNamesKey, value)
                .apply();
    }

    public static String getAllAccountNames(final Context context) {
        return getSharedPreferences(context).getString(allAccountNamesKey, "");
    }

    public static void saveAllAccountPasswords(final Context context, final String value) {
        getSharedPreferences(context).edit()
                .putString(allAccountPasswordsKey, value)
                .apply();
    }

    public static String getAllAccountPasswords(final Context context) {
        return getSharedPreferences(context).getString(allAccountPasswordsKey, "");
    }

    //endregion

    public static void saveIntroStatus(final Context context, final int value) {
        getSharedPreferences(context).edit()
                .putInt(introStatusKey, value)
                .apply();
    }

    public static int getIntroStatus(final Context context) {
        return getSharedPreferences(context).getInt(introStatusKey, -1);
    }

    public static void saveLogsString(final Context context, final String value) {
        getSharedPreferences(context).edit()
                .putString(logsStorageKey, value)
                .apply();
    }

    public static String getLogsString(final Context context) {
        return getSharedPreferences(context).getString(logsStorageKey, "");
    }

    public static void saveCrashStatus(final Context context, final boolean value) {
        getSharedPreferences(context).edit()
                .putBoolean(crashStatusKey, value)
                .apply();
    }

    public static boolean getCrashStatus(final Context context) {
        return getSharedPreferences(context).getBoolean(crashStatusKey, false);
    }

    public static void saveLastSentReportTime(final Context context, final long value) {
        getSharedPreferences(context).edit()
                .putLong(lastSentReportTimeKey, value)
                .apply();
    }

    public static long getLastSentReportTime(final Context context) {
        return getSharedPreferences(context).getLong(lastSentReportTimeKey, -1L);
    }

    /*
      Questa funzione, facendo una richiesta HTTP, aumenta il numero di visite
      Piratepx tiene traccia delle visite per 30 giorni
      In questo momento stiamo contando le visite per:
      - Nuove installazioni
      - Login studente/genitore
      - Errori di webview
      - Crash
      - Aggiornamenti
      - Easter egg

      @param name Nome dell'oggetto su cui aumentare le visite
     */
    /*public static void increaseVisitCount(String name){
        if(BuildConfig.BUILD_TYPE.equals("debug")){
            //Ignora le build di debug
            return;
        }
        try {
            Jsoup.newSession().url("https://app.piratepx.com/ship?p=6af8462c-efe0-4f9a-96d0-ec9a8bbb9060&i=" + name)
                    .get();
        } catch (IOException ignored) { } //Non ci interessa se va in errore

    }*/
}
