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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractMigrator {

    private int count;
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

        HippoNodeIterator hardHandles = getNodes();
        long size = hardHandles.getTotalSize();

        if (size <= 0) {
            log.debug("No nodes to migrate");
            return;
        }

        log.info("{} nodes to migrate", size);

        final long progressInterval = (size + 99) / 100;
        while (size > 0) {
            if (cancelled) {
                break;
            }
            migrate(hardHandles);
            if (count % progressInterval == 0) {
                long progress = Math.round(100.0 * count / size);
                log.info("Progress: {} %", progress);
            }
            hardHandles = getNodes();
            size = hardHandles.getTotalSize();
        }
        log.info("Finished migrating handles to new model");
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

    protected abstract void migrate(final Node node) throws RepositoryException;

    protected abstract HippoNodeIterator getNodes() throws RepositoryException;

}
