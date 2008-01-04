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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoQuery;

/**
 */
public class QueryDecorator extends AbstractDecorator implements HippoQuery {

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
        return factory.getNodeDecorator(session, node);
    }

    /**
     * @inheritDoc
     */
    public String[] getArguments() {
        String queryString = getStatement();
        if(queryString.contains("?")) {
            int argumentCount = getArgumentCount();
            String[] arguments = new String[argumentCount];
            for (int i=0; i<argumentCount; i++) {
                arguments[i] = "?";
            }
            return arguments;
        } else {
            Set<String> arguments = new HashSet<String>();
            for (int position=0; position>=0; position=queryString.indexOf("$", position)) {
                int endPosition = position + 1;
                if(Character.isJavaIdentifierStart(queryString.charAt(endPosition))) {
                    do {
                        ++endPosition;
                    } while(Character.isJavaIdentifierPart(queryString.charAt(endPosition)));
                }
                arguments.add(queryString.substring(position,endPosition));
            }
            return arguments.toArray(new String[arguments.size()]);
        }
    }

    /**
     * @inheritDoc
     */
    public int getArgumentCount() {
        String queryString = getStatement();
        int count = -1;
        if(queryString.contains("?")) {
            for (int position=0; position>=0; position=queryString.indexOf("?", position)) {
                ++count;
            }
        } else {
            Set<String> arguments = new HashSet<String>();
            for (int position=0; position>=0; position=queryString.indexOf("$", position)) {
                ++count;
            }
        }
        return count;
    }

    public QueryResult execute(String argument) throws RepositoryException {
        String queryString = getStatement();
        if(queryString.contains("?")) {
            int count = getArgumentCount();
            String[] arguments = new String[count];
            for(int i=0; i<count; i++) {
                arguments[i] = argument;
            }
            return execute(arguments);
        } else {
            String[] argumentNames = getArguments();
            Map<String,String> arguments = new TreeMap<String,String>();
            for(int i=0; i<argumentNames.length; i++) {
                arguments.put(argumentNames[i], argument);
            }
            return execute(arguments);
        }
    }

    /**
     * @inheritDoc
     */
    public QueryResult execute(String[] arguments) throws RepositoryException {
        String queryString = getStatement();
        for (int i=0; i<arguments.length; i++) {
            queryString = queryString.replaceFirst("?", arguments[i]);
        }
        Query q = session.getWorkspace().getQueryManager().createQuery(queryString, getLanguage());
        return factory.getQueryResultDecorator(session, q.execute());
    }

    /**
     * @inheritDoc
     */
    public QueryResult execute(Map<String,String> arguments) throws RepositoryException {
        String queryString = getStatement();
        String[] argumentNames = getArguments();
        for (int i=0; i<argumentNames.length; i++) {
            queryString = queryString.replace("$"+argumentNames[i], arguments.get(argumentNames[i]));
        }
        Query q = session.getWorkspace().getQueryManager().createQuery(queryString, getLanguage());
        return factory.getQueryResultDecorator(session, q.execute());
    }
}
