/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.utilities.xml;


import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import static junit.framework.Assert.assertTrue;

public class SystemViewFilterTest {

    @Test
    public void testSystemViewFilterBuildsIndexedPath() throws Exception {
        final InputSource inputSource = new InputSource(getClass().getClassLoader().getResourceAsStream("sv.xml"));
        final TestHandler handler = new TestHandler(new DefaultHandler(), "/");
        final XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(handler);
        reader.parse(inputSource);
        assertTrue(handler.checkedNodePaths.contains("/test"));
        assertTrue(handler.checkedNodePaths.contains("/test/foo[2]"));
    }

    private static class TestHandler extends SystemViewFilter {

        private List<String> checkedNodePaths = new ArrayList<String>();

        public TestHandler(final ContentHandler handler, final String rootPath) {
            super(handler, rootPath);
        }

        @Override
        protected boolean shouldFilterNode(final String path, final String name) throws SAXException {
            checkedNodePaths.add(path);
            return false;
        }
    }
}
