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
import java.io.IOException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_NAME;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_TYPE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_VALUE;

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
    protected boolean skip(final Property prop) throws RepositoryException {
        if (isVersioningProperty(prop) || isLockProperty(prop)) {
            return true;
        }
        return super.skip(prop);
    }

    @Override
    protected void entering(Property prop, int level) throws RepositoryException, SAXException {

        if (prop.getType() == PropertyType.REFERENCE) {
            final AttributesImpl attrs = new AttributesImpl();
            addAttribute(attrs, SV_NAME, CDATA_TYPE, prop.getName() + Reference.REFERENCE_SUFFIX);
            addAttribute(attrs, SV_TYPE, ENUMERATION_TYPE, PropertyType.TYPENAME_STRING);

            startElement(NameConstants.SV_PROPERTY, attrs);

            Value[] vals = getValues(prop);

            Reference ref = new Reference(basePath, vals, prop.getName());
            Value[] derefVals = ref.getPathValues(session);

            for (Value val : derefVals) {
                startElement(SV_VALUE, new AttributesImpl());
                try {
                    ValueHelper.serialize(val, false, false, new ContentHandlerWriter(contentHandler));
                } catch (IOException ioe) {
                    Throwable t = ioe.getCause();
                    if (t != null && t instanceof SAXException) {
                        throw (SAXException) t;
                    } else {
                        throw new SAXException(ioe);
                    }
                }
                endElement(SV_VALUE);
            }
        } else {
            super.entering(prop, level);
        }
    }

    private boolean isVersioningProperty(Property prop) throws RepositoryException {
        if (prop.getName().startsWith(JCR_PREFIX)) {
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
        if (prop.getName().startsWith(JCR_PREFIX)) {
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
