/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.jcr.PropertyType;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static org.onehippo.cm.migration.Esv2Yaml.log;

public class EsvParser extends DefaultHandler {

    private static final String NS_SV_URI = "http://www.jcp.org/jcr/sv/1.0";
    private static final String NS_SV_PREFIX = "sv";
    private static final String NS_ESV_URI = "http://www.onehippo.org/jcr/xmlimport";
    private static final String NS_ESV_PREFIX = "esv";

    private static final String SV_NODE = "node";
    private static final String SV_PROPERTY = "property";
    private static final String SV_VALUE = "value";
    private static final String SV_TYPE = "type";
    private static final String SV_NAME = "name";
    private static final String SV_MULTIPLE = "multiple";

    private static final String ESV_MERGE = "merge";
    private static final String ESV_LOCATION = "location";
    private static final String ESV_FILE = "file";

    private static final String JCR_PRIMARYTYPE = "jcr:primaryType";
    private static final String JCR_MIXINTYPES = "jcr:mixinTypes";
    private static final String JCR_UUID = "jcr:uuid";

    private static final Map<String, String> NS_MAP;
    static {
        HashMap<String, String> map = new HashMap<>();
        map.put(NS_SV_URI, NS_SV_PREFIX);
        map.put(NS_ESV_URI, NS_ESV_PREFIX);
        NS_MAP = Collections.unmodifiableMap(map);
    }

    private final File baseDir;
    private final Stack<EsvNode> stack = new Stack<>();

    private Locator locator;
    private String resourcePath;
    private EsvNode rootNode;
    private EsvProperty currentProperty;
    private EsvValue currentValue;
    private StringBuilder valueBuilder;

    public EsvParser(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator;
    }

