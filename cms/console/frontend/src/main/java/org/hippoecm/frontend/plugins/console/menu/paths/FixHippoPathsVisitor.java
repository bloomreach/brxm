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
package org.hippoecm.frontend.plugins.console.menu.paths;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.commons.iterator.NodeIterable;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.repository.util.JcrUtils;


public class FixHippoPathsVisitor implements ItemVisitor {

    private static final int BATCH_SIZE = 50;

    private int counter = 0;
    private boolean batchSave;

    FixHippoPathsVisitor(boolean batchSave) {
        this.batchSave = batchSave;
    }

    @Override
    public void visit(final Property property) throws RepositoryException {
    }

    @Override
    public void visit(final Node node) throws RepositoryException {
        if (!JcrHelper.isVirtualNode(node)) {
            final Property hippoPathsProperty = JcrUtils.getPropertyIfExists(node, "hippo:paths");
            if (hippoPathsProperty != null) {
                ensureCheckedOut(node);
                fixHippoPathsProperty(hippoPathsProperty);
                saveIfNeeded(node.getSession());
            }
            for (Node child : new NodeIterable(node.getNodes())) {
                visit(child);
            }
        }
    }

    private void fixHippoPathsProperty(Property hippoPathsProperty) throws RepositoryException {
        hippoPathsProperty.remove();
        counter++;
    }

    private void saveIfNeeded(Session session) throws RepositoryException {
        boolean shouldSave = batchSave && counter % BATCH_SIZE == 0;
        if (shouldSave) {
            session.save();
        }
    }

    private void ensureCheckedOut(final Node node) throws RepositoryException {
        if (!node.isCheckedOut()) {
            Node checkoutCandidate = node;
            while (checkoutCandidate != null && !checkoutCandidate.isNodeType("mix:versionable")) {
                checkoutCandidate = checkoutCandidate.getParent();
            }
            checkout(checkoutCandidate);
        }
    }

    private void checkout(Node node) throws RepositoryException {
        final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        versionManager.checkout(node.getPath());
    }

}
