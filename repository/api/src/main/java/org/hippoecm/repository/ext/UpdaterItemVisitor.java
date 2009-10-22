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
package org.hippoecm.repository.ext;

import java.io.Reader;
import java.util.LinkedList;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.InvalidItemStateException;

import org.hippoecm.repository.api.HippoNode;

/**
 * 
 * @author berry
 */
public abstract class UpdaterItemVisitor implements ItemVisitor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * 
     */
    final protected boolean breadthFirst;
    private LinkedList<Item> currentQueue;
    private LinkedList<Item> nextQueue;
    /**
     * 
     */
    protected int currentLevel;

    /**
     * 
     */
    protected UpdaterItemVisitor() {
        this(false);
    }

    /**
     * 
     * @param breadthFirst
     */
    protected UpdaterItemVisitor(boolean breadthFirst) {
        this.breadthFirst = breadthFirst;
        if (breadthFirst) {
            currentQueue = new LinkedList<Item>();
            nextQueue = new LinkedList<Item>();
        }
        currentLevel = 0;
    }

    /**
     * 
     * @param property
     * @param level
     * @throws javax.jcr.RepositoryException
     */
    protected abstract void entering(Property property, int level)
            throws RepositoryException;

    /**
     * 
     * @param node
     * @param level
     * @throws javax.jcr.RepositoryException
     */
    protected abstract void entering(Node node, int level)
            throws RepositoryException;

    /**
     * 
     * @param property
     * @param level
     * @throws javax.jcr.RepositoryException
     */
    protected abstract void leaving(Property property, int level)
            throws RepositoryException;

    /**
     * 
     * @param node
     * @param level
     * @throws javax.jcr.RepositoryException
     */
    protected abstract void leaving(Node node, int level)
            throws RepositoryException;

    public void visit(Property property) throws RepositoryException {
        entering(property, currentLevel);
        leaving(property, currentLevel);
    }

    public void visit(Node node) throws RepositoryException {
        if (node.getPath().equals("/jcr:system")) {
            return;
        }
        if(node instanceof HippoNode) {
            Node canonical = ((HippoNode) node).getCanonicalNode();
            if(canonical == null || canonical.isSame(node)) {
                return;
            }
        }

        try {
            if (!breadthFirst) {
                // depth-first traversal
                entering(node, currentLevel);
                currentLevel++;
                PropertyIterator propIter = node.getProperties();
                while (propIter.hasNext()) {
                    propIter.nextProperty().accept(this);
                }
                NodeIterator nodeIter = node.getNodes();
                while (nodeIter.hasNext()) {
                    nodeIter.nextNode().accept(this);
                }
                currentLevel--;
                leaving(node, currentLevel);
            } else {
                // breadth-first traversal
                entering(node, currentLevel);
                leaving(node, currentLevel);

                PropertyIterator propIter = node.getProperties();
                while (propIter.hasNext()) {
                    nextQueue.addLast(propIter.nextProperty());
                }
                NodeIterator nodeIter = node.getNodes();
                while (nodeIter.hasNext()) {
                    nextQueue.addLast(nodeIter.nextNode());
                }

                while (!currentQueue.isEmpty() || !nextQueue.isEmpty()) {
                    if (currentQueue.isEmpty()) {
                        currentLevel++;
                        currentQueue = nextQueue;
                        nextQueue = new LinkedList<Item>();
                    }
                    Item item = currentQueue.removeFirst();
                    item.accept(this);
                }
                currentLevel = 0;
            }
        } catch (InvalidItemStateException ex) {
            // deliberate ignore
        } catch (RepositoryException ex) {
            currentLevel = 0;
            throw ex;
        }
    }

    public void visit(Node node, int level, boolean leaving) throws RepositoryException {
        visit(node);
    }
    

    /**
     * 
     */
    public static class Default extends UpdaterItemVisitor {
        /**
         * 
         */
        public Default() {
        }

        /**
         * 
         * @param breadthFirst
         */
        public Default(boolean breadthFirst) {
            super(breadthFirst);
        }

        @Override
        public final void visit(Property property) throws RepositoryException {
        }

        @Override
        public final void visit(Node node) throws RepositoryException {
            entering(node, 0);
            leaving(node, 0);
        }

        /**
         * 
         * @param node
         * @param level
         * @throws javax.jcr.RepositoryException
         */
        protected void entering(Node node, int level)
                throws RepositoryException {
        }

        /**
         * 
         * @param property
         * @param level
         * @throws javax.jcr.RepositoryException
         */
        protected void entering(Property property, int level)
                throws RepositoryException {
        }

        /**
         * 
         * @param node
         * @param level
         * @throws javax.jcr.RepositoryException
         */
        protected void leaving(Node node, int level)
                throws RepositoryException {
        }

        /**
         * 
         * @param property
         * @param level
         * @throws javax.jcr.RepositoryException
         */
        protected void leaving(Property property, int level)
                throws RepositoryException {
        }
    }

    /**
     * 
     */
    public static abstract class Iterated extends Default {
        private boolean isAtomic = false;
        /**
         * 
         * @param session
         * @return
         * @throws javax.jcr.RepositoryException
         */
        public abstract NodeIterator iterator(Session session) throws RepositoryException;
        public final Iterated setAtomic() {
            isAtomic = true;
            return this;
        }
        public final boolean isAtomic() {
            return isAtomic;
        }
        @Override
        public final void visit(Node node, int level, boolean leaving) throws RepositoryException {
            if(leaving)
                leaving(node, level);
            else
                entering(node, level);
        }
        @Override
        protected final void entering(Node node, int level)
                throws RepositoryException {
        }
        @Override
        protected final void entering(Property property, int level)
                throws RepositoryException {
        }
    }

    /**
     * 
     */
    public static class QueryVisitor extends Iterated {
        String statement;
        String language;
        /**
         * 
         * @param statement
         * @param language
         */
        public QueryVisitor(String statement, String language) {
            this.statement = statement;
            this.language = language;
        }
        /**
         * 
         * @param session
         * @return
         * @throws javax.jcr.RepositoryException
         */
        public NodeIterator iterator(Session session) throws RepositoryException {
            Query query = session.getWorkspace().getQueryManager().createQuery(statement, language);
            QueryResult result = query.execute();
            return result.getNodes();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "QueryVisitor["+statement+"]";
        }

    }

    /**
     * 
     */
    public static class NodeTypeVisitor extends Iterated {
        String nodeType;
        /**
         * 
         * @param nodeType
         */
        public NodeTypeVisitor(String nodeType) {
          this.nodeType = nodeType;
        }
        /**
         * 
         * @param session
         * @return
         * @throws javax.jcr.RepositoryException
         */
        public NodeIterator iterator(Session session) throws RepositoryException {
            Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM "+nodeType, javax.jcr.query.Query.SQL);
            QueryResult result = query.execute();
            return result.getNodes();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "NodeTypeVisitor["+nodeType+"]";
        }

    }

    /**
     * 
     */
    public final static class NamespaceVisitor extends UpdaterItemVisitor {
        /**
         * 
         */
        public String prefix;
        /**
         * 
         */
        public Reader cndReader;
        /**
         * 
         */
        public String cndName;
        /**
         * 
         */
        public UpdaterContext context;

        /**
         * 
         * @param context
         * @param prefix
         * @param cndName
         * @param cndReader
         */
        public NamespaceVisitor(UpdaterContext context, String prefix, String cndName, Reader cndReader) {
            this.prefix = prefix;
            this.cndName = cndName;
            this.cndReader = cndReader;
            this.context = context;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "NamespaceVisitor["+prefix+"]";
        }

        /**
         * 
         * @param property
         * @param level
         * @throws javax.jcr.RepositoryException
         */
        @Override
        protected void entering(Property property, int level) throws RepositoryException {
        }

        /**
         * 
         * @param node
         * @param level
         * @throws javax.jcr.RepositoryException
         */
        @Override
        protected void entering(Node node, int level) throws RepositoryException {
        }

        /**
         * 
         * @param property
         * @param level
         * @throws javax.jcr.RepositoryException
         */
        @Override
        protected void leaving(Property property, int level) throws RepositoryException {
        }

        /**
         * 
         * @param node
         * @param level
         * @throws javax.jcr.RepositoryException
         */
        @Override
        protected void leaving(Node node, int level) throws RepositoryException {
        }
    }
}
