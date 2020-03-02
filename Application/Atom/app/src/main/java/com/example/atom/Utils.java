package com.example.atom;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final String LOG_TAG = Utils.class.getSimpleName() + " Logging:";

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
}
