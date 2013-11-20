/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.repository.upgrade;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.update.BaseNodeUpdateVisitor;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.LoggerFactory;

public abstract class BaseContentUpdateVisitor extends BaseNodeUpdateVisitor {

    protected Session session;

    @Override
    public void initialize(final Session session) throws RepositoryException {
        this.session = session;
        session.getWorkspace().getObservationManager().setUserData(HippoNodeType.HIPPO_IGNORABLE);
        setLogger(LoggerFactory.getLogger(getClass()));
    }

    @Override
    public boolean undoUpdate(final Node node) throws RepositoryException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy() {
    }


    protected void removeMixin(final Node node, final String mixin) throws RepositoryException {
        if (node.isNodeType(mixin)) {
            JcrUtils.ensureIsCheckedOut(node, true);
            final List<Reference> references = removeReferences(node);
            try {
                node.removeMixin(mixin);
                JcrUtils.ensureIsCheckedOut(node, true);
                node.addMixin(JcrConstants.MIX_REFERENCEABLE);
            } finally {
                restoreReferences(references);
            }
        }
    }


    protected void restoreReferences(final List<Reference> references) throws RepositoryException {
        for (Reference reference : references) {
            Node node = reference.getNode();
            String property = reference.getPropertyName();
            if (reference.getValue() != null) {
                node.setProperty(property, reference.getValue());
            } else {
                node.setProperty(property, reference.getValues());
            }
        }
    }

    protected List<Reference> removeReferences(final Node handle) throws RepositoryException {
        final List<Reference> references = new LinkedList<>();
        for (Property property : new PropertyIterable(handle.getReferences())) {
            final Node node = property.getParent();
            JcrUtils.ensureIsCheckedOut(node, true);
            final String propertyName = property.getName();
            if (!HippoNodeType.HIPPO_RELATED.equals(propertyName)) {
                references.add(new Reference(property));
            }
            property.remove();
        }
        return references;
    }

    static class Reference {
        private final Node node;
        private final String propertyName;
        private final Value value;
        private final Value[] values;

        Reference(Property property) throws RepositoryException {
            this.node = property.getParent();
            this.propertyName = property.getName();
            if (property.isMultiple()) {
                this.value = property.getValue();
                this.values = null;
            } else {
                this.value = null;
                this.values = property.getValues();
            }
        }

        Node getNode() {
            return node;
        }

        String getPropertyName() {
            return propertyName;
        }

        Value getValue() {
            return value;
        }

        Value[] getValues() {
            return values;
        }
    }

}
