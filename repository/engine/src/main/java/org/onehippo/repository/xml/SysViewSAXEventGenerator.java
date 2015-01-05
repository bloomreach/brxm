/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository.xml;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Collection;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.value.ValueHelper;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import static javax.jcr.PropertyType.BINARY;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_NAME;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_PROPERTY;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_TYPE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_VALUE;

/**
 * A <code>SysViewSAXEventGenerator</code> instance can be used to generate SAX events
 * representing the serialized form of an item in System View XML.
 */
public class SysViewSAXEventGenerator extends AbstractSAXEventGenerator {

    public static final String CDATA_TYPE = "CDATA";
    public static final String ENUMERATION_TYPE = "ENUMERATION";

    private static final String NS_XMLSCHEMA_INSTANCE_URI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String NS_XMLSCHEMA_INSTANCE_PREFIX = "xsi";
    private static final String NS_XMLSCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
    private static final String NS_XMLSCHEMA_PREFIX = "xs";
    private static final String NS_XMLIMPORT_URI = "http://www.onehippo.org/jcr/xmlimport";
    private static final String NS_XMLIMPORT_PREFIX = "h";
    
    private static final Name SV_MULTIPLE = NameFactoryImpl.getInstance().create(Name.NS_SV_URI, "multiple");

    private static final Attributes ATTRS_EMPTY = new AttributesImpl();
    private static final Attributes ATTRS_BINARY_ENCODED_VALUE;
    static {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(Name.NS_XMLNS_URI, NS_XMLSCHEMA_INSTANCE_PREFIX, "xmlns:" + NS_XMLSCHEMA_INSTANCE_PREFIX, CDATA_TYPE, NS_XMLSCHEMA_INSTANCE_URI);
        attrs.addAttribute(Name.NS_XMLNS_URI, NS_XMLSCHEMA_PREFIX, "xmlns:" + NS_XMLSCHEMA_PREFIX, CDATA_TYPE, NS_XMLSCHEMA_URI);
        attrs.addAttribute(NS_XMLSCHEMA_INSTANCE_URI, "type", NS_XMLSCHEMA_INSTANCE_PREFIX + ":type", "CDATA", NS_XMLSCHEMA_PREFIX + ":base64Binary");
        ATTRS_BINARY_ENCODED_VALUE = attrs;
    }

    private final NameResolver resolver;
    private Collection<File> binaries;

    public SysViewSAXEventGenerator(Node node, boolean noRecurse,
                                    boolean skipBinary,
                                    ContentHandler contentHandler)
            throws RepositoryException {
        super(node, noRecurse, skipBinary, contentHandler);
        resolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
    }

    public SysViewSAXEventGenerator(Node node, boolean noRecurse, ContentHandler handler, Collection<File> binaries) throws RepositoryException {
        this(node, noRecurse, false, handler);
        this.binaries = binaries;
    }

    @Override
    protected void entering(Node node, int level) throws RepositoryException, SAXException {
        final AttributesImpl attrs = new AttributesImpl();

        String nodeName;
        if (node.getDepth() == 0) {
            nodeName = jcrRoot;
        } else {
            nodeName = node.getName();
        }

        addAttribute(attrs, SV_NAME, CDATA_TYPE, nodeName);
        startElement(NameConstants.SV_NODE, attrs);
    }

    @Override
    protected void enteringProperties(Node node, int level) throws RepositoryException, SAXException {
    }

    @Override
    protected void leavingProperties(Node node, int level) throws RepositoryException, SAXException {
    }

    @Override
    protected void leaving(Node node, int level) throws RepositoryException, SAXException {
        endElement(NameConstants.SV_NODE);
    }

    @Override
    protected void entering(Property prop, int level) throws RepositoryException, SAXException {

        final AttributesImpl attrs = new AttributesImpl();
        addAttribute(attrs, SV_NAME, CDATA_TYPE, prop.getName());
        final String typeName = PropertyType.nameFromValue(prop.getType());
        addAttribute(attrs, SV_TYPE, ENUMERATION_TYPE, typeName);
        if (prop.isMultiple()) {
            addAttribute(attrs, SV_MULTIPLE, CDATA_TYPE, String.valueOf(true));
        }

        startElement(SV_PROPERTY, attrs);

        if (prop.getType() == BINARY && skipBinary) {
            startElement(SV_VALUE, new AttributesImpl());
            endElement(SV_VALUE);
        } else {
            Value[] vals = getValues(prop);
            for (final Value val : vals) {
                exportValue(val);
            }
        }
    }

