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

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link DeclarativeKeystrokesConverter}.
 */
public class DeclarativeKeystrokesConverterTest {

    @Test
    public void nullConvertsToNull() {
        assertNull(DeclarativeKeystrokesConverter.convertToNumericKeystrokes(null));
    }

    @Test
    public void nonArrayConvertsToNull() throws IOException {
        assertNull(DeclarativeKeystrokesConverter.convertToNumericKeystrokes(Json.object()));
    }

    @Test
    public void noKeystrokesConvertsToEmptyArray() {
        final ArrayNode converted = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(Json.array());
        assertEquals(0, converted.size());
    }

    @Test
    public void emptyKeystrokeIsIgnored() throws IOException {
        final ArrayNode keystrokes = Json.array("[ [ ] ]");
        final ArrayNode converted = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes);
        assertEquals(0, converted.size());
    }

    @Test
    public void numericKeystrokeDoesNotChange() throws IOException {
        final ArrayNode f1Help = Json.array("[ [ 112, 'help' ] ]");
        assertEquals(f1Help.toString(), DeclarativeKeystrokesConverter.convertToNumericKeystrokes(f1Help).toString());
    }

    @Test
    public void numberStringConvertsToAsciiCodeOfNumber() throws IOException {
        final ArrayNode keystrokes = Json.array("[ [ '4', 'command' ] ]");
        final int asciiCodeOf4 = 52;
        assertSingleKeystroke(asciiCodeOf4, "command", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes));
    }

    @Test
    public void characterStringConvertsToAsciiCodeOfUpperCaseLetter() throws IOException {
        final ArrayNode keystrokes = Json.array("[ [ 'a', 'some command' ] ]");
        final int asciiCodeOfA = 65;
        assertSingleKeystroke(asciiCodeOfA, "some command", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes));
    }

    @Test
    public void functionKey() throws IOException {
        final ArrayNode keystrokes = Json.array("[ [ 'f1', 'help' ] ]");
        final int codeOfF1 = 112;
        assertSingleKeystroke(codeOfF1, "help", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes));
    }

    @Test
    public void controlA() throws IOException {
        final ArrayNode keystrokes = Json.array("[ [ 'ctrl', 'a', 'select all' ] ]");
        assertSingleKeystroke(CKEditorConfig.CTRL + 'A', "select all", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes));
    }

    @Test
    public void controlAltShiftX() throws IOException {
        final ArrayNode ctrlAltShiftX = Json.array("[ [ 'ctrl', 'alt', 'shift', 'x', 'very special command' ] ]");
        assertSingleKeystroke(CKEditorConfig.CTRL + CKEditorConfig.ALT + CKEditorConfig.SHIFT + 'X',
                "very special command", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(ctrlAltShiftX));
    }

    @Test
    public void keysOrderIsIrrelevant() throws IOException {
        final ArrayNode keystrokes = Json.array("[ [ 'p', 'ctrl', 'alt', 'command' ] ]");
        assertSingleKeystroke(CKEditorConfig.CTRL + CKEditorConfig.ALT + 'P',
                "command", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes));
    }

    @Test
    public void keystrokeWithOneElementIsIgnored() throws IOException {
        final ArrayNode keystrokes = Json.array("[ [ 'a' ] ]");
        final ArrayNode converted = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes);
        assertEquals(0, converted.size());
    }

    @Test
    public void allKeystrokeAreConverted() throws IOException {
        final ArrayNode keystrokes = Json.array("[ [ 'ctrl', 'a', 'select all' ], [ 'alt', 'b', 'show blocks' ], [ 'ctrl', 'alt', 'x', 'special cut' ] ]");
        final ArrayNode converted = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes);
        assertEquals(3, converted.size());
        assertKeystroke(CKEditorConfig.CTRL + 'A', "select all", converted.get(0));
        assertKeystroke(CKEditorConfig.ALT + 'B', "show blocks", converted.get(1));
        assertKeystroke(CKEditorConfig.CTRL + + CKEditorConfig.ALT + 'X', "special cut", converted.get(2));
    }

    @Test
    public void onlyIllegalKeystrokesAreIgnored() throws IOException {
        final ArrayNode keystrokes = Json.array("[ [ 'ctrl', 'x', 'command 1' ], [ 'a' ], [ 'alt', 'b', 'command 2' ] ]");
        final ArrayNode converted = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes);
        assertEquals(2, converted.size());
        assertKeystroke(CKEditorConfig.CTRL + 'X', "command 1", converted.get(0));
        assertKeystroke(CKEditorConfig.ALT + 'B', "command 2", converted.get(1));
    }

    private void assertSingleKeystroke(final int number, final String command, final ArrayNode converted) throws IOException {
        assertEquals(1, converted.size());
        assertKeystroke(number, command, converted.get(0));
    }

    private void assertKeystroke(final int number, final String command, final JsonNode keystroke) throws IOException {
        assertTrue(keystroke.isArray());
        assertEquals(number, keystroke.get(0).intValue());
        assertEquals(command, keystroke.get(1).asText());
    }
}
