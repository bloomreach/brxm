/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The StringCodecFactory allows you access to symbolic named StringCodec's.
 * Typically a single instance of the StringCodecFactory is stored in the application framework.
 */
public class StringCodecFactory {

    private Map<String, StringCodec> codecs;

    /**
     * Initialized a StringCodecFactory with the given and fixed StringCodec mappings.
     * @param codecs a map of codecs to bind to their symbolic names.  The map becomes immutable.
     */
    public StringCodecFactory(Map<String, StringCodec> codecs) {
        // create a private immutable copy of the mapping
        this.codecs = new HashMap<>(codecs.size());
        // force all keys to lowercase
        for (Map.Entry<String, StringCodec> entry : codecs.entrySet()) {
            String key = entry.getKey();
            if (key != null) {
                key = key.toLowerCase();
    }
            this.codecs.put(key, entry.getValue());
        }
    }

    /**
     * Requests the default encoder to use.
     * @return returns the fall-back or default encoder to use, or null if non was defined.
     */
    public StringCodec getStringCodec() {
        return codecs.get(null);
    }

    /**
     * Requests which encoder to use for the given symbolic name.
     * @param encoding the symbolic name of the encoder that is requested
     * @return the {@link StringCodec} to use, which might be a fall-back encoder or null if non was defined.
     */
    public StringCodec getStringCodec(String encoding) {
        return getStringCodec(encoding, null);
    }

    /**
     * Requests which encoder to use for the given symbolic name and locale. If a locale is defined as
     * <code>language_country</code>, e.g. <code>en_GB</code>, it will first try <code>language_country</code> and then
     * <code>language</code>.
     *
     * @param encoding the symbolic name of the encoder that is requested
     * @param locale the locale for the requested codec
     * @return the {@link StringCodec} to use, which might be a fall-back encoder or null if non was defined.
     */
    public StringCodec getStringCodec(String encoding, String locale) {

        if (encoding != null) {
            encoding  = encoding.toLowerCase();
        }

        if (locale != null) {
            locale = locale.toLowerCase();
            if (codecs.containsKey(encoding + "." + locale)) {
                return  codecs.get(encoding + "." + locale);
            }
            else if(locale.indexOf('_') > 0) { // Check language only but skip locales like _GB
                locale = locale.substring(0, locale.indexOf('_'));
                if (codecs.containsKey(encoding + "." + locale)) {
                    return  codecs.get(encoding + "." + locale);
                }
            }
        }

        if (codecs.containsKey(encoding)) {
            return codecs.get(encoding);
        } else {
            return codecs.get(null);
        }
    }

    /**
     * Usage of this class discouraged.  If should only be used by frameworks to initialize the StringCodecFactory instance.
     * <p/>
     * Performs an identical encoding, i.e. returns an identical string for encoding and decoding.
     */
    public static final class IdentEncoding implements StringCodec {
        public String encode(String plain) {
            return plain;
        }

        public String decode(String encoded) {
            return encoded;
        }
    }

