/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.console.menu.recompute;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.iterator.NodeIterable;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.repository.api.HippoNode;


public class RecomputeVisitor implements ItemVisitor {

    private static final int BATCH_SIZE = 50;

    private int counter = 0;
    private boolean batchSave;

    RecomputeVisitor(boolean batchSave) {
        this.batchSave = batchSave;
    }

    @Override
    public void visit(final Property property) throws RepositoryException {
    }

    @Override
    public void visit(final Node node) throws RepositoryException {
        if (!JcrHelper.isVirtualNode(node)) {
            if (node instanceof HippoNode && node.isNodeType("hippo:derived")) {
                if (((HippoNode) node).recomputeDerivedData()) {
                    counter++;
                    saveIfNeeded(node.getSession());
                }
            }
            for (Node child : new NodeIterable(node.getNodes())) {
                visit(child);
            }
        }
    }

    private void saveIfNeeded(Session session) throws RepositoryException {
        boolean shouldSave = batchSave && counter % BATCH_SIZE == 0;
        if (shouldSave) {
            session.save();
        }
    }

}
