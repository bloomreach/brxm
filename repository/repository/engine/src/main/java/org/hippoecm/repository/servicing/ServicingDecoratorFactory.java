/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.servicing;

import java.util.WeakHashMap;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.core.XASession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicingDecoratorFactory
  implements DecoratorFactory
{
    private final Logger log = LoggerFactory.getLogger(ServicingDecoratorFactory.class);

    protected WeakHashMap<Repository,RepositoryDecorator> repositoryDecorators;
    protected WeakHashMap<Session,ServicingSessionImpl> sessionDecorators;
    protected WeakHashMap<Workspace,ServicingWorkspaceImpl> workspaceDecorators;

    public ServicingDecoratorFactory() {
        repositoryDecorators = new WeakHashMap<Repository,RepositoryDecorator>();
        sessionDecorators = new WeakHashMap<Session,ServicingSessionImpl>();
        workspaceDecorators = new WeakHashMap<Workspace,ServicingWorkspaceImpl>();
    }

    public Repository getRepositoryDecorator(Repository repository) {
        if(!repositoryDecorators.containsKey(repository)) {
            RepositoryDecorator wrapper = new RepositoryDecorator(this, repository);
            repositoryDecorators.put(repository, wrapper);
            return wrapper;
        } else
            return repositoryDecorators.get(repository);
    }
    public Session getSessionDecorator(Repository repository, Session session) {
        if(!sessionDecorators.containsKey(session)) {
            ServicingSessionImpl wrapper;
            if(session instanceof XASession) {
                try {
                    wrapper = new ServicingSessionImpl(this, repository, (XASession)session);
                    sessionDecorators.put(session, wrapper);
                    return wrapper;
                } catch(RepositoryException ex) {
                    log.error("cannot compose transactional session, reverting to regular session");
                    // fall through
                }
            }
            wrapper = new ServicingSessionImpl(this, repository, session);
            sessionDecorators.put(session, wrapper);
            return wrapper;
        } else
            return sessionDecorators.get(session);
    }
    public Workspace getWorkspaceDecorator(Session session, Workspace workspace) {
        if(!workspaceDecorators.containsKey(workspace)) {
            ServicingWorkspaceImpl wrapper = new ServicingWorkspaceImpl(this, session, workspace);
            workspaceDecorators.put(workspace, wrapper);
            return wrapper;
        } else
            return workspaceDecorators.get(workspace);
    }
    public Node getNodeDecorator(Session session, Node node, NodeView selection) {
        if (node instanceof Version) {
            return getVersionDecorator(session, (Version) node);
        } else if (node instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) node);
        } else {
            try {
                return new ServicingNodeImpl(this, session, node, node.getPath(), node.getDepth(), selection);
            } catch(RepositoryException ex) {
                log.error("cannot compose virtual node, reverting to regular node");
            }
            return new ServicingNodeImpl(this, session, node, selection);
        }
    }
    public Node getNodeDecorator(Session session, Node node, String path, int depth, NodeView selection) {
        if (node instanceof Version) {
            return getVersionDecorator(session, (Version) node, path, depth);
        } else if (node instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) node, path, depth);
        } else {
            try {
                return new ServicingNodeImpl(this, session, node, path, depth, selection);
            } catch(RepositoryException ex) {
                log.error("cannot compose virtual node, reverting to regular node");
            }
            return new ServicingNodeImpl(this, session, node, selection);
        }
    }
    public Property getPropertyDecorator(Session session, Property property) {
        return new PropertyDecorator(this, session, property);
    }
    public Lock getLockDecorator(Session session, Lock lock) {
        return new LockDecorator(this, session, lock);
    }
    public Version getVersionDecorator(Session session, Version version) {
        return new VersionDecorator(this, session, version);
    }
    public Version getVersionDecorator(Session session, Version version, String path, int depth) {
        return new VersionDecorator(this, session, version);
    }
    public VersionHistory getVersionHistoryDecorator(Session session,
                                                     VersionHistory versionHistory) {
        return new VersionHistoryDecorator(this, session, versionHistory);
    }
    public VersionHistory getVersionHistoryDecorator(Session session,
                                                     VersionHistory versionHistory,
                                                     String path, int depth) {
        try {
            return new VersionHistoryDecorator(this, session, versionHistory, path, depth);
        } catch(RepositoryException ex) {
            log.error("cannot compose virtual node, reverting to regular node");
            return new VersionHistoryDecorator(this, session, versionHistory);
        }
    }
    public Item getItemDecorator(Session session, Item item) {
        if (item instanceof Version) {
            return getVersionDecorator(session, (Version) item);
        } else if (item instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) item);
        } else if (item instanceof Node) {
            /* FIXME: this clears any NodeView we have in place, because we do
             * not have access to the current NodeView
             */
            return getNodeDecorator(session, (Node) item, null);
        } else if (item instanceof Property) {
            return getPropertyDecorator(session, (Property) item);
        } else {
            return new ItemDecorator(this, session, item);
        }
    }
    public QueryManager getQueryManagerDecorator(Session session,
                                                 QueryManager queryManager) {
        return new QueryManagerDecorator(this, session, queryManager);
    }
    public Query getQueryDecorator(Session session, Query query) {
        return new QueryDecorator(this, session, query);
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
}
