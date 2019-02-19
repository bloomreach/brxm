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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HtmlUtilsTest {

    private static final String NORMAL_SPACE = Character.toString((char) 32);
    private static final String NON_BREAKING_SPACE = Character.toString((char) 160);

    @Test
    public void testValidHtml() throws Exception {
        assertNotEmpty("aap noot mies");
    }

    @Test
    public void testValidComplexHtml() throws Exception {
        assertNotEmpty("\n\raap<br /><p/>");
    }

    @Test
    public void testEmptyHtml() throws Exception {
        assertEmpty("");
    }

    @Test
    public void testEmptyHtmlWithBr() throws Exception {
        assertEmpty("<br />");
    }

    @Test
    public void testEmptyHtmlWithSpace() throws Exception {
        assertEmpty("<p>" + NORMAL_SPACE + "</p>");
    }

    @Test
    public void testEmptyHtmlWithNonBreakingSpace() throws Exception {
        assertEmpty("<p>" + NON_BREAKING_SPACE + "</p>");
    }

    @Test
    public void testEmptyHtmlWithInvisibleCharacters() throws Exception {
        assertEmpty("\f\n\t\r");
    }

    @Test
    public void testImageHtml() throws Exception {
        assertNotEmpty("<img src=\"xxx\" />");
    }

    @Test
    public void testObjectHtml() throws Exception {
        assertNotEmpty("<object data=\"http://www.youtube.com/v/3Rj9oiNZiog&amp;hl=en\" height=\"355\" type=\"application/x-shockwave-flash\" width=\"425\"><param name=\"movie\" value=\"http://www.youtube.com/v/3Rj9oiNZiog&amp;hl=en\"/><param name=\"wmode\" value=\"transparent\"/></object>");
    }

    @Test
    public void testEmbedHtml() throws Exception {
        assertNotEmpty("<EMBED TYPE=\"application/x-mplayer2\" SRC=\"videofilename.wmv\" NAME=\"MediaPlayer\" WIDTH=\"192\" HEIGHT=\"290\" ShowControls=\"1\" ShowStatusBar=\"1\" ShowDisplay=\"1\" autostart=\"0\"> </EMBED>");
    }

    @Test
    public void testAppletHtml() throws Exception {
        assertNotEmpty("<APPLET CODE=\"MyApplet.class\" WIDTH=200 HEIGHT=50><PARAM NAME=TEXT VALUE=\"Hi There\"></APPLET>");
    }

    @Test
    public void testFormHtml() throws Exception {
        assertNotEmpty("<form name=\"myForm\"><input type=\"button\" value=\"button\" /></form>");
    }

    @Test
    public void testIframeHtml() throws Exception {
        assertNotEmpty("<iframe src=\"http://example.com\"></iframe>");
    }

    private void assertNotEmpty(final String text) {
        assertFalse(isEmpty(text));
    }

    private void assertEmpty(final String text) {
        assertTrue(isEmpty(text));
    }

    private boolean isEmpty(final String text) {
        final String html = "<html><body>" + text + "</body></html>";
        return HtmlUtils.isEmpty(html);
    }

}
