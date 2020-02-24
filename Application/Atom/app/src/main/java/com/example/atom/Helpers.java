package com.example.atom;

import android.util.Log;
import android.util.Patterns;

import java.io.Console;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helpers {

    static Pattern alphabets = Pattern.compile("[^a-zA-Z ]");

    static Pattern pdfRemover = Pattern.compile("\\.pdf");
    static Pattern capitalizer = Pattern.compile("\\b[a-z](\\S*)\\b");
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
        String formated = filename;
        while (matcher.find()) {
            formated = filename.substring(0, matcher.start()) + filename.substring(matcher.end());
        }

        matcher = specialRemover.matcher(formated);
        while (matcher.find()) {
            formated = formated.replaceAll(specialRemover.pattern(), " ");
        }

        matcher = capitalizer.matcher(formated);
        while (matcher.find()) {
            String matched = matcher.group();
            String replacement = ((Character)matched.charAt(0)).toString().toUpperCase() + matched.substring(matched.length() > 1 ? 1 : 0).toLowerCase();
            formated = formated.replaceFirst(matcher.group(), replacement);
        }


        return formated;
    }
}
