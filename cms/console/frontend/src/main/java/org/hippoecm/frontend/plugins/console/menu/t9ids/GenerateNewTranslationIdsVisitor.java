/*
 *  Copyright 2011-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.t9ids;

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.visitor.FilteringItemVisitor;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;

public class GenerateNewTranslationIdsVisitor extends FilteringItemVisitor {

    private String handleId;
    private String handleT9Id;

    public GenerateNewTranslationIdsVisitor() {
        setWalkProperties(false);
        setTraversalPredicate(IsNotVirtualPredicate.INSTANCE);
    }

    @Override
    protected void entering(final Property property, final int level) throws RepositoryException {
    }

    @Override
    protected void leaving(final Property property, final int level) throws RepositoryException {
    }

    @Override
    protected void entering(final Node node, final int level) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            handleId = node.getIdentifier();
            handleT9Id = UUID.randomUUID().toString();
        }

        if (node.hasProperty(HippoTranslationNodeType.ID)) {
            final String newTranslationId = createTranslationId(node);
            node.setProperty(HippoTranslationNodeType.ID, newTranslationId);
        }
    }

    private String createTranslationId(final Node node) throws RepositoryException {
        return parentIsHandle(node) ? handleT9Id : UUID.randomUUID().toString();
    }

    private boolean parentIsHandle(final Node node) throws RepositoryException {
        if (handleId != null) {
            final Node parent = node.getParent();
            return parent != null && parent.getIdentifier().equals(handleId);
        }
        return false;
    }

    @Override
    protected void leaving(final Node node, final int level) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            handleId = null;
            handleT9Id = null;
        }
    }
}

