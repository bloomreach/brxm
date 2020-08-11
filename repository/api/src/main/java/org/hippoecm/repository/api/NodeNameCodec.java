/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public final class NodeNameCodec {

    /** Pattern on an encoded character */
    private static final Pattern ENCODE_PATTERN = Pattern.compile("_x\\p{XDigit}{4}_");

    /** Padding characters */
    private static final char[] PADDING = new char[] {'0', '0', '0'};

    private boolean forceSimpleName;

    /**
     * <p>Encode the name to a valid JCR name. If the name is prefixed with a
     * namespace, the prefix is not encoded but returned as-is.</p>
     * <p>Calling encode multiple times on the same string will return
     * the same result as encoding the string once.</p>
     * <p>An IllegalArgumentException is thrown when the name is empty or null.</p>
     * @param name the name to encode
     * @return the encoded name
     */
    public static final String encode(final String name) {
        return encode(name, false);
    }

    /**
     * <p>Encode the char to a valid JCR string.</p>
     * <p>Calling encode multiple times on the same string will return
     * the same result as encoding the string once.</p>
     * @param c the char to encode
     * @return the encoded char as string
     */
    public static final String encode(final char c) {
        return encodeOneCharSimpleName(c);
    }

    /**
     * <p>Encode the name to a valid JCR name. If the name is prefixed with a
     * namespace, the prefix is not encoded but returned as-is. The force
     * simple name option, forces the codec to encode the name as simple name by
     * encoding colons.</p>
     * <p>Calling encode multiple times on the same string will return
     * the same result as encoding the string once.</p>
     * <p>An IllegalArgumentException is thrown when the name is empty or null.</p>
     * @param name the name to encode
     * @param forceSimpleName force the name to be interpreted as a simple (non prefixed) name
     * @return the encoded name
     */

    public static final String encode(final String name, final boolean forceSimpleName) {
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
        // length > 2, it could be "a:b"
        int pos = name.indexOf(':');
        if (!forceSimpleName && pos > 0) {
            // just return refix as-is.
            return name.substring(0, pos) + ':' + encodeLocalName(name.substring(pos + 1));
        } else {
            return encodeThreeOrMoreCharName(name);
        }
    }

    /**
     * Decode the name string with the ISO9075 coding standard.
     * <p>Calling decode multiple times on the same string will return
     * the same result as decoding the string once.</p>
     * @param name the name to decode
     * @return the decoded name
     */
    public static final String decode(final String name) {
        // quick check
        if (name.indexOf("_x") < 0) {
            // not encoded
            return name;
        }
        StringBuffer decoded = new StringBuffer();
        Matcher m = ENCODE_PATTERN.matcher(name);
        while (m.find()) {
            char ch = (char) Integer.parseInt(m.group().substring(2, 6), 16);
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
