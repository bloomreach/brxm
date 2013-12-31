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
import java.util.Collection;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class PhysicalSysViewSAXEventGenerator extends SysViewSAXEventGenerator {

    public PhysicalSysViewSAXEventGenerator(Node node, boolean noRecurse, boolean skipBinary,
            ContentHandler contentHandler) throws RepositoryException {
        super(node, noRecurse, skipBinary, contentHandler);
    }

    public PhysicalSysViewSAXEventGenerator(Node node, boolean noRecurse,
                                            ContentHandler contentHandler,
                                            Collection<File> binaries)
            throws RepositoryException {
        super(node, noRecurse, contentHandler, binaries);
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
        if (prop.getName().equals(HippoNodeType.HIPPO_PATHS)) {
            return;
        }
        super.process(prop, level);
    }

}