    /**
     * Direct usage of this class discouraged.  If should only be used by frameworks to initialize the StringCodecFactory instance.
     * <p/>
     * Performs a one-way encoding (no decoding possible) for translating any UTF-8 String to a suitable set of characters that can be used in URIs.
     * @see <a href="doc-files/encoding.html">Encoding of node names</a>
     */
    public static class UriEncoding implements StringCodec {
        public String encode(String utf8) {
            StringBuffer sb = new StringBuffer();
            char[] chars = utf8.toCharArray();
            boolean lastSpace = true;
            for (int i = 0; i < chars.length; i++) {
                boolean appendSpace = false;
                if ((chars[i] >= 0x00 && chars[i] <= 0x1f) || chars[i] == 0x7f || (chars[i] >= 0x80 && chars[i] <= 0x9f)) {
                    // control character
                } else if (chars[i] >= 0x80 && chars[i] <= 0xff) {
                    switch (chars[i]) {
                        case 0xA0: appendSpace = true; break;
                        case 0xA1:                         break;
                        case 0xA2: sb.append("ct");        break;
                        case 0xA3: sb.append("gbp");       break;
                        case 0xA4: sb.append("");          break;
                        case 0xA5: sb.append("yen");       break;
                        case 0xA6: sb.append("-");         break;
                        case 0xA7:                         break;
                        case 0xA8:                         break;
                        case 0xA9:                         break;
                        case 0xAA:                         break;
                        case 0xAB:                         break;
                        case 0xAC:                         break;
                        case 0xAD: sb.append("-");         break;
                        case 0xAE:                         break;
                        case 0xAF: sb.append("-");         break;
                        case 0xB0:                         break;
                        case 0xB1: sb.append("-");         break;
                        case 0xB2:                         break;
                        case 0xB3:                         break;
                        case 0xB4:                         break;
                        case 0xB5:                         break;
                        case 0xB6:                         break;
                        case 0xB7:                         break;
                        case 0xB8:                         break;
                        case 0xB9:                         break;
                        case 0xBA:                         break;
                        case 0xBB:                         break;
                        case 0xBC:                         break;
                        case 0xBD:                         break;
                        case 0xBE:                         break;

                        case 0xC0: sb.append('a');         break; // 192 À
                        case 0xC1: sb.append('a');         break; // 193 Á
                        case 0xC2: sb.append('a');         break; // 194 Â
                        case 0xC3: sb.append('a');         break; // 195 Ã
                        case 0xC4: sb.append('a');         break; // 196 Ä
                        case 0xC5: sb.append('a');         break; // 197 Å

                        case 0xC6: sb.append("ae");        break; // 198 Æ
                        case 0xC7: sb.append('c');         break; // 199 Ç

                        case 0xC8: sb.append('e');         break; // 200 È
                        case 0xC9: sb.append('e');         break; // 201 É
                        case 0xCA: sb.append('e');         break; // 202 Ê
                        case 0xCB: sb.append('e');         break; // 203 Ë

                        case 0xCC: sb.append('i');         break; // 204 Ì
                        case 0xCD: sb.append('i');         break; // 205 Í
                        case 0xCE: sb.append('i');         break; // 206 Î
                        case 0xCF: sb.append('i');         break; // 207 Ï

                        case 0xD0: sb.append("d");         break;

                        case 0xD1: sb.append("n");         break; // 209 Ñ
                        case 0xD2: sb.append("o");         break; // 210 Ò
                        case 0xD3: sb.append("o");         break; // 211 Ó
                        case 0xD4: sb.append("o");         break; // 212 Ô
                        case 0xD5: sb.append("o");         break; // 213 Õ
                        case 0xD6: sb.append("o");         break; // 214 Ö
                        case 0xD7: sb.append("x");         break; // 215 ×

                        case 0xD8: sb.append("o");         break;
                        case 0xD9: sb.append("u");         break;
                        case 0xDA: sb.append("u");         break;
                        case 0xDB: sb.append("u");         break;
                        case 0xDC: sb.append("u");         break;
                        case 0xDD: sb.append("y");         break;
                        case 0xDE: sb.append("y");         break;
                        case 0xDF: sb.append("ss");        break;
                        case 0xE0: sb.append("a");         break;
                        case 0xE1: sb.append("a");         break;
                        case 0xE2: sb.append("a");         break;
                        case 0xE3: sb.append("a");         break;
                        case 0xE4: sb.append("a");         break;
                        case 0xE5: sb.append("a");         break;
                        case 0xE6: sb.append("ae");        break;
                        case 0xE7: sb.append("c");         break;
                        case 0xE8: sb.append("e");         break;
                        case 0xE9: sb.append("e");         break;
                        case 0xEA: sb.append("e");         break;
                        case 0xEB: sb.append("e");         break;
                        case 0xEC: sb.append("i");         break;
                        case 0xED: sb.append("i");         break;
                        case 0xEE: sb.append("i");         break;
                        case 0xEF: sb.append("i");         break;
                        case 0xF0: sb.append("d");         break;
                        case 0xF1: sb.append("n");         break;
                        case 0xF2: sb.append("o");         break;
                        case 0xF3: sb.append("o");         break;
                        case 0xF4: sb.append("o");         break;
                        case 0xF5: sb.append("o");         break;
                        case 0xF6: sb.append("o");         break;
                        case 0xF7:                         break;
                        case 0xF8: sb.append("o");         break;
                        case 0xF9: sb.append("u");         break;
                        case 0xFA: sb.append("u");         break;
                        case 0xFB: sb.append("u");         break;
                        case 0xFC: sb.append("u");         break;
                        case 0xFD: sb.append("u");         break;
                        case 0xFE: sb.append("y");         break;
                        case 0xFF: sb.append("y");         break;
                        default:
                    }
                } else if (chars[i] >= 0x00 && chars[i] <= 0x7f) {
                    switch (chars[i]) {
                        case 0x20:
                            appendSpace = true;
                            break;
                        case 0x21:
                            break; // !
                        case 0x22:
                            break; // "
                        case 0x23:
                            break; // #
                        case 0x24:
                            sb.append("usd");
                            break; // $
                        case 0x25:
                            break; // %
                        case 0x26:
                            break; // &
                        case 0x27:
                            break; // '
                        case 0x28:
                            break; // (
                        case 0x29:
                            break; // )
                        case 0x2A:
                            appendSpace = true;
                            break;
                        case 0x2B:
                            appendSpace = true;
                            break; // +
                        case 0x2C:
                            break; // ,
                        case 0x2F:
                            sb.append("-");
                            break; // /
                        case 0x3A:
                            appendSpace = true;
                            break; // :
                        case 0x3B:
                            appendSpace = true;
                            break; // ;
                        case 0x3C:
                            break; // <
                        case 0x3D:
                            sb.append("-");
                            break; // =
                        case 0x3E:
                            break; // >
                        case 0x3F:
                            break; // ?
                        case 0x40:
                            sb.append("-at-");
                            break; // @
                        case 0x5B:
                            break; // [
                        case 0x5C:
                            sb.append("-");
                            break; // \\
                        case 0x5D:
                            break; // ]
                        case 0x5E:
                            break; // ^
                        case 0x60:
                            break; // `
                        case 0x7B:
                            break; // {
                        case 0x7C:
                            sb.append("-");
                            break; // |
                        case 0x7D:
                            break; // }
                        case 0x7E:
                            sb.append("-");
                            break; // ~
                        default:
                            sb.append(Character.toLowerCase(chars[i]));
                    }
                } else {
                    switch (chars[i]) {
                        case 0xc2a0:
                            appendSpace = true;
                            break;
                        case 0xc2a1:
                            break;
                        case 0xc2a2:
                            sb.append("ct");
                            break;
                        case 0xc2a3:
                            sb.append("gbp");
                            break;
                        case 0xc2a4:
                            break;
                        case 0xc2a5:
                            sb.append("yen");
                            break;
                        case 0xc2a6:
                            sb.append("-");
                            break;
                        case 0xc2a7:
                            break;
                        case 0xc2a8:
                            break;
                        case 0xc2a9:
                            break;
                        case 0xc2aa:
                            break;
                        case 0xc2ab:
                            break;
                        case 0xc2ac:
                            break;
                        case 0xc2ad:
                            sb.append("-");
                            break;
                        case 0xc2ae:
                            break;
                        case 0xc2af:
                            sb.append("-");
                            break;
                        case 0xc2b0:
                            break;
                        case 0xc2b1:
                            sb.append("-");
                            break;
                        case 0xc2b2:
                            break;
                        case 0xc2b3:
                            break;
                        case 0xc2b4:
                            break;
                        case 0xc2b5:
                            break;
                        case 0xc2b6:
                            break;
                        case 0xc2b7:
                            break;
                        case 0xc2b8:
                            break;
                        case 0xc2b9:
                            break;
                        case 0xc2ba:
                            break;
                        case 0xc2bb:
                            break;
                        case 0xc2bc:
                            break;
                        case 0xc2bd:
                            break;
                        case 0xc2be:
                            break;
                        case 0xc2bf:
                            break;
                        case 0xc380:
                            sb.append("a");
                            break;
                        case 0xc381:
                            sb.append("a");
                            break;
                        case 0xc382:
                            sb.append("a");
                            break;
                        case 0xc383:
                            sb.append("a");
                            break;
                        case 0xc384:
                            sb.append("a");
                            break;
                        case 0xc385:
                            sb.append("a");
                            break;
                        case 0xc386:
                            sb.append("ae");
                            break;
                        case 0xc387:
                            sb.append("c");
                            break;
                        case 0xc388:
                            sb.append("e");
                            break;
                        case 0xc389:
                            sb.append("e");
                            break;
                        case 0xc38a:
                            sb.append("e");
                            break;
                        case 0xc38b:
                            sb.append("e");
                            break;
                        case 0xc38c:
                            sb.append("i");
                            break;
                        case 0xc38d:
                            sb.append("i");
                            break;
                        case 0xc38e:
                            sb.append("i");
                            break;
                        case 0xc38f:
                            sb.append("i");
                            break;
                        case 0xc390:
                            sb.append("d");
                            break;
                        case 0xc391:
                            sb.append("n");
                            break;
                        case 0xc392:
                            sb.append("o");
                            break;
                        case 0xc393:
                            sb.append("o");
                            break;
                        case 0xc394:
                            sb.append("o");
                            break;
                        case 0xc395:
                            sb.append("o");
                            break;
                        case 0xc396:
                            sb.append("o");
                            break;
                        case 0xc397:
                            sb.append("x");
                            break;
                        case 0xc398:
                            sb.append("o");
                            break;
                        case 0xc399:
                            sb.append("u");
                            break;
                        case 0xc39a:
                            sb.append("u");
                            break;
                        case 0xc39b:
                            sb.append("u");
                            break;
                        case 0xc39c:
                            sb.append("u");
                            break;
                        case 0xc39d:
                            sb.append("y");
                            break;
                        case 0xc39e:
                            sb.append("y");
                            break;
                        case 0xc39f:
                            sb.append("ss");
                            break;
                        case 0xc3a0:
                            sb.append("a");
                            break;
                        case 0xc3a1:
                            sb.append("a");
                            break;
                        case 0xc3a2:
                            sb.append("a");
                            break;
                        case 0xc3a3:
                            sb.append("a");
                            break;
                        case 0xc3a4:
                            sb.append("a");
                            break;
                        case 0xc3a5:
                            sb.append("a");
                            break;
                        case 0xc3a6:
                            sb.append("ae");
                            break;
                        case 0xc3a7:
                            sb.append("c");
                            break;
                        case 0xc3a8:
                            sb.append("e");
                            break;
                        case 0xc3a9:
                            sb.append("e");
                            break;
                        case 0xc3aa:
                            sb.append("e");
                            break;
                        case 0xc3ab:
                            sb.append("e");
                            break;
                        case 0xc3ac:
                            sb.append("i");
                            break;
                        case 0xc3ad:
                            sb.append("i");
                            break;
                        case 0xc3ae:
                            sb.append("i");
                            break;
                        case 0xc3af:
                            sb.append("i");
                            break;
                        case 0xc3b0:
                            sb.append("d");
                            break;
                        case 0xc3b1:
                            sb.append("n");
                            break;
                        case 0xc3b2:
                            sb.append("o");
                            break;
                        case 0xc3b3:
                            sb.append("o");
                            break;
                        case 0xc3b4:
                            sb.append("o");
                            break;
                        case 0xc3b5:
                            sb.append("o");
                            break;
                        case 0xc3b6:
                            sb.append("o");
                            break;
                        case 0xc3b7:
                            sb.append("-");
                            break;
                        case 0xc3b8:
                            sb.append("o");
                            break;
                        case 0xc3b9:
                            sb.append("u");
                            break;
                        case 0xc3ba:
                            sb.append("u");
                            break;
                        case 0xc3bb:
                            sb.append("u");
                            break;
                        case 0xc3bc:
                            sb.append("u");
                            break;
                        case 0xc3bd:
                            sb.append("y");
                            break;
                        case 0xc3be:
                            sb.append("y");
                            break;
                        case 0xc3bf:
                            sb.append("y");
                            break;
                        default:
                            sb.append(Character.toLowerCase(chars[i]));
                    }
                }
                if (appendSpace) {
                    if (!lastSpace) {
                        sb.append("-");
                    }
                    lastSpace = true;
                } else {
                    lastSpace = false;
                }
            }

            // delete an ending space-replacement or '.'
            int length;
            while ((length = sb.length()) > 0 && (sb.charAt(length - 1) == '-' || sb.charAt(length - 1) == '.')) {
                sb.deleteCharAt(length - 1);
            }

            return new String(sb);
        }

