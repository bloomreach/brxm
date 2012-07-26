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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.InvalidItemStateException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;

/**
 * 
 */
public abstract class UpdaterItemVisitor implements ItemVisitor {

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
            if(canonical == null || !canonical.isSame(node)) {
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

        /**
         * THIS METHOD WAS NEVER PART OF THE API, NO NOT USE UNLESS TRUELY NECESSARY AND REPORT AN ISSUE WITH YOUR USE CASE.
         * 
         * @return the same instance on which this method was invoked
         * @deprecated
         */
        @Deprecated
        public final Iterated setAtomic() {
            isAtomic = true;
            return this;
        }

        /**
         * THIS METHOD WAS NEVER PART OF THE API, NO NOT USE UNLESS TRUELY NECESSARY AND REPORT AN ISSUE WITH YOUR USE CASE.
         * @return whether the #setAtomic method has been called on this visitor
         */
        public final boolean isAtomic() {
            return isAtomic;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public final void visit(Node node, int level, boolean leaving) throws RepositoryException {
            if(leaving)
                leaving(node, level);
            else
                entering(node, level);
        }
        /**
         * {@inheritDoc}
         */
        @Override
        protected final void entering(Node node, int level)
                throws RepositoryException {
        }
        /**
         * {@inheritDoc}
         */
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

    public static class PathVisitor extends Iterated {
        private String relPath;

        public PathVisitor(String path) {
            if(path.startsWith("/jcr:root")) {
                path = path.substring("jcr:root".length());
            }
            while(path.startsWith("/"))
                path = path.substring(1);
            this.relPath = path;
        }

        public NodeIterator iterator(Session session) throws RepositoryException {
            final Node node;
            Node root = session.getRootNode();
            if(root.hasNode(relPath)) {
                node = root.getNode(relPath);
            } else {
                node = ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getNode(root, relPath);
            }
            return new NodeIterator() {
                boolean done = false;
                public Node nextNode() {
                    if(done)
                        throw new NoSuchElementException();
                    done = true;
                    return node;
                }
                public void skip(long skipNum) {
                    if(skipNum > 0)
                        done = true;
                }
                public long getSize() {
                    return (node != null ? 1 : 0);
                }
                public long getPosition() {
                    return (done ? 1 : 0);
                }
                public boolean hasNext() {
                    return (!done && node != null);
                }
                public Object next() {
                    return nextNode();
                }
                public void remove() {
                    throw new UnsupportedOperationException("Unsupported operation");
                }
            };
        }

        @Override
        public String toString() {
            return "PathVisitor[/"+relPath+"]";
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
    public static final class NamespaceVisitor extends UpdaterItemVisitor {
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
         * @param context the passed context to the UpdaterModel.register()
         * @param prefix the prefix of the namespace to visit
         * @param cndName the name of the CND file
         * @param cndReader a reader of the CND file
         * @deprecated
         */
        @Deprecated
        public NamespaceVisitor(UpdaterContext context, String prefix, String cndName, Reader cndReader) {
            this.prefix = prefix;
            this.cndName = cndName;
            this.cndReader = cndReader;
            this.context = context;
        }

        /**
         * @param context the passed context to the UpdaterModel.register()
         * @param prefix the prefix of the namespace to visit
         * @param istream should not be null, if null this namespace visitor will be ignored and a warning will be logged.  If the CND is to be autocreated, instead of passing null use the other constructor.
         */
        public NamespaceVisitor(UpdaterContext context, String prefix, InputStream istream) {
            this.prefix = prefix;
            if(istream == null) {
                /* having both cndName and cndReader as null will signal an missing
                 * stream.
                 */
                this.cndName = null;
                this.cndReader = null;
            } else {
                this.cndName = "-";
                this.cndReader = new InputStreamReader(istream);
            }
            this.context = context;
        }

        /**
         * @param context the passed context to the UpdaterModel.register()
         * @param prefix the prefix of the namespace to visit
         */
        public NamespaceVisitor(UpdaterContext context, String prefix) {
            this.prefix = prefix;
            this.cndName = "-";
            this.cndReader = null;
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
