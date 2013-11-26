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
package org.hippoecm.frontend.model.tree;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.tree.DefaultTreeState;
import org.apache.wicket.extensions.markup.html.tree.ITreeStateListener;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JCR tree model implementation that can be shared by multiple tree instances.
 * It is observable and can therefore not be used to register listeners.  Use the {@link JcrTreeModel}
 * instead to register {@link TreeModelListener}s.
 */
public class ObservableTreeModel extends DefaultTreeModel implements IJcrTreeModel, IObservable, IDetachable {

    private static final long serialVersionUID = 1L;

    public class ObservableTreeModelEvent implements IEvent<ObservableTreeModel> {

        private JcrEvent jcrEvent;

        public ObservableTreeModelEvent(JcrEvent event) {
            this.jcrEvent = event;
        }

        public JcrEvent getJcrEvent() {
            return jcrEvent;
        }

        public ObservableTreeModel getSource() {
            return ObservableTreeModel.this;
        }

    }

    class ExpandedNode implements Serializable, IDetachable {

        private boolean expanded = false;
        private final IJcrTreeNode treeNode;
        private final String identifier;
        private final Map<String, ExpandedNode> children;
        private IObserver observer;

        ExpandedNode(final IJcrTreeNode treeNode) {
            this.treeNode = treeNode;
            this.children = new TreeMap<String, ExpandedNode>();
            IModel<Node> nodeModel = treeNode.getNodeModel();
            String id = "<unknown>";
            try {
                final Node node = nodeModel.getObject();
                id = node.getIdentifier();
            } catch (RepositoryException e) {
                log.error("Could not determine node identifier", e);
            }
            identifier = id;
        }

        ExpandedNode get(String[] path, int index) {
            if (path == null) {
                return null;
            }
            if (path.length == index) {
                return this;
            }
            String name = path[index];
            if (name.indexOf('[') < 0) {
                name = name + "[1]";
            }
            if (!children.containsKey(name)) {
                log.info("Reloading children because " + name + " is expected to be there, but isn't");
                reloadChildren();
            }
            ExpandedNode child = children.get(name);
            if (child != null) {
                return child.get(path, index + 1);
            } else {
                log.warn("Unable to find child " + name + " in observable tree model for " + treeNode.getNodeModel());
                return null;
            }
        }

        void collapse() {
            stopObservation();
            for (Map.Entry<String, ExpandedNode> entry : children.entrySet()) {
                entry.getValue().collapse();
            }
            children.clear();
            expanded = false;
        }

        void expand() {
            loadChildren(this.children);
            expanded = true;

            startObservation();
        }

        private void loadChildren(Map<String, ExpandedNode> children) {
            final Enumeration nodeChildren = treeNode.children();
            while (nodeChildren.hasMoreElements()) {
                final IJcrTreeNode childNode = (IJcrTreeNode) nodeChildren.nextElement();
                IModel<Node> nodeModel = childNode.getNodeModel();
                try {
                    final Node node = nodeModel.getObject();
                    String name = node.getName() + "[" + node.getIndex() + "]";
                    children.put(name, new ExpandedNode(childNode));
                } catch (InvalidItemStateException e) {
                    log.debug("Reloading child failed: removed by another session");
                } catch (RepositoryException e) {
                    log.error("Unable to load child in tree node", e);
                }
            }
        }

        private Map<String, String> byId(Map<String, ExpandedNode> nodes) {
            Map<String, String> ids = new TreeMap<String, String>();
            for (Map.Entry<String, ExpandedNode> entry : nodes.entrySet()) {
                ids.put(entry.getValue().identifier, entry.getKey());
            }
            return ids;
        }

        void reloadChildren () {
            Map<String, ExpandedNode> newChildren = new TreeMap<String, ExpandedNode>();
            loadChildren(newChildren);

            Map<String, String> newIds = byId(newChildren);
            Map<String, String> oldIds = byId(children);

            for (Map.Entry<String, String> entry : oldIds.entrySet()) {
                final String id = entry.getKey();
                final String name = entry.getValue();
                if (!newIds.containsKey(id)) {
                    final ExpandedNode expandedNode = children.remove(name);
                    expandedNode.stopObservation();
                } else if (!newIds.get(id).equals(name)) {
                    final ExpandedNode expandedNode = children.remove(name);
                    children.put(newIds.get(id), expandedNode);
                }
            }

            for (Map.Entry<String, String> entry : newIds.entrySet()) {
                final String id = entry.getKey();
                if (!oldIds.containsKey(id)) {
                    final String name = entry.getValue();
                    final ExpandedNode expandedNode = newChildren.get(name);
                    children.put(name, expandedNode);
                    expandedNode.startObservation();
                }
            }
        }

