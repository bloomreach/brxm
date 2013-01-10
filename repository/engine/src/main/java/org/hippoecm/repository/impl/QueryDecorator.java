/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.apache.jackrabbit.core.query.QueryImpl;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.decorating.DecoratorFactory;

public class QueryDecorator extends org.hippoecm.repository.decorating.QueryDecorator implements HippoQuery {

    protected final Query query;
    protected Map<String, Value> arguments = null;
    private HardcodedQuery implementation = null;

    private static final String MAGIC_NAMED_START = "MAGIC";
    private static final String MAGIC_NAMED_END = "CIGAM";

    public QueryDecorator(DecoratorFactory factory, Session session, Query query) {
        super(factory, session, query);
        this.query = query;
    }

    public QueryDecorator(DecoratorFactory factory, Session session, Query query, Node node) {
        super(factory, session, query);
        this.query = query;
        try {
            if (node.isNodeType(HippoNodeType.NT_IMPLEMENTATION) && node.hasProperty(HippoNodeType.HIPPO_CLASSNAME)) {
                String classname = node.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                this.implementation = (HardcodedQuery)Class.forName(classname).newInstance();
            }
        } catch (ClassNotFoundException ex) {
            // FIXME log some error
        } catch (InstantiationException ex) {
            // FIXME log some error
        } catch (IllegalAccessException ex) {
            // FIXME log some error
        } catch (RepositoryException ex) {
            // FIXME log some error
        }
    }

    public Session getSession() {
        return session;
    }

    /**
     * @inheritDoc
     */
    public QueryResult execute() throws RepositoryException {
        if (arguments != null) {
            return execute((Map<String, String>)null);
        } else {
            return factory.getQueryResultDecorator(session, execute(query));
        }
    }

    /**
     * @inheritDoc
     */
    public String getStatement() {
        String queryString = query.getStatement();
        String[] argumentNames = getArguments();
        for (int i = 0; i < argumentNames.length; i++) {
            queryString = queryString.replaceAll(MAGIC_NAMED_START + argumentNames[i] + MAGIC_NAMED_END, "\\$" + argumentNames[i]);
        }
        return queryString;
    }

