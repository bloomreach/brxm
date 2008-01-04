/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.api;

import org.apache.jackrabbit.util.ISO9075;


/**
 * <h2>Helper class for encoding and decoding (the localname of) Qualified names</h2>
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
public class ISO9075Helper {
    //private static final char[] realChars = {'/', ':', '[', ']', '*', '\'', '"', '|'};
    //private static final String[] realChars = {"/", ":", "[", "]", "*", "'", "\"", "|"};
    //private static final String[] encodedChars = {"_x002F_", "_x003A_", '[', ']', "_x002A_", "_x0027_", "_x0022_", "_x007C_"};

    private static final String colon = ":";
    private static final String colonISO9075 = "_x003A_";

    /**
     * Constructor
     */
    private ISO9075Helper() {};

    static public String encodeLocalName(String name) {
        return encodeColon(ISO9075.encode(name));
    }
    public static String decodeLocalName(String name) {
        return ISO9075.decode(name);
    }

    static public String encodeColon(String name) {
        return name.replaceAll(colon,colonISO9075);
    }
    static public String decodeColon(String name) {
        return name.replaceAll(colonISO9075,colon);
    }

}