        public void startObservation() {
            if (expanded && observationContext != null) {
                observer = new IObserver() {

                    @Override
                    public IObservable getObservable() {
                        return (IObservable) treeNode.getNodeModel();
                    }

                    @Override
                    public void onEvent(final Iterator events) {
                        reloadChildren();

                        EventCollection<IEvent<ObservableTreeModel>> treeModelEvents = new EventCollection<IEvent<ObservableTreeModel>>();
                        while (events.hasNext()) {
                            IEvent<ObservableTreeModel> treeModelEvent = new ObservableTreeModelEvent(
                                    (JcrEvent) events.next());
                            treeModelEvents.add(treeModelEvent);
                        }
                        observationContext.notifyObservers(treeModelEvents);
                    }
                };
                observationContext.registerObserver(observer);
                for (Map.Entry<String, ExpandedNode> entry : children.entrySet()) {
                    entry.getValue().startObservation();
                }
            }
        }

        public void stopObservation() {
            for (Map.Entry<String, ExpandedNode> entry : children.entrySet()) {
                entry.getValue().stopObservation();
            }
            if (observer != null) {
                observationContext.unregisterObserver(observer);
            }
        }

        @Override
        public void detach() {
            treeNode.detach();
            for (Map.Entry<String, ExpandedNode> entry : children.entrySet()) {
                entry.getValue().detach();
            }
        }
    }

    final static Logger log = LoggerFactory.getLogger(ObservableTreeModel.class);

    private IObservationContext<ObservableTreeModel> observationContext;
    protected final IJcrTreeNode root;
    private final String rootPath;
    private final ExpandedNode expandedRoot;

    public ObservableTreeModel(IJcrTreeNode rootModel) {
        super(rootModel);

        root = rootModel;
        expandedRoot = new ExpandedNode(root);
        final IModel<Node> jcrNodeModel = root.getNodeModel();
        String path;
        try {
            path = jcrNodeModel.getObject().getPath();
            if ("/".equals(path)) {
                path = "";
            }
        } catch (RepositoryException e) {
            log.error("fail", e);
            path = "";
        }
        rootPath = path;
    }

    public void setTreeState(final DefaultTreeState state) {
        expandNodes(state, expandedRoot);
        state.addTreeStateListener(new ObservableTreeStateListener());
    }

    private void expandNodes(DefaultTreeState state, ExpandedNode node) {
        if (state.isNodeExpanded(node.treeNode)) {
            for (Map.Entry<String, ExpandedNode> entry : node.children.entrySet()) {
                expandNodes(state, entry.getValue());
            }
        }
    }

    public TreePath lookup(JcrNodeModel nodeModel) {
        IJcrTreeNode node = root;
        if (nodeModel != null) {
            String basePath = ((JcrNodeModel) root.getNodeModel()).getItemModel().getPath();
            String path = nodeModel.getItemModel().getPath();
            if (path != null && path.startsWith(basePath)) {
                String[] elements = StringUtils.split(path.substring(basePath.length()), '/');
                List<Object> nodes = new LinkedList<Object>();
                nodes.add(node);
                try {
                    for (String element : elements) {
                        IJcrTreeNode child = node.getChild(element);
                        if (child != null) {
                            nodes.add(child);
                            node = child;
                        } else {
                            break;
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error("Unable to find node in tree", ex.getMessage());
                }
                return new TreePath(nodes.toArray(new Object[nodes.size()]));
            }
        }
        return null;
    }

    public void detach() {
        expandedRoot.detach();
    }

    @SuppressWarnings("unchecked")
    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        this.observationContext = (IObservationContext<ObservableTreeModel>) context;
    }

    public void startObservation() {
        expandedRoot.startObservation();
    }

    public void stopObservation() {
        expandedRoot.stopObservation();
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        throw new UnsupportedOperationException();
    }

    private class ObservableTreeStateListener implements ITreeStateListener, Serializable {

        @Override
        public void allNodesCollapsed() {
            expandedRoot.collapse();
        }

        @Override
        public void allNodesExpanded() {
            expandAll(expandedRoot);
        }

        private void expandAll(final ExpandedNode expandedNode) {
            expandedNode.expand();
            for (ExpandedNode expandedChild : expandedNode.children.values()) {
                expandAll(expandedChild);
            }
        }

        @Override
        public void nodeCollapsed(final Object node) {
            ExpandedNode stateNode = find(node);
            if (stateNode != null) {
                stateNode.collapse();
            }
        }

        private ExpandedNode find(Object node) {
            IJcrTreeNode treeNode = (IJcrTreeNode) node;
            final IModel<Node> jcrNodeModel = treeNode.getNodeModel();
            try {
                String path = jcrNodeModel.getObject().getPath();
                if (!path.startsWith(rootPath)) {
                    log.warn("Path " + path + " is not a descendant of " + rootPath);
                    return null;
                }
                if ("/".equals(path)) {
                    path = "";
                }
                if (path.length() == rootPath.length()) {
                    return expandedRoot;
                }
                path = path.substring(rootPath.length() + 1);
                String[] elements = path.split("/");
                return expandedRoot.get(elements, 0);
            } catch (RepositoryException e) {
                log.error("Could not collapse node", e);
            }
            return null;
        }

        @Override
        public void nodeExpanded(final Object node) {
            ExpandedNode stateNode = find(node);
            if (stateNode != null) {
                stateNode.expand();
            }
        }

        @Override
        public void nodeSelected(final Object node) {
        }

        @Override
        public void nodeUnselected(final Object node) {
        }
    }
}
