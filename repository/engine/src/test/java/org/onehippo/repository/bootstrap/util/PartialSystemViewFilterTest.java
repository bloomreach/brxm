/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.bootstrap.util;

import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.onehippo.repository.bootstrap.util.PartialSystemViewFilter;
import org.onehippo.repository.xml.DefaultContentHandler;
import org.xml.sax.InputSource;

import static org.apache.commons.lang.StringUtils.deleteWhitespace;
import static org.junit.Assert.assertEquals;

public class PartialSystemViewFilterTest {

    @Test
    public void testPartialSystemViewFilter() throws Exception {
        final InputStream input = getClass().getResourceAsStream("PartialSystemViewFilterTest_input.xml");
        final String partialContent = getPartialContent(input, "foo/bar");
        final InputStream expected = getClass().getResourceAsStream("PartialSystemViewFilterTest_expected.xml");
        assertEquals(deleteWhitespace(IOUtils.toString(expected)), deleteWhitespace(partialContent));
    }

    public String getPartialContent(InputStream in, String startPath) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        SAXParser parser = factory.newSAXParser();

        StringWriter out = new StringWriter();
        TransformerHandler handler = ((SAXTransformerFactory)SAXTransformerFactory.newInstance()).newTransformerHandler();
        Transformer transformer = handler.getTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        handler.setResult(new StreamResult(out));

        parser.parse(new InputSource(in), new DefaultContentHandler(new PartialSystemViewFilter(handler, startPath)));
        return out.toString();
    }
}
