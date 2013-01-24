/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class ProxyContentHandler implements ContentHandler {

    protected final ContentHandler handler;


    public ProxyContentHandler(ContentHandler handler) {
        this.handler = handler;
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
        handler.setDocumentLocator(locator);
    }

    @Override
    public void startDocument() throws SAXException {
        handler.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        handler.endDocument();
    }

    @Override
    public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
        handler.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(final String prefix) throws SAXException {
        handler.endPrefixMapping(prefix);
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {
        handler.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        handler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        handler.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
        handler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(final String target, final String data) throws SAXException {
        handler.processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(final String name) throws SAXException {
        handler.skippedEntity(name);
    }
}
