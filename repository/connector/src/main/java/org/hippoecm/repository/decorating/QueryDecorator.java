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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoQuery;

/**
 */
public abstract class QueryDecorator extends AbstractDecorator implements HippoQuery {

    protected final Query query;

    public QueryDecorator(DecoratorFactory factory, Session session, Query query) {
        super(factory, session);
        this.query = query;
    }

    /**
     * @inheritDoc
     */
    public QueryResult execute() throws RepositoryException {
        return factory.getQueryResultDecorator(session, query.execute());
    }

    /**
     * @inheritDoc
     */
    public String getStatement() {
        return query.getStatement();
    }

    /**
     * @inheritDoc
     */
    public String getLanguage() {
        return query.getLanguage();
    }

    /**
     * @inheritDoc
     */
    public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException {
        return query.getStoredQueryPath();
    }

    /**
     * @inheritDoc
     */
    public Node storeAsNode(String absPath) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        Node node = query.storeAsNode(absPath);
        return node;
    }

    public Node storeAsNode(String absPath, String type) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        if(!absPath.startsWith("/")) {
                throw new RepositoryException(absPath + " is not an absolute path");
        }
        Node queryNode = session.getRootNode().addNode(absPath.substring(1), type);
        queryNode.setProperty("jcr:language", getLanguage());
        queryNode.setProperty("jcr:statement", getStatement());
        return factory.getNodeDecorator(session, queryNode);
    }

    /**
     * @inheritDoc
     */
    public abstract String[] getArguments();

    /**
     * @inheritDoc
     */
    public abstract int getArgumentCount();

    /**
     * @inheritDoc
     */
    public abstract QueryResult execute(Map<String,String> arguments) throws RepositoryException;

    public abstract void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException;

    public abstract void setLimit(long limit);

    public abstract void setOffset(long offset);
}
