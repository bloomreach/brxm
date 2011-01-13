package org.hippoecm.repository;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/*
 * Issues:
 * - where to store key -> workflow configuration
 * - multiple queries -> to be implemented
 * - multiple queries but mixed in text/html -> google translate doesn't make much difference between the two
 * - what to do if text is too large to be translated in a single request
 * - retrieve result from text -> just send the html, do not chunk texts
 * - urlencode request text -> done
 */

public class TranslateTest {
    private String[] languages = new String[] {"Afrikaans", "af",
                                       "Albanian", "sq",
                                       "Arabic", "ar",
                                       "Belarusian", "be",
                                       "Bulgarian", "bg",
                                       "Catalan", "ca",
                                       "Chinese Simplified", "zh-CN",
                                       "Chinese raditional", "zh-TW",
                                       "Croatian", "hr",
                                       "Czech", "cs",
                                       "Danish", "da",
                                       "Dutch", "nl",
                                       "English", "en",
                                       "Estonian", "et",
                                       "Filipino", "tl",
                                       "Finnish", "fi",
                                       "French", "fr",
                                       "Galician", "gl",
                                       "German", "de",
                                       "Greek", "el",
                                       "Haitian Creole", "ht",
                                       "Hebrew", "iw",
                                       "Hindi", "hi",
                                       "Hungarian", "hu",
                                       "Icelandic", "is",
                                       "Indonesian", "id",
                                       "Irish", "ga",
                                       "Italian", "it",
                                       "Japanese", "ja",
                                       "Latvian", "lv",
                                       "Lithuanian", "lt",
                                       "Macedonian", "mk",
                                       "Malay", "ms",
                                       "Maltese", "mt",
                                       "Norwegian", "no",
                                       "Persian", "fa",
                                       "Polish", "pl",
                                       "Portuguese", "pt",
                                       "Romanian", "ro",
                                       "Russian", "ru",
                                       "Serbian", "sr",
                                       "Slovak", "sk",
                                       "Slovenian", "sl",
                                       "Spanish", "es",
                                       "Swahili", "sw",
                                       "Swedish", "sv",
                                       "Thai", "th",
                                       "Turkish", "tr",
                                       "Ukrainian", "uk",
                                       "Vietnamese", "vi",
                                       "Welsh", "cy",
                                       "Yiddish", "yi"};
    private int timeout = 10 * 1000; // timeout in milliseconds
    private String key;
    private String translationURL2 = "https://www.googleapis.com/language/translate/v2";
    private String translationURL1 = "https://ajax.googleapis.com/ajax/services/language/translate";
    private boolean useGet = false;
    private boolean useGoogleSecond = false;
    private boolean useAlwaysDetect = true;

    public TranslateTest() {
        this("INSERT-YOUR-KEY-HERE");
    }

    public TranslateTest(String googleKey) {
        this.key = googleKey;
    }

    @Ignore
    public void dummy() {
    }

