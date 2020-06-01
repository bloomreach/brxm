/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.query.lucene;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class StandardHippoAnalyzerTest {

    private static final String[] EXP_CJK_STOP_WORDS = { "a", "and", "are", "as", "at", "be", "but", "by", "for", "if",
            "in", "into", "is", "it", "no", "not", "of", "on", "or", "s", "such", "t", "that", "the", "their", "then",
            "there", "these", "they", "this", "to", "was", "will", "with", "", "www" };

    private static final String[] EXP_SPANISH_STOP_WORDS = { " a ", " ante ", " bajo ", " con ", " de ", " desde ",
            " durante ", " en ", " entre ", " excepto ", " hacia ", " hasta ", " mediante ", " para ", " por ",
            " salvo ", " segun ", " según ", " sin ", " sobre ", " y ", " o ", " u ", " tras ", " el ", " la ", " lo ",
            " los ", " las ", " un ", " una ", " unos ", " unas " };

    private static final String[] EXP_DUTCH_STOP_SET = { "de", "en", "van", "ik", "te", "dat", "die", "in", "een", "hij",
            "het", "niet", "zijn", "is", "was", "op", "aan", "met", "als", "voor", "had", "er", "maar", "om", "hem",
            "dan", "zou", "of", "wat", "mijn", "men", "dit", "zo", "door", "over", "ze", "zich", "bij", "ook", "tot",
            "je", "mij", "uit", "der", "daar", "haar", "naar", "heb", "hoe", "heeft", "hebben", "deze", "u", "want",
            "nog", "zal", "me", "zij", "nu", "ge", "geen", "omdat", "iets", "worden", "toch", "al", "waren", "veel",
            "meer", "doen", "toen", "moet", "ben", "zonder", "kan", "hun", "dus", "alles", "onder", "ja", "eens",
            "hier", "wie", "werd", "altijd", "doch", "wordt", "wezen", "kunnen", "ons", "zelf", "tegen", "na", "reeds",
            "wil", "kon", "niets", "uw", "iemand", "geweest", "andere" };

    private static final String[] EXP_GERMAN_STOP_WORDS = { "einer", "eine", "eines", "einem", "einen", "der", "die", "das",
            "dass", "daß", "du", "er", "sie", "es", "was", "wer", "wie", "wir", "und", "oder", "ohne", "mit", "am",
            "im", "in", "aus", "auf", "ist", "sein", "war", "wird", "ihr", "ihre", "ihres", "als", "für", "von",
            "mit", "dich", "dir", "mich", "mir", "mein", "sein", "kein", "durch", "wegen", "wird" };

    private static final String[] EXP_FRENCH_STOP_WORDS = { "a", "afin", "ai", "ainsi", "après", "attendu", "au",
            "aujourd", "auquel", "aussi", "autre", "autres", "aux", "auxquelles", "auxquels", "avait", "avant", "avec",
            "avoir", "c", "car", "ce", "ceci", "cela", "celle", "celles", "celui", "cependant", "certain", "certaine",
            "certaines", "certains", "ces", "cet", "cette", "ceux", "chez", "ci", "combien", "comme", "comment",
            "concernant", "contre", "d", "dans", "de", "debout", "dedans", "dehors", "delà", "depuis", "derrière",
            "des", "désormais", "desquelles", "desquels", "dessous", "dessus", "devant", "devers", "devra", "divers",
            "diverse", "diverses", "doit", "donc", "dont", "du", "duquel", "durant", "dès", "elle", "elles", "en",
            "entre", "environ", "est", "et", "etc", "etre", "eu", "eux", "excepté", "hormis", "hors", "hélas", "hui",
            "il", "ils", "j", "je", "jusqu", "jusque", "l", "la", "laquelle", "le", "lequel", "les", "lesquelles",
            "lesquels", "leur", "leurs", "lorsque", "lui", "là", "ma", "mais", "malgré", "me", "merci", "mes",
            "mien", "mienne", "miennes", "miens", "moi", "moins", "mon", "moyennant", "même", "mêmes", "n", "ne",
            "ni", "non", "nos", "notre", "nous", "néanmoins", "nôtre", "nôtres", "on", "ont", "ou", "outre", "où",
            "par", "parmi", "partant", "pas", "passé", "pendant", "plein", "plus", "plusieurs", "pour", "pourquoi",
            "proche", "près", "puisque", "qu", "quand", "que", "quel", "quelle", "quelles", "quels", "qui", "quoi",
            "quoique", "revoici", "revoilà", "s", "sa", "sans", "sauf", "se", "selon", "seront", "ses", "si", "sien",
            "sienne", "siennes", "siens", "sinon", "soi", "soit", "son", "sont", "sous", "suivant", "sur", "ta", "te",
            "tes", "tien", "tienne", "tiennes", "tiens", "toi", "ton", "tous", "tout", "toute", "toutes", "tu", "un",
            "une", "va", "vers", "voici", "voilà", "vos", "votre", "vous", "vu", "vôtre", "vôtres", "y", "à",
            "ça", "ès", "été", "être", "ô" };

    private static final String[] EXP_BRAZILIAN_STOP_WORDS = { "a", "ainda", "alem", "ambas", "ambos", "antes", "ao",
            "aonde", "aos", "apos", "aquele", "aqueles", "as", "assim", "com", "como", "contra", "contudo", "cuja",
            "cujas", "cujo", "cujos", "da", "das", "de", "dela", "dele", "deles", "demais", "depois", "desde", "desta",
            "deste", "dispoe", "dispoem", "diversa", "diversas", "diversos", "do", "dos", "durante", "e", "ela",
            "elas", "ele", "eles", "em", "entao", "entre", "essa", "essas", "esse", "esses", "esta", "estas", "este",
            "estes", "ha", "isso", "isto", "logo", "mais", "mas", "mediante", "menos", "mesma", "mesmas", "mesmo",
            "mesmos", "na", "nas", "nao", "nas", "nem", "nesse", "neste", "nos", "o", "os", "ou", "outra", "outras",
            "outro", "outros", "pelas", "pelas", "pelo", "pelos", "perante", "pois", "por", "porque", "portanto",
            "proprio", "propios", "quais", "qual", "qualquer", "quando", "quanto", "que", "quem", "quer", "se", "seja",
            "sem", "sendo", "seu", "seus", "sob", "sobre", "sua", "suas", "tal", "tambem", "teu", "teus", "toda",
            "todas", "todo", "todos", "tua", "tuas", "tudo", "um", "uma", "umas", "uns" };

    private static final String[] EXP_CZECH_STOP_WORDS = { "a", "s", "k", "o", "i", "u", "v", "z", "dnes", "cz",
            "t\u00edmto", "bude\u0161", "budem", "byli", "jse\u0161", "m\u016fj", "sv\u00fdm", "ta", "tomto", "tohle",
            "tuto", "tyto", "jej", "zda", "pro\u010d", "m\u00e1te", "tato", "kam", "tohoto", "kdo", "kte\u0159\u00ed",
            "mi", "n\u00e1m", "tom", "tomuto", "m\u00edt", "nic", "proto", "kterou", "byla", "toho", "proto\u017ee",
            "asi", "ho", "na\u0161i", "napi\u0161te", "re", "co\u017e", "t\u00edm", "tak\u017ee", "sv\u00fdch",
            "jej\u00ed", "sv\u00fdmi", "jste", "aj", "tu", "tedy", "teto", "bylo", "kde", "ke", "prav\u00e9", "ji",
            "nad", "nejsou", "\u010di", "pod", "t\u00e9ma", "mezi", "p\u0159es", "ty", "pak", "v\u00e1m", "ani",
            "kdy\u017e", "v\u0161ak", "neg", "jsem", "tento", "\u010dl\u00e1nku", "\u010dl\u00e1nky", "aby", "jsme",
            "p\u0159ed", "pta", "jejich", "byl", "je\u0161t\u011b", "a\u017e", "bez", "tak\u00e9", "pouze",
            "prvn\u00ed", "va\u0161e", "kter\u00e1", "n\u00e1s", "nov\u00fd", "tipy", "pokud", "m\u016f\u017ee",
            "strana", "jeho", "sv\u00e9", "jin\u00e9", "zpr\u00e1vy", "nov\u00e9", "nen\u00ed", "v\u00e1s", "jen",
            "podle", "zde", "u\u017e", "b\u00fdt", "v\u00edce", "bude", "ji\u017e", "ne\u017e", "kter\u00fd", "by",
            "kter\u00e9", "co", "nebo", "ten", "tak", "m\u00e1", "p\u0159i", "od", "po", "jsou", "jak",
            "dal\u0161\u00ed", "ale", "si", "se", "ve", "to", "jako", "za", "zp\u011bt", "ze", "do", "pro", "je", "na",
            "atd", "atp", "jakmile", "p\u0159i\u010dem\u017e", "j\u00e1", "on", "ona", "ono", "oni", "ony", "my", "vy",
            "j\u00ed", "ji", "m\u011b", "mne", "jemu", "tomu", "t\u011bm", "t\u011bmu", "n\u011bmu", "n\u011bmu\u017e",
            "jeho\u017e", "j\u00ed\u017e", "jeliko\u017e", "je\u017e", "jako\u017e", "na\u010de\u017e", };

    private static final String[] EXP_ALL_STOP_WORDS;

    static {
        EXP_ALL_STOP_WORDS = new String[EXP_CJK_STOP_WORDS.length + EXP_SPANISH_STOP_WORDS.length + EXP_DUTCH_STOP_SET.length
                + EXP_GERMAN_STOP_WORDS.length + EXP_FRENCH_STOP_WORDS.length + EXP_BRAZILIAN_STOP_WORDS.length
                + EXP_CZECH_STOP_WORDS.length];
        int destPos = 0;

        System.arraycopy(EXP_CJK_STOP_WORDS, 0, EXP_ALL_STOP_WORDS, destPos, EXP_CJK_STOP_WORDS.length);
        destPos += EXP_CJK_STOP_WORDS.length;

        System.arraycopy(EXP_SPANISH_STOP_WORDS, 0, EXP_ALL_STOP_WORDS, destPos, EXP_SPANISH_STOP_WORDS.length);
        destPos += EXP_SPANISH_STOP_WORDS.length;

        System.arraycopy(EXP_DUTCH_STOP_SET, 0, EXP_ALL_STOP_WORDS, destPos, EXP_DUTCH_STOP_SET.length);
        destPos += EXP_DUTCH_STOP_SET.length;

        System.arraycopy(EXP_GERMAN_STOP_WORDS, 0, EXP_ALL_STOP_WORDS, destPos, EXP_GERMAN_STOP_WORDS.length);
        destPos += EXP_GERMAN_STOP_WORDS.length;

        System.arraycopy(EXP_FRENCH_STOP_WORDS, 0, EXP_ALL_STOP_WORDS, destPos, EXP_FRENCH_STOP_WORDS.length);
        destPos += EXP_FRENCH_STOP_WORDS.length;

        System.arraycopy(EXP_BRAZILIAN_STOP_WORDS, 0, EXP_ALL_STOP_WORDS, destPos, EXP_BRAZILIAN_STOP_WORDS.length);
        destPos += EXP_BRAZILIAN_STOP_WORDS.length;

        System.arraycopy(EXP_CZECH_STOP_WORDS, 0, EXP_ALL_STOP_WORDS, destPos, EXP_CZECH_STOP_WORDS.length);
        destPos += EXP_CZECH_STOP_WORDS.length;
    }

    private static final Set<String> EXP_DEFAULT_STOP_SET = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(EXP_ALL_STOP_WORDS)));

    @Test
    public void testCompatibility() throws Exception {
        assertArrayEquals(EXP_CJK_STOP_WORDS, StandardHippoAnalyzer.CJK_STOP_WORDS);
        assertArrayEquals(EXP_SPANISH_STOP_WORDS, StandardHippoAnalyzer.SPANISH_STOP_WORDS);
        assertArrayEquals(EXP_DUTCH_STOP_SET, StandardHippoAnalyzer.DUTCH_STOP_SET);
        assertArrayEquals(EXP_GERMAN_STOP_WORDS, StandardHippoAnalyzer.GERMAN_STOP_WORDS);
        assertArrayEquals(EXP_FRENCH_STOP_WORDS, StandardHippoAnalyzer.FRENCH_STOP_WORDS);
        assertArrayEquals(EXP_BRAZILIAN_STOP_WORDS, StandardHippoAnalyzer.BRAZILIAN_STOP_WORDS);
        assertArrayEquals(EXP_CZECH_STOP_WORDS, StandardHippoAnalyzer.CZECH_STOP_WORDS);
        assertArrayEquals(EXP_ALL_STOP_WORDS, StandardHippoAnalyzer.ALL_STOP_WORDS);
        assertEquals(EXP_DEFAULT_STOP_SET, StandardHippoAnalyzer.DEFAULT_STOP_SET);
    }

    @Test
    public void testAsciiCodesOnlyInStopWordsProperties() throws Exception {
        String [] localeStrings = StandardHippoAnalyzer.getAvailableLocaleStringsFromStopWordsProperties();

        String resourceName;
        URL resource;
        InputStream input;
        char [] chars;
        char ch;

        for (String localeString : localeStrings) {
            resourceName = StandardHippoAnalyzer.class.getSimpleName() + "_" + localeString + ".properties";
            resource = StandardHippoAnalyzer.class.getResource(resourceName);

            if (resource == null) {
                fail("Cannot find stop words resource: " + resourceName);
            }

            input = resource.openStream();
            chars = IOUtils.toCharArray(input, "UTF-8");

            for (int i = 0; i < chars.length; i++) {
                ch = chars[i];

                if ((int) ch > 127) {
                    fail("Non-Java-escaped character found. " + resource.toString()
                            + ". Please correct it to use Java unicode encoding ('\\uXXXX') only.");
                }
            }

            input.close();
        }
    }
}
