/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.util.List;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import static org.onehippo.cms7.autoexport.Constants.NAME;
import static org.onehippo.cms7.autoexport.Constants.NODE;
import static org.onehippo.cms7.autoexport.Constants.PROPERTY;
import static org.onehippo.cms7.autoexport.Constants.SV_URI;

/**
 * Filters out all namespace declarations except {http://www.jcp.org/jcr/sv/1.0};
 * excludes all declared subcontexts from export, filters out uuid properties on declared paths,
 * and filters global exclusions
 */
final class FilterContentHandler implements ContentHandler {


    private final ContentHandler handler;
    private final Path path;
    private final List<String> subContextPaths;
    private final List<String> filterUuidPaths;
    private final ExclusionContext exclusionContext;
    private String svprefix;
    private boolean skip = false;
    private String context = null;

    FilterContentHandler(ContentHandler handler, String rootPath, List<String> subContextPaths, List<String> filterUuidPaths, ExclusionContext exclusionContext) {
        this.handler = handler;
        this.path = new Path(rootPath);
        this.subContextPaths = subContextPaths;
        this.filterUuidPaths = filterUuidPaths;
        this.exclusionContext = exclusionContext;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
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
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // only forward prefix mappings in the jcr/sv namespace
        if (uri.equals(SV_URI)) {
            svprefix = prefix;
            handler.startPrefixMapping(prefix, uri);
        }
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (prefix.equals(svprefix)) {
            handler.endPrefixMapping(prefix);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if ((localName.equals(NODE) || localName.equals(PROPERTY)) && uri.equals(SV_URI)) {
            String name = atts.getValue(SV_URI, NAME);
            path.push(name);
            if (!skip) {
                boolean isExcludedNode = localName.equals(NODE) && shouldFilterElement(path.toString());
                boolean isExcludedUuidProperty = localName.equals(PROPERTY) && name.equals("jcr:uuid") && isFilteredUuidPath(path.toString());
                if (isExcludedNode || isExcludedUuidProperty) {
                    skip = true;
                    context = path.toString();
                }
            }
        }
        if (skip) {
            return;
        }
        handler.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ((localName.equals(NODE) || localName.equals(PROPERTY)) && uri.equals(SV_URI)) {
            if (skip && context.equals(path.toString())) {
                context = null;
                skip = false;
                path.pop();
                return;
            }
            path.pop();
        }
        if (skip) {
            return;
        }
        handler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (skip) {
            return;
        }
        handler.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (skip) {
            return;
        }
        handler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        handler.processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        handler.skippedEntity(name);
    }
    
    private boolean shouldFilterElement(String path) {
        for (String subContextPath : subContextPaths) {
            if (path.equals(subContextPath)) {
                return true;
            }
        }
        return exclusionContext.isExcluded(path);
    }
    
    private boolean isFilteredUuidPath(String path) {
        for (String filterPath : filterUuidPaths) {
            if (path.startsWith(filterPath)) {
                return true;
            }
        }
        return false;
    }

    private static final class Path {
        private final Stack<String> stack = new Stack<String>();
        private final String prefixPath;

        private String stringValue;
        
        private Path(String rootPath) {
            this.prefixPath = rootPath.equals("/") ? "" : rootPath;
        }

        void push(String element) {
            stack.push(element);
            stringValue = null;
        }

        void pop() {
            stack.pop();
            stringValue = null;
        }

        @Override
        public String toString() {
            if (stringValue == null) {
                StringBuilder sb = new StringBuilder();
                for (String element : stack) {
                    sb.append("/").append(element);
                }
                stringValue = prefixPath + sb.toString();
            }
            return stringValue;
        }
    }
}