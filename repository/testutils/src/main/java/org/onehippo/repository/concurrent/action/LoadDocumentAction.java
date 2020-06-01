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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNode;

/**
 * Read all nodes under a document node.
 */
public class LoadDocumentAction extends Action {

    public LoadDocumentAction(final ActionContext context) {
        super(context);
    }

    @Override
    public boolean canOperateOnNode(Node node) throws Exception {
        return node.getParent().isNodeType("hippo:handle");
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        traverse(node);
        node.getSession().refresh(true);
        return node;
    }
    
    private void traverse(Node node) throws RepositoryException {
        if (isVirtual(node)) {
            return;
        }
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node child = nodes.nextNode();
            traverse(child);
        }
    }
    
    private boolean isVirtual(Node node) throws RepositoryException {
        return node instanceof HippoNode && !node.isSame(((HippoNode) node).getCanonicalNode());
    }

    @Override
    public double getWeight() {
        return 2.0;
    }

    @Override
    public boolean isWriteAction() {
        return false;
    }

}
