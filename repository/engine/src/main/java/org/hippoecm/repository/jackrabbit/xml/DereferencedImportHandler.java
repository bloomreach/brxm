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
package org.hippoecm.repository.jackrabbit.xml;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.spi.Name;
import org.onehippo.repository.api.ContentResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DereferencedImportHandler extends ImportHandler {


    private static Logger log = LoggerFactory.getLogger(DereferencedImportHandler.class);

    private final Importer importer;
    private final NamespaceRegistryImpl nsReg;
    private final SessionImpl session;
    private final ContentResourceLoader contentResourceLoader;
    private Map<String, String> localNamespaceMappings;
    private DereferencedSysViewImportHandler targetHandler;

    public DereferencedImportHandler(Importer importer, SessionImpl session, NamespaceRegistryImpl nsReg, final ContentResourceLoader contentResourceLoader) throws RepositoryException {
        super(importer, session);
        this.importer = importer;
        this.nsReg = nsReg;
        this.session = session;
        this.contentResourceLoader = contentResourceLoader;
    }

    //---------------------------------------------------------< ErrorHandler >
    /**
     * {@inheritDoc}
     */
    public void warning(SAXParseException e) throws SAXException {
        // log exception and carry on...
        log.warn("warning encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream", e);
    }

    /**
     * {@inheritDoc}
     */
    public void error(SAXParseException e) throws SAXException {
        // log exception and carry on...
        log.error("error encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream: " + e.toString());
    }

    /**
     * {@inheritDoc}
     */
    public void fatalError(SAXParseException e) throws SAXException {
        // log and re-throw exception
        log.error("fatal error encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream: " + e.toString());
        throw e;
    }
    //-------------------------------------------------------< ContentHandler >
    /**
     * {@inheritDoc}
     */
    public void startDocument() throws SAXException {
        targetHandler = null;

        try {
            localNamespaceMappings = new HashMap<String, String>();
            String[] uris = nsReg.getURIs();
            for (int i = 0; i < uris.length; i++) {
                localNamespaceMappings.put(session.getPrefix(uris[i]), uris[i]);
            }
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void endDocument() throws SAXException {
        // delegate to target handler
        targetHandler.endDocument();
    }

    /**
     * Records the given namespace mapping to be included in the local
     * namespace context. The local namespace context is instantiated
     * in {@link #startElement(String, String, String, Attributes)} using
     * all the the namespace mappings recorded for the current XML element.
     * <p>
     *
     * @param prefix namespace prefix
     * @param uri namespace URI
     */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
    }

    /**
     * {@inheritDoc}
     */
    public void endPrefixMapping(String prefix) throws SAXException {
        /**
         * nothing to do here as namespace context has already been popped
         * in endElement event
         */
    }

    /**
     * {@inheritDoc}
     */
    public void startElement(String namespaceURI, String localName, String qName,
                             Attributes atts) throws SAXException {
        if (targetHandler == null) {
            // the namespace of the first element determines the type of XML
            // (system view/document view)
            if (Name.NS_SV_URI.equals(namespaceURI)) {
                targetHandler = new DereferencedSysViewImportHandler(importer, contentResourceLoader, session.getValueFactory());
            } else {
                throw new SAXException("Only sys views are supported, unknow: " + namespaceURI);
            }

            targetHandler.startDocument();
        }

        // Start a namespace context for this element
        targetHandler.startNamespaceContext(localNamespaceMappings);
        localNamespaceMappings.clear();

        // delegate to target handler
        targetHandler.startElement(namespaceURI, localName, qName, atts);
    }

    /**
     * {@inheritDoc}
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        // delegate to target handler
        targetHandler.characters(ch, start, length);
    }

    /**
     * Delegates the call to the underlying target handler and asks the
     * handler to end the current namespace context.
     * {@inheritDoc}
     */
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        targetHandler.endElement(namespaceURI, localName, qName);
        targetHandler.endNamespaceContext();
    }

    /**
     * {@inheritDoc}
     */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

}
