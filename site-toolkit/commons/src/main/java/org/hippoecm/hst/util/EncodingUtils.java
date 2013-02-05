/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncodingUtils {

    private final static Logger log = LoggerFactory.getLogger(EncodingUtils.class);
    
    public static String getEncodedPath(String unEncoded, HttpServletRequest request){
        StringBuilder encodedPath = new StringBuilder();
        String characterEncoding = request.getCharacterEncoding();
        if (characterEncoding == null) {
            characterEncoding = "ISO-8859-1";
        }
        try {
            for (String path : unEncoded.split("/")) {
                if (!"".equals(path)) {
                    encodedPath.append("/").append(URLEncoder.encode(path, characterEncoding));
                }
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Unsupported encoding: ", e);
            return unEncoded;
        }
        return encodedPath.toString();
    }
    
    public static String isoLatin1AccentReplacer(String input) {
        if(input == null) {
            return null;
        }

        char[] inputChars = input.toCharArray();
        // Worst-case length required:
        char[] output = new char[inputChars.length * 2];

        int outputPos = 0;

        int pos = 0;
        int length = inputChars.length;

        for (int i = 0; i < length; i++, pos++) {
            final char c = inputChars[pos];

            // Quick test: if it's not in range then just keep
            // current character
            if (c < '\u00c0')
                output[outputPos++] = c;
            else {
                switch (c) {
                case '\u00C0': // À
                case '\u00C1': // Á
                case '\u00C2': // Â
                case '\u00C3': // Ã
                case '\u00C4': // Ä
                case '\u00C5': // Å
                    output[outputPos++] = 'A';
                    break;
                case '\u00C6': // Æ
                    output[outputPos++] = 'A';
                    output[outputPos++] = 'E';
                    break;
                case '\u00C7': // Ç
                    output[outputPos++] = 'C';
                    break;
                case '\u00C8': // È
                case '\u00C9': // É
                case '\u00CA': // Ê
                case '\u00CB': // Ë
                    output[outputPos++] = 'E';
                    break;
                case '\u00CC': // Ì
                case '\u00CD': // Í
                case '\u00CE': // Î
                case '\u00CF': // Ï
                    output[outputPos++] = 'I';
                    break;
                case '\u00D0': // Ð
                    output[outputPos++] = 'D';
                    break;
                case '\u00D1': // Ñ
                    output[outputPos++] = 'N';
                    break;
                case '\u00D2': // Ò
                case '\u00D3': // Ó
                case '\u00D4': // Ô
                case '\u00D5': // Õ
                case '\u00D6': // Ö
                case '\u00D8': // Ø
                    output[outputPos++] = 'O';
                    break;
                case '\u0152': // Œ
                    output[outputPos++] = 'O';
                    output[outputPos++] = 'E';
                    break;
                case '\u00DE': // Þ
                    output[outputPos++] = 'T';
                    output[outputPos++] = 'H';
                    break;
                case '\u00D9': // Ù
                case '\u00DA': // Ú
                case '\u00DB': // Û
                case '\u00DC': // Ü
                    output[outputPos++] = 'U';
                    break;
                case '\u00DD': // Ý
                case '\u0178': // Ÿ
                    output[outputPos++] = 'Y';
                    break;
                case '\u00E0': // à
                case '\u00E1': // á
                case '\u00E2': // â
                case '\u00E3': // ã
                case '\u00E4': // ä
                case '\u00E5': // å
                    output[outputPos++] = 'a';
                    break;
                case '\u00E6': // æ
                    output[outputPos++] = 'a';
                    output[outputPos++] = 'e';
                    break;
                case '\u00E7': // ç
                    output[outputPos++] = 'c';
                    break;
                case '\u00E8': // è
                case '\u00E9': // é
                case '\u00EA': // ê
                case '\u00EB': // ë
                    output[outputPos++] = 'e';
                    break;
                case '\u00EC': // ì
                case '\u00ED': // í
                case '\u00EE': // î
                case '\u00EF': // ï
                    output[outputPos++] = 'i';
                    break;
                case '\u00F0': // ð
                    output[outputPos++] = 'd';
                    break;
                case '\u00F1': // ñ
                    output[outputPos++] = 'n';
                    break;
                case '\u00F2': // ò
                case '\u00F3': // ó
                case '\u00F4': // ô
                case '\u00F5': // õ
                case '\u00F6': // ö

                case '\u00F8': // ø
                    output[outputPos++] = 'o';
                    break;
                case '\u0153': // œ
                    output[outputPos++] = 'o';
                    output[outputPos++] = 'e';
                    break;
                case '\u00DF': // ß
                    output[outputPos++] = 's';
                    output[outputPos++] = 's';
                    break;
                case '\u00FE': // þ
                    output[outputPos++] = 't';
                    output[outputPos++] = 'h';
                    break;
                case '\u00F9': // ù
                case '\u00FA': // ú
                case '\u00FB': // û
                case '\u00FC': // ü
                    output[outputPos++] = 'u';
                    break;
                case '\u00FD': // ý
                case '\u00FF': // ÿ
                    output[outputPos++] = 'y';
                    break;
                default:
                    output[outputPos++] = c;
                    break;
                }
            }
        }

        // now take only the populated chars from output
        char[] outputChars = new char[outputPos];
        System.arraycopy(output, 0, outputChars, 0, outputPos);
        return new String(outputChars);
    }
}
