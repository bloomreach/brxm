/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.validation.util;

import org.junit.Test;
import org.onehippo.cms.services.validation.util.HtmlUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HtmlUtilsTest {

    private static final String NORMAL_SPACE = Character.toString((char) 32);
    private static final String NON_BREAKING_SPACE = Character.toString((char) 160);

    @Test
    public void nullIsEmpty() {
        assertTrue(HtmlUtils.isEmpty(null));
    }

    @Test
    public void blankStringIsEmpty() {
        assertEmpty("");
        assertEmpty(" ");
    }

    @Test
    public void invisibleCharactersIsEmpty() {
        assertEmpty("\f\n\t\r");
    }

    @Test
    public void simpleHtmlIsNotEmpty() {
        assertNotEmpty("aap noot mies");
    }

    @Test
    public void complexHtmlIsNotEmpty()  {
        assertNotEmpty("\n\raap<br /><p/>");
    }

    @Test
    public void brIsEmpty() {
        assertEmpty("<br />");
    }

    @Test
    public void paragraphWithNormalSpaceIsEmpty() {
        assertEmpty("<p>" + NORMAL_SPACE + "</p>");
    }

    @Test
    public void paragraphWithNonBreakingSpaceIsEmpty() {
        assertEmpty("<p>" + NON_BREAKING_SPACE + "</p>");
    }

    @Test
    public void imageIsNotEmpty() {
        assertNotEmpty("<img src=\"xxx\" />");
    }

    @Test
    public void objectIsNotEmpty() {
        assertNotEmpty("<object data=\"http://www.youtube.com/v/3Rj9oiNZiog&amp;hl=en\" height=\"355\" type=\"application/x-shockwave-flash\" width=\"425\"><param name=\"movie\" value=\"http://www.youtube.com/v/3Rj9oiNZiog&amp;hl=en\"/><param name=\"wmode\" value=\"transparent\"/></object>");
    }

    @Test
    public void embedIsNotEmpty() {
        assertNotEmpty("<EMBED TYPE=\"application/x-mplayer2\" SRC=\"videofilename.wmv\" NAME=\"MediaPlayer\" WIDTH=\"192\" HEIGHT=\"290\" ShowControls=\"1\" ShowStatusBar=\"1\" ShowDisplay=\"1\" autostart=\"0\"> </EMBED>");
    }

    @Test
    public void appletIsNotEmpty() {
        assertNotEmpty("<APPLET CODE=\"MyApplet.class\" WIDTH=200 HEIGHT=50><PARAM NAME=TEXT VALUE=\"Hi There\"></APPLET>");
    }

    @Test
    public void formIsNotEmpty() {
        assertNotEmpty("<form name=\"myForm\"><input type=\"button\" value=\"button\" /></form>");
    }

    @Test
    public void iframeIsNotEmpty() {
        assertNotEmpty("<iframe src=\"http://example.com\"></iframe>");
    }

    private static String wrapWithHtmlBody(final String text) {
        return "<html><body>" + text + "</body></html>";
    }

    private static void assertNotEmpty(final String text) {
        assertFalse(HtmlUtils.isEmpty(text));
        assertFalse(HtmlUtils.isEmpty(wrapWithHtmlBody(text)));
    }

    private static void assertEmpty(final String text) {
        assertTrue(HtmlUtils.isEmpty(text));
        assertTrue(HtmlUtils.isEmpty(wrapWithHtmlBody(text)));
    }

}
