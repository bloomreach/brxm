/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.NodeIterator;
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
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JCR tree model implementation that can be shared by multiple tree instances.
 * It is observable and can therefore not be used to register listeners.  Use the {@link JcrTreeModel}
 * instead to register {@link TreeModelListener}s.
 *
 * Each instance of this class should be registered separately. Hence this class does not override {@link #equals}
 * nor {@link #hashCode}.
 */
public class ObservableTreeModel extends DefaultTreeModel implements IJcrTreeModel, IObservable, IDetachable {

    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(ObservableTreeModel.class);

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

    /**
     * Special purpose event that marks the change of a translation node
     */
    public class TranslationEvent extends ObservableTreeModelEvent {

        public TranslationEvent(final JcrEvent event) {
            super(event);
        }
    }

    class TranslationNode extends ObservableNode {

        public TranslationNode(final JcrNodeModel model) {
            super(model);
        }

        @Override
        ObservableTreeModelEvent createEvent(final JcrEvent event) {
            return new TranslationEvent(event);
        }
    }

    class ExpandedNode extends ObservableNode {

        private boolean expanded = false;
        private final IJcrTreeNode treeNode;
        private final Map<String, ExpandedNode> children;
        private final Map<String, TranslationNode> translations;

        ExpandedNode(final IJcrTreeNode treeNode) {
            super((JcrNodeModel) treeNode.getNodeModel());
            
            this.treeNode = treeNode;
            this.children = new TreeMap<>();
            this.translations = loadTranslations(new TreeMap<String, TranslationNode>()); 
        }

        private Map<String, TranslationNode> loadTranslations(final TreeMap<String, TranslationNode> translations) {
            final Node node = treeNode.getNodeModel().getObject();
            try {
                final NodeIterator it = node.getNodes("hippo:translation");
                while (it.hasNext()) {
                    final JcrNodeModel translationModel = new JcrNodeModel(it.nextNode());
                    final TranslationNode translationNode = new TranslationNode(translationModel);
                    translations.put(translationNode.getId(), translationNode);
                }
            } catch (RepositoryException e) {
                log.error("Could not create observable for hippo translation node", e);
            }

            if (log.isDebugEnabled()) {
                log.debug("Loaded {} translation(s) for node {} ", translations.size(), JcrUtils.getNodePathQuietly(node));
            }

            return translations;
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
                log.info("Reloading children because {} is expected to be there, but isn't", name);
                reloadChildren();
            }
            ExpandedNode child = children.get(name);
            if (child != null) {
                return child.get(path, index + 1);
            } else {
                log.warn("Unable to find child {} in observable tree model for {}", name, getPath());
                return null;
            }
        }

        void collapse() {
            stopObservation();

            for (ExpandedNode child : children.values()) {
                child.collapse();
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
            Map<String, String> ids = new TreeMap<>();
            for (Map.Entry<String, ExpandedNode> entry : nodes.entrySet()) {
                ids.put(entry.getValue().getId(), entry.getKey());
            }
            return ids;
        }

        void reloadChildren () {
            Map<String, ExpandedNode> newChildren = new TreeMap<>();
            loadChildren(newChildren);

            Map<String, String> newIds = byId(newChildren);
            Map<String, String> oldIds = byId(children);

            for (Map.Entry<String, String> entry : oldIds.entrySet()) {
                final String id = entry.getKey();
                final String name = entry.getValue();
                if (!newIds.containsKey(id)) {
                    final ExpandedNode expandedNode = children.remove(name);
                    expandedNode.stopObservation();
                    expandedNode.detach();
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
        
        void reloadTranslations() {
            Map<String, TranslationNode> newTranslations = loadTranslations(new TreeMap<String, TranslationNode>());
            
            for (String id : translations.keySet()) {
                if (!newTranslations.containsKey(id)) {
                    final ObservableNode translationNode = translations.remove(id);
                    translationNode.stopObservation();
                    translationNode.detach();
                }
            }
            
            for (TranslationNode newTranslationNode : newTranslations.values()) {
                if (!translations.containsKey(newTranslationNode.getId())) {
                    translations.put(newTranslationNode.getId(), newTranslationNode);
                    newTranslationNode.startObservation();
                }
            }
        }

        @Override
        void onEvent() {
            reloadTranslations();
            reloadChildren();
        }

        public void startObservation() {
            super.startObservation();

            for (ObservableNode node : translations.values()) {
                node.startObservation();
            }

            if (expanded) {
                for (ObservableNode node : children.values()) {
                    node.startObservation();
                }
            }
        }

        public void stopObservation() {
            for (ObservableNode node : children.values()) {
                node.stopObservation();
            }
            for (ObservableNode node : translations.values()) {
                node.stopObservation();
            }
            super.stopObservation();
        }
        
        @Override
        public void detach() {

            for (IDetachable detachable : children.values()) {
                detachable.detach();
            }
            for (IDetachable detachable : translations.values()) {
                detachable.detach();
            }
            treeNode.detach();
            super.detach();
        }
    }

    class ObservableNode implements Serializable, IDetachable {

        private String id;
        private String name;
        private String path;
        private IObserver observer;

        public ObservableNode(final JcrNodeModel model) {
            final Node node = model.getObject();

            try {
                id = node.getIdentifier();
            } catch (RepositoryException e) {
                log.error("Could not determine node identifier", e);
                id = "<unknown>";
            }

            try {
                name = node.getName();
            } catch (RepositoryException e) {
                log.error("Could not determine node name", e);
                name = "<unknown>";
            }
            
            path = model.getItemModel().getPath();
        }

        public void startObservation() {
            if (observationContext != null) {
                
                if (observer != null) {
                    log.warn("Observer is still running for {}", path);
                    return;
                }
                
                if (log.isDebugEnabled()) {
                    log.debug("Start observation of node {}", path);
                }

                observer = new IObserver() {
                    @Override
                    public IObservable getObservable() {
                        return new JcrNodeModel(path);
                    }

                    @Override
                    public void onEvent(final Iterator events) {
                        if (log.isDebugEnabled()) {
                            log.debug("Event caught for node {}", path);
                        }
                        ObservableNode.this.onEvent();
                        
                        EventCollection<IEvent<ObservableTreeModel>> treeModelEvents = new EventCollection<>();
                        while (events.hasNext()) {
                            final JcrEvent jcrEvent = (JcrEvent) events.next();
                            final ObservableTreeModelEvent event = createEvent(jcrEvent);
                            treeModelEvents.add(event);
                        }
                        
                        observationContext.notifyObservers(treeModelEvents);
                    }
                };
                observationContext.registerObserver(observer);
            }
        }
        
        public void stopObservation() {
            if (observer != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Stop observation of node {}", path);
                }
                
                observationContext.unregisterObserver(observer);
                observer = null;
            }
        }

        String getId() {
            return id;
        }

        String getName() {
            return name;
        }
        
        String getPath() {
            return path;
        }

        void onEvent() {
        }

        ObservableTreeModelEvent createEvent(final JcrEvent event) {
            return new ObservableTreeModelEvent(event);
        }

        @Override
        public void detach() {
            model.detach();
        }
    }

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
            for (ExpandedNode child : node.children.values()) {
                expandNodes(state, child);
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
                List<Object> nodes = new LinkedList<>();
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
                    log.warn("Path {} is not a descendant of {}", path, rootPath);
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