    @Test
    public void test() throws Exception {
        List<String> list = new LinkedList<String>();
        list.add("Het is al geruime tijd een bekend gegeven dat een lezer, tijdens het bekijken van de layout van een pagina, afgeleid wordt door de tekstuele inhoud. Het belangrijke punt van het gebruik van Lorem Ipsum is dat het uit een min of meer normale verdeling van letters bestaat, in tegenstelling tot \"Hier uw tekst, hier uw tekst\" wat het tot min of meer leesbaar nederlands maakt. Veel desktop publishing pakketten en web pagina editors gebruiken tegenwoordig Lorem Ipsum als hun standaard model tekst, en een zoekopdracht naar \"lorem ipsum\" ontsluit veel websites die nog in aanbouw zijn. Verscheidene versies hebben zich ontwikkeld in de loop van de jaren, soms per ongeluk soms expres (ingevoegde humor en dergelijke).\n"+
                "Er zijn vele variaties van passages van Lorem Ipsum beschikbaar maar het merendeel heeft te lijden gehad van wijzigingen in een of andere vorm, door ingevoegde humor of willekeurig gekozen woorden die nog niet half geloofwaardig ogen. Als u een passage uit Lorum Ipsum gaat gebruiken dient u zich ervan te verzekeren dat er niets beschamends midden in de tekst verborgen zit. Alle Lorum Ipsum generators op Internet hebben de eigenschap voorgedefinieerde stukken te herhalen waar nodig zodat dit de eerste echte generator is op internet. Het gebruikt een woordenlijst van 200 latijnse woorden gecombineerd met een handvol zinsstructuur modellen om een Lorum Ipsum te genereren die redelijk overkomt. De gegenereerde Lorum Ipsum is daardoor altijd vrij van herhaling, ingevoegde humor of ongebruikelijke woorden etc.");
        list.add("Open Source is ons motto. Hippo committeert zich aan open source en open standaarden, omdat we geloven dat dit het nieuwe paradigma voor software ontwikkeling is. Actieve deelname aan open source software ontwikkeling is een onderdeel van onze strategie."
                + "Open Source betekent dat de broncode van de software vrij kopieerbaar en verspreidbaar is. De broncode is de tekst die programmeurs schrijven voordat deze wordt vertaald naar een voor computers leesbaar formaat."
                + "Doordat de broncode toegankelijk is, kunnen andere programmeurs zelf bepalen of de software geschikt is voor gebruik binnen een specifieke context en of er veranderingen doorgevoerd moeten worden. Gebruikers zijn dan niet langer gebonden aan één softwareleverancier."
                + "Open source software groeit organisch; slechte ideeën sterven uit, goede ideeën groeien door en worden hergebruikt in andere projecten. Vanuit een wereldwijd draagvlak worden deze concepten door vele ontwikkelaars ingebracht. Zij dragen de gezamenlijke zorg voor een continue kwaliteitscontrole en stellen strikte eisen aan de uitwisselbaarheid van de software. Dit wordt bereikt door internationaal geaccepteerde standaarden op te stellen, ofwel open standaarden. XML is een van de bekendste standaarden."
                + "De software wordt beter door het succes van voorgaande projecten en de expertise van ontwikkelaars en gebruikers wereldwijd. De kwaliteitsontwikkeling van open source software wordt vooral gedreven door de wensen van de eindgebruiker.");
        long t1 = System.currentTimeMillis();
        translate(list, "en", "nl");
        long t2 = System.currentTimeMillis();
        System.err.println("ELAPSED TIME "+(t2-t1));
        System.err.println(list.get(1));
    }

    public void translate(List<String> texts, String targetLanguage) throws Exception {
        translate(texts, targetLanguage, null);
    }

    public void translate(List<String> texts, String targetLanguage, String sourceLanguage) throws Exception {
        StringBuilder parameters = new StringBuilder();
        StringBuilder urlSpec;
        if(!useGoogleSecond) {
            urlSpec = new StringBuilder(translationURL1);
            parameters.append("v=1.0&format=text");
            if (!useAlwaysDetect && sourceLanguage != null && !sourceLanguage.trim().equals("")) {
                parameters.append("&langpair=").append(sourceLanguage).append("%7C").append(targetLanguage);
            } else {
                parameters.append("&langpair=%7C").append(targetLanguage);
            }
        } else {
            urlSpec = new StringBuilder(translationURL2);
            parameters.append("prettyprint=false&format=text&key=").append(key);
            if (sourceLanguage != null && !sourceLanguage.trim().equals("")) {
                parameters.append("&source=").append(sourceLanguage);
            }
            parameters.append("&target=").append(targetLanguage);
            parameters.append("&format=html");
        }
        for (String text : texts) {
            parameters.append("&q=").append(URLEncoder.encode(text, "UTF-8"));
        }
        if (useGet) {
            urlSpec.append("?").append(parameters);
        }
        URL url = new URL(urlSpec.toString());
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        PrintWriter parametersWriter = null;
        if (useGet) {
            connection.setRequestMethod("GET");
        } else {
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.addRequestProperty("X-HTTP-Method-Override", "GET");
            parametersWriter = new PrintWriter(connection.getOutputStream());
            parametersWriter.write(parameters.toString());
            parametersWriter.flush();
        }
        try {
            StringBuilder sb = new StringBuilder();
            Reader responseReader = new InputStreamReader(connection.getInputStream());
            char[] buffer = new char[1024];
            int len;
            do {
                len = responseReader.read(buffer);
                if (len > 0) {
                    sb.append(buffer);
                }
            } while (len >= 0);
            System.err.println("----------\n"+sb.toString()+"\n----------");
            JSONObject jsonObject = new JSONObject(sb.toString());
            if (!useGoogleSecond) {
                JSONArray jsonArray = jsonObject.getJSONArray("responseData");
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (jsonArray.getJSONObject(i).getString("responseStatus").equals("200")) {
                        texts.set(i, jsonArray.getJSONObject(i).getJSONObject("responseData").getString("translatedText"));
                    }
                }
            } else {
                JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("translations");
                for (int i = 0; i < jsonArray.length(); i++) {
                    texts.set(i, jsonArray.getJSONObject(i).getString("translatedText"));
                }
            }
        } finally { // http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
            connection.getInputStream().close();
            if (connection.getErrorStream() != null) {
                connection.getErrorStream().close();
                if (parametersWriter != null) {
                    parametersWriter.close();
                }
            }
        }
    }
}
