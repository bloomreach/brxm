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
package org.hippoecm.repository.decorating;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import org.apache.jackrabbit.core.query.QueryImpl;

import org.hippoecm.repository.api.HippoQuery;

/**
 */
public class QueryDecorator extends AbstractDecorator implements HippoQuery {

    protected final Query query;
    protected Map<String,Value> arguments;

    private final static String MAGIC_NAMED_START = "MAGIC";
    private final static String MAGIC_NAMED_END = "CIGAM";

    public QueryDecorator(DecoratorFactory factory, Session session, Query query) {
        super(factory, session);
        this.query = query;
        this.arguments = null;
    }

    /**
     * @inheritDoc
     */
    public QueryResult execute() throws RepositoryException {
        if (arguments != null)
            return execute((Map<String,String>)null);
        else
            return factory.getQueryResultDecorator(session, query.execute());
    }

    /**
     * @inheritDoc
     */
    public String getStatement() {
        String queryString = query.getStatement();
        String[] argumentNames = getArguments();
        System.err.println("\n\n\n");
        for (int i=0; i<argumentNames.length; i++) {
            queryString = queryString.replaceAll(MAGIC_NAMED_START+argumentNames[i]+MAGIC_NAMED_END, "\\$"+argumentNames[i]);
        }
        return queryString;
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
        String queryString = query.getStatement();
        Set<String> arguments = new HashSet<String>();
        for (int position=queryString.indexOf(MAGIC_NAMED_START); position >=0; position=queryString.indexOf(MAGIC_NAMED_START, position)) {
            position += MAGIC_NAMED_START.length();
            int endPosition = position;
            if (Character.isJavaIdentifierStart(queryString.charAt(endPosition))) {
                do {
                    ++endPosition;
                } while (endPosition<queryString.length() && Character.isJavaIdentifierPart(queryString.charAt(endPosition)) &&
                         !queryString.substring(endPosition).startsWith(MAGIC_NAMED_END));
            }
            if (queryString.substring(endPosition).startsWith(MAGIC_NAMED_END)) {
                arguments.add(queryString.substring(position,endPosition));
                position = endPosition + MAGIC_NAMED_END.length();
            }
        }
        return arguments.toArray(new String[arguments.size()]);
    }

    /**
     * @inheritDoc
     */
    public int getArgumentCount() {
        String[] arguments = getArguments();
        return arguments != null ? arguments.length : 0;
    }

    /**
     * @inheritDoc
     */
    public QueryResult execute(Map<String,String> arguments) throws RepositoryException {
        String queryString = query.getStatement();
        String[] argumentNames = getArguments();
        if (arguments != null) {
            for (int i=0; i<argumentNames.length; i++) {
                if (arguments.containsKey(argumentNames[i]))
                    queryString = queryString.replace(MAGIC_NAMED_START+argumentNames[i]+MAGIC_NAMED_END,
                                                      arguments.get(argumentNames[i]));
            }
        }
        if (this.arguments != null) {
            for (Map.Entry<String,Value> entry : this.arguments.entrySet()) {
                queryString = queryString.replace(MAGIC_NAMED_START+entry.getKey()+MAGIC_NAMED_END,
                                                  entry.getValue().getString());
            }
        }
        Query q = session.getWorkspace().getQueryManager().createQuery(queryString, getLanguage());
        return factory.getQueryResultDecorator(session, q.execute());
    }

    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException {
        arguments.put(varName, value);
    }

    public void setLimit(long limit) throws RepositoryException {
        ((QueryImpl)query).setLimit(limit);
    }

    public void setOffset(long offset) throws RepositoryException {
        ((QueryImpl)query).setOffset(offset);
    }

    static String mangleArguments(String statement) {
        for (int position = statement.indexOf("$"); position >= 0; position = statement.indexOf("$", position)) {
            int endPosition = position + 1;
            if (Character.isJavaIdentifierStart(statement.charAt(endPosition))) {
                do {
                    ++endPosition;
                } while (endPosition<statement.length() && Character.isJavaIdentifierPart(statement.charAt(endPosition)));
                statement = statement.substring(0,position) + MAGIC_NAMED_START + statement.substring(position+1,endPosition) + MAGIC_NAMED_END + statement.substring(endPosition);
                endPosition += MAGIC_NAMED_START.length() + MAGIC_NAMED_END.length() - 1;
            }
            position = endPosition;
        }
        return statement;
    }
}