    protected Value[] getValues(final Property prop) throws RepositoryException {
        if (prop.getDefinition().isMultiple()) {
            return prop.getValues();
        } else {
            return new Value[] { prop.getValue() };
        }
    }

    private void exportValue(final Value val) throws RepositoryException, SAXException {

        if (val.getType() == BINARY && binaries != null) {
            File file = createBinaryFile(val);
            binaries.add(file);
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(NS_XMLIMPORT_URI, "file", "h:file", CDATA_TYPE, file.getName());
            contentHandler.startPrefixMapping(NS_XMLIMPORT_PREFIX, NS_XMLIMPORT_URI);
            startElement(SV_VALUE, attributes);
            endElement(SV_VALUE);
            contentHandler.endPrefixMapping(NS_XMLIMPORT_PREFIX);
        } else {
            Attributes attributes = ATTRS_EMPTY;
            final boolean mustSendBinary = mustSendBinary(val);
            if (mustSendBinary) {
                contentHandler.startPrefixMapping(NS_XMLSCHEMA_INSTANCE_PREFIX, NS_XMLSCHEMA_INSTANCE_URI);
                contentHandler.startPrefixMapping(NS_XMLSCHEMA_PREFIX, NS_XMLSCHEMA_URI);
                attributes = ATTRS_BINARY_ENCODED_VALUE;
            }
            startElement(SV_VALUE, attributes);

            try {
                ValueHelper.serialize(val, false, mustSendBinary, new ContentHandlerWriter(contentHandler));
            } catch (IOException ioe) {
                Throwable t = ioe.getCause();
                if (t != null && t instanceof SAXException) {
                    throw (SAXException) t;
                } else {
                    throw new SAXException(ioe);
                }
            }

            endElement(SV_VALUE);

            if (mustSendBinary) {
                contentHandler.endPrefixMapping(NS_XMLSCHEMA_INSTANCE_PREFIX);
                contentHandler.endPrefixMapping(NS_XMLSCHEMA_PREFIX);
            }
        }

    }

    private boolean mustSendBinary(final Value val) throws RepositoryException {
        if (val.getType() != BINARY) {
            final String ser = val.getString();
            for (int i = 0; i < ser.length(); i++) {
                char c = ser.charAt(i);
                if (c >= 0 && c < 32 && c != '\r' && c != '\n' && c != '\t') {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void leaving(Property prop, int level)
            throws RepositoryException, SAXException {
        endElement(SV_PROPERTY);
    }


    protected void addAttribute(AttributesImpl attributes, Name name, String type, String value)
            throws NamespaceException {
        attributes.addAttribute(name.getNamespaceURI(), name.getLocalName(), resolver.getJCRName(name), type, value);
    }

    protected void startElement(Name name, Attributes attributes) throws NamespaceException, SAXException {
        contentHandler.startElement(name.getNamespaceURI(), name.getLocalName(), resolver.getJCRName(name), attributes);
    }

    protected void endElement(Name name) throws NamespaceException, SAXException {
        contentHandler.endElement(name.getNamespaceURI(), name.getLocalName(), resolver.getJCRName(name));
    }

    private File createBinaryFile(final Value value) throws SAXException, RepositoryException {
        try {
            final File file = File.createTempFile("binary", ".bin");
            final InputStream in = value.getBinary().getStream();
            final FileOutputStream out = new FileOutputStream(file);
            try {
                IOUtils.copy(in, out);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }
            return file;
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    protected static class ContentHandlerWriter extends Writer {
        private final ContentHandler handler;

        public ContentHandlerWriter(final ContentHandler handler) {
            this.handler = handler;
        }

        @Override
        public void close() {}

        @Override
        public void flush() {}

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            try {
                handler.characters(cbuf, off, len);
            } catch (SAXException se) {
                throw new IOException(se.toString());
            }
        }
    }

}
