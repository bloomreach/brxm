/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.ckeditor;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a JSON array with declarative and numeric CKEditor keystrokes configuration
 * into a JSON array with only numeric keystroke configuration.
 *
 * CKEditor keystroke configuration is numeric. Each keystroke consists of array of two elements:
 * the keystroke number, and the command to execute. The calculation of the number requires an addition
 * of ASCII codes of characters and special numbers for Ctrl, Alt, and Shift. For example,
 * a key 'F1' that executes 'help' is normally configured as <code>[ 112, 'help' ]</code>.
 *
 * This class also recognizes a more declarative keystroke style, where the various keys
 * in a keystroke are represented as separate string elements in a keystroke configuration array.
 * The last array element is still the command to execute. For example, the keystroke 'Ctrl-Alt-H' that executes
 * 'help' can be represented as <code>[ 'Ctrl', 'Alt', 'H', 'help' ]</code>.
 *
 * Special key names in declarative keystrokes are 'Ctrl', 'Alt', 'Shift' and 'F1' to 'F12'. Other characters are
 * represented as a single-character string. Key names are not case-sensitive.
 */
class DeclarativeKeystrokesConverter {

    private static final Logger log = LoggerFactory.getLogger(DeclarativeKeystrokesConverter.class);
    private static final Map<String, Integer> KEY_MAP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        KEY_MAP.put("Ctrl", CKEditorConfig.CTRL);
        KEY_MAP.put("Shift", CKEditorConfig.SHIFT);
        KEY_MAP.put("Alt", CKEditorConfig.ALT);

        // F1 to F12
        for (int i = 1; i <= 12; i++) {
            KEY_MAP.put("F" + i, 111 + i);
        }

        // letters use the ASCII code of the uppercase variant
        for (char letter = 'A'; letter < 'Z'; letter++) {
            KEY_MAP.put(String.valueOf(letter), asciiCode(letter));
        }
    }

    private DeclarativeKeystrokesConverter() {
        // prevent instantiation
    }

    /**
     * Converts all declarative keystrokes in the given array to numeric CKEditor keystrokes.
     * Existing numeric CKEditor keystrokes are not changed. Unparsable keystrokes are ignored.
     *
     * @param keystrokes the array of declarative and/or numeric CKEditor keystrokes. Can be null.
     * @return an array of numeric CKEditor keystrokes, or null if the given keystrokes were null
     *         or not an array.
     */
    static ArrayNode convertToNumericKeystrokes(JsonNode keystrokes) {
        if (keystrokes == null || !keystrokes.isArray()) {
            return null;
        }

        final ArrayNode result = Json.array();

        for (int i = 0; i < keystrokes.size(); i++) {
            try {
                final JsonNode keystroke = keystrokes.get(i);
                if (keystroke.isArray()) {
                    final ArrayNode parsedKeystroke = parseKeystroke((ArrayNode) keystroke);
                    result.add(parsedKeystroke);
                }
            } catch (ParseException e) {
                log.warn("Ignoring unparsable keystroke #{} in keystroke array {}", i, keystrokes.toString());
            }
        }

        return result;
    }

    private static ArrayNode parseKeystroke(final ArrayNode keystroke) throws ParseException {
        if (keystroke.size() < 2) {
            throw new ParseException("Keystroke array " + keystroke.toString() + " is too short, must contain at least two elements");
        }

        final JsonNode first = keystroke.get(0);
        final String command = keystroke.get(keystroke.size() - 1).asText();

        if (first.isIntegralNumber()) {
            // native CKEditor keystroke, return as-is
            return keystroke;
        } else if (first.isTextual()) {
            // parse custom keystroke of one or more strings
            final int keystrokeNumber = parseKeystrokeNumber(keystroke);
            final ArrayNode array = Json.array();
            array.add(keystrokeNumber);
            array.add(command);
            return array;
        }
        throw new ParseException("First keystroke element must be a number of string, but got '" + first + "'");
    }

    private static int parseKeystrokeNumber(final ArrayNode keystroke) throws ParseException {
        int number = 0;
        for (int i = 0; i < keystroke.size() - 1; i++) {
            String keystrokeElement = keystroke.get(i).asText();
            if (StringUtils.isNotEmpty(keystrokeElement)) {
                number += getKeystrokeNumber(keystrokeElement);
            }
        }
        return number;
    }

    private static int getKeystrokeNumber(final String keystrokeElement) throws ParseException {
        Integer numberOrNull = KEY_MAP.get(keystrokeElement);
        if (numberOrNull != null) {
            return numberOrNull;
        }
        if (keystrokeElement.length() == 1) {
            return asciiCode(keystrokeElement.charAt(0));
        }
        throw new ParseException("Unknown keystroke element: '" + keystrokeElement + "'");
    }

    private static int asciiCode(final char c) {
        return (int)c;
    }


    private static class ParseException extends Exception {

        public ParseException(final String message) {
            super(message);
        }
    }
}