        public String decode(String encoded) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    /**
     * Usage of this class discouraged.  If should only be used by frameworks to initialize the StringCodecFactory instance.
     * <p/>
     * Performs encoding and decoding (the localname of) Qualified names
     *
     * <p>
     * Implements the encode and decode routines based on ISO 9075-14:2003.<br/>
     * If a character <code>c</code> is not valid in the localname of a QName
     * it is encoded in the form: '_x' + hexValueOf(c) + '_' (UTF-16)
     * </p>
     * <p>
     * Qualified name: a qualified name is a combination of a namespace URI
     * and a local part. Instances of this class are used to internally represent
     * the names of JCR content items and other objects within a content repository.
     * </p>
     * <p>
     * The prefixed JCR name format of a qualified name is specified by
     * section 4.6 of the the JCR 1.0 specification (JSR 170) as follows:
     * <pre>
     * name                ::= simplename | prefixedname
     * simplename          ::= onecharsimplename |
     *                         twocharsimplename |
     *                         threeormorecharname
     * prefixedname        ::= prefix ':' localname
     * localname           ::= onecharlocalname |
     *                         twocharlocalname |
     *                         threeormorecharname
     * onecharsimplename   ::= (* Any Unicode character except:
     *                            '.', '/', ':', '[', ']', '*',
     *                            ''', '"', '|' or any whitespace
     *                            character *)
     * twocharsimplename   ::= '.' onecharsimplename |
     *                         onecharsimplename '.' |
     *                         onecharsimplename onecharsimplename
     * onecharlocalname    ::= nonspace
     * twocharlocalname    ::= nonspace nonspace
     * threeormorecharname ::= nonspace string nonspace
     * prefix              ::= (* Any valid XML Name *)
     * string              ::= char | string char
     * char                ::= nonspace | ' '
     * nonspace            ::= (* Any Unicode character except:
     *                            '/', ':', '[', ']', '*',
     *                            ''', '"', '|' or any whitespace
     *                            character *)
     * </pre>
     * <p>
     * In addition to the prefixed JCR name format, a qualified name can also
     * be represented using the format "<code>{namespaceURI}localPart</code>".
     */
    public static class ISO9075Helper implements StringCodec {
        //private static final char[] realChars = {'/', ':', '[', ']', '*', '\'', '"', '|'};
        //private static final String[] realChars = {"/", ":", "[", "]", "*", "'", "\"", "|"};
        //private static final String[] encodedChars = {"_x002F_", "_x003A_", '[', ']', "_x002A_", "_x0027_", "_x0022_", "_x007C_"};
        private static final String colon = ":";
        private static final String colonISO9075 = "_x003A_";

        public String encode(String plain) {
            return encodeLocalName(plain);
        }

        public String decode(String encoded) {
            return decodeLocalName(encoded);
        }

        /**
         * @see StringCodec#encode(String)
         */
        public static String encodeLocalName(String name) {
            return encodeColon(encodeImpl(name));
        }

        /**
         * @see StringCodec#decode(String)
         */
        public static String decodeLocalName(String name) {
            return decodeImpl(name);
        }

        /**
         *
         * @param name
         */
        public static String encodeColon(String name) {
            return name.replaceAll(colon, colonISO9075);
        }

        /**
         *
         * @param name
         */
        public static String decodeColon(String name) {
            return name.replaceAll(colonISO9075, colon);
        }
        /** Pattern on an encoded character */
        private static final Pattern ENCODE_PATTERN = Pattern.compile("_x\\p{XDigit}{4}_");
        /** Padding characters */
        private static final char[] PADDING = new char[] {'0', '0', '0'};
        /** All the possible hex digits */
        private static final String HEX_DIGITS = "0123456789abcdefABCDEF";

        /**
         * Encodes <code>name</code> as specified in ISO 9075.
         * @param name the <code>String</code> to encode.
         * @return the encoded <code>String</code> or <code>name</code> if it does
         *   not need encoding.
         */
        private static String encodeImpl(String name) {
            // quick check for root node name
            if (name.length() == 0) {
                return name;
            }
            if (isValidName(name) && name.indexOf("_x") < 0) {
                // already valid
                return name;
            } else {
                // encode
                StringBuffer encoded = new StringBuffer();
                for (int i = 0; i < name.length(); i++) {
                    if (i == 0) {
                        // first character of name
                        if (isNameStart(name.charAt(i))) {
                            if (needsEscaping(name, i)) {
                                // '_x' must be encoded
                                encode('_', encoded);
                            } else {
                                encoded.append(name.charAt(i));
                            }
                        } else {
                            // not valid as first character -> encode
                            encode(name.charAt(i), encoded);
                        }
                    } else if (!isName(name.charAt(i))) {
                        encode(name.charAt(i), encoded);
                    } else {
                        if (needsEscaping(name, i)) {
                            // '_x' must be encoded
                            encode('_', encoded);
                        } else {
                            encoded.append(name.charAt(i));
                        }
                    }
                }
                return encoded.toString();
            }
        }

        private static String decodeImpl(String name) {
            // quick check
            if (name.indexOf("_x") < 0) {
                // not encoded
                return name;
            }
            StringBuffer decoded = new StringBuffer();
            Matcher m = ENCODE_PATTERN.matcher(name);
            while (m.find()) {
                char ch = (char)Integer.parseInt(m.group().substring(2, 6), 16);
                if (ch == '$' || ch == '\\') {
                    m.appendReplacement(decoded, "\\" + ch);
                } else {
                    m.appendReplacement(decoded, Character.toString(ch));
                }
            }
            m.appendTail(decoded);
            return decoded.toString();
        }

        private static void encode(char c, StringBuffer b) {
            b.append("_x");
            String hex = Integer.toHexString(c);
            b.append(PADDING, 0, 4 - hex.length());
            b.append(hex);
            b.append("_");
        }

        private static boolean needsEscaping(String name, int location)
                throws ArrayIndexOutOfBoundsException {
            if (name.charAt(location) == '_' && name.length() >= location + 6) {
                return name.charAt(location + 1) == 'x' && HEX_DIGITS.indexOf(name.charAt(location + 2)) != -1 && HEX_DIGITS.indexOf(name.charAt(location + 3)) != -1 && HEX_DIGITS.indexOf(name.charAt(location + 4)) != -1 && HEX_DIGITS.indexOf(name.charAt(location + 5)) != -1;
            } else {
                return false;
            }
        }
        private static final byte[] CHARS = new byte[1 << 16];
        private static final int MASK_NAME_START = 0x04;
        private static final int MASK_NAME = 0x08;

