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

import java.util.Iterator;
import java.util.Stack;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.onehippo.cms7.utilities.xml.ProxyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_NAME;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_NODE;

/**
 * ProxyContentHandler that forwards SAX events only below a certain node path.
 */
public class PartialSystemViewFilter extends ProxyContentHandler {

    private final String startPath;
    private final Stack<String> currentPath = new Stack<>();
    private int depth = -1;

    public PartialSystemViewFilter(final ContentHandler handler, final String startPath) {
        super(handler);
        this.startPath = startPath;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final org.xml.sax.Attributes atts) throws SAXException {
        if (!skip()) {
            depth++;
            super.startElement(uri, localName, qName, atts);
            return;
        }
        final Name name = NameFactoryImpl.getInstance().create(uri, localName);
        if (name.equals(SV_NODE)) {
            String svName = atts.getValue(SV_NAME.getNamespaceURI(), SV_NAME.getLocalName());
            currentPath.push(svName);
            if (isStart()) {
                depth++;
                super.startElement(uri, localName, qName, atts);
            }
        }
    }

    private boolean skip() {
        return depth < 0;
    }

    private boolean isStart() {
        final StringBuilder sb = new StringBuilder();
        final Iterator<String> pathElements = currentPath.iterator();
        while (pathElements.hasNext()) {
            sb.append(pathElements.next());
            if (pathElements.hasNext()) {
                sb.append("/");
            }
        }
        return startPath.equals(sb.toString());
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if (!skip()) {
            super.endElement(uri, localName, qName);
            depth--;
            return;
        }
        Name name = NameFactoryImpl.getInstance().create(uri, localName);
        if (name.equals(SV_NODE)) {
            currentPath.pop();
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        if (!skip()) {
            super.characters(ch, start, length);
        }
    }
}
