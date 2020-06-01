/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor;

import java.io.IOException;
import java.io.StringWriter;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.Serializer;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// These tests verify fixes in the external HtmlCleaner module
public class HtmlCleanerTest {

    private HtmlCleaner cleaner;

    @Before
    public void setUp() {
        final CleanerProperties properties = new CleanerProperties();
        properties.setOmitHtmlEnvelope(true);
        properties.setOmitXmlDeclaration(true);
        cleaner = new HtmlCleaner(properties);
    }

    // Verify fix for CMS-9563
    @Test
    public void expectScriptTagIsNotRemoved() throws Exception {
        final String original = "<h1><style type=\"text/css\" scoped>h1 {color:black;}</style>42</h1>";
        final String expected = "<h1><style type=\"text/css\" scoped=\"scoped\">h1 {color:black;}</style>42</h1>";

        assertEquals(expected, toString(cleaner.clean(original)));
    }

    // Verify fix for CMS-9570
    @Test
    public void expectSingleQuoteToNotBeEncoded() throws Exception {
        final String original = "foo ' bar";
        final String expected = "foo ' bar";

        assertEquals(expected, toString(cleaner.clean(original)));
    }

    // Fix for CMS-10469
    @Test
    public void expectLoopFinishes() throws Exception {
        final String original = "<div>\n" +
                " <picture>\n" +
                "<source media=\"(max-width: 700px)\" sizes=\"(max-width: 500px) 50vw, 10vw\"\n" +
                "srcset=\"stick-figure-narrow.png 138w, stick-figure-hd-narrow.png 138w\">\n" +
                "\n" +
                "<source media=\"(max-width: 1400px)\" sizes=\"(max-width: 1000px) 100vw, 50vw\"\n" +
                "srcset=\"stick-figure.png 416w, stick-figure-hd.png 416w\">\n" +
                "\n" +
                "<img src=\"stick-original.png\" alt=\"Human\">\n" +
                "</picture>\n" +
                "</div>";
        final String expected = "<div>\n" +
                " <picture>\n" +
                "<audio><source media=\"(max-width: 700px)\" sizes=\"(max-width: 500px) 50vw, 10vw\" srcset=\"stick-figure-narrow.png 138w, stick-figure-hd-narrow.png 138w\" />\n" +
                "\n" +
                "<source media=\"(max-width: 1400px)\" sizes=\"(max-width: 1000px) 100vw, 50vw\" srcset=\"stick-figure.png 416w, stick-figure-hd.png 416w\" />\n" +
                "\n" +
                "<img src=\"stick-original.png\" alt=\"Human\" />\n" +
                "</audio></picture></div>";

        assertEquals(expected, toString(cleaner.clean(original)));
    }


    private String toString(final TagNode tagNode) throws IOException {
        Serializer serializer = new SimpleHtmlSerializer(cleaner.getProperties());
        final StringWriter writer = new StringWriter();
        serializer.write(tagNode, writer, "UTF-8");
        return writer.getBuffer().toString().trim();
    }
}
