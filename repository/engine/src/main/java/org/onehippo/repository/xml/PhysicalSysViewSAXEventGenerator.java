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
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_COUNT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETSEARCH;

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
        if (skip(node)) {
            return;
        }
        super.process(node, level);
    }

    @Override
    protected void process(Property prop, int level) throws RepositoryException, SAXException {
        if (skip(prop)) {
            return;
        }
        super.process(prop, level);
    }

    protected boolean skip(final Node node) throws RepositoryException {
        return JcrUtils.isVirtual(node);
    }

    protected boolean skip(final Property prop) throws RepositoryException {
        final String primaryNodeTypeName = prop.getParent().getPrimaryNodeType().getName();
        if (primaryNodeTypeName.equals(NT_FACETSEARCH) && HIPPO_COUNT.equals(prop.getName())) {
            return true;
        }
        if (prop.getName().equals(HIPPO_PATHS)) {
            return true;
        }
        return false;
    }
}
