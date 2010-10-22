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

import java.util.Map;

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
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoQuery;

/**
 */
public class QueryDecorator extends AbstractDecorator implements HippoQuery {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final HippoQuery query;

    protected QueryDecorator(DecoratorFactory factory, SessionDecorator session, HippoQuery query) {
        super(factory, session);
        this.query = query;
    }

    @Override
    protected void repair(Session upstreamSession) throws RepositoryException {
        throw new RepositoryException("Query has become invalid");
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    /**
     * @inheritDoc
     */
    public QueryResult execute() throws RepositoryException {
        check();
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
        check();
        return query.getStoredQueryPath();
    }

    /**
     * @inheritDoc
     */
    public Node storeAsNode(String absPath) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        check();
        Node node = query.storeAsNode(absPath);
        return node;
    }

    public Node storeAsNode(String absPath, String type) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        check();
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
    public String[] getArguments() throws RepositoryException {
        check();
        return query.getArguments();
    }

    /**
     * @inheritDoc
     */
    public int getArgumentCount() throws RepositoryException {
        check();
        return query.getArgumentCount();
    }

    /**
     * @inheritDoc
     */
    public QueryResult execute(Map<String,String> arguments) throws RepositoryException {
        check();
        return query.execute(arguments);
    }

    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException {
        check();
        query.bindValue(varName, value);
    }

    public void setLimit(long limit) {
        query.setLimit(limit);
    }

    public void setOffset(long offset) {
        query.setOffset(offset);
    }

    public String[] getBindVariableNames() throws RepositoryException {
        check();
        return query.getBindVariableNames();
    }

}
