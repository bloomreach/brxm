/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.tools.projectexport;

import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.model.IDetachable;

import org.hippoecm.frontend.session.UserSession;

class ExportTreeModel extends DefaultTreeModel implements TreeModel, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(ExportTreeModel.class);

    private static final long serialVersionUID = 1L;

    private abstract class AbstractTreeNode implements TreeNode, Serializable {
        protected TreeNode parent;
        protected List<AbstractTreeNode> children = null;

        AbstractTreeNode(TreeNode parent) {
            this.parent = parent;
        }

        protected abstract void substantiate();

        public TreeNode getChildAt(int childIndex) {
            substantiate();
            return (children != null ? children.get(childIndex) : null);
        }

        public int getChildCount() {
            substantiate();
            return (children != null ? children.size() : 0);
        }

        public TreeNode getParent() {
            return parent;
        }

        public int getIndex(TreeNode node) {
            substantiate();
            return children.indexOf(node);
        }

        public abstract boolean getAllowsChildren();

        public abstract boolean isLeaf();

        public Enumeration<TreeNode> children() {
            substantiate();
            final Iterator<AbstractTreeNode> iter = (children != null ? children.iterator() : null);
            return new Enumeration<TreeNode>() {
                public boolean hasMoreElements() {
                    return iter != null && iter.hasNext();
                }

                public TreeNode nextElement() {
                    if (iter == null) {
                        throw new NoSuchElementException();
                    }
                    return iter.next();
                }
            };
        }
    }

    private class ElementTreeNode extends AbstractTreeNode implements TreeNode, Serializable {
        Element element;

        ElementTreeNode(ExportEngine export) {
            super(null);
            element = null;
            if (export != null) {
                List<Element> elements = export.getElements(null);
                if (elements != null) {
                    children = new LinkedList<AbstractTreeNode>();
                    for (Element child : elements) {
                        children.add(new ElementTreeNode(export, this, child));
                    }
                } else {
                    children = null;
                }
            } else {
                children = null;
            }
        }

        private ElementTreeNode(ExportEngine export, ElementTreeNode parent, Element element) {
            super(parent);
            this.element = element;
            List<Element> elements = export.getElements(element);

            if (elements != null) {
                children = new LinkedList<AbstractTreeNode>();
                for (Element child : elements) {
                    children.add(new ElementTreeNode(export, this, child));
                }
            } else {
                children = null;
            }
        }
        
        protected void substantiate() {  
        }

        public boolean getAllowsChildren() {
            substantiate();
            return children != null;
        }

        public boolean isLeaf() {
            substantiate();
            return !getAllowsChildren() || children.size() == 0;
        }

        @Override
        public String toString() {
            return (element != null ? element.getFullName() : "Projects");
        }
    }

    private class ContentTreeNode extends AbstractTreeNode implements TreeNode, Serializable {
        private String path;
        private ElementTreeNode backing;

        ContentTreeNode(ElementTreeNode parent, String path) {
            super(parent);
            this.backing = parent;
            this.path = path;
        }
        private ContentTreeNode(ContentTreeNode parent, ElementTreeNode ancestor, String path) {
            super(parent);
            this.parent = parent;
            this.backing = ancestor;
        }

        protected void substantiate() {
            if(children != null)
                return;
            children = new LinkedList<AbstractTreeNode>();
            try {
                Node node = ((Element.ContentElement)backing.element).getCurrent();
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    children.add(new ContentTreeNode(this, backing, child.getPath()));
                }
            } catch(RepositoryException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            }
        }

        public boolean getAllowsChildren() {
            return true;
        }

        public boolean isLeaf() {
            substantiate();
            return children.size() == 0;
        }
    }

    private ExportEngine exporter;

    ExportTreeModel(ExportEngine export) {
        super(null);
        this.exporter = export;
        setRoot(new ElementTreeNode(exporter));
    }

    ExportTreeModel() {
        super(null);
        setRoot(new ElementTreeNode(getExporter()));
    }

    public void detach() {
        // ignore, the transient fields will go null
    }
    
    ExportEngine getExporter() {
        if (exporter == null) {
            try {
                exporter = new ExportEngine(((UserSession)org.apache.wicket.Session.get()).getJcrSession());
            } catch (RepositoryException ex) {
                log.error("Everything is broken", ex);
            } catch (IOException ex) {
                log.error("Things are broken", ex);
            } catch (NotExportableException ex) {
                log.error("Some things are broken", ex);
            }
        }
        return exporter;
    }

    TreeNode backingTreeNode(TreeNode node) {
        if (node instanceof ElementTreeNode) {
            if (node.getParent() != null && node.getParent().getParent() != null) {
                while (node.getParent().getParent() != null) {
                    node = node.getParent();
                }
            }
            return node;
        } else {
            return null;
        }
    }

    Element backingElement(TreeNode node) {
        if (node instanceof ElementTreeNode) {
            return ((ElementTreeNode)node).element;
        } else {
            return null;
        }
    }
}
