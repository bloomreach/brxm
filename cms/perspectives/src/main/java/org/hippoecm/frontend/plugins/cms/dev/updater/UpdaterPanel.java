/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.dev.updater;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModelListener;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.markup.html.tree.DefaultTreeState;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IOUtils;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbPanel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.JcrTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdaterPanel extends PanelPluginBreadCrumbPanel {

    private final static Logger log = LoggerFactory.getLogger(UpdaterPanel.class);

    private static final String UPDATE_PATH = "/hippo:configuration/hippo:update";
    private static final String UPDATE_QUEUE_PATH = UPDATE_PATH + "/hippo:queue";
    private static final String UPDATE_REGISTRY_PATH = UPDATE_PATH + "/hippo:registry";
    private static final String UPDATE_HISTORY_PATH = UPDATE_PATH + "/hippo:history";

    private static final Label EMPTY_EDITOR = new Label("updater-editor");
    private static final Map<String, String> TREE_LABELS = new HashMap<String, String>(3);

    static {
        TREE_LABELS.put("hippo:registry", "Registry");
        TREE_LABELS.put("hippo:queue", "Queue");
        TREE_LABELS.put("hippo:history", "History");
    }

    private final IPluginContext context;

    private final JcrTree tree;
    private final JcrTreeModel treeModel;

    private Component editor;
    private String path;

    public UpdaterPanel(final String componentId, final IBreadCrumbModel breadCrumbModel, final IPluginContext context) {
        super(componentId, breadCrumbModel);

        this.context = context;

        final Form form = new Form("new-form");
        final AjaxButton newButton = new AjaxButton("new-button") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> currentForm) {
                newUpdater();
            }
        };
        form.add(newButton);

        add(form);

        treeModel = new JcrTreeModel(new JcrTreeNode(new JcrNodeModel(UPDATE_PATH), null)) {

            @Override
            protected TreeModelEvent newTreeModelEvent(final Event event) throws RepositoryException {
                if (event.getPath().equals(getNodePath())) {
                    updateUI();
                }
                return super.newTreeModelEvent(event);
            }
        };
        context.registerService(treeModel, IObserver.class.getName());

        breadCrumbModel.addListener(new IBreadCrumbModelListener() {
            @Override
            public void breadCrumbActivated(final IBreadCrumbParticipant previousParticipant, final IBreadCrumbParticipant breadCrumbParticipant) {
            }

            @Override
            public void breadCrumbAdded(final IBreadCrumbParticipant breadCrumbParticipant) {
            }

            @Override
            public void breadCrumbRemoved(final IBreadCrumbParticipant breadCrumbParticipant) {
                if (breadCrumbParticipant == UpdaterPanel.this) {
                    breadCrumbModel.removeListener(this);
                    context.unregisterService(treeModel, IObserver.class.getName());
                }
            }
        });

        tree = new JcrTree("updater-tree", treeModel) {

            @Override
            protected void populateTreeItem(final WebMarkupContainer item, final int level) {
                super.populateTreeItem(item, level);
                final Component nodeLink = item.get("nodeLink");
                if (nodeLink != null) {
                    nodeLink.add(new AttributeModifier("class", "node-link"));
                }
            }

            @Override
            protected void onNodeLinkClicked(final AjaxRequestTarget target, final TreeNode clickedNode) {
                UpdaterPanel.this.setDefaultModel(((IJcrTreeNode) clickedNode).getNodeModel());
            }

            @Override
            protected ITreeState newTreeState() {
                DefaultTreeState state = new DefaultTreeState();
                treeModel.setTreeState(state);
                state.expandAll();
                return state;
            }

            @Override
            protected ResourceReference getNodeIcon(final TreeNode node) {
                final IModel<Node> nodeModel = ((IJcrTreeNode) node).getNodeModel();
                if (nodeModel == null) {
                    return super.getNodeIcon(node);
                }
                return isUpdater(nodeModel.getObject()) ? super.getItem() : super.getFolderOpen();
            }

            @Override
            protected Component newJunctionLink(final MarkupContainer parent, final String id, final String imageId, final TreeNode node) {
                final MarkupContainer junctionLink = new WebMarkupContainer(id) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        tag.put("onclick", "return false");
                    }
                };
                junctionLink.add(new Label("image"));

                return junctionLink;
            }

            @Override
            public String renderNode(final TreeNode treeNode) {
                Node node = ((IJcrTreeNode) treeNode).getNodeModel().getObject();
                if (node != null) {
                    try {
                        final String nodeName = node.getName();
                        final String label = TREE_LABELS.get(nodeName);
                        return label != null ? label : nodeName;
                    } catch (RepositoryException e) {
                        log.error("Failed to render tree node", e);
                        return "<error>";
                    }
                }
                return "<null>";
            }
        };
        tree.setRootLess(true);
        tree.setOutputMarkupId(true);
        add(tree);

        editor = EMPTY_EDITOR;
        add(editor);

        final Label title = new Label("updater-title", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return getUpdaterTitle();
            }
        });
        add(title);

        setOutputMarkupId(true);
    }

    private String getUpdaterTitle() {
        if (isQueuedUpdater()) {
            return "Monitoring updater run " + getUpdaterName();
        }
        if (isRegisteredUpdater()) {
            return "Editing updater " + getUpdaterName();
        }
        if (isArchivedUpdater()) {
            return "Viewing updater run " + getUpdaterName();
        }
        return getTitle(null).getObject();
    }

    public IModel<String> getTitle(final Component component) {
        return new StringResourceModel("updater-editor-title", this, null);
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        updateUI();
    }

    private void updateUI() {
        expandAndSelectNodeInTree();
        updateEditor();
        path = null;
        RequestCycle.get().find(AjaxRequestTarget.class).add(this);
    }

    private void updateEditor() {
        if (isQueuedUpdater()) {
            replace(editor = new UpdaterQueueEditor(getDefaultModel(), context, this));
        }
        else if (isRegisteredUpdater()) {
            replace(editor = new UpdaterRegistryEditor(getDefaultModel(), context, this));
        }
        else if (isArchivedUpdater()) {
            replace(editor = new UpdaterHistoryEditor(getDefaultModel(), context, this));
        }
        else {
            if (editor != EMPTY_EDITOR) {
                replace(editor = EMPTY_EDITOR);
            }
        }
    }

    private void expandAndSelectNodeInTree() {
        final JcrNodeModel model = (JcrNodeModel) getDefaultModel();
        final TreePath treePath = treeModel.lookup(model);
        final ITreeState treeState = tree.getTreeState();
        for (Object n : treePath.getPath()) {
            final TreeNode treeNode = (TreeNode) n;
            if (!treeState.isNodeExpanded(treeNode)) {
                treeState.expandNode(treeNode);
            }
        }
        treeState.selectNode(treePath.getLastPathComponent(), true);
    }

    private String getNodePath() {
        final Node node = (Node) getDefaultModelObject();
        try {
            if (node != null) {
                path = node.getPath();
            }
        } catch (RepositoryException e) {
            log.error("Error while getting node path", e);
        }
        return path != null ? path : "";
    }

    public String getUpdaterName() {
        final Node node = (Node) getDefaultModelObject();
        try {
            if (node != null) {
                return node.getName();
            }
        } catch (RepositoryException e) {
            log.error("Failed to get name of updater");
        }
        return "";
    }

    private boolean isRegisteredUpdater() {
        return getNodePath().startsWith(UPDATE_REGISTRY_PATH + "/");
    }

    private boolean isQueuedUpdater() {
        return getNodePath().startsWith(UPDATE_QUEUE_PATH + "/");
    }

    private boolean isArchivedUpdater() {
        return getNodePath().startsWith(UPDATE_HISTORY_PATH + "/");
    }

    private static boolean isUpdater(Node node) {
        if (node != null) {
            try {
                return node.isNodeType("hipposys:updaterinfo");
            } catch (RepositoryException e) {
                log.error("Failed to determine whether node is updater node", e);
            }
        }
        return false;
    }

    private void newUpdater() {
        final Session session = UserSession.get().getJcrSession();
        try {
            final Node registry = session.getNode(UPDATE_REGISTRY_PATH);
            final Node node = addUpdater(registry, 1);
            session.save();
            setDefaultModel(new JcrNodeModel(node));
        } catch (RepositoryException e) {
            final String message = "An unexpected error occurred: " + e.getMessage();
            error(message);
            log.error(message, e);
        } catch (IOException e) {
            final String message = "An unexpected error occurred: " + e.getMessage();
            error(message);
            log.error(message, e);
        }
    }

    private Node addUpdater(final Node registry, int index) throws IOException, RepositoryException {
        try {
            final Node node = registry.addNode("new-" + index, "hipposys:updaterinfo");
            node.setProperty("hipposys:script", IOUtils.toString(UpdaterEditor.class.getResource("UpdaterTemplate.groovy").openStream()));
            node.setProperty("hipposys:path", "/");
            return node;
        } catch (ItemExistsException e) {
            return addUpdater(registry, index+1);
        }
    }

}
