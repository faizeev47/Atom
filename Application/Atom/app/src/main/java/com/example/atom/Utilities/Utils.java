package com.example.atom.Utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.example.atom.LoginActivity;
import com.example.atom.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final String LOG_TAG = Utils.class.getSimpleName() + " Logging:";

    public static void fadeAppearViewObject(View view, long duration) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(duration);
    }

    public static void fadeAppearTextView(TextView view, String text, long duration) {
        view.setAlpha(0f);
        view.setText(text);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(duration);
    }

    public static int getTimeOfDayDrawableIndex(int hourOfDay) {
        if (hourOfDay >= 5 && hourOfDay < 9) {
            return 0;
        } else if (hourOfDay >= 9 && hourOfDay < 16) {
            return 1;
        } else if (hourOfDay >= 16 && hourOfDay < 18) {
            return 2;
        } else {
            return 3;
        }
    }


    public static String toFirstCaps(String string) {
        return Character.toString(string.charAt(0)).toUpperCase() + string.substring(1, string.length());
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static boolean connectionActive(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager != null && connectivityManager.getActiveNetwork() != null;
    }

    static Pattern alphabets = Pattern.compile("[^a-zA-Z ]");

    static Pattern pdfRemover = Pattern.compile("\\.pdf");
    static Pattern capitalizer = Pattern.compile("\\b[a-z](\\S*)\\b");
    static Pattern wordSeperator = Pattern.compile("[a-z][A-Z]");
    static Pattern specialRemover = Pattern.compile("[_]");

    public static void redirectToLogin(Context packageContext, Activity currentActivity) {
        Intent redirectLoginIntent = new Intent(packageContext, LoginActivity.class);
        packageContext.startActivity(redirectLoginIntent);
        currentActivity.finish();
    }

    public static boolean isEmailValid(String email) {
        if (email == null) {
            return false;
        }
        if (!email.contains("@")) {
            return false;
        } else {
            return Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }
    }

    public static boolean containsSpecial(String word) {
        if (word == null) {
            return false;
        }
        if (word.isEmpty()) {
            return false;
        }
        else {
            return alphabets.matcher(word).find();
        }
    }

    public static boolean passwordIsLong(String password) {
        return password.length() >= 8;
    }

    public static String formatFileName(String filename) {
        Matcher matcher = pdfRemover.matcher(filename);
        String formatted = filename;
        while (matcher.find()) {
            formatted = filename.substring(0, matcher.start()) + filename.substring(matcher.end());
        }

        matcher = specialRemover.matcher(formatted);
        while (matcher.find()) {
            formatted = formatted.replaceAll(specialRemover.pattern(), " ");
        }

        matcher = wordSeperator.matcher(formatted);
        while (matcher.find()) {
            formatted =  formatted.substring(0, matcher.start() + 1) + " " + formatted.substring(matcher.start() + 1);
            matcher = wordSeperator.matcher(formatted);
        }

        matcher = capitalizer.matcher(formatted);
        while (matcher.find()) {
            String matched = matcher.group();
            String replacement = ((Character)matched.charAt(0)).toString().toUpperCase() + matched.substring(matched.length() > 1 ? 1 : 0).toLowerCase();
            formatted = formatted.replaceFirst(matcher.group(), replacement);
        }


        return formatted;
    }

    public static String getIdFromEmail(String email) {
        if (email.isEmpty()) {
            return "";
        }
        String outputStr = "";
        for (int i = 0, l = email.length(); i < l; i++) {
            outputStr += Integer.toString((int)email.charAt(i));
        }
        return outputStr;
    }

    public static long getClockDifference(Date startClock, Date endClock, boolean inSeconds) {
        long startTime = startClock.getTime();
        long endTime = endClock.getTime();
        return inSeconds ? (endTime - startTime) / 1000 : endTime - startTime ;
    }

    public static String getTimeWithPeriod(int hours) {
        return String.format(Locale.ENGLISH, "%02d:00 %s", hours % 12, hours >= 12 ? "p.m." : "a.m.");
    }

    public static String getFormattedTime(long timeInSeconds) {
        long seconds = timeInSeconds % 60;
        long hours = (timeInSeconds - seconds) / 3600;
        long minutes = (timeInSeconds - (hours * 3600) - seconds)/60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static class PercentFormatter extends ValueFormatter
    {

        public DecimalFormat mFormat;
        private PieChart pieChart;
        private boolean percentSignSeparated;

        public PercentFormatter() {
            mFormat = new DecimalFormat("###,###,##0.0");
            percentSignSeparated = true;
        }

        // Can be used to remove percent signs if the chart isn't in percent mode
        public PercentFormatter(PieChart pieChart) {
            this();
            this.pieChart = pieChart;
        }

        // Can be used to remove percent signs if the chart isn't in percent mode
        public PercentFormatter(PieChart pieChart, boolean percentSignSeparated) {
            this(pieChart);
            this.percentSignSeparated = percentSignSeparated;
        }

        @Override
        public String getFormattedValue(float value) {
            return mFormat.format(value) + (percentSignSeparated ? " %" : "%");
        }

        @Override
        public String getPieLabel(float value, PieEntry pieEntry) {
            if (pieChart != null && pieChart.isUsePercentValuesEnabled()) {
                // Converted to percent
                return getFormattedValue(value);
            } else {
                // raw value, skip percent sign
                return mFormat.format(value);
            }
        }

    }

}


