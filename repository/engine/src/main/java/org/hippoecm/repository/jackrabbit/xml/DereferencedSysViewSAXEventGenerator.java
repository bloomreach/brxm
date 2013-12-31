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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.value.ValueHelper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * SysViewSAXEventGenerator with the following changes:
 * - virtual nodes are not exported
 * - references are rewritten to paths
 * - some auto generated properties are dropped (hippo:path)
 *
 * Store references as: [MULTI_VALUE|SINGLE_VALUE]+REFERENCE_SEPARATOR+propname+REFERENCE_SEPARATOR+refpath
 */
public class DereferencedSysViewSAXEventGenerator extends PhysicalSysViewSAXEventGenerator {

    private static Logger log = LoggerFactory.getLogger(DereferencedSysViewSAXEventGenerator.class);

    /** shortcut for quick checks */
    private static final String JCR_PREFIX = "jcr:";

    /** use one factory */
    private static final NameFactory FACTORY = NameFactoryImpl.getInstance();

    /** base export path */
    private final String basePath;


    public DereferencedSysViewSAXEventGenerator(Node node, boolean noRecurse, boolean skipBinary,
            ContentHandler contentHandler) throws RepositoryException {
        super(node, noRecurse, skipBinary, contentHandler);
        // strip node name of base path
        basePath = node.getPath();
        log.info("Starting export of '" + basePath + "' noRecurse:" + noRecurse + " skipBinary:" + skipBinary);
    }

    public DereferencedSysViewSAXEventGenerator(Node node, boolean noRecurse,
                                                ContentHandler handler, Collection<File> binaries)
        throws RepositoryException {
        super(node, noRecurse, handler, binaries);
        basePath = node.getPath();
    }

    @Override
    protected void process(Property prop, int level) throws RepositoryException, SAXException {


        if (prop.getParent().getPrimaryNodeType().getName().equals(HippoNodeType.NT_FACETSEARCH)
                && HippoNodeType.HIPPO_COUNT.equals(prop.getName())) {
            // this is a virtual hippo:count property
            return;
        }

        if (isVersioningProperty(prop)) {
            // don't export version info
            return;
        }

        if (isLockProperty(prop)) {
            // don't export lock info
            return;
        }

        super.process(prop, level);
    }


    @Override
    protected void entering(Property prop, int level)
            throws RepositoryException, SAXException {

        if (prop.getType() == PropertyType.REFERENCE) {
            AttributesImpl attrs = new AttributesImpl();

            // name attribute -> hippo:pathreference_propname
            addAttribute(attrs, NameConstants.SV_NAME, CDATA_TYPE, prop.getName() + Reference.REFERENCE_SUFFIX);

            // type attribute
            try {
                addAttribute(attrs, NameConstants.SV_TYPE, ENUMERATION_TYPE, PropertyType.TYPENAME_STRING);
            } catch (IllegalArgumentException e) {
                // should never be getting here
                throw new RepositoryException(
                        "unexpected property-type ordinal: " + prop.getType(), e);
            }

            // start property element
            startElement(NameConstants.SV_PROPERTY, attrs);

            boolean multiValued = prop.getDefinition().isMultiple();
            Value[] vals;
            if (multiValued) {
                vals = prop.getValues();
            } else {
                vals = new Value[]{prop.getValue()};
            }

            Reference ref = new Reference(basePath, vals, prop.getName());
            Value[] derefVals = ref.getPathValues(session);

            for (int i = 0; i < derefVals.length; i++) {
                Value val = derefVals[i];

                Attributes attributes = new AttributesImpl();

                // start value element
                startElement(NameConstants.SV_VALUE, attributes);

                // characters
                Writer writer = new Writer() {
                    @Override
                    public void close() /*throws IOException*/ {
                    }

                    @Override
                    public void flush() /*throws IOException*/ {
                    }

                    @Override
                    public void write(char[] cbuf, int off, int len) throws IOException {
                        try {
                            contentHandler.characters(cbuf, off, len);
                        } catch (SAXException se) {
                            throw new IOException(se.toString());
                        }
                    }
                };
                try {
                    ValueHelper.serialize(val, false, false, writer);
                    // no need to close our Writer implementation
                    //writer.close();
                } catch (IOException ioe) {
                    // check if the exception wraps a SAXException
                    // (see Writer.write(char[], int, int) above)
                    Throwable t = ioe.getCause();
                    if (t != null && t instanceof SAXException) {
                        throw (SAXException) t;
                    } else {
                        throw new SAXException(ioe);
                    }
                }

                // end value element
                endElement(NameConstants.SV_VALUE);
            }
        } else {
            super.entering(prop, level);
        }
    }

    private boolean isVersioningProperty(Property prop) throws RepositoryException {
        // quick check
        if (prop.getName().startsWith(JCR_PREFIX)) {
            // full check
            Name propQName = NameParser.parse(prop.getName(), nsResolver, FACTORY);
            if (NameConstants.JCR_PREDECESSORS.equals(propQName)) {
                return true;
            }
            if (NameConstants.JCR_ISCHECKEDOUT.equals(propQName)) {
                return true;
            }
            if (NameConstants.JCR_BASEVERSION.equals(propQName)) {
                return true;
            }
            if (NameConstants.JCR_VERSIONHISTORY.equals(propQName)) {
                return true;
            }
            if (NameConstants.JCR_VERSIONLABELS.equals(propQName)) {
                return true;
            }
            if (NameConstants.JCR_SUCCESSORS.equals(propQName)) {
                return true;
            }
            if (NameConstants.JCR_CHILDVERSIONHISTORY.equals(propQName)) {
                return true;
            }
            if (NameConstants.JCR_ROOTVERSION.equals(propQName)) {
                return true;
            }
            if (NameConstants.JCR_VERSIONABLEUUID.equals(propQName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLockProperty(Property prop) throws RepositoryException{
        // quick check
        if (prop.getName().startsWith(JCR_PREFIX)) {
            // full check
            Name propQName = NameParser.parse(prop.getName(), nsResolver, FACTORY);
            if (NameConstants.JCR_LOCKOWNER.equals(propQName)) {
                return true;
            }
            if (NameConstants.JCR_LOCKISDEEP.equals(propQName)) {
                return true;
            }
        }
        return false;
    }

}
