/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.filter.Element;
import org.onehippo.cms7.services.htmlprocessor.serialize.HtmlSerializer;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class HtmlProcessorTest {

    private HtmlProcessor processor;
    private MockNode document;

    @Before
    public void setUp() throws RepositoryException {
        document = new MockNode("document");
        MockNode.root().addNode(document);
    }

    @Test
    public void nullTextDoesNotChange() throws Exception {
        final HtmlProcessorConfig htmlProcessorConfig = new HtmlProcessorConfig();
        processor = new HtmlProcessorImpl(htmlProcessorConfig);

        assertNoChanges(null);
    }

    @Test
    public void emptyTextDoesNotChange() throws Exception {
        final HtmlProcessorConfig htmlProcessorConfig = new HtmlProcessorConfig();
        processor = new HtmlProcessorImpl(htmlProcessorConfig);

        assertNoChanges("");
    }

    @Test
    public void htmlEntitiesArePreserved() throws IOException {
        testPreserved("&gt;&lt;&amp;&nbsp;");
    }

    @Test
    public void htmlCommentsArePreserved() throws IOException {
        testPreserved("<!-- comment -->");
    }

    @Test
    public void utfCharactersArePreserved() throws IOException {
        testPreserved("łФ௵سლ");
    }

    @Test
    public void codeBlockIsPreserved() throws IOException {
        testPreserved("<pre class=\"sh_xml\">&lt;hst:defineObjects/&gt;\n" +
                              "&lt;c:set var=\"isPreview\" value=\"${hstRequest.requestContext.preview}\"/&gt;\n" +
                              "</pre>");
    }

    @Test
    public void htmlAndBodyElementAreNotPreserved() throws Exception {
        final HtmlProcessorConfig config = new HtmlProcessorConfig();
        config.setFilter(false);
        config.setConvertLineEndings(false);
        processor = new HtmlProcessorImpl(config);

        final String read = processor.read("<html><body><p>text</p></body></html>", null);
        assertEquals("<p>text</p>", read);

        final String write = processor.write("<html><body><p>text</p></body></html>", null);
        assertEquals("<p>text</p>", write);
    }

    @Test
    public void XmlDeclarationIsNotPreserved() throws Exception {
        final HtmlProcessorConfig config = new HtmlProcessorConfig();
        config.setFilter(false);
        config.setConvertLineEndings(false);
        processor = new HtmlProcessorImpl(config);

        final String read = processor.read("<?xml version=\"1.0\"?> <p>text</p>", null);
        assertEquals("<p>text</p>", read);

        final String write = processor.write("<?xml version=\"1.0\"?> <p>text</p>", null);
        assertEquals("<p>text</p>", write);
    }

    @Test
    public void htmlCommentsCanBeStripped() throws Exception {
        final HtmlProcessorConfig config = new HtmlProcessorConfig();
        config.setFilter(false);
        config.setConvertLineEndings(false);
        config.setOmitComments(true);
        processor = new HtmlProcessorImpl(config);

        final String read = processor.read("<!-- comment --><p>text</p>", null);
        assertEquals("<p>text</p>", read);

        final String write = processor.write("<!-- comment --><p>text</p>", null);
        assertEquals("<p>text</p>", write);
    }

    @Test
    public void javascriptProtocolInAttributesCanBeStripped() throws Exception {
        final HtmlProcessorConfig config = new HtmlProcessorConfig();
        config.setFilter(true);
        final List<Element> whiteList = new ArrayList<>();
        final Element element = Element.create("a", "href", "onclick");
        whiteList.add(element);
        config.setWhitelistElements(whiteList);
        config.setConvertLineEndings(false);
        config.setOmitComments(true);
        config.setOmitJavascriptProtocol(true);
        processor = new HtmlProcessorImpl(config);

        String read = processor.read("<a href=\"#\" onclick=\"javascript:lancerPu('XXXcodepuXXX')\">XXXTexteXXX</a>",
                                     null);
        assertEquals("<a href=\"#\" onclick=\"javascript:lancerPu('XXXcodepuXXX')\">XXXTexteXXX</a>", read);

        String write = processor.write("<a onclick=\"javascript:lancerPu('XXXcodepuXXX')\" href=\"#\">XXXTexteXXX</a>",
                                       null);
        assertEquals("<a onclick=\"\" href=\"#\">XXXTexteXXX</a>", write);

        config.setOmitJavascriptProtocol(false);
        processor = new HtmlProcessorImpl(config);
        read = processor.read("<a href=\"#\" onclick=\"javascript:lancerPu('XXXcodepuXXX')\">XXXTexteXXX</a>", null);
        assertEquals("<a href=\"#\" onclick=\"javascript:lancerPu('XXXcodepuXXX')\">XXXTexteXXX</a>", read);

        write = processor.write("<a onclick=\"javascript:lancerPu('XXXcodepuXXX')\" href=\"#\">XXXTexteXXX</a>", null);
        assertEquals("<a onclick=\"javascript:lancerPu('XXXcodepuXXX')\" href=\"#\">XXXTexteXXX</a>", write);

    }

    @Test
    public void characterReferencesInAttributesAreNotNormalized() throws IOException {
        for (HtmlSerializer serializer : HtmlSerializer.values()) {
            final HtmlProcessorConfig config = new HtmlProcessorConfig();
            config.setFilter(true);
            config.setSerializer(serializer);

            final Element table = Element.create("table", "summary");
            config.setWhitelistElements(Collections.singletonList(table));

            processor = new HtmlProcessorImpl(config);

            final String html = "<table summary=\"&quot; onmouseover=alert('hi')\"></table>";
            final String written = processor.write(html, Collections.emptyList());

            assertEquals(serializer.name(), html, written);
        }
    }

    @Test
    public void testReadVisitor() throws Exception {
        final HtmlProcessorConfig htmlProcessorConfig = new HtmlProcessorConfig();
        processor = new HtmlProcessorImpl(htmlProcessorConfig);

        final TagNameCollector one = new TagNameCollector();
        final TagNameCollector two = new TagNameCollector();

        final List<TagVisitor> readVisitors = Arrays.asList(one, two);
        final String html = processor.read("<h1>Heading 1</h1><h2>Heading 2</h2>", readVisitors);
        assertEquals("<h1>Heading 1</h1><h2>Heading 2</h2>", html);

        final List<String> tagsOne = one.getTags();
        assertEquals(2, tagsOne.size());
        assertThat(tagsOne, CoreMatchers.hasItems("h1", "h2"));

        final List<String> tagsTwo = two.getTags();
        assertEquals(2, tagsTwo.size());
        assertThat(tagsTwo, CoreMatchers.hasItems("h1", "h2"));
    }

    @Test
    public void testVisitorsRunBeforeFilter() throws Exception {
        final HtmlProcessorConfig htmlProcessorConfig = new HtmlProcessorConfig();
        htmlProcessorConfig.setFilter(true);
        htmlProcessorConfig.setWhitelistElements(Arrays.asList(Element.create("h1"), Element.create("h2")));
        processor = new HtmlProcessorImpl(htmlProcessorConfig);

        final TagNameCollector one = new TagNameCollector();

        final List<TagVisitor> writeVisitors = Collections.singletonList(one);
        final String html = processor.write("<h1>Heading 1</h1><h2>Heading 2</h2><script>alert(\"xss\")</script>",
                                            writeVisitors);
        assertEquals("<h1>Heading 1</h1><h2>Heading 2</h2>", html);

        final List<String> tagsOne = one.getTags();
        assertEquals(3, tagsOne.size());
        assertThat(tagsOne, CoreMatchers.hasItems("h1", "h2", "script"));
    }

    private void testPreserved(final String html) throws IOException {
        final HtmlProcessorConfig config = new HtmlProcessorConfig();
        config.setConvertLineEndings(false);
        processor = new HtmlProcessorImpl(config);
        final String processedHtml = processor.write(html, null);
        assertEquals(html, processor.read(processedHtml, null));
    }

    private void assertNoChanges(final String text) throws RepositoryException, IOException {
        final long sizeBefore = document.getNodes().getSize();

        assertEquals("Stored text should be returned without changes",
                     emptyIfNull(text), processor.read(text, null));

        assertEquals("Text should be stored without changes",
                     emptyIfNull(text), processor.write(text, null));

        assertEquals("Number of child facet nodes should not have changed",
                     sizeBefore, document.getNodes().getSize());
    }

    private static String emptyIfNull(final String text) {
        return text != null ? text : StringUtils.EMPTY;
    }

    private static class TagNameCollector implements TagVisitor {

        private final List<String> tags = new ArrayList<>();

        List<String> getTags() {
            return tags;
        }

        @Override
        public void onRead(final Tag parent, final Tag tag) throws RepositoryException {
            if (tag != null) {
                final String name = tag.getName();
                if (name != null) {
                    tags.add(name);
                }
            }
        }

        @Override
        public void onWrite(final Tag parent, final Tag tag) throws RepositoryException {
            if (tag != null) {
                final String name = tag.getName();
                if (name != null) {
                    tags.add(name);
                }
            }
        }

        @Override
        public void before() {
        }

        @Override
        public void after() {
        }

        @Override
        public void release() {
        }
    }

}
