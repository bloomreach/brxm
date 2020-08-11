/*
 *  Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.xml.DocumentViewExporter;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.JcrUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Document view exporter that does not export virtual nodes.
 */
public class HippoDocumentViewExporter extends DocumentViewExporter {

    public HippoDocumentViewExporter(Session session, ContentHandler handler, boolean recurse, boolean binary) {
        super(session, handler, recurse, binary);
    }

    @Override
    protected void exportNodes(Node node) throws RepositoryException, SAXException {
        if (!JcrUtils.isVirtual(node)) {
            super.exportNodes(node);
        }
    }

}
