/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.PropInfo;
import org.apache.jackrabbit.core.xml.TextValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static org.onehippo.repository.xml.EnhancedSystemViewConstants.ENHANCED_IMPORT_URI;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.FILE;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.LOCATION;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.MERGE;

public class EnhancedSystemViewImportHandler extends DefaultHandler {
    
    private static final Name SV_MULTIPLE = NameFactoryImpl.getInstance().create(Name.NS_SV_URI, "multiple");

    private static Logger log = LoggerFactory.getLogger(EnhancedSystemViewImportHandler.class);

    /**
     * stack of ImportState instances; an instance is pushed onto the stack
     * in the startElement method every time a sv:node element is encountered;
     * the same instance is popped from the stack in the endElement method
     * when the corresponding sv:node element is encountered.
     */
    private final Stack<ImportState> stack = new Stack<ImportState>();

    /**
     * fields used temporarily while processing sv:property and sv:value elements
     */
    private Name currentPropName;
    private int currentPropType = PropertyType.UNDEFINED;
    private Boolean currentPropMultiple = null;
    private String currentMergeBehavior = null;
    private String currentMergeLocation = null;
    // list of AppendableValue objects
    private ArrayList<BufferedStringValue> currentPropValues = new ArrayList<BufferedStringValue>();
    private BufferedStringValue currentPropValue;
    private ArrayList<URL> currentBinaryPropValueURLs = new ArrayList<>();
    private URL currentBinaryPropValueURL;
    private final ContentResourceLoader contentResourceLoader;
    private final ValueFactory valueFactory;
    private final Importer importer;
    private InternalHippoSession resolver;

    public EnhancedSystemViewImportHandler(NodeImpl importTargetNode, ImportContext importContext, InternalHippoSession session) throws RepositoryException {
        this.importer = new EnhancedSystemViewImporter(importTargetNode, importContext, session);
        this.contentResourceLoader = importContext.getContentResourceLoader();
        this.valueFactory = session.getValueFactory();
        this.resolver = session;
    }

    //---------------------------------------------------------< ErrorHandler >

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

