/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.onehippo.cms7.ckeditor.CKEditorConstants;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link DeclarativeKeystrokesConverter}.
 */
public class DeclarativeKeystrokesConverterTest {

    @Test
    public void nullConvertsToNull() {
        assertNull(DeclarativeKeystrokesConverter.convertToNumericKeystrokes(null));
    }

    @Test
    public void noKeystrokesConvertsToEmptyArray() {
        final JSONArray converted = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(new JSONArray());
        assertEquals(0, converted.length());
    }

    @Test
    public void emptyKeystrokeIsIgnored() throws JSONException {
        final JSONArray keystrokes = new JSONArray("[ [ ] ]");
        final JSONArray converted = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes);
        assertEquals(0, converted.length());
    }

    @Test
    public void numericKeystrokeDoesNotChange() throws JSONException {
        final JSONArray f1Help = new JSONArray("[ [ 112, 'help' ] ]");
        assertEquals(f1Help.toString(), DeclarativeKeystrokesConverter.convertToNumericKeystrokes(f1Help).toString());
    }

    @Test
    public void numberStringConvertsToAsciiCodeOfNumber() throws JSONException {
        final JSONArray keystrokes = new JSONArray("[ [ '4', 'command' ] ]");
        final int asciiCodeOf4 = 52;
        assertSingleKeystroke(asciiCodeOf4, "command", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes));
    }

    @Test
    public void characterStringConvertsToAsciiCodeOfUpperCaseLetter() throws JSONException {
        final JSONArray keystrokes = new JSONArray("[ [ 'a', 'some command' ] ]");
        final int asciiCodeOfA = 65;
        assertSingleKeystroke(asciiCodeOfA, "some command", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes));
    }

    @Test
    public void functionKey() throws JSONException {
        final JSONArray keystrokes = new JSONArray("[ [ 'f1', 'help' ] ]");
        final int codeOfF1 = 112;
        assertSingleKeystroke(codeOfF1, "help", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes));
    }

    @Test
    public void controlA() throws JSONException {
        final JSONArray keystrokes = new JSONArray("[ [ 'ctrl', 'a', 'select all' ] ]");
        assertSingleKeystroke(CKEditorConstants.CTRL + 'A', "select all", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes));
    }

    @Test
    public void controlAltShiftX() throws JSONException {
        final JSONArray ctrlAltShiftX = new JSONArray("[ [ 'ctrl', 'alt', 'shift', 'x', 'very special command' ] ]");
        assertSingleKeystroke(CKEditorConstants.CTRL + CKEditorConstants.ALT + CKEditorConstants.SHIFT + 'X',
                "very special command", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(ctrlAltShiftX));
    }

    @Test
    public void keysOrderIsIrrelevant() throws JSONException {
        final JSONArray keystrokes = new JSONArray("[ [ 'p', 'ctrl', 'alt', 'command' ] ]");
        assertSingleKeystroke(CKEditorConstants.CTRL + CKEditorConstants.ALT + 'P',
                "command", DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes));
    }

    @Test
    public void keystrokeWithOneElementIsIgnored() throws JSONException {
        final JSONArray keystrokes = new JSONArray("[ [ 'a' ] ]");
        final JSONArray converted = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes);
        assertEquals(0, converted.length());
    }

    @Test
    public void allKeystrokeAreConverted() throws JSONException {
        final JSONArray keystrokes = new JSONArray("[ [ 'ctrl', 'a', 'select all' ], [ 'alt', 'b', 'show blocks' ], [ 'ctrl', 'alt', 'x', 'special cut' ] ]");
        final JSONArray converted = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes);
        assertEquals(3, converted.length());
        assertKeystroke(CKEditorConstants.CTRL + 'A', "select all", converted.getJSONArray(0));
        assertKeystroke(CKEditorConstants.ALT + 'B', "show blocks", converted.getJSONArray(1));
        assertKeystroke(CKEditorConstants.CTRL + + CKEditorConstants.ALT + 'X', "special cut", converted.getJSONArray(2));
    }

    @Test
    public void onlyIllegalKeystrokesAreIgnored() throws JSONException {
        final JSONArray keystrokes = new JSONArray("[ [ 'ctrl', 'x', 'command 1' ], [ 'a' ], [ 'alt', 'b', 'command 2' ] ]");
        final JSONArray converted = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(keystrokes);
        assertEquals(2, converted.length());
        assertKeystroke(CKEditorConstants.CTRL + 'X', "command 1", converted.getJSONArray(0));
        assertKeystroke(CKEditorConstants.ALT + 'B', "command 2", converted.getJSONArray(1));
    }

    private void assertSingleKeystroke(final int number, final String command, final JSONArray converted) throws JSONException {
        assertEquals(1, converted.length());
        assertKeystroke(number, command, converted.getJSONArray(0));
    }

    private void assertKeystroke(final int number, final String command, final JSONArray keystroke) throws JSONException {
        assertEquals(number, keystroke.get(0));
        assertEquals(command, keystroke.get(1));
    }

}