    public EsvNode parse(InputStream is, String resourcePath) throws IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            factory.setValidating(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            SAXParser parser = factory.newSAXParser();
            this.resourcePath = resourcePath;
            parser.parse(new InputSource(is), this);
            return rootNode;
        } catch (FactoryConfigurationError e) {
            throw new RuntimeException(
                    "SAX parser implementation not available", e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("SAX parser configuration error", e);
        } catch (SAXException e) {
            Exception exception = e.getException();
            if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new IOException("Error parsing XML import", e);
            }
        }
        finally {
            stack.clear();
            rootNode = null;
            currentProperty = null;
            currentValue = null;
            valueBuilder = null;
        }
    }

    public void warning(SAXParseException e) throws SAXException {
        log.warn("warning encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream", e);
    }

    public void error(SAXParseException e) throws SAXException {
        log.error("error encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream: " + e.toString());
    }

    public void fatalError(SAXParseException e) throws SAXException {
        log.error("fatal error encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream: " + e.toString());
        throw e;
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (isName(namespaceURI, localName, NS_SV_PREFIX, SV_NODE)) {
            startNode(atts);
        } else if (isName(namespaceURI, localName, NS_SV_PREFIX, SV_PROPERTY)) {
            startProperty(atts);
        } else if (isName(namespaceURI, localName, NS_SV_PREFIX, SV_VALUE)) {
            startValue(atts);
        } else {
            throw new SAXException("Unknown element: " + qName + getParseLocation());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        appendValue(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        appendValue(ch, start, length);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (isName(namespaceURI, localName, NS_SV_PREFIX, SV_NODE)) {
            endNode();
        } else if (isName(namespaceURI, localName, NS_SV_PREFIX, SV_PROPERTY)) {
            endProperty();
        } else if (isName(namespaceURI, localName, NS_SV_PREFIX, SV_VALUE)) {
            endValue();
        } else {
            throw new SAXException("Unknown element: " + qName + getParseLocation());
        }
    }

    private void startNode(final Attributes atts) throws SAXException {
        if (currentValue != null) {
            throw new SAXException("Invalid sv:node element within sv:value element");
        }
        else if (currentProperty != null) {
            throw new SAXException("Invalid sv:node element within sv:property element");
        }
        String svName = getAttribute(atts, NS_SV_URI, SV_NAME);
        if (isEmpty(svName)) {
            throw new SAXException("Empty or missing mandatory sv:name attribute for sv:node element" + getParseLocation());
        }
        int index = -1;
        int offset = svName.indexOf('[');
        if (offset != -1) {
            index = Integer.valueOf(svName.substring(offset+1, svName.length()-1));
            svName = svName.substring(0, offset);
        }
        EsvNode newNode = new EsvNode(svName, index);
        String strMerge = getAttribute(atts, NS_ESV_URI, ESV_MERGE);
        if (strMerge != null) {
            if (isEmpty(strMerge)) {
                throw new SAXException("Empty esv:merge value for sv:node element" + getParseLocation());
            }
            EsvMerge esvMerge = EsvMerge.lookup(strMerge);
            if (esvMerge == null || !esvMerge.isForNode()) {
                throw new SAXException("Unknown or invalid esv:merge value: "+strMerge+" for sv:node element" + getParseLocation());
            }
            newNode.setMerge(esvMerge);
        }
        newNode.setMergeLocation(getAttribute(atts, NS_ESV_URI, ESV_LOCATION));

        if (rootNode == null) {
            rootNode = newNode;
        }
        else {
            getCurrentNode().getChildren().add(newNode);
        }
        stack.push(newNode);
    }

    private void startProperty(final Attributes atts) throws SAXException {
        if (currentValue != null) {
            throw new SAXException("Invalid sv:property element within sv:value element" + getParseLocation());
        }
        else if (currentProperty != null) {
            throw new SAXException("Invalid sv:property element within sv:property element" + getParseLocation());
        }
        final EsvNode node = getCurrentNode();
        if (node == null) {
            throw new SAXException("Invalid sv:property element outside sv:node element" + getParseLocation());
        }
        String svName = getAttribute(atts, NS_SV_URI, SV_NAME);
        if (isEmpty(svName)) {
            throw new SAXException("Empty or missing mandatory sv:name attribute for sv:property element" + getParseLocation());
        }
        String strType = getAttribute(atts, NS_SV_URI, SV_TYPE);
        if (isEmpty(strType)) {
            throw new SAXException("Empty or missing mandatory sv:type attribute for sv:property element" + getParseLocation());
        }
        int jcrType;
        try {
            jcrType = PropertyType.valueFromName(strType);
        } catch (IllegalArgumentException e) {
            throw new SAXException("Unsupported property sv:type: " + strType + getParseLocation());
        }
        currentProperty = new EsvProperty(svName, jcrType);
        String strMultiple = getAttribute(atts, NS_SV_URI, SV_MULTIPLE);
        if (!isEmpty(strMultiple)) {
            currentProperty.setMultiple(Boolean.valueOf(strMultiple));
        }
        String strMerge = getAttribute(atts, NS_ESV_URI, ESV_MERGE);
        if (strMerge != null) {
            if (isEmpty(strMerge)) {
                throw new SAXException("Empty esv:merge value for sv:property element" + getParseLocation());
            }
            EsvMerge esvMerge = EsvMerge.lookup(strMerge);
            if (esvMerge == null || !esvMerge.isForProperty()) {
                throw new SAXException("Unknown or invalid esv:merge value: "+strMerge+" for sv:property element" + getParseLocation());
            }
            currentProperty.setMerge(esvMerge);
        }
        currentProperty.setMergeLocation(getAttribute(atts, NS_ESV_URI, ESV_LOCATION));
    }

    private void startValue(final Attributes atts) throws SAXException {
        if (currentValue != null) {
            throw new SAXException("Invalid sv:value element within sv:value element" + getParseLocation());
        }
        else if (currentProperty == null) {
            throw new SAXException("Invalid sv:value element outside sv:property element" + getParseLocation());
        }
        String esvFile = getAttribute(atts, NS_ESV_URI, ESV_FILE);
        if (esvFile != null) {
            currentValue = new EsvValue(getPath(esvFile));
        }
        else {
            currentValue = new EsvValue("xs:base64Binary".equals(atts.getValue("xsi:type")));
        }
    }

    private void endNode() throws SAXException {
        stack.pop();
    }

    private void endProperty() throws SAXException {
        if (JCR_PRIMARYTYPE.equals(currentProperty.getName())) {
            setPrimaryType(getCurrentNode(), currentProperty);
        }
        else if (JCR_MIXINTYPES.equals(currentProperty.getName())) {
            setMixinTypes(getCurrentNode(), currentProperty);
        }
        else if (JCR_UUID.equals(currentProperty.getName())) {
            setUuid(getCurrentNode(), currentProperty);
        }
        else {
            getCurrentNode().getProperties().add(currentProperty);
        }
        currentProperty = null;
    }

    private void endValue() throws SAXException {
        if (!currentValue.isPath() && valueBuilder != null) {
            currentValue.setValue(valueBuilder.toString());
        }
        if (currentValue.getValue() != null) {
            currentProperty.getValues().add(currentValue);
        }
        currentValue = null;
        valueBuilder = null;
    }

    private void appendValue(final char[] ch, final int start, final int length) throws SAXException {
        if (currentValue != null) {
            if (valueBuilder == null) {
                valueBuilder = new StringBuilder();
            }
            valueBuilder.append(ch, start, length);
        }
    }

    private EsvNode getCurrentNode() {
        return stack.isEmpty() ? null : stack.peek();
    }

    private void setPrimaryType(EsvNode node, EsvProperty property) throws SAXException {
        if(!property.getValues().isEmpty()) {
            node.setType(property.getValues().get(0).getValue());
        } else {
            throw new SAXException("Missing value for property "+JCR_PRIMARYTYPE + getParseLocation());
        }
    }

    private void setMixinTypes(EsvNode node, EsvProperty property) throws SAXException {
        for (EsvValue value : property.getValues()) {
            node.getMixins().add(value.getValue());
        }
    }

    private void setUuid(EsvNode node, EsvProperty property) throws SAXException {
        if(!property.getValues().isEmpty()) {
            node.setUuid(property.getValues().get(0).getValue());
        } else {
            throw new SAXException("Missing value for property "+JCR_UUID + getParseLocation());
        }
    }

    private String getPath(String esvFile) throws SAXException {
        if (isEmpty(esvFile)) {
            throw new SAXException("Empty esv:file property for sv:value element" + getParseLocation());
        }
        final String path = esvFile.startsWith("/") ? esvFile.substring(1) : esvFile;
        final File file = new File(baseDir, path);
        if (!file.exists() || !file.isFile()) {
            throw new SAXException("File esv:file: "+esvFile+" not found or not a file" + getParseLocation());
        }
        return path;
    }

    private String getParseLocation() {
        return new StringBuilder(" at ")
                .append(resourcePath)
                .append(" (")
                .append(locator.getLineNumber())
                .append(", ")
                .append(locator.getColumnNumber())
                .append(")")
                .toString();
    }

    private static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    private static boolean isName(final String uri, final String localName, final String prefix, final String name) {
        return prefix.equals(NS_MAP.get(uri)) && name.equals(localName);
    }

    private static String getAttribute(final Attributes attr, final String uri, final String localName) {
        return attr.getValue(uri, localName);
    }
}