        static {

            // Initializing the Character Flag Array
            // Code generated by: XMLCharGenerator.

            CHARS[9] = 35;
            CHARS[10] = 19;
            CHARS[13] = 19;
            CHARS[32] = 51;
            CHARS[33] = 49;
            CHARS[34] = 33;
            Arrays.fill(CHARS, 35, 38, (byte)49); // Fill 3 of value (byte) 49
            CHARS[38] = 1;
            Arrays.fill(CHARS, 39, 45, (byte)49); // Fill 6 of value (byte) 49
            Arrays.fill(CHARS, 45, 47, (byte)-71); // Fill 2 of value (byte) -71
            CHARS[47] = 49;
            Arrays.fill(CHARS, 48, 58, (byte)-71); // Fill 10 of value (byte) -71
            CHARS[58] = 61;
            CHARS[59] = 49;
            CHARS[60] = 1;
            CHARS[61] = 49;
            CHARS[62] = 33;
            Arrays.fill(CHARS, 63, 65, (byte)49); // Fill 2 of value (byte) 49
            Arrays.fill(CHARS, 65, 91, (byte)-3); // Fill 26 of value (byte) -3
            Arrays.fill(CHARS, 91, 93, (byte)33); // Fill 2 of value (byte) 33
            CHARS[93] = 1;
            CHARS[94] = 33;
            CHARS[95] = -3;
            CHARS[96] = 33;
            Arrays.fill(CHARS, 97, 123, (byte)-3); // Fill 26 of value (byte) -3
            Arrays.fill(CHARS, 123, 183, (byte)33); // Fill 60 of value (byte) 33
            CHARS[183] = -87;
            Arrays.fill(CHARS, 184, 192, (byte)33); // Fill 8 of value (byte) 33
            Arrays.fill(CHARS, 192, 215, (byte)-19); // Fill 23 of value (byte) -19
            CHARS[215] = 33;
            Arrays.fill(CHARS, 216, 247, (byte)-19); // Fill 31 of value (byte) -19
            CHARS[247] = 33;
            Arrays.fill(CHARS, 248, 306, (byte)-19); // Fill 58 of value (byte) -19
            Arrays.fill(CHARS, 306, 308, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 308, 319, (byte)-19); // Fill 11 of value (byte) -19
            Arrays.fill(CHARS, 319, 321, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 321, 329, (byte)-19); // Fill 8 of value (byte) -19
            CHARS[329] = 33;
            Arrays.fill(CHARS, 330, 383, (byte)-19); // Fill 53 of value (byte) -19
            CHARS[383] = 33;
            Arrays.fill(CHARS, 384, 452, (byte)-19); // Fill 68 of value (byte) -19
            Arrays.fill(CHARS, 452, 461, (byte)33); // Fill 9 of value (byte) 33
            Arrays.fill(CHARS, 461, 497, (byte)-19); // Fill 36 of value (byte) -19
            Arrays.fill(CHARS, 497, 500, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 500, 502, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 502, 506, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 506, 536, (byte)-19); // Fill 30 of value (byte) -19
            Arrays.fill(CHARS, 536, 592, (byte)33); // Fill 56 of value (byte) 33
            Arrays.fill(CHARS, 592, 681, (byte)-19); // Fill 89 of value (byte) -19
            Arrays.fill(CHARS, 681, 699, (byte)33); // Fill 18 of value (byte) 33
            Arrays.fill(CHARS, 699, 706, (byte)-19); // Fill 7 of value (byte) -19
            Arrays.fill(CHARS, 706, 720, (byte)33); // Fill 14 of value (byte) 33
            Arrays.fill(CHARS, 720, 722, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 722, 768, (byte)33); // Fill 46 of value (byte) 33
            Arrays.fill(CHARS, 768, 838, (byte)-87); // Fill 70 of value (byte) -87
            Arrays.fill(CHARS, 838, 864, (byte)33); // Fill 26 of value (byte) 33
            Arrays.fill(CHARS, 864, 866, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 866, 902, (byte)33); // Fill 36 of value (byte) 33
            CHARS[902] = -19;
            CHARS[903] = -87;
            Arrays.fill(CHARS, 904, 907, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[907] = 33;
            CHARS[908] = -19;
            CHARS[909] = 33;
            Arrays.fill(CHARS, 910, 930, (byte)-19); // Fill 20 of value (byte) -19
            CHARS[930] = 33;
            Arrays.fill(CHARS, 931, 975, (byte)-19); // Fill 44 of value (byte) -19
            CHARS[975] = 33;
            Arrays.fill(CHARS, 976, 983, (byte)-19); // Fill 7 of value (byte) -19
            Arrays.fill(CHARS, 983, 986, (byte)33); // Fill 3 of value (byte) 33
            CHARS[986] = -19;
            CHARS[987] = 33;
            CHARS[988] = -19;
            CHARS[989] = 33;
            CHARS[990] = -19;
            CHARS[991] = 33;
            CHARS[992] = -19;
            CHARS[993] = 33;
            Arrays.fill(CHARS, 994, 1012, (byte)-19); // Fill 18 of value (byte) -19
            Arrays.fill(CHARS, 1012, 1025, (byte)33); // Fill 13 of value (byte) 33
            Arrays.fill(CHARS, 1025, 1037, (byte)-19); // Fill 12 of value (byte) -19
            CHARS[1037] = 33;
            Arrays.fill(CHARS, 1038, 1104, (byte)-19); // Fill 66 of value (byte) -19
            CHARS[1104] = 33;
            Arrays.fill(CHARS, 1105, 1117, (byte)-19); // Fill 12 of value (byte) -19
            CHARS[1117] = 33;
            Arrays.fill(CHARS, 1118, 1154, (byte)-19); // Fill 36 of value (byte) -19
            CHARS[1154] = 33;
            Arrays.fill(CHARS, 1155, 1159, (byte)-87); // Fill 4 of value (byte) -87
            Arrays.fill(CHARS, 1159, 1168, (byte)33); // Fill 9 of value (byte) 33
            Arrays.fill(CHARS, 1168, 1221, (byte)-19); // Fill 53 of value (byte) -19
            Arrays.fill(CHARS, 1221, 1223, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 1223, 1225, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 1225, 1227, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 1227, 1229, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 1229, 1232, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 1232, 1260, (byte)-19); // Fill 28 of value (byte) -19
            Arrays.fill(CHARS, 1260, 1262, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 1262, 1270, (byte)-19); // Fill 8 of value (byte) -19
            Arrays.fill(CHARS, 1270, 1272, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 1272, 1274, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 1274, 1329, (byte)33); // Fill 55 of value (byte) 33
            Arrays.fill(CHARS, 1329, 1367, (byte)-19); // Fill 38 of value (byte) -19
            Arrays.fill(CHARS, 1367, 1369, (byte)33); // Fill 2 of value (byte) 33
            CHARS[1369] = -19;
            Arrays.fill(CHARS, 1370, 1377, (byte)33); // Fill 7 of value (byte) 33
            Arrays.fill(CHARS, 1377, 1415, (byte)-19); // Fill 38 of value (byte) -19
            Arrays.fill(CHARS, 1415, 1425, (byte)33); // Fill 10 of value (byte) 33
            Arrays.fill(CHARS, 1425, 1442, (byte)-87); // Fill 17 of value (byte) -87
            CHARS[1442] = 33;
            Arrays.fill(CHARS, 1443, 1466, (byte)-87); // Fill 23 of value (byte) -87
            CHARS[1466] = 33;
            Arrays.fill(CHARS, 1467, 1470, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[1470] = 33;
            CHARS[1471] = -87;
            CHARS[1472] = 33;
            Arrays.fill(CHARS, 1473, 1475, (byte)-87); // Fill 2 of value (byte) -87
            CHARS[1475] = 33;
            CHARS[1476] = -87;
            Arrays.fill(CHARS, 1477, 1488, (byte)33); // Fill 11 of value (byte) 33
            Arrays.fill(CHARS, 1488, 1515, (byte)-19); // Fill 27 of value (byte) -19
            Arrays.fill(CHARS, 1515, 1520, (byte)33); // Fill 5 of value (byte) 33
            Arrays.fill(CHARS, 1520, 1523, (byte)-19); // Fill 3 of value (byte) -19
            Arrays.fill(CHARS, 1523, 1569, (byte)33); // Fill 46 of value (byte) 33
            Arrays.fill(CHARS, 1569, 1595, (byte)-19); // Fill 26 of value (byte) -19
            Arrays.fill(CHARS, 1595, 1600, (byte)33); // Fill 5 of value (byte) 33
            CHARS[1600] = -87;
            Arrays.fill(CHARS, 1601, 1611, (byte)-19); // Fill 10 of value (byte) -19
            Arrays.fill(CHARS, 1611, 1619, (byte)-87); // Fill 8 of value (byte) -87
            Arrays.fill(CHARS, 1619, 1632, (byte)33); // Fill 13 of value (byte) 33
            Arrays.fill(CHARS, 1632, 1642, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 1642, 1648, (byte)33); // Fill 6 of value (byte) 33
            CHARS[1648] = -87;
            Arrays.fill(CHARS, 1649, 1720, (byte)-19); // Fill 71 of value (byte) -19
            Arrays.fill(CHARS, 1720, 1722, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 1722, 1727, (byte)-19); // Fill 5 of value (byte) -19
            CHARS[1727] = 33;
            Arrays.fill(CHARS, 1728, 1743, (byte)-19); // Fill 15 of value (byte) -19
            CHARS[1743] = 33;
            Arrays.fill(CHARS, 1744, 1748, (byte)-19); // Fill 4 of value (byte) -19
            CHARS[1748] = 33;
            CHARS[1749] = -19;
            Arrays.fill(CHARS, 1750, 1765, (byte)-87); // Fill 15 of value (byte) -87
            Arrays.fill(CHARS, 1765, 1767, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 1767, 1769, (byte)-87); // Fill 2 of value (byte) -87
            CHARS[1769] = 33;
            Arrays.fill(CHARS, 1770, 1774, (byte)-87); // Fill 4 of value (byte) -87
            Arrays.fill(CHARS, 1774, 1776, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 1776, 1786, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 1786, 2305, (byte)33); // Fill 519 of value (byte) 33
            Arrays.fill(CHARS, 2305, 2308, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[2308] = 33;
            Arrays.fill(CHARS, 2309, 2362, (byte)-19); // Fill 53 of value (byte) -19
            Arrays.fill(CHARS, 2362, 2364, (byte)33); // Fill 2 of value (byte) 33
            CHARS[2364] = -87;
            CHARS[2365] = -19;
            Arrays.fill(CHARS, 2366, 2382, (byte)-87); // Fill 16 of value (byte) -87
            Arrays.fill(CHARS, 2382, 2385, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 2385, 2389, (byte)-87); // Fill 4 of value (byte) -87
            Arrays.fill(CHARS, 2389, 2392, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 2392, 2402, (byte)-19); // Fill 10 of value (byte) -19
            Arrays.fill(CHARS, 2402, 2404, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 2404, 2406, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2406, 2416, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 2416, 2433, (byte)33); // Fill 17 of value (byte) 33
            Arrays.fill(CHARS, 2433, 2436, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[2436] = 33;
            Arrays.fill(CHARS, 2437, 2445, (byte)-19); // Fill 8 of value (byte) -19
            Arrays.fill(CHARS, 2445, 2447, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2447, 2449, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 2449, 2451, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2451, 2473, (byte)-19); // Fill 22 of value (byte) -19
            CHARS[2473] = 33;
            Arrays.fill(CHARS, 2474, 2481, (byte)-19); // Fill 7 of value (byte) -19
            CHARS[2481] = 33;
            CHARS[2482] = -19;
            Arrays.fill(CHARS, 2483, 2486, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 2486, 2490, (byte)-19); // Fill 4 of value (byte) -19
            Arrays.fill(CHARS, 2490, 2492, (byte)33); // Fill 2 of value (byte) 33
            CHARS[2492] = -87;
            CHARS[2493] = 33;
            Arrays.fill(CHARS, 2494, 2501, (byte)-87); // Fill 7 of value (byte) -87
            Arrays.fill(CHARS, 2501, 2503, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2503, 2505, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 2505, 2507, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2507, 2510, (byte)-87); // Fill 3 of value (byte) -87
            Arrays.fill(CHARS, 2510, 2519, (byte)33); // Fill 9 of value (byte) 33
            CHARS[2519] = -87;
            Arrays.fill(CHARS, 2520, 2524, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 2524, 2526, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[2526] = 33;
            Arrays.fill(CHARS, 2527, 2530, (byte)-19); // Fill 3 of value (byte) -19
            Arrays.fill(CHARS, 2530, 2532, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 2532, 2534, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2534, 2544, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 2544, 2546, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 2546, 2562, (byte)33); // Fill 16 of value (byte) 33
            CHARS[2562] = -87;
            Arrays.fill(CHARS, 2563, 2565, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2565, 2571, (byte)-19); // Fill 6 of value (byte) -19
            Arrays.fill(CHARS, 2571, 2575, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 2575, 2577, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 2577, 2579, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2579, 2601, (byte)-19); // Fill 22 of value (byte) -19
            CHARS[2601] = 33;
            Arrays.fill(CHARS, 2602, 2609, (byte)-19); // Fill 7 of value (byte) -19
            CHARS[2609] = 33;
            Arrays.fill(CHARS, 2610, 2612, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[2612] = 33;
            Arrays.fill(CHARS, 2613, 2615, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[2615] = 33;
            Arrays.fill(CHARS, 2616, 2618, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 2618, 2620, (byte)33); // Fill 2 of value (byte) 33
            CHARS[2620] = -87;
            CHARS[2621] = 33;
            Arrays.fill(CHARS, 2622, 2627, (byte)-87); // Fill 5 of value (byte) -87
            Arrays.fill(CHARS, 2627, 2631, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 2631, 2633, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 2633, 2635, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2635, 2638, (byte)-87); // Fill 3 of value (byte) -87
            Arrays.fill(CHARS, 2638, 2649, (byte)33); // Fill 11 of value (byte) 33
            Arrays.fill(CHARS, 2649, 2653, (byte)-19); // Fill 4 of value (byte) -19
            CHARS[2653] = 33;
            CHARS[2654] = -19;
            Arrays.fill(CHARS, 2655, 2662, (byte)33); // Fill 7 of value (byte) 33
            Arrays.fill(CHARS, 2662, 2674, (byte)-87); // Fill 12 of value (byte) -87
            Arrays.fill(CHARS, 2674, 2677, (byte)-19); // Fill 3 of value (byte) -19
            Arrays.fill(CHARS, 2677, 2689, (byte)33); // Fill 12 of value (byte) 33
            Arrays.fill(CHARS, 2689, 2692, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[2692] = 33;
            Arrays.fill(CHARS, 2693, 2700, (byte)-19); // Fill 7 of value (byte) -19
            CHARS[2700] = 33;
            CHARS[2701] = -19;
            CHARS[2702] = 33;
            Arrays.fill(CHARS, 2703, 2706, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[2706] = 33;
            Arrays.fill(CHARS, 2707, 2729, (byte)-19); // Fill 22 of value (byte) -19
            CHARS[2729] = 33;
            Arrays.fill(CHARS, 2730, 2737, (byte)-19); // Fill 7 of value (byte) -19
            CHARS[2737] = 33;
            Arrays.fill(CHARS, 2738, 2740, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[2740] = 33;
            Arrays.fill(CHARS, 2741, 2746, (byte)-19); // Fill 5 of value (byte) -19
            Arrays.fill(CHARS, 2746, 2748, (byte)33); // Fill 2 of value (byte) 33
            CHARS[2748] = -87;
            CHARS[2749] = -19;
            Arrays.fill(CHARS, 2750, 2758, (byte)-87); // Fill 8 of value (byte) -87
            CHARS[2758] = 33;
            Arrays.fill(CHARS, 2759, 2762, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[2762] = 33;
            Arrays.fill(CHARS, 2763, 2766, (byte)-87); // Fill 3 of value (byte) -87
            Arrays.fill(CHARS, 2766, 2784, (byte)33); // Fill 18 of value (byte) 33
            CHARS[2784] = -19;
            Arrays.fill(CHARS, 2785, 2790, (byte)33); // Fill 5 of value (byte) 33
            Arrays.fill(CHARS, 2790, 2800, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 2800, 2817, (byte)33); // Fill 17 of value (byte) 33
            Arrays.fill(CHARS, 2817, 2820, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[2820] = 33;
            Arrays.fill(CHARS, 2821, 2829, (byte)-19); // Fill 8 of value (byte) -19
            Arrays.fill(CHARS, 2829, 2831, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2831, 2833, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 2833, 2835, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2835, 2857, (byte)-19); // Fill 22 of value (byte) -19
            CHARS[2857] = 33;
            Arrays.fill(CHARS, 2858, 2865, (byte)-19); // Fill 7 of value (byte) -19
            CHARS[2865] = 33;
            Arrays.fill(CHARS, 2866, 2868, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 2868, 2870, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2870, 2874, (byte)-19); // Fill 4 of value (byte) -19
            Arrays.fill(CHARS, 2874, 2876, (byte)33); // Fill 2 of value (byte) 33
            CHARS[2876] = -87;
            CHARS[2877] = -19;
            Arrays.fill(CHARS, 2878, 2884, (byte)-87); // Fill 6 of value (byte) -87
            Arrays.fill(CHARS, 2884, 2887, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 2887, 2889, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 2889, 2891, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 2891, 2894, (byte)-87); // Fill 3 of value (byte) -87
            Arrays.fill(CHARS, 2894, 2902, (byte)33); // Fill 8 of value (byte) 33
            Arrays.fill(CHARS, 2902, 2904, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 2904, 2908, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 2908, 2910, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[2910] = 33;
            Arrays.fill(CHARS, 2911, 2914, (byte)-19); // Fill 3 of value (byte) -19
            Arrays.fill(CHARS, 2914, 2918, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 2918, 2928, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 2928, 2946, (byte)33); // Fill 18 of value (byte) 33
            Arrays.fill(CHARS, 2946, 2948, (byte)-87); // Fill 2 of value (byte) -87
            CHARS[2948] = 33;
            Arrays.fill(CHARS, 2949, 2955, (byte)-19); // Fill 6 of value (byte) -19
            Arrays.fill(CHARS, 2955, 2958, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 2958, 2961, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[2961] = 33;
            Arrays.fill(CHARS, 2962, 2966, (byte)-19); // Fill 4 of value (byte) -19
            Arrays.fill(CHARS, 2966, 2969, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 2969, 2971, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[2971] = 33;
            CHARS[2972] = -19;
            CHARS[2973] = 33;
            Arrays.fill(CHARS, 2974, 2976, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 2976, 2979, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 2979, 2981, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 2981, 2984, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 2984, 2987, (byte)-19); // Fill 3 of value (byte) -19
            Arrays.fill(CHARS, 2987, 2990, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 2990, 2998, (byte)-19); // Fill 8 of value (byte) -19
            CHARS[2998] = 33;
            Arrays.fill(CHARS, 2999, 3002, (byte)-19); // Fill 3 of value (byte) -19
            Arrays.fill(CHARS, 3002, 3006, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 3006, 3011, (byte)-87); // Fill 5 of value (byte) -87
            Arrays.fill(CHARS, 3011, 3014, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 3014, 3017, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[3017] = 33;
            Arrays.fill(CHARS, 3018, 3022, (byte)-87); // Fill 4 of value (byte) -87
            Arrays.fill(CHARS, 3022, 3031, (byte)33); // Fill 9 of value (byte) 33
            CHARS[3031] = -87;
            Arrays.fill(CHARS, 3032, 3047, (byte)33); // Fill 15 of value (byte) 33
            Arrays.fill(CHARS, 3047, 3056, (byte)-87); // Fill 9 of value (byte) -87
            Arrays.fill(CHARS, 3056, 3073, (byte)33); // Fill 17 of value (byte) 33
            Arrays.fill(CHARS, 3073, 3076, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[3076] = 33;
            Arrays.fill(CHARS, 3077, 3085, (byte)-19); // Fill 8 of value (byte) -19
            CHARS[3085] = 33;
            Arrays.fill(CHARS, 3086, 3089, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[3089] = 33;
            Arrays.fill(CHARS, 3090, 3113, (byte)-19); // Fill 23 of value (byte) -19
            CHARS[3113] = 33;
            Arrays.fill(CHARS, 3114, 3124, (byte)-19); // Fill 10 of value (byte) -19
            CHARS[3124] = 33;
            Arrays.fill(CHARS, 3125, 3130, (byte)-19); // Fill 5 of value (byte) -19
            Arrays.fill(CHARS, 3130, 3134, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 3134, 3141, (byte)-87); // Fill 7 of value (byte) -87
            CHARS[3141] = 33;
            Arrays.fill(CHARS, 3142, 3145, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[3145] = 33;
            Arrays.fill(CHARS, 3146, 3150, (byte)-87); // Fill 4 of value (byte) -87
            Arrays.fill(CHARS, 3150, 3157, (byte)33); // Fill 7 of value (byte) 33
            Arrays.fill(CHARS, 3157, 3159, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 3159, 3168, (byte)33); // Fill 9 of value (byte) 33
            Arrays.fill(CHARS, 3168, 3170, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 3170, 3174, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 3174, 3184, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 3184, 3202, (byte)33); // Fill 18 of value (byte) 33
            Arrays.fill(CHARS, 3202, 3204, (byte)-87); // Fill 2 of value (byte) -87
            CHARS[3204] = 33;
            Arrays.fill(CHARS, 3205, 3213, (byte)-19); // Fill 8 of value (byte) -19
            CHARS[3213] = 33;
            Arrays.fill(CHARS, 3214, 3217, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[3217] = 33;
            Arrays.fill(CHARS, 3218, 3241, (byte)-19); // Fill 23 of value (byte) -19
            CHARS[3241] = 33;
            Arrays.fill(CHARS, 3242, 3252, (byte)-19); // Fill 10 of value (byte) -19
            CHARS[3252] = 33;
            Arrays.fill(CHARS, 3253, 3258, (byte)-19); // Fill 5 of value (byte) -19
            Arrays.fill(CHARS, 3258, 3262, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 3262, 3269, (byte)-87); // Fill 7 of value (byte) -87
            CHARS[3269] = 33;
            Arrays.fill(CHARS, 3270, 3273, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[3273] = 33;
            Arrays.fill(CHARS, 3274, 3278, (byte)-87); // Fill 4 of value (byte) -87
            Arrays.fill(CHARS, 3278, 3285, (byte)33); // Fill 7 of value (byte) 33
            Arrays.fill(CHARS, 3285, 3287, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 3287, 3294, (byte)33); // Fill 7 of value (byte) 33
            CHARS[3294] = -19;
            CHARS[3295] = 33;
            Arrays.fill(CHARS, 3296, 3298, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 3298, 3302, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 3302, 3312, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 3312, 3330, (byte)33); // Fill 18 of value (byte) 33
            Arrays.fill(CHARS, 3330, 3332, (byte)-87); // Fill 2 of value (byte) -87
            CHARS[3332] = 33;
            Arrays.fill(CHARS, 3333, 3341, (byte)-19); // Fill 8 of value (byte) -19
            CHARS[3341] = 33;
            Arrays.fill(CHARS, 3342, 3345, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[3345] = 33;
            Arrays.fill(CHARS, 3346, 3369, (byte)-19); // Fill 23 of value (byte) -19
            CHARS[3369] = 33;
            Arrays.fill(CHARS, 3370, 3386, (byte)-19); // Fill 16 of value (byte) -19
            Arrays.fill(CHARS, 3386, 3390, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 3390, 3396, (byte)-87); // Fill 6 of value (byte) -87
            Arrays.fill(CHARS, 3396, 3398, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 3398, 3401, (byte)-87); // Fill 3 of value (byte) -87
            CHARS[3401] = 33;
            Arrays.fill(CHARS, 3402, 3406, (byte)-87); // Fill 4 of value (byte) -87
            Arrays.fill(CHARS, 3406, 3415, (byte)33); // Fill 9 of value (byte) 33
            CHARS[3415] = -87;
            Arrays.fill(CHARS, 3416, 3424, (byte)33); // Fill 8 of value (byte) 33
            Arrays.fill(CHARS, 3424, 3426, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 3426, 3430, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 3430, 3440, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 3440, 3585, (byte)33); // Fill 145 of value (byte) 33
            Arrays.fill(CHARS, 3585, 3631, (byte)-19); // Fill 46 of value (byte) -19
            CHARS[3631] = 33;
            CHARS[3632] = -19;
            CHARS[3633] = -87;
            Arrays.fill(CHARS, 3634, 3636, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 3636, 3643, (byte)-87); // Fill 7 of value (byte) -87
            Arrays.fill(CHARS, 3643, 3648, (byte)33); // Fill 5 of value (byte) 33
            Arrays.fill(CHARS, 3648, 3654, (byte)-19); // Fill 6 of value (byte) -19
            Arrays.fill(CHARS, 3654, 3663, (byte)-87); // Fill 9 of value (byte) -87
            CHARS[3663] = 33;
            Arrays.fill(CHARS, 3664, 3674, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 3674, 3713, (byte)33); // Fill 39 of value (byte) 33
            Arrays.fill(CHARS, 3713, 3715, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[3715] = 33;
            CHARS[3716] = -19;
            Arrays.fill(CHARS, 3717, 3719, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 3719, 3721, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[3721] = 33;
            CHARS[3722] = -19;
            Arrays.fill(CHARS, 3723, 3725, (byte)33); // Fill 2 of value (byte) 33
            CHARS[3725] = -19;
            Arrays.fill(CHARS, 3726, 3732, (byte)33); // Fill 6 of value (byte) 33
            Arrays.fill(CHARS, 3732, 3736, (byte)-19); // Fill 4 of value (byte) -19
            CHARS[3736] = 33;
            Arrays.fill(CHARS, 3737, 3744, (byte)-19); // Fill 7 of value (byte) -19
            CHARS[3744] = 33;
            Arrays.fill(CHARS, 3745, 3748, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[3748] = 33;
            CHARS[3749] = -19;
            CHARS[3750] = 33;
            CHARS[3751] = -19;
            Arrays.fill(CHARS, 3752, 3754, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 3754, 3756, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[3756] = 33;
            Arrays.fill(CHARS, 3757, 3759, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[3759] = 33;
            CHARS[3760] = -19;
            CHARS[3761] = -87;
            Arrays.fill(CHARS, 3762, 3764, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 3764, 3770, (byte)-87); // Fill 6 of value (byte) -87
            CHARS[3770] = 33;
            Arrays.fill(CHARS, 3771, 3773, (byte)-87); // Fill 2 of value (byte) -87
            CHARS[3773] = -19;
            Arrays.fill(CHARS, 3774, 3776, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 3776, 3781, (byte)-19); // Fill 5 of value (byte) -19
            CHARS[3781] = 33;
            CHARS[3782] = -87;
            CHARS[3783] = 33;
            Arrays.fill(CHARS, 3784, 3790, (byte)-87); // Fill 6 of value (byte) -87
            Arrays.fill(CHARS, 3790, 3792, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 3792, 3802, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 3802, 3864, (byte)33); // Fill 62 of value (byte) 33
            Arrays.fill(CHARS, 3864, 3866, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 3866, 3872, (byte)33); // Fill 6 of value (byte) 33
            Arrays.fill(CHARS, 3872, 3882, (byte)-87); // Fill 10 of value (byte) -87
            Arrays.fill(CHARS, 3882, 3893, (byte)33); // Fill 11 of value (byte) 33
            CHARS[3893] = -87;
            CHARS[3894] = 33;
            CHARS[3895] = -87;
            CHARS[3896] = 33;
            CHARS[3897] = -87;
            Arrays.fill(CHARS, 3898, 3902, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 3902, 3904, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 3904, 3912, (byte)-19); // Fill 8 of value (byte) -19
            CHARS[3912] = 33;
            Arrays.fill(CHARS, 3913, 3946, (byte)-19); // Fill 33 of value (byte) -19
            Arrays.fill(CHARS, 3946, 3953, (byte)33); // Fill 7 of value (byte) 33
            Arrays.fill(CHARS, 3953, 3973, (byte)-87); // Fill 20 of value (byte) -87
            CHARS[3973] = 33;
            Arrays.fill(CHARS, 3974, 3980, (byte)-87); // Fill 6 of value (byte) -87
            Arrays.fill(CHARS, 3980, 3984, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 3984, 3990, (byte)-87); // Fill 6 of value (byte) -87
            CHARS[3990] = 33;
            CHARS[3991] = -87;
            CHARS[3992] = 33;
            Arrays.fill(CHARS, 3993, 4014, (byte)-87); // Fill 21 of value (byte) -87
            Arrays.fill(CHARS, 4014, 4017, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 4017, 4024, (byte)-87); // Fill 7 of value (byte) -87
            CHARS[4024] = 33;
            CHARS[4025] = -87;
            Arrays.fill(CHARS, 4026, 4256, (byte)33); // Fill 230 of value (byte) 33
            Arrays.fill(CHARS, 4256, 4294, (byte)-19); // Fill 38 of value (byte) -19
            Arrays.fill(CHARS, 4294, 4304, (byte)33); // Fill 10 of value (byte) 33
            Arrays.fill(CHARS, 4304, 4343, (byte)-19); // Fill 39 of value (byte) -19
            Arrays.fill(CHARS, 4343, 4352, (byte)33); // Fill 9 of value (byte) 33
            CHARS[4352] = -19;
            CHARS[4353] = 33;
            Arrays.fill(CHARS, 4354, 4356, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[4356] = 33;
            Arrays.fill(CHARS, 4357, 4360, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[4360] = 33;
            CHARS[4361] = -19;
            CHARS[4362] = 33;
            Arrays.fill(CHARS, 4363, 4365, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[4365] = 33;
            Arrays.fill(CHARS, 4366, 4371, (byte)-19); // Fill 5 of value (byte) -19
            Arrays.fill(CHARS, 4371, 4412, (byte)33); // Fill 41 of value (byte) 33
            CHARS[4412] = -19;
            CHARS[4413] = 33;
            CHARS[4414] = -19;
            CHARS[4415] = 33;
            CHARS[4416] = -19;
            Arrays.fill(CHARS, 4417, 4428, (byte)33); // Fill 11 of value (byte) 33
            CHARS[4428] = -19;
            CHARS[4429] = 33;
            CHARS[4430] = -19;
            CHARS[4431] = 33;
            CHARS[4432] = -19;
            Arrays.fill(CHARS, 4433, 4436, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 4436, 4438, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 4438, 4441, (byte)33); // Fill 3 of value (byte) 33
            CHARS[4441] = -19;
            Arrays.fill(CHARS, 4442, 4447, (byte)33); // Fill 5 of value (byte) 33
            Arrays.fill(CHARS, 4447, 4450, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[4450] = 33;
            CHARS[4451] = -19;
            CHARS[4452] = 33;
            CHARS[4453] = -19;
            CHARS[4454] = 33;
            CHARS[4455] = -19;
            CHARS[4456] = 33;
            CHARS[4457] = -19;
            Arrays.fill(CHARS, 4458, 4461, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 4461, 4463, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 4463, 4466, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 4466, 4468, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[4468] = 33;
            CHARS[4469] = -19;
            Arrays.fill(CHARS, 4470, 4510, (byte)33); // Fill 40 of value (byte) 33
            CHARS[4510] = -19;
            Arrays.fill(CHARS, 4511, 4520, (byte)33); // Fill 9 of value (byte) 33
            CHARS[4520] = -19;
            Arrays.fill(CHARS, 4521, 4523, (byte)33); // Fill 2 of value (byte) 33
            CHARS[4523] = -19;
            Arrays.fill(CHARS, 4524, 4526, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 4526, 4528, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 4528, 4535, (byte)33); // Fill 7 of value (byte) 33
            Arrays.fill(CHARS, 4535, 4537, (byte)-19); // Fill 2 of value (byte) -19
            CHARS[4537] = 33;
            CHARS[4538] = -19;
            CHARS[4539] = 33;
            Arrays.fill(CHARS, 4540, 4547, (byte)-19); // Fill 7 of value (byte) -19
            Arrays.fill(CHARS, 4547, 4587, (byte)33); // Fill 40 of value (byte) 33
            CHARS[4587] = -19;
            Arrays.fill(CHARS, 4588, 4592, (byte)33); // Fill 4 of value (byte) 33
            CHARS[4592] = -19;
            Arrays.fill(CHARS, 4593, 4601, (byte)33); // Fill 8 of value (byte) 33
            CHARS[4601] = -19;
            Arrays.fill(CHARS, 4602, 7680, (byte)33); // Fill 3078 of value (byte) 33
            Arrays.fill(CHARS, 7680, 7836, (byte)-19); // Fill 156 of value (byte) -19
            Arrays.fill(CHARS, 7836, 7840, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 7840, 7930, (byte)-19); // Fill 90 of value (byte) -19
            Arrays.fill(CHARS, 7930, 7936, (byte)33); // Fill 6 of value (byte) 33
            Arrays.fill(CHARS, 7936, 7958, (byte)-19); // Fill 22 of value (byte) -19
            Arrays.fill(CHARS, 7958, 7960, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 7960, 7966, (byte)-19); // Fill 6 of value (byte) -19
            Arrays.fill(CHARS, 7966, 7968, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 7968, 8006, (byte)-19); // Fill 38 of value (byte) -19
            Arrays.fill(CHARS, 8006, 8008, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 8008, 8014, (byte)-19); // Fill 6 of value (byte) -19
            Arrays.fill(CHARS, 8014, 8016, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 8016, 8024, (byte)-19); // Fill 8 of value (byte) -19
            CHARS[8024] = 33;
            CHARS[8025] = -19;
            CHARS[8026] = 33;
            CHARS[8027] = -19;
            CHARS[8028] = 33;
            CHARS[8029] = -19;
            CHARS[8030] = 33;
            Arrays.fill(CHARS, 8031, 8062, (byte)-19); // Fill 31 of value (byte) -19
            Arrays.fill(CHARS, 8062, 8064, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 8064, 8117, (byte)-19); // Fill 53 of value (byte) -19
            CHARS[8117] = 33;
            Arrays.fill(CHARS, 8118, 8125, (byte)-19); // Fill 7 of value (byte) -19
            CHARS[8125] = 33;
            CHARS[8126] = -19;
            Arrays.fill(CHARS, 8127, 8130, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 8130, 8133, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[8133] = 33;
            Arrays.fill(CHARS, 8134, 8141, (byte)-19); // Fill 7 of value (byte) -19
            Arrays.fill(CHARS, 8141, 8144, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 8144, 8148, (byte)-19); // Fill 4 of value (byte) -19
            Arrays.fill(CHARS, 8148, 8150, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 8150, 8156, (byte)-19); // Fill 6 of value (byte) -19
            Arrays.fill(CHARS, 8156, 8160, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 8160, 8173, (byte)-19); // Fill 13 of value (byte) -19
            Arrays.fill(CHARS, 8173, 8178, (byte)33); // Fill 5 of value (byte) 33
            Arrays.fill(CHARS, 8178, 8181, (byte)-19); // Fill 3 of value (byte) -19
            CHARS[8181] = 33;
            Arrays.fill(CHARS, 8182, 8189, (byte)-19); // Fill 7 of value (byte) -19
            Arrays.fill(CHARS, 8189, 8400, (byte)33); // Fill 211 of value (byte) 33
            Arrays.fill(CHARS, 8400, 8413, (byte)-87); // Fill 13 of value (byte) -87
            Arrays.fill(CHARS, 8413, 8417, (byte)33); // Fill 4 of value (byte) 33
            CHARS[8417] = -87;
            Arrays.fill(CHARS, 8418, 8486, (byte)33); // Fill 68 of value (byte) 33
            CHARS[8486] = -19;
            Arrays.fill(CHARS, 8487, 8490, (byte)33); // Fill 3 of value (byte) 33
            Arrays.fill(CHARS, 8490, 8492, (byte)-19); // Fill 2 of value (byte) -19
            Arrays.fill(CHARS, 8492, 8494, (byte)33); // Fill 2 of value (byte) 33
            CHARS[8494] = -19;
            Arrays.fill(CHARS, 8495, 8576, (byte)33); // Fill 81 of value (byte) 33
            Arrays.fill(CHARS, 8576, 8579, (byte)-19); // Fill 3 of value (byte) -19
            Arrays.fill(CHARS, 8579, 12293, (byte)33); // Fill 3714 of value (byte) 33
            CHARS[12293] = -87;
            CHARS[12294] = 33;
            CHARS[12295] = -19;
            Arrays.fill(CHARS, 12296, 12321, (byte)33); // Fill 25 of value (byte) 33
            Arrays.fill(CHARS, 12321, 12330, (byte)-19); // Fill 9 of value (byte) -19
            Arrays.fill(CHARS, 12330, 12336, (byte)-87); // Fill 6 of value (byte) -87
            CHARS[12336] = 33;
            Arrays.fill(CHARS, 12337, 12342, (byte)-87); // Fill 5 of value (byte) -87
            Arrays.fill(CHARS, 12342, 12353, (byte)33); // Fill 11 of value (byte) 33
            Arrays.fill(CHARS, 12353, 12437, (byte)-19); // Fill 84 of value (byte) -19
            Arrays.fill(CHARS, 12437, 12441, (byte)33); // Fill 4 of value (byte) 33
            Arrays.fill(CHARS, 12441, 12443, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 12443, 12445, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 12445, 12447, (byte)-87); // Fill 2 of value (byte) -87
            Arrays.fill(CHARS, 12447, 12449, (byte)33); // Fill 2 of value (byte) 33
            Arrays.fill(CHARS, 12449, 12539, (byte)-19); // Fill 90 of value (byte) -19
            CHARS[12539] = 33;
            Arrays.fill(CHARS, 12540, 12543, (byte)-87); // Fill 3 of value (byte) -87
            Arrays.fill(CHARS, 12543, 12549, (byte)33); // Fill 6 of value (byte) 33
            Arrays.fill(CHARS, 12549, 12589, (byte)-19); // Fill 40 of value (byte) -19
            Arrays.fill(CHARS, 12589, 19968, (byte)33); // Fill 7379 of value (byte) 33
            Arrays.fill(CHARS, 19968, 40870, (byte)-19); // Fill 20902 of value (byte) -19
            Arrays.fill(CHARS, 40870, 44032, (byte)33); // Fill 3162 of value (byte) 33
            Arrays.fill(CHARS, 44032, 55204, (byte)-19); // Fill 11172 of value (byte) -19
            Arrays.fill(CHARS, 55204, 55296, (byte)33); // Fill 92 of value (byte) 33
            Arrays.fill(CHARS, 57344, 65534, (byte)33); // Fill 8190 of value (byte) 33
        }

        private static boolean isNameStart(int c) {
            return c < 0x10000 && (CHARS[c] & MASK_NAME_START) != 0;
        }

        private static boolean isName(int c) {
            return c < 0x10000 && (CHARS[c] & MASK_NAME) != 0;
        }

        private static boolean isValidName(String name) {
            if (name.length() == 0)
                return false;
            char ch = name.charAt(0);
            if (isNameStart(ch) == false)
                return false;
            for (int i = 1; i < name.length(); i++) {
                ch = name.charAt(i);
                if (isName(ch) == false) {
                    return false;
                }
            }
            return true;
        } // isValidName(String):boolean
    }

    /**
     * <h2>Helper class for encoding and decoding node names</h2>
     *
     * <p>
     * Implements the encode and decode routines based on ISO 9075-14:2003 for
     * node names.It encodes and decode the minimal set to garanty the node names
     * are jcr valid.<br/>
     * If a character <code>c</code> is not valid in the node name it is encoded
     * in the form: '_x' + hexValueOf(c) + '_' (UTF-16). It only
     * </p>
     * <p>
     * If a node name string is encoded twice or decoded twice it should return the
     * same string. If a node name string doesn't contain invalid characters the
     * encode functions should return the string itself. If a string doesn't contain
     * any encoded chars it should return the string itself. Eg:<br/>
     * <ul>
     *   <li>decode(encode(x)) = x</li>
     *   <li>encode(decode(x)) = x</li>
     *   <li>encode(encode(x)) = encode(x)</li>
     *   <li>decode(decode(x)) = decode(x)</li>
     *   <li>encode(valid_chars) = valid_chars</li>
     * </ul>
     * </p>
     * <p>
     * Qualified name: a qualified name is a combination of a namespace URI
     * and a local part. Instances of this class are used to internally represent
     * the names of JCR content items and other objects within a content repository.
     * </p>
     * <p>
     * The prefixed JCR name format of a qualified name is specified by
     * section 4.6 of the the JCR 1.0 specification (JSR 170) as follows:
     * <pre>
     * name                ::= simplename | prefixedname
     * simplename          ::= onecharsimplename |
     *                         twocharsimplename |
     *                         threeormorecharname
     * prefixedname        ::= prefix ':' localname
     * localname           ::= onecharlocalname |
     *                         twocharlocalname |
     *                         threeormorecharname
     * onecharsimplename   ::= (* Any Unicode character except:
     *                            '.', '/', ':', '[', ']', '*',
     *                           ''', '"', '|' or any whitespace
     *                            character *)
     * twocharsimplename   ::= '.' onecharsimplename |
     *                         onecharsimplename '.' |
     *                         onecharsimplename onecharsimplename
     * onecharlocalname    ::= nonspace
     * twocharlocalname    ::= nonspace nonspace
     * threeormorecharname ::= nonspace string nonspace
     * prefix              ::= (* Any valid non-empty XML NCName *)
     * string              ::= char | string char
     * char                ::= nonspace | ' '
     * nonspace            ::= (* Any Unicode character except:
     *                            '/', ':', '[', ']', '*',
     *                            ''', '"', '|' or any whitespace
     *                            character *)
     * </pre>
     * <p>
     */
    public static class NameEncoding implements StringCodec {
        /** Pattern on an encoded character */
        private static final Pattern ENCODE_PATTERN = Pattern.compile("_x\\p{XDigit}{4}_");
        /** Padding characters */
        private static final char[] PADDING = new char[] {'0', '0', '0'};

        /**
         * <p>Encode the char to a valid JCR string.</p>
         * <p>Calling encode multiple times on the same string will return
         * the same result as encoding the string once.</p>
         * @param c the char to encode
         * @return the encoded char as string
         */
        public final String encode(final char c) {
            return encodeOneCharSimpleName(c);
        }

        /**
         * <p>Encode the name to a valid JCR name, it is not possible to specify a
         * namespace, any passed namespace prefix will be encoded as well
         * <p>Calling encode multiple times on the same string will return
         * the same result as encoding the string once.</p>
         * <p>An IllegalArgumentException is thrown when the name is empty or null.</p>
         * @param name the name to encode
         * @return the encoded name
         */
        public final String encode(final String name) {
            if (name == null) {
                throw new IllegalArgumentException("Node name can not be null.");
            }
            if (name.length() == 0) {
                throw new IllegalArgumentException("Node name can not be empty.");
            }
            if (name.length() == 1) {
                return encodeOneCharSimpleName(name.charAt(0));
            }
            if (name.length() == 2) {
                return encodeTwoCharSimpleName(name.charAt(0), name.charAt(1));
            }
            return encodeThreeOrMoreCharName(name);
        }

        /**
         * Decode the name string with the ISO9075 coding standard.
         * <p>Calling decode multiple times on the same string will return
         * the same result as decoding the string once.</p>
         * @param name the name to decode
         * @return the decoded name
         */
        public String decode(final String name) {
            // quick check
            if (name.indexOf("_x") < 0) {
                // not encoded
                return name;
            }
            StringBuffer decoded = new StringBuffer();
            Matcher m = ENCODE_PATTERN.matcher(name);
            while (m.find()) {
                char ch = (char)Integer.parseInt(m.group().substring(2, 6), 16);
                if (ch == '$' || ch == '\\') {
                    m.appendReplacement(decoded, "\\" + ch);
                } else {
                    m.appendReplacement(decoded, Character.toString(ch));
                }
            }
            m.appendTail(decoded);
            return decoded.toString();
        }

        private static final String encodeOneCharSimpleName(final char c) {
            if (!isOneCharSimpleName(c)) {
                return ISO9075Encode(c);
            } else {
                char[] s = new char[1];
                s[0] = c;
                return new String(s);
            }
        }

        private static final String encodeTwoCharSimpleName(final char first, final char second) {
            if (first == '.') {
                return "." + encodeOneCharSimpleName(second);
            } else if (second == '.') {
                return encodeOneCharSimpleName(first) + ".";
            } else {
                return encodeOneCharSimpleName(first) + encodeOneCharSimpleName(second);
            }
        }

        private static final String encodeThreeOrMoreCharName(final String name) {
            int last = name.length() - 1;
            StringBuilder sb = new StringBuilder(last + 13); // reserve space

            // non space at start
            if (isNonSpace(name.charAt(0))) {
                sb.append(name.charAt(0));
            } else {
                sb.append(ISO9075Encode(name.charAt(0)));
            }

            // char = nonspace | ' '
            for (int i = 1; i < last; i++) {
                char c = name.charAt(i);
                if (c == ' ' || isNonSpace(c)) {
                    sb.append(c);
                } else {
                    sb.append(ISO9075Encode(c));
                }
            }

            // non space at end
            if (isNonSpace(name.charAt(last))) {
                sb.append(name.charAt(last));
            } else {
                sb.append(ISO9075Encode(name.charAt(last)));
            }
            return sb.toString();
        }

        private static final String encodeLocalName(final String localName) {
            if (localName.length() == 0) {
                throw new IllegalArgumentException("Local name part cannot be empty.");
            } else if (localName.length() == 1) {
                if (isNonSpace(localName.charAt(0))) {
                    return localName;
                } else {
                    return ISO9075Encode(localName.charAt(0));
                }
            } else if (localName.length() == 2) {
                StringBuilder sb = new StringBuilder();
                // unrolled loop
                if (isNonSpace(localName.charAt(0))) {
                    sb.append(localName.charAt(0));
                } else {
                    sb.append(ISO9075Encode(localName.charAt(0)));
                }
                if (isNonSpace(localName.charAt(1))) {
                    sb.append(localName.charAt(1));
                } else {
                    sb.append(ISO9075Encode(localName.charAt(1)));
                }
                return sb.toString();
            } else {
                return encodeThreeOrMoreCharName(localName);
            }

        }

        /**
         * Corresponds to the jcr node name definition with the same name.
         * @param c
         * @return
         */
        private static final boolean isOneCharSimpleName(final char c) {
            if (c == '.' || c == '/' || c == ':' || c == '[' || c == ']' || c == '*' || c == '\'' || c == '"' || c == '|') {
                return false;
            } else if (Character.isWhitespace(c)) {
                return false;
            } else {
                return true;
            }
        }

        /**
         * Corresponds to the jcr node name definition with the same name.
         * @param c
         * @return
         */
        private static final boolean isNonSpace(final char c) {
            if (c == '/' || c == ':' || c == '[' || c == ']' || c == '*' || c == '\'' || c == '"' || c == '|') {
                return false;
            } else if (Character.isWhitespace(c)) {
                return false;
            } else {
                return true;
            }
        }

        private static final String ISO9075Encode(final char c) {
            StringBuilder b = new StringBuilder();
            b.append("_x");
            String hex = Integer.toHexString(c);
            b.append(PADDING, 0, 4 - hex.length());
            b.append(hex);
            b.append("_");
            return b.toString();
        }
    }
}
