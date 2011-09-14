/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.decorating.checked;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
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

import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;

public class CheckedDecoratorFactory implements DecoratorFactory {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    public CheckedDecoratorFactory() {
    }

    public Repository getRepositoryDecorator(Repository repository) {
        if(repository instanceof RepositoryDecorator)
            return repository;
        return new RepositoryDecorator(this, repository);
    }

    public Session getSessionDecorator(Repository repository, Session session, Credentials credentials, String workspace) {
        if(session instanceof SessionDecorator)
            return session;
        return new SessionDecorator(this, repository, (HippoSession) session, credentials, workspace);
    }

    public Workspace getWorkspaceDecorator(SessionDecorator session, Workspace workspace) {
        if(workspace instanceof WorkspaceDecorator)
            return workspace;
        return new WorkspaceDecorator(this, session, (HippoWorkspace) workspace);
    }

    protected Node getBasicNodeDecorator(SessionDecorator session, Node node) {
        if(node instanceof NodeDecorator)
            return node;
        return new NodeDecorator(this, session, (HippoNode) node);
    }

    public Node getNodeDecorator(SessionDecorator session, Node node) {
        if (node instanceof Version) {
            return getVersionDecorator(session, (Version) node);
        } else if (node instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) node);
        } else {
            return getBasicNodeDecorator(session, node);
        }
    }

    public Property getPropertyDecorator(SessionDecorator session, Property property) {
        if (property instanceof PropertyDecorator)
            return property;
        try {
            Node parent = session.getRootNode();
            try {
                parent = getNodeDecorator(session, property.getParent());
            } catch(ItemNotFoundException ex) {
            }
            return new PropertyDecorator(this, session, property, parent);
        } catch (RepositoryException ex) {
            return null;
        }
    }

    public Property getPropertyDecorator(SessionDecorator session, Property property, NodeDecorator parent) {
        if(property instanceof PropertyDecorator)
            return property;
        return new PropertyDecorator(this, session, property, parent);
    }

    public Lock getLockDecorator(SessionDecorator session, Lock lock) {
        if(lock instanceof LockDecorator)
            return lock;
        return new LockDecorator(this, session, lock);
    }

    public Version getVersionDecorator(SessionDecorator session, Version version) {
        if(version instanceof VersionDecorator)
            return version;
        return new VersionDecorator(this, session, version);
    }

    public VersionHistory getVersionHistoryDecorator(SessionDecorator session, VersionHistory versionHistory) {
        if(versionHistory instanceof VersionHistoryDecorator)
            return versionHistory;
        return new VersionHistoryDecorator(this, session, versionHistory);
    }

    public Item getItemDecorator(SessionDecorator session, Item item) {
        if (item instanceof Version) {
            return getVersionDecorator(session, (Version) item);
        } else if (item instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) item);
        } else if (item instanceof Node) {
            return getNodeDecorator(session, (Node) item);
        } else if (item instanceof Property) {
            return getPropertyDecorator(session, (Property) item);
        } else {
            return null;
        }
    }

    public QueryManager getQueryManagerDecorator(SessionDecorator session,
                                                 QueryManager queryManager) {
        if(queryManager instanceof QueryManagerDecorator)
            return queryManager;
        return new QueryManagerDecorator(this, session, queryManager);
    }

    public Query getQueryDecorator(SessionDecorator session, Query query) {
        if(query instanceof QueryDecorator)
            return query;
        return new QueryDecorator(this, session, (HippoQuery) query);
    }

    public Query getQueryDecorator(SessionDecorator session, Query query, Node node) {
        if(query instanceof QueryDecorator)
            return query;
        return new QueryDecorator(this, session, (HippoQuery) query);
    }

    public QueryResult getQueryResultDecorator(SessionDecorator session,
                                               QueryResult result) {
        if(result instanceof QueryResultDecorator)
            return result;
        return new QueryResultDecorator(this, session, result);
    }

    public ValueFactory getValueFactoryDecorator(SessionDecorator session,
                                                 ValueFactory valueFactory) {
        if(valueFactory instanceof ValueFactoryDecorator)
            return valueFactory;
        return new ValueFactoryDecorator(this, session, valueFactory);
    }

    public ItemVisitor getItemVisitorDecorator(SessionDecorator session,
                                               ItemVisitor visitor) {
        if(visitor instanceof ItemVisitorDecorator)
            return visitor;
        return new ItemVisitorDecorator(this, session, visitor);
    }

    public DocumentManager getDocumentManagerDecorator(SessionDecorator session, DocumentManager documentManager) {
        if(documentManager instanceof DocumentManagerDecorator)
            return documentManager;
        return new DocumentManagerDecorator(this, session, documentManager);
    }

    public WorkflowManager getWorkflowManagerDecorator(SessionDecorator session, WorkflowManager workflowManager) {
        if(workflowManager instanceof WorkflowManagerDecorator)
            return workflowManager;
        return new WorkflowManagerDecorator(session, workflowManager);
    }

    public HierarchyResolver getHierarchyResolverDecorator(SessionDecorator session, HierarchyResolver hierarchyResolver) {
        if(hierarchyResolver instanceof HierarchyResolverDecorator)
            return hierarchyResolver;
        return new HierarchyResolverDecorator(session, hierarchyResolver);
    }
}
