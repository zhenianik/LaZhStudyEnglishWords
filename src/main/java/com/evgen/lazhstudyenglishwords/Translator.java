package com.evgen.lazhstudyenglishwords;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translator {

    private static final String sourceLang = "en";
    private static final String targetLang = "ru";

    public static String translate(String msg) throws Exception {

        String result = "";

        if (msg == null || msg.equals("")) return "";

        msg = URLEncoder.encode(msg, "UTF-8");
        URL url = new URL("http://translate.googleapis.com/translate_a/single?client=gtx&sl=" + sourceLang + "&tl="
                + targetLang + "&dt=t&q=" + msg + "&ie=UTF-8&oe=UTF-8");

        URLConnection uc = url.openConnection();
        uc.setRequestProperty("User-Agent", "Mozilla/5.0");


        try (BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"))) {
            String inputLine;

            if ((inputLine = in.readLine()) != null) {

                Pattern p = Pattern.compile("\"([^\"]*)\"");
                Matcher m = p.matcher(inputLine);
                while (m.find()) {
                    result = m.group(1);
                    break;
                }
            }

        }
        return result;
    }
}