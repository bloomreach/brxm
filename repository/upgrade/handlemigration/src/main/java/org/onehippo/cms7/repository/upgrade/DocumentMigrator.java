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
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.util.JcrConstants;

/**
 * Queries for all nodes of type hippo:harddocument and for each node found:
 * - replaces hippo:harddocument mixin with mix:referenceable
 */
class DocumentMigrator extends AbstractMigrator {

    DocumentMigrator(final Session session) {
        super(session);
    }

    @Override
    protected void migrate(final Node node) throws RepositoryException {
        log.debug("Migrating {}", node.getPath());
        try {
            removeMixin(node, HippoNodeType.NT_HARDDOCUMENT);
            session.save();
        } finally {
            session.refresh(false);
        }
    }

    @Override
    protected HippoNodeIterator getNodes() throws RepositoryException {
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery("SELECT * FROM hippo:harddocument ORDER BY jcr:name", Query.SQL);
        return (HippoNodeIterator) query.execute().getNodes();
    }

}
