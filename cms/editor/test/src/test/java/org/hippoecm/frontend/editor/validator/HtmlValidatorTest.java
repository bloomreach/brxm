/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.validator;

import java.util.Set;

import org.hippoecm.frontend.validation.ValidationException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HtmlValidatorTest {
    
    private static final String NORMAL_SPACE = Character.toString((char) 32);
    private static final String NON_BREAKING_SPACE = Character.toString((char) 160);

    @Test
    public void testValidHtml() throws Exception {
        String text = "aap noot mies";
        Set<String> violations = validate(text);
        assertEquals(0, violations.size());
    }

    @Test
    public void testValidComplexHtml() throws Exception {
        String text = "\n\raap<br /><p/>";
        Set<String> violations = validate(text);
        assertEquals(0, violations.size());
    }

    @Test
    public void testEmptyHtml() throws Exception {
        String text = "";
        Set<String> violations = validate(text);
        assertEquals(1, violations.size());
    }

    @Test
    public void testEmptyHtmlWithBr() throws Exception {
        String text = "<br />";
        Set<String> violations = validate(text);
        assertEquals(1, violations.size());
    }

    @Test
    public void testEmptyHtmlWithSpace() throws Exception {
        String text = "<p>" + NORMAL_SPACE + "</p>";
        Set<String> violations = validate(text);
        assertEquals(1, violations.size());
    }

    @Test
    public void testEmptyHtmlWithNonBreakingSpace() throws Exception {
        String text = "<p>" + NON_BREAKING_SPACE + "</p>";
        Set<String> violations = validate(text);
        assertEquals(1, violations.size());
    }

    @Test
    public void testEmptyHtmlWithInvisibleCharacters() throws Exception {
        String text = "\f\n\t\r";
        Set<String> violations = validate(text);
        assertEquals(1, violations.size());
    }

    @Test
    public void testImageHtml() throws Exception {
        String text = "<img src=\"xxx\" />";
        Set<String> violations = validate(text);
        assertEquals(0, violations.size());
    }
    
    @Test
    public void testObjectHtml() throws Exception {
        String text = "<object data=\"http://www.youtube.com/v/3Rj9oiNZiog&amp;hl=en\" height=\"355\" type=\"application/x-shockwave-flash\" width=\"425\"><param name=\"movie\" value=\"http://www.youtube.com/v/3Rj9oiNZiog&amp;hl=en\"/><param name=\"wmode\" value=\"transparent\"/></object>";
        Set<String> violations = validate(text);
        assertEquals(0, violations.size());
    }

    @Test
    public void testEmbedHtml() throws Exception {
        String text = "<EMBED TYPE=\"application/x-mplayer2\" SRC=\"videofilename.wmv\" NAME=\"MediaPlayer\" WIDTH=\"192\" HEIGHT=\"290\" ShowControls=\"1\" ShowStatusBar=\"1\" ShowDisplay=\"1\" autostart=\"0\"> </EMBED>";
        Set<String> violations = validate(text);
        assertEquals(0, violations.size());
    }

    @Test
    public void testAppletHtml() throws Exception {
        String text = "<APPLET CODE=\"MyApplet.class\" WIDTH=200 HEIGHT=50><PARAM NAME=TEXT VALUE=\"Hi There\"></APPLET>";
        Set<String> violations = validate(text);
        assertEquals(0, violations.size());
    }

    @Test
    public void testFormHtml() throws Exception {
        String text = "<form name=\"myForm\"><input type=\"button\" value=\"button\" /></form>";
        Set<String> violations = validate(text);
        assertEquals(0, violations.size());
    }

    private Set<String> validate(final String text) throws ValidationException {
        String html = "<html><body>" + text + "</body></html>";
        return new HtmlValidator().validateNonEmpty(html);
    }

}
