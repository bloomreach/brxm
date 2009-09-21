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

import org.hippoecm.repository.api.HippoNode;

public abstract class UpdaterItemVisitor implements ItemVisitor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final protected boolean breadthFirst;
    private LinkedList<Item> currentQueue;
    private LinkedList<Item> nextQueue;
    public int currentLevel;

    protected UpdaterItemVisitor() {
        this(false);
    }

    protected UpdaterItemVisitor(boolean breadthFirst) {
        this.breadthFirst = breadthFirst;
        if (breadthFirst) {
            currentQueue = new LinkedList<Item>();
            nextQueue = new LinkedList<Item>();
        }
        currentLevel = 0;
    }

    protected abstract void entering(Property property, int level)
            throws RepositoryException;

    protected abstract void entering(Node node, int level)
            throws RepositoryException;

    protected abstract void leaving(Property property, int level)
            throws RepositoryException;

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
        } catch (RepositoryException ex) {
            currentLevel = 0;
            throw ex;
        }
    }

    public static class Default extends UpdaterItemVisitor {
        public Default() {
        }

        public Default(boolean breadthFirst) {
            super(breadthFirst);
        }

        @Override
        public void visit(Property property) throws RepositoryException {
            super.visit(property);
        }
        @Override
        public void visit(Node node) throws RepositoryException {
            super.visit(node);
        }
        protected void entering(Node node, int level)
                throws RepositoryException {
        }

        protected void entering(Property property, int level)
                throws RepositoryException {
        }

        protected void leaving(Node node, int level)
                throws RepositoryException {
        }

        protected void leaving(Property property, int level)
                throws RepositoryException {
        }
    }

    public static abstract class Iterated extends Default {
        public abstract NodeIterator iterator(Session session) throws RepositoryException;
    }

    public static class QueryVisitor extends Iterated {
        String statement;
        String language;
        public QueryVisitor(String statement, String language) {
            this.statement = statement;
            this.language = language;
        }
        public NodeIterator iterator(Session session) throws RepositoryException {
            Query query = session.getWorkspace().getQueryManager().createQuery(statement, language);
            QueryResult result = query.execute();
            return result.getNodes();
        }
    }

    public static class NodeTypeVisitor extends Iterated {
        String nodeType;
        public NodeTypeVisitor(String nodeType) {
          this.nodeType = nodeType;
        }
        public NodeIterator iterator(Session session) throws RepositoryException {
            Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM "+nodeType, javax.jcr.query.Query.SQL);
            QueryResult result = query.execute();
            return result.getNodes();
        }
        @Override
        public void visit(Node node) throws RepositoryException {
            if (node.getPath().equals("/jcr:system")) {
                return;
            }
            if (node instanceof HippoNode) {
                Node canonical = ((HippoNode)node).getCanonicalNode();
                if (canonical == null || canonical.isSame(node)) {
                    return;
                }
            }
            try {
                // depth-first traversal
                if (node.isNodeType(nodeType)) {
                    entering(node, currentLevel);
                }
                currentLevel++;
                PropertyIterator propIter = node.getProperties();
                while (propIter.hasNext()) {
                    Property prop = propIter.nextProperty();
                    if (node.isNodeType(nodeType))
                        prop.accept(this);
                }
                NodeIterator nodeIter = node.getNodes();
                while (nodeIter.hasNext()) {
                    Node child = nodeIter.nextNode();
                    child.accept(this);
                }
                currentLevel--;
                if (node.isNodeType(nodeType))
                    leaving(node, currentLevel);
            } catch (RepositoryException ex) {
                currentLevel = 0;
                throw ex;
            }
        }
    }

    public final static class NamespaceVisitor extends UpdaterItemVisitor {
        public String namespace;
        public String oldURI;
        public String newURI;
        public String oldPrefix;
        public String newPrefix;
        public Reader cndReader;
        public String cndName;
        UpdaterContext context;
        public NamespaceVisitor(UpdaterContext context, String prefix, String cndName, Reader cndReader) {
            this.namespace = prefix;
            this.cndName = cndName;
            this.cndReader = cndReader;
            this.context = context;
        }
        @Override
        public final void visit(Node node) throws RepositoryException {
            super.visit(node); // FIXME
        }
        @Override
        protected final void entering(Node node, int level)
                throws RepositoryException {
            NodeType[] nodeTypes = context.getNodeTypes(node);
            if (nodeTypes.length > 0 && nodeTypes[0].getName().startsWith(namespace + ":")) {
                context.setPrimaryNodeType(node, newPrefix + ":" + nodeTypes[0].getName().substring(namespace.length() + 1));
            }
            if (nodeTypes.length > 1) {
                boolean mixinsChanged = false;
                String[] mixins = new String[nodeTypes.length - 1];
                for (int i = 1; i < nodeTypes.length; i++) {
                    if (nodeTypes[i].getName().startsWith(namespace + ":")) {
                        mixins[i - 1] = newPrefix + ":" + nodeTypes[i].getName().substring(namespace.length() + 1);
                        mixinsChanged = true;
                    } else {
                        mixins[i - 1] = nodeTypes[i].getName();
                    }
                }
                if (mixinsChanged) {
                    node.setProperty("jcr:mixinTypes", mixins);
                }
            }
        }

        @Override
        protected final void entering(Property property, int level)
                throws RepositoryException {
       }
        @Override
        protected final void leaving(Node node, int level)
                throws RepositoryException {
          for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
            Node child = iter.nextNode();
            if (child.getName().startsWith(namespace + ":")) {
              context.setName(child, newPrefix + ":" + child.getName().substring(child.getName().indexOf(":") + 1));
            }
          }
        }
        @Override
        protected final void leaving(Property property, int level)
                throws RepositoryException {
          if(property.getName().startsWith(namespace+":")) {
            context.setName(property, newPrefix+":"+property.getName().substring(property.getName().indexOf(":")+1));
          }
         }
    }
}
