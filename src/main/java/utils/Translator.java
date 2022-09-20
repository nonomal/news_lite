package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class Translator {

    public String translate(String langFrom, String langTo, String text) {
        StringBuilder response;
        String scriptUrl = Common.SCRIPT_URL;
        try {
            String urlStr = scriptUrl +
                    "?q=" + URLEncoder.encode(text, "UTF-8") +
                    "&target=" + langTo +
                    "&source=" + langFrom;
            URL url = new URL(urlStr);
            response = new StringBuilder();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Translit().toTranslit(response.toString());
    }

    static class Translit {
        private static final Map<String, String> letters = new HashMap<>();
        static {
            letters.put("А", "A");
            letters.put("Б", "B");
            letters.put("В", "V");
            letters.put("Г", "G");
            letters.put("Д", "D");
            letters.put("Е", "E");
            letters.put("Ё", "E");
            letters.put("Ж", "Zh");
            letters.put("З", "Z");
            letters.put("И", "I");
            letters.put("Й", "I");
            letters.put("К", "K");
            letters.put("Л", "L");
            letters.put("М", "M");
            letters.put("Н", "N");
            letters.put("О", "O");
            letters.put("П", "P");
            letters.put("Р", "R");
            letters.put("С", "S");
            letters.put("Т", "T");
            letters.put("У", "U");
            letters.put("Ф", "F");
            letters.put("Х", "Kh");
            letters.put("Ц", "C");
            letters.put("Ч", "Ch");
            letters.put("Ш", "Sh");
            letters.put("Щ", "Sch");
            letters.put("Ъ", "'");
            letters.put("Ы", "Y");
            letters.put("Ь", "'");
            letters.put("Э", "E");
            letters.put("Ю", "Yu");
            letters.put("Я", "Ya");
            letters.put("а", "a");
            letters.put("б", "b");
            letters.put("в", "v");
            letters.put("г", "g");
            letters.put("д", "d");
            letters.put("е", "e");
            letters.put("ё", "e");
            letters.put("ж", "zh");
            letters.put("з", "z");
            letters.put("и", "i");
            letters.put("й", "i");
            letters.put("к", "k");
            letters.put("л", "l");
            letters.put("м", "m");
            letters.put("н", "n");
            letters.put("о", "o");
            letters.put("п", "p");
            letters.put("р", "r");
            letters.put("с", "s");
            letters.put("т", "t");
            letters.put("у", "u");
            letters.put("ф", "f");
            letters.put("х", "h");
            letters.put("ц", "c");
            letters.put("ч", "ch");
            letters.put("ш", "sh");
            letters.put("щ", "sch");
            letters.put("ъ", "'");
            letters.put("ы", "y");
            letters.put("ь", "'");
            letters.put("э", "e");
            letters.put("ю", "yu");
            letters.put("я", "ya");
        }

        private String toTranslit(String text) {
            StringBuilder sb = new StringBuilder(text.length());
            for (int i = 0; i<text.length(); i++) {
                String l = text.substring(i, i+1);
                sb.append(letters.getOrDefault(l, l));
            }
            return sb.toString().toLowerCase();
        }
    }

}