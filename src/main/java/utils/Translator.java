package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Translator {

    public String translate(String langFrom, String langTo, String text) {
        StringBuilder response = null;
        String scriptUrl = "https://script.google.com/macros/s/AKfycby5Oh70-SremozUCw4uZl6EopFwkqRCXU4PkuPOYjpcIFWwojpHUq2lQoVia896Doy7fg/exec";
        try {
            String urlStr = scriptUrl +
                    "?q=" + URLEncoder.encode(text, "UTF-8") +
                    "&target=" + langTo +
                    "&source=" + langFrom;
            URL url = new URL(urlStr);
            response = new StringBuilder();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (IOException e) {
            Common.console("translate error: " + e.getMessage());
        }
        return response.toString();
    }
}