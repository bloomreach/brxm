/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.decorating;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

public abstract class DecoratorFactoryImpl implements DecoratorFactory {

    public DecoratorFactoryImpl() {
    }

    public Repository getRepositoryDecorator(Repository repository) {
        return new RepositoryDecorator(this, repository);
    }

    public abstract Session getSessionDecorator(Repository repository, Session session);

    public abstract Workspace getWorkspaceDecorator(Session session, Workspace workspace);

    protected abstract Node getBasicNodeDecorator(Session session, Node node);

    protected Item getBasicItemDecorator(Session session, Item item) {
        return new ItemDecorator(this, session, item);
    }

    public Node getNodeDecorator(Session session, Node node) {
        if (node instanceof Version) {
            return getVersionDecorator(session, (Version) node);
        } else if (node instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) node);
        } else {
            return getBasicNodeDecorator(session, node);
        }
    }

    public Property getPropertyDecorator(Session session, Property property) {
        return new PropertyDecorator(this, session, property);
    }

    public abstract Version getVersionDecorator(Session session, Version version);

    public abstract VersionHistory getVersionHistoryDecorator(Session session, VersionHistory versionHistory);

    public Item getItemDecorator(Session session, Item item) {
        if (item instanceof Version) {
            return getVersionDecorator(session, (Version) item);
        } else if (item instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) item);
        } else if (item instanceof Node) {
            return getNodeDecorator(session, (Node) item);
        } else if (item instanceof Property) {
            return getPropertyDecorator(session, (Property) item);
        } else {
            return getBasicItemDecorator(session, item);
        }
    }

    public QueryManager getQueryManagerDecorator(Session session,
                                                 QueryManager queryManager) {
        return new QueryManagerDecorator(this, session, queryManager);
    }

    public abstract Query getQueryDecorator(Session session, Query query);

    public Query getQueryDecorator(Session session, Query query, Node node) {
        return getQueryDecorator(session, query);
    }

    public QueryResult getQueryResultDecorator(Session session,
                                               QueryResult result) {
        return new QueryResultDecorator(this, session, result);
    }

    public ValueFactory getValueFactoryDecorator(Session session,
                                                 ValueFactory valueFactory) {
        return new ValueFactoryDecorator(this, session, valueFactory);
    }

    public ItemVisitor getItemVisitorDecorator(Session session,
                                               ItemVisitor visitor) {
        return new ItemVisitorDecorator(this, session, visitor);
    }

    @Override
    public LockManager getLockManagerDecorator(Session session, LockManager lockManager) {
        return new LockManagerDecorator(session, lockManager);
    }
}