    /**
     * @inheritDoc
     */
    public String[] getArguments() {
        String queryString = query.getStatement();
        Set<String> arguments = new HashSet<String>();
        for (int position = queryString.indexOf(MAGIC_NAMED_START); position >= 0; position = queryString.indexOf(MAGIC_NAMED_START, position)) {
            position += MAGIC_NAMED_START.length();
            int endPosition = position;
            if (Character.isJavaIdentifierStart(queryString.charAt(endPosition))) {
                do {
                    ++endPosition;
                } while (endPosition < queryString.length() && Character.isJavaIdentifierPart(queryString.charAt(endPosition)) &&
                        !queryString.substring(endPosition).startsWith(MAGIC_NAMED_END));
            }
            if (queryString.substring(endPosition).startsWith(MAGIC_NAMED_END)) {
                arguments.add(queryString.substring(position, endPosition));
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
    public QueryResult execute(Map<String, String> arguments) throws RepositoryException {
        String queryString = query.getStatement();
        if (arguments != null) {
            for (Map.Entry<String,String> entry : arguments.entrySet()) {
                bindValue(entry.getKey(), session.getValueFactory().createValue(entry.getValue()));
            }
        }
        if (this.arguments != null) {
            for (Map.Entry<String, Value> entry : this.arguments.entrySet()) {
                queryString = queryString.replace(MAGIC_NAMED_START + entry.getKey() + MAGIC_NAMED_END, entry.getValue().getString());
            }
        }
        Query q = session.getWorkspace().getQueryManager().createQuery(queryString, getLanguage());
        return factory.getQueryResultDecorator(session, execute(q));
    }

    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException {
        if (query.getStatement().contains(MAGIC_NAMED_START)) {
            if(arguments == null)
                arguments = new HashMap<String, Value>();
            arguments.put(varName, value);
        } else {
            query.bindValue(varName, value);
        }
    }

    public void setLimit(long limit) {
        ((QueryImpl)query).setLimit(limit);
    }

    public void setOffset(long offset) {
        ((QueryImpl)query).setOffset(offset);
    }

    static String mangleArguments(String statement) {
        if (statement == null ) throw new IllegalArgumentException("Query statement is null");
        for (int position = statement.indexOf("$"); position >= 0; position = statement.indexOf("$", position)) {
            int endPosition = position + 1;
            if (Character.isJavaIdentifierStart(statement.charAt(endPosition))) {
                do {
                    ++endPosition;
                } while (endPosition < statement.length() && Character.isJavaIdentifierPart(statement.charAt(endPosition)));
                statement = statement.substring(0, position) + MAGIC_NAMED_START + statement.substring(position + 1, endPosition) + MAGIC_NAMED_END + statement.substring(endPosition);
                endPosition += MAGIC_NAMED_START.length() + MAGIC_NAMED_END.length() - 1;
            }
            position = endPosition;
        }
        return statement;
    }

    public String[] getBindVariableNames() throws RepositoryException {
        return ((QueryImpl)query).getBindVariableNames();
    }

    public static interface HardcodedQuery {
        public List execute(Session session, HippoQuery query, Map<String,Value> arguments) throws RepositoryException;
    }

    private QueryResult execute(Query query) throws RepositoryException {
        if (implementation != null) {
            final List result = implementation.execute(session, this, arguments);
            return new QueryResult() {
                public String[] getColumnNames() throws RepositoryException {
                    return null;
                }

                public RowIterator getRows() throws RepositoryException {

                    return new RowIterator() {
                        int index = -1;

                        public Row nextRow() {
                            final Object row = result.get(++index);
                            return new Row() {
                                public Value getValue(String column) {
                                    try {
                                        if (row instanceof Node) {
                                            Node node = (Node)row;
                                            if (node.hasProperty(column))
                                                return node.getProperty(column).getValue();
                                            else
                                                return null;
                                        } else if (row instanceof Item[]) {
                                            Item[] items = (Item[])row;
                                            for (int i = 0; i < items.length; i++) {
                                                if (items[i].getName().equals(column)) {
                                                    if (items[i].isNode()) {
                                                        return ((Property)(((Node)items[i]).getPrimaryItem())).getValue();
                                                    } else {
                                                        return ((Property)items[i]).getValue();
                                                    }
                                                }
                                            }
                                            return null;
                                        } else {
                                            return null;
                                        }
                                    } catch (RepositoryException ex) {
                                        // FIXME log some error
                                        return null;
                                    }
                                }

                                public Value[] getValues() {
                                    try {
                                        if (row instanceof Node) {
                                            Node node = (Node)row;
                                            ValueFactory valueFactory = node.getSession().getValueFactory();
                                            Value[] values = new Value[3];
                                            values[0] = valueFactory.createValue(node.getPrimaryNodeType().getName(), PropertyType.NAME);
                                            values[1] = valueFactory.createValue(node.getPath(), PropertyType.PATH);
                                            values[2] = valueFactory.createValue(100);
                                            return values;
                                        } else if (row instanceof Item[]) {
                                            Item[] items = (Item[])row;
                                            Value[] values = new Value[items.length];
                                            for (int i = 0; i < items.length; i++) {
                                                if (items[i].isNode())
                                                    values[i] = ((Property)(((Node)items[i]).getPrimaryItem())).getValue();
                                                else
                                                    values[i] = ((Property)items[i]).getValue();
                                            }
                                            return values;
                                        } else
                                            return null;
                                    } catch (RepositoryException ex) {
                                        // FIXME log some error
                                        return null;
                                    }
                                }

                                public Node getNode() throws RepositoryException {
                                    if(row instanceof Node) {
                                        return (Node)row;
                                    } else if(row instanceof Item[]) {
                                        Item[] items = (Item[]) row;
                                        if(items.length > 0) {
                                            if(items[0] instanceof Node) {
                                                return (Node)items[0];
                                            } else {
                                                return items[0].getParent();
                                            }
                                        } else
                                            throw new UnsupportedOperationException();
                                    } else if(row instanceof Row) {
                                        return ((Row)row).getNode();
                                    } else
                                        throw new UnsupportedOperationException();
                                }

                                public Node getNode(String selectorName) throws RepositoryException {
                                    throw new UnsupportedOperationException("Not supported yet.");
                                }

                                public String getPath() throws RepositoryException {
                                    throw new UnsupportedOperationException("Not supported yet.");
                                }

                                public String getPath(String selectorName) throws RepositoryException {
                                    throw new UnsupportedOperationException("Not supported yet.");
                                }

                                public double getScore() throws RepositoryException {
                                    throw new UnsupportedOperationException("Not supported yet.");
                                }

                                public double getScore(String selectorName) throws RepositoryException {
                                    throw new UnsupportedOperationException("Not supported yet.");
                                }
                            };
                        }

                        public long getPosition() {
                            return index;
                        }

                        public long getSize() {
                            return result.size();
                        }

                        public void skip(long count) {
                            index += count;
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }

                        public Object next() {
                            return nextRow();
                        }

                        public boolean hasNext() {
                            return index + 1 < result.size();
                        }
                    };
                }

                public NodeIterator getNodes() throws RepositoryException {
                    return new NodeIterator() {
                        int index = -1;

                        public Node nextNode() {
                            try {
                                Object row = result.get(++index);
                                if (row instanceof Node)
                                    return (Node)row;
                                else if (row instanceof Item[])
                                    return ((Item[])row)[0].getParent();
                                else
                                    return null;
                            } catch (AccessDeniedException ex) {
                                return null; // proper return
                            } catch (ItemNotFoundException ex) {
                                return null; // cannot happen
                            } catch (RepositoryException ex) {
                                // FIXME log some error
                                return null;
                            }
                        }

                        public long getPosition() {
                            return index;
                        }

                        public long getSize() {
                            return result.size();
                        }

                        public void skip(long count) {
                            index += count;
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }

                        public Object next() {
                            return nextNode();
                        }

                        public boolean hasNext() {
                            return index + 1 < result.size();
                        }
                    };
                }

                public String[] getSelectorNames() throws RepositoryException {
                    return new String[0];
                }
            };

        } else {
            return query.execute();
        }
    }
}
