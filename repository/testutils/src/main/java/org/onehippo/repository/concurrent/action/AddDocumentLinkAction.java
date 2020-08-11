/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.concurrent.action;

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.hippoecm.repository.api.HippoNodeIterator;

/**
 * Adds a facet select node to a document node.
 */
public class AddDocumentLinkAction extends Action {
    
    private Random random = new Random(System.currentTimeMillis());

    public AddDocumentLinkAction(ActionContext context) {
        super(context);
    }

    @Override
    public boolean canOperateOnNode(Node node) throws Exception {
        return node.isNodeType("hippostdpubwf:document");
    }

    @Override
    public boolean isWriteAction() {
        return true;
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        Node target = selectRandomHandleNode(node.getSession());
        if (target != null && target.isNodeType("mix:referenceable")) {
            if (!node.isCheckedOut()) {
                node.checkout();
            }
            String uuid = target.getIdentifier();
            String linkName = "link";
            do {
                linkName += random.nextInt(10);
            } while (node.hasNode(linkName));
            Node link = node.addNode(linkName, "hippo:facetselect");
            link.setProperty("hippo:facets", new Value[] {});
            link.setProperty("hippo:values", new Value[] {});
            link.setProperty("hippo:modes", new Value[] {});
            link.setProperty("hippo:docbase", uuid);
            node.getSession().save();
        }
        return node;
    }

    private Node selectRandomHandleNode(Session session) throws RepositoryException {
        HippoNodeIterator documents = findAllHandleNodes(session);
        Node target = null;
        if (documents.hasNext()) {
            int index = random.nextInt((int)documents.getTotalSize());
            if (index > 0) {
                documents.skip(index);
            }
            if (documents.hasNext()) {
                target = documents.nextNode();
            }
        }
        return target;
    }
    
    private HippoNodeIterator findAllHandleNodes(Session session) throws RepositoryException {
        // order by clause forces result.getSize() != -1 (we need the size)
        String stmt = "/jcr:root" + context.getDocumentBasePath() + "//element(*,hippo:handle) order by @jcr:score descending";
        Query query = session.getWorkspace().getQueryManager().createQuery(stmt, Query.XPATH);
        return (HippoNodeIterator) query.execute().getNodes();
    }

}
