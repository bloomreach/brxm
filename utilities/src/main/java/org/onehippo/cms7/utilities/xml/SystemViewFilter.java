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
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Base class for SAX content handlers that implement a JCR system view xml filter.
 */
public class SystemViewFilter extends ProxyContentHandler {

    protected static final String NODE = "node";
    protected static final String PROPERTY = "property";
    protected static final String NAME = "name";
    protected static final String SV_URI = "http://www.jcp.org/jcr/sv/1.0";

    protected final Path path;

    private boolean skip = false;
    private String context = null;

    public SystemViewFilter(final ContentHandler handler, final String rootPath) {
        super(handler);
        this.path = new Path(rootPath);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if ((localName.equals(NODE) || localName.equals(PROPERTY)) && uri.equals(SV_URI)) {
            String name = atts.getValue(SV_URI, NAME);
            path.push(name);
            if (!skip) {
                final String absPath = path.toString();
                skip = (localName.equals(NODE) && shouldFilterNode(absPath, name))
                        || (localName.equals(PROPERTY) && shouldFilterProperty(absPath, name));
                if (skip) {
                    startSkipping(uri, localName, qName, atts);
                    context = absPath;
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
                endSkipping(uri, localName, qName);
                return;
            }
            path.pop();
        }
        if (skip) {
            return;
        }
        handler.endElement(uri, localName, qName);
    }

    /**
     * Called from {@link #startElement(String, String, String, org.xml.sax.Attributes)} when it starts
     * skipping.
     */
    protected void startSkipping(final String uri, final String localName, final String qName, final Attributes atts)
            throws SAXException {
    }

    /**
     * Called from {@link #endElement(String, String, String)} when skipping ends.
     */
    protected void endSkipping(final String uri, final String localName, final String qName) throws SAXException {
    }

    /**
     * Override this method to signal which properties need to be filtered from the system view xml.
     *
     * @param path  the path of the property
     * @param name  the name of the property
     * @return  whether to exclude the property from the system view xml
     */
    protected boolean shouldFilterProperty(final String path, final String name) throws SAXException {
        return false;
    }

    /**
     * Override this method to signal which nodes need to be filtered from the system view xml.
     *
     * @param path  the path of the node
     * @param name  the name of the node
     * @return  whether to exclude the node from system view xml
     */
    protected boolean shouldFilterNode(final String path, final String name) throws SAXException {
        return false;
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


    private static final class Path {
        private final Stack<Node> stack = new Stack<Node>();
        private final String prefixPath;

        private String stringValue;

        private Path(String rootPath) {
            this.prefixPath = rootPath.equals("/") ? "" : rootPath;
        }

        void push(String element) {
            Node node = new Node(element);
            if (!stack.isEmpty()) {
                final Node parent = stack.peek();
                if (parent != null) {
                    parent.addChild(node);
                }
            }
            stack.push(node);
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
                Node parent = null;
                for (Node node : stack) {
                    sb.append("/").append(node.name);
                    if (parent != null) {
                        int index = 1;
                        for (Node child : parent.children) {
                            if (child == node) {
                                break;
                            } else if (node.name.equals(child.name)) {
                                index++;
                            }
                        }
                        if (index != 1) {
                            sb.append('[').append(index).append(']');
                        }
                    }
                    parent = node;
                }
                stringValue = prefixPath + sb.toString();
            }
            return stringValue;
        }

    }

    private static final class Node {
        private final String name;
        private List<Node> children;

        private Node(String name) {
            this.name = name;
        }

        private void addChild(final Node child) {
            if (children == null) {
                children = new ArrayList<Node>();
            }
            children.add(child);
        }
    }

}