    private void processNode(ImportState state, boolean start, boolean end) throws SAXException {
        if (!start && !end) {
            return;
        }
        Name[] mixinNames = null;
        if (state.mixinNames != null) {
            mixinNames = state.mixinNames.toArray(new Name[state.mixinNames.size()]);
        }
        NodeId id = null;
        if (state.uuid != null) {
            id = NodeId.valueOf(state.uuid);
        }
        EnhancedNodeInfo node = new EnhancedNodeInfo(state.nodeName, state.nodeTypeName, mixinNames, id, state.mergeBehavior, state.location, state.index);
        // call Importer
        try {
            if (start) {
                importer.startNode(node, state.props);
                // dispose temporary property values
                for (org.apache.jackrabbit.core.xml.PropInfo pi : state.props) {
                    pi.dispose();
                }

            }
            if (end) {
                importer.endNode(node);
            }
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    //-------------------------------------------------------< ContentHandler >

    @Override
    public void startDocument() throws SAXException {
        try {
            importer.start();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            importer.end();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        Name name = NameFactoryImpl.getInstance().create(namespaceURI, localName);
        // check element name
        if (name.equals(NameConstants.SV_NODE)) {
            // sv:node element

            // node name (value of sv:name attribute)
            int index = 1;
            String svName = getAttribute(atts, NameConstants.SV_NAME);
            if (svName == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:node"));
            }

            int offset = svName.indexOf('[');
            if (offset != -1) {
                index = Integer.valueOf(svName.substring(offset+1, svName.length()-1));
                svName = svName.substring(0, offset);
            }

            if (!stack.isEmpty()) {
                // process current node first
                ImportState current = stack.peek();
                // need to start current node
                if (!current.started) {
                    processNode(current, true, false);
                    current.started = true;
                }
            }

            // push new ImportState instance onto the stack
            ImportState state = new ImportState();
            state.mergeBehavior = atts.getValue(ENHANCED_IMPORT_URI, MERGE);
            state.location = atts.getValue(ENHANCED_IMPORT_URI, LOCATION);
            try {
                state.nodeName = resolver.getQName(svName);
                state.index = index;
            } catch (NameException | NamespaceException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal node name: " + svName, e));
            }
            stack.push(state);
        } else if (name.equals(NameConstants.SV_PROPERTY)) {
            // sv:property element

            // reset temp fields
            currentPropValues.clear();

            // property name (value of sv:name attribute)
            String svName = getAttribute(atts, NameConstants.SV_NAME);
            if (svName == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:property"));
            }
            try {
                currentPropName = resolver.getQName(svName);
            } catch (NameException | NamespaceException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal property name: " + svName, e));
            }

            // property type (sv:type attribute)
            String type = getAttribute(atts, NameConstants.SV_TYPE);
            if (type == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:type attribute of element sv:property"));
            }
            
            // property multiple (sv:multiple attribute)
            String multiple = getAttribute(atts, SV_MULTIPLE);
            if (multiple == null || multiple.equals("")) {
                currentPropMultiple = null;
            } else {
                currentPropMultiple = Boolean.valueOf(multiple);
            }
            
            currentMergeBehavior = atts.getValue(ENHANCED_IMPORT_URI, MERGE);
            currentMergeLocation = atts.getValue(ENHANCED_IMPORT_URI, LOCATION);
            try {
                currentPropType = PropertyType.valueFromName(type);
            } catch (IllegalArgumentException e) {
                throw new SAXException(new InvalidSerializedDataException("Unknown property type: " + type, e));
            }
        } else if (name.equals(NameConstants.SV_VALUE)) {
            // sv:value element
            final String fileName = atts.getValue(ENHANCED_IMPORT_URI, FILE);
            if (fileName != null) {
                try {
                    currentBinaryPropValueURL = contentResourceLoader != null ? contentResourceLoader.getResource(fileName) : null;
                    if (currentBinaryPropValueURL == null) {
                        throw new SAXException("Missing file resource: " + fileName);
                    }
                } catch (MalformedURLException e) {
                    throw new SAXException("Malformed file resource path: " + fileName);
                }
            } else {
                currentPropValue = new BufferedStringValue(resolver, ValueFactoryImpl.getInstance());
            }
        } else {
            throw new SAXException(new InvalidSerializedDataException(
                    "Unexpected element in system view xml document: " + name));
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (currentPropValue != null) {
            // property value (character data of sv:value element)
            try {
                currentPropValue.append(ch, start, length);
            } catch (IOException ioe) {
                throw new SAXException("error while processing property value", ioe);
            }
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (currentPropValue != null) {
            // property value

            // data reported by the ignorableWhitespace event within
            // sv:value tags is considered part of the value
            try {
                currentPropValue.append(ch, start, length);
            } catch (IOException ioe) {
                throw new SAXException("error while processing property value", ioe);
            }
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        Name name = NameFactoryImpl.getInstance().create(namespaceURI, localName);
        // check element name
        ImportState state = stack.peek();
        if (name.equals(NameConstants.SV_NODE)) {
            // sv:node element
            if (!state.started) {
                // need to start & end current node
                processNode(state, true, true);
                state.started = true;
            } else {
                // need to end current node
                processNode(state, false, true);
            }
            // pop current state from stack
            stack.pop();
        } else if (name.equals(NameConstants.SV_PROPERTY)) {
            // sv:property element

            // check if all system properties (jcr:primaryType, jcr:uuid etc.)
            // have been collected and create node as necessary
            if (currentPropName.equals(NameConstants.JCR_PRIMARYTYPE)) {
                BufferedStringValue val = currentPropValues.get(0);
                String s = null;
                try {
                    s = val.retrieve();
                    state.nodeTypeName = resolver.getQName(s);
                } catch (IOException ioe) {
                    throw new SAXException("error while retrieving value", ioe);
                } catch (NameException | NamespaceException e) {
                    throw new SAXException(new InvalidSerializedDataException("illegal node type name: " + s, e));
                }
            } else if (currentPropName.equals(NameConstants.JCR_MIXINTYPES)) {
                if (state.mixinNames == null) {
                    state.mixinNames = new ArrayList<Name>(currentPropValues.size());
                }
                for (BufferedStringValue val : currentPropValues) {
                    String s = null;
                    try {
                        s = val.retrieve();
                        Name mixin = resolver.getQName(s);
                        state.mixinNames.add(mixin);
                    } catch (IOException ioe) {
                        throw new SAXException("error while retrieving value", ioe);
                    } catch (NameException | NamespaceException e) {
                        throw new SAXException(new InvalidSerializedDataException("illegal mixin type name: " + s, e));
                    }
                }
            } else if (currentPropName.equals(NameConstants.JCR_UUID)) {
                if(currentPropValues.size() > 0) {
                    BufferedStringValue val = currentPropValues.get(0);
                    try {
                        state.uuid = val.retrieve();
                    } catch (IOException ioe) {
                        throw new SAXException("error while retrieving value", ioe);
                    }
                }
            } else {
                EnhancedPropInfo prop = new EnhancedPropInfo(resolver, currentPropName, currentPropType, currentPropMultiple, currentPropValues
                        .toArray(new TextValue[currentPropValues.size()]), currentMergeBehavior, currentMergeLocation,
                        currentBinaryPropValueURLs.toArray(new URL[currentBinaryPropValueURLs.size()]), valueFactory);
                state.props.add(prop);
            }
            // reset temp fields
            currentPropValues.clear();
            currentBinaryPropValueURLs.clear();
        } else if (name.equals(NameConstants.SV_VALUE)) {
            // sv:value element
            if (currentPropValue != null) {
                currentPropValues.add(currentPropValue);
            }
            if (currentBinaryPropValueURL != null) {
                currentBinaryPropValueURLs.add(currentBinaryPropValueURL);
            }
            // reset temp fields
            currentPropValue = null;
            currentBinaryPropValueURL = null;
        } else {
            throw new SAXException(new InvalidSerializedDataException("invalid element in system view xml document: "
                    + localName));
        }
    }

    //--------------------------------------------------------< inner classes >

    class ImportState {

        Name nodeName;
        Name nodeTypeName;
        ArrayList<Name> mixinNames;
        String uuid;
        int index;
        List<PropInfo> props = new ArrayList<>();
        boolean started = false;
        String mergeBehavior;
        String location;
    }

    //-------------------------------------------------------------< private >

    /**
    * Returns the value of the named XML attribute.
    *
    * @param attributes set of XML attributes
    * @param name attribute name
    * @return attribute value,
    *         or <code>null</code> if the named attribute is not found
    */
    private static String getAttribute(Attributes attributes, Name name) {
        return attributes.getValue(name.getNamespaceURI(), name.getLocalName());
    }

}
