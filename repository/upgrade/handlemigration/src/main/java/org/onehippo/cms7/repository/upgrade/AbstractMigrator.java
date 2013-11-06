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
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractMigrator {

    private int count;
    private long totalSize;
    protected final Session session;
    protected boolean cancelled = false;
    protected Logger log = LoggerFactory.getLogger(getClass());


    AbstractMigrator(final Session session) {
        this.session = session;
    }

    void init() throws RepositoryException {
        session.getWorkspace().getObservationManager().setUserData(HippoNodeType.HIPPO_IGNORABLE);
    }

    void cancel() {
        cancelled = true;
    }

    void shutdown() {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    void migrate() throws RepositoryException {
        log.debug("Running migration tool");

        HippoNodeIterator nodes = getNodes();
        totalSize = nodes.getTotalSize();

        if (totalSize <= 0) {
            log.debug("No " + getNodeType() + "s to migrate");
            return;
        }

        log.info("{} {}s to migrate", totalSize, getNodeType());

        long size = totalSize;
        while (size > 0) {
            if (cancelled) {
                break;
            }
            migrate(nodes);
            nodes = getNodes();
            size = nodes.getTotalSize();
        }
        log.info("Finished migrating {}s to new model", getNodeType());
    }

    private void migrate(final HippoNodeIterator nodes) {
        for (Node node : new NodeIterable(nodes)) {
            if (cancelled) {
                break;
            }
            try {
                log.debug("Migrating {}", node.getPath());
                migrate(node);
                count++;
                throttle();
                if (count % ((totalSize + 99) / 100) == 0) {
                    long progress = Math.round(100.0 * count / totalSize);
                    log.info("Progress: {} %", progress);
                }
                if (count % 100 == 0) {
                    log.info("Migrated {} nodes", count);
                    return;
                }
            } catch (RepositoryException e) {
                log.error("Failed to migrate " + JcrUtils.getNodePathQuietly(node), e);
            }
        }
    }

    private void throttle() {
        if (count % 10 == 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignore) {
            }
        } else {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignore) {
            }
        }
    }

    private HippoNodeIterator getNodes() throws RepositoryException {
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery("SELECT * FROM " + getNodeType() + " ORDER BY jcr:name", Query.SQL);
        return (HippoNodeIterator) query.execute().getNodes();
    }

    protected abstract String getNodeType();

    protected abstract void migrate(final Node node) throws RepositoryException;

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
