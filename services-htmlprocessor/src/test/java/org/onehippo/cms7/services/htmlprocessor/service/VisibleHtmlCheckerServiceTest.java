/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.service;

import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VisibleHtmlCheckerServiceTest {

    private static final String NORMAL_SPACE = Character.toString((char) 32);
    private static final String NON_BREAKING_SPACE = Character.toString((char) 160);

    private final VisibleHtmlCheckerService service = new VisibleHtmlCheckerService(MockNode.root());

    @Test
    public void nullIsInvisible() {
        assertFalse(service.isVisible(null));
    }

    @Test
    public void blankStringIsEmpty() {
        assertInvisible("");
        assertInvisible(" ");
    }

    @Test
    public void invisibleCharactersIsEmpty() {
        assertInvisible("\f\n\t\r");
    }

    @Test
    public void simpleHtmlIsNotEmpty() {
        assertVisible("aap noot mies");
    }

    @Test
    public void complexHtmlIsNotEmpty()  {
        assertVisible("\n\raap<br /><p/>");
    }

    @Test
    public void brIsEmpty() {
        assertInvisible("<br />");
    }

    @Test
    public void paragraphWithNormalSpaceIsEmpty() {
        assertInvisible("<p>" + NORMAL_SPACE + "</p>");
    }

    @Test
    public void paragraphWithNonBreakingSpaceIsEmpty() {
        assertInvisible("<p>" + NON_BREAKING_SPACE + "</p>");
    }

    @Test
    public void imageIsNotEmpty() {
        assertVisible("<img src=\"xxx\" />");
    }

    @Test
    public void objectIsNotEmpty() {
        assertVisible("<object data=\"http://www.youtube.com/v/3Rj9oiNZiog&amp;hl=en\" height=\"355\" type=\"application/x-shockwave-flash\" width=\"425\"><param name=\"movie\" value=\"http://www.youtube.com/v/3Rj9oiNZiog&amp;hl=en\"/><param name=\"wmode\" value=\"transparent\"/></object>");
    }

    @Test
    public void embedIsNotEmpty() {
        assertVisible("<EMBED TYPE=\"application/x-mplayer2\" SRC=\"videofilename.wmv\" NAME=\"MediaPlayer\" WIDTH=\"192\" HEIGHT=\"290\" ShowControls=\"1\" ShowStatusBar=\"1\" ShowDisplay=\"1\" autostart=\"0\"> </EMBED>");
    }

    @Test
    public void appletIsNotEmpty() {
        assertVisible("<APPLET CODE=\"MyApplet.class\" WIDTH=200 HEIGHT=50><PARAM NAME=TEXT VALUE=\"Hi There\"></APPLET>");
    }

    @Test
    public void formIsNotEmpty() {
        assertVisible("<form name=\"myForm\"><input type=\"button\" value=\"button\" /></form>");
    }

    @Test
    public void iframeIsNotEmpty() {
        assertVisible("<iframe src=\"http://example.com\"></iframe>");
    }

    private static String wrapWithHtmlBody(final String text) {
        return "<html><body>" + text + "</body></html>";
    }

    private void assertVisible(final String text) {
        assertTrue(service.isVisible(text));
        assertTrue(service.isVisible(wrapWithHtmlBody(text)));
    }

    private void assertInvisible(final String text) {
        assertFalse(service.isVisible(text));
        assertFalse(service.isVisible(wrapWithHtmlBody(text)));
    }
}
