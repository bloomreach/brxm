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
package org.hippoecm.hst.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;

public class TestSimpleHtmlExtractor {

    private static final String SIMPLE_HTML = 
        "<html>\n" + 
        "<head>\n" + 
        "<title>Hello</title>\n" + 
        "</head>\n" + 
        "<body>\n" + 
        "<h1>Hello, World!</h1>\n" + 
        "</body>\n" + 
        "</html>";
    
    private static final String EMPTY_BODY_HTML = 
        "<html>\n" + 
        "<head>\n" + 
        "<title>Hello</title>\n" + 
        "</head>\n" + 
        "<body></body>\n" + 
        "</html>";
    
    private static final String NULL_BODY_HTML = 
        "<html>\n" + 
        "<head>\n" + 
        "<title>Hello</title>\n" + 
        "</head>\n" + 
        "<body/>\n" + 
        "</html>";
    
    private static final String A_VULNERABLE_INPUT = "\"><script>alert(1)</script>"; 
    
    @Test
    public void testInnerHtmlExtraction() throws Exception {
        String titleInnerHtml = SimpleHtmlExtractor.getInnerHtml(SIMPLE_HTML, "title", false);
        assertEquals("title content is not properly extracted: " + titleInnerHtml.trim(), "Hello", titleInnerHtml.trim());

        String bodyInnerHtml = SimpleHtmlExtractor.getInnerHtml(SIMPLE_HTML, "body", false);
        assertEquals("body content is not properly extracted: " + bodyInnerHtml.trim(), "<h1>Hello, World!</h1>", bodyInnerHtml.trim());
        
        titleInnerHtml = SimpleHtmlExtractor.getInnerHtml(SIMPLE_HTML, "title", true);
        assertEquals("title content is not properly extracted: " + titleInnerHtml.trim(), "Hello", titleInnerHtml.trim());

        bodyInnerHtml = SimpleHtmlExtractor.getInnerHtml(SIMPLE_HTML, "body", true);
        assertEquals("body content is not properly extracted: " + bodyInnerHtml.trim(), "<h1>Hello, World!</h1>", bodyInnerHtml.trim());
        
        bodyInnerHtml = SimpleHtmlExtractor.getInnerHtml(EMPTY_BODY_HTML, "body", false);
        assertEquals("body content is not properly extracted: " + bodyInnerHtml, "", bodyInnerHtml);
        bodyInnerHtml = SimpleHtmlExtractor.getInnerHtml(NULL_BODY_HTML, "body", false);
        assertNull("body content is not properly extracted: " + bodyInnerHtml, bodyInnerHtml);
    }
    
    @Test
    public void testInnerTextExtraction() throws Exception {
        String titleInnerText = SimpleHtmlExtractor.getInnerText(SIMPLE_HTML, "title");
        assertEquals("title content is not properly extracted: " + titleInnerText.trim(), "Hello", titleInnerText.trim());

        String bodyInnerText = SimpleHtmlExtractor.getInnerText(SIMPLE_HTML, "body");
        assertEquals("body content is not properly extracted: " + bodyInnerText.trim(), "Hello, World!", bodyInnerText.trim());
        
        // Test with broken html markups.
        bodyInnerText = SimpleHtmlExtractor.getInnerText(SIMPLE_HTML.substring(0, SIMPLE_HTML.indexOf("World")), "body");
        assertEquals("body content is not properly extracted: " + bodyInnerText.trim(), "Hello,", bodyInnerText.trim());
    }
    
    @Test
    public void testSimpleBenchmark() throws Exception {
        String featuresHtml = readFeaturesHtml();
        
        String bodyInnerHtml = SimpleHtmlExtractor.getInnerHtml(featuresHtml, "body", false);
        String bodyInnerHtml2 = SimpleHtmlExtractor.getInnerHtml(featuresHtml, "body", true);
        
        long t1 = 0L, t2 = 0L;
        
        t1 = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 10; j++) {
                bodyInnerHtml = SimpleHtmlExtractor.getInnerHtml(featuresHtml, "body", false);
            }
        }

        t2 = System.currentTimeMillis();
        
        t1 = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 10; j++) {
                bodyInnerHtml = SimpleHtmlExtractor.getInnerHtml(featuresHtml, "body", true);
            }
        }

        t2 = System.currentTimeMillis();
    }
    
    @Test
    public void testExtractingTextFromHtml() throws Exception {
        String simpleText = SimpleHtmlExtractor.getText(SIMPLE_HTML);
        assertTrue(simpleText.contains("Hello"));
        assertFalse(simpleText.contains("<title>Hello</title>"));
        assertTrue(simpleText.contains("Hello, World!"));
        assertFalse(simpleText.contains("<h1>Hello, World!</h1>"));
        
        String textFromVulnerableInput = SimpleHtmlExtractor.getText(A_VULNERABLE_INPUT);
        assertTrue(textFromVulnerableInput.contains("alert(1)"));
        assertFalse(textFromVulnerableInput.contains("<script>alert(1)</script>"));
    }
    
    private String readFeaturesHtml() throws Exception {
        String html = "";
        
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        StringWriter sw = null;
        PrintWriter out = null;
        
        try {
            is = getClass().getClassLoader().getResourceAsStream("org/hippoecm/hst/utils/features.html");
            isr = new InputStreamReader(is, "ISO-8859-1");
            br = new BufferedReader(isr);
            sw = new StringWriter(14000);
            out = new PrintWriter(sw);
            
            String line = br.readLine();
            
            while (line != null) {
                out.println(line);
                line = br.readLine();
            }
            
            out.flush();
            
            html = sw.toString();
        } finally {
            if (is != null) try { is.close(); } catch (Exception ce) { }
            if (isr != null) try { isr.close(); } catch (Exception ce) { }
            if (br != null) try { br.close(); } catch (Exception ce) { }
            if (sw != null) try { sw.close(); } catch (Exception ce) { }
            if (out != null) try { out.close(); } catch (Exception ce) { }
        }
        
        return html;
    }
    
}
