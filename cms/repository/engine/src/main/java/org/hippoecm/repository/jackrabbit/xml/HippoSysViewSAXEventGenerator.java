/*
 *  Copyright 2008 Hippo.
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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.xml.SysViewSAXEventGenerator;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * SysViewSAXEventGenerator with the following changes:
 * - virtual nodes are not exported
 * - references are rewritten to paths
 * - some auto generated properties are dropped (hippo:path)
 */
public class HippoSysViewSAXEventGenerator extends SysViewSAXEventGenerator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    private static Logger log = LoggerFactory.getLogger(HippoSysViewSAXEventGenerator.class);
    
    /** '*' is not valid in property name, but can of course be used in value */
    private final static char REFERENCE_SEPARATOR = '*';
    
    /** shortcut for quick checks */
    private final static String JCR_PREFIX = "jcr:";
    
    /** use one factory */
    private static final NameFactory FACTORY = NameFactoryImpl.getInstance();

    public HippoSysViewSAXEventGenerator(Node node, boolean noRecurse, boolean skipBinary,
            ContentHandler contentHandler) throws RepositoryException {
        super(node, noRecurse, skipBinary, contentHandler);
    }

    @Override
    protected void process(Node node, int level) throws RepositoryException, SAXException {
        if (node instanceof HippoNode) {
            HippoNode hippoNode = (HippoNode) node;
            try {
                if (hippoNode.getCanonicalNode() == null) {
                    // virtual node
                    return;
                }
                if (!hippoNode.getCanonicalNode().isSame(hippoNode)) {
                    // virtual node
                    return;
                }
            } catch (ItemNotFoundException e) {
                // can happen only for virtual nodes while HREPTWO-599
                return;
            }

        }
        // if here, we have a physical node: continue
        super.process(node, level);
    }

    @Override
    protected void process(Property prop, int level) throws RepositoryException, SAXException {
        if (prop.getParent().getPrimaryNodeType().getName().equals(HippoNodeType.NT_FACETSEARCH)
                && HippoNodeType.HIPPO_COUNT.equals(prop.getName())) {
            // this is a virtual hippo:count property
            return;
        }
        
        if (HippoNodeType.HIPPO_PATHS.equals(prop.getName())) {
            // must be regenerated on import
            return;
        }
        
        if (isVersioningProperty(prop)) {
            // don't export version info
            return;
        }
        
        if (isLockProperty(prop)) {
            // don't export lock info
        }
        
        if (prop.getType() == PropertyType.REFERENCE) {
            
            // dereference and create a new property
            try {
                Node node = session.getNodeByUUID(prop.getString());
                Property pathRef = prop.getParent().setProperty(HippoNodeType.HIPPO_PATHREFERENCE, node.getName() + REFERENCE_SEPARATOR + node.getPath());
                super.process(pathRef, level);
                return;
            } catch (ItemNotFoundException e) {
                // inconsistent workspace??
                log.error("Referenced node '"+prop.getString()+"' not found for item: " + prop.getPath());
                return;
            } finally {
                if (prop.getParent().hasProperty(HippoNodeType.HIPPO_PATHREFERENCE)) {
                    prop.getParent().getProperty(HippoNodeType.HIPPO_PATHREFERENCE).remove();
                }
            }
        }
        
        super.process(prop, level);
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
