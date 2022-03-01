/*
 * Copyright 2012-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.updater;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.tree.DefaultTreeState;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.io.IOUtils;
import org.hippoecm.frontend.ajax.BrSubmit;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.SystemPanel;
import org.hippoecm.frontend.plugins.cms.browse.tree.CmsJcrTree;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.tree.icon.DefaultTreeNodeIconProvider;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_PATH;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_SCRIPT;

public class UpdaterPanel extends SystemPanel {

    private final static Logger log = LoggerFactory.getLogger(UpdaterPanel.class);

    private static final String UPDATE_PATH = "/hippo:configuration/hippo:update";
    private static final String UPDATE_QUEUE_PATH = UPDATE_PATH + "/hippo:queue";
    private static final String UPDATE_REGISTRY_PATH = UPDATE_PATH + "/hippo:registry";
    private static final String UPDATE_HISTORY_PATH = UPDATE_PATH + "/hippo:history";

    private static final Label EMPTY_EDITOR = new Label("updater-editor");
    private static final Map<String, String> CUSTOM_NODE_LABELS = createNodeNameMap();

    private final FeedbackPanel feedback;


    private static Map<String, String> createNodeNameMap() {
        final Map<String, String> map = new HashMap<>();
        map.put("hippo:registry", "Registry");
        map.put("hippo:queue", "Queue");
        map.put("hippo:history", "History");
        return Collections.unmodifiableMap(map);
    }

    private final IPluginContext context;

    private final UpdaterTree tree;
    private final JcrTreeModel treeModel;
    private final Label title;

    private Component editor;
    private String path;

    private final Form form;

    private class UpdaterTree extends CmsJcrTree {

        public UpdaterTree(final String id, final JcrTreeModel treeModel, final ITreeNodeTranslator treeNodeTranslator,
                           final ITreeNodeIconProvider iconService) {
            super(id, treeModel, treeNodeTranslator, iconService);
        }

        @Override
        protected void populateTreeItem(final WebMarkupContainer item, final int level) {
            super.populateTreeItem(item, level);
            final Component nodeLink = item.get("nodeLink");
            if (nodeLink != null) {
                nodeLink.add(ClassAttribute.append("node-link"));
            }
        }

        @Override
        protected void onNodeLinkClicked(final AjaxRequestTarget target, final TreeNode clickedNode) {
            if (clickedNode instanceof IJcrTreeNode) {
                final ITreeState state = getTreeState();
                if (state.isNodeExpanded(clickedNode)) {
                    // super has already switched selection.
                    if (!state.isNodeSelected(clickedNode)) {
                        state.collapseNode(clickedNode);
                    }
                } else {
                    state.expandNode(clickedNode);
                }

                final IJcrTreeNode treeNodeModel = (IJcrTreeNode) clickedNode;
                UpdaterPanel.this.setDefaultModel(treeNodeModel.getNodeModel());
            }
        }

        @Override
        protected void onJunctionLinkClicked(final AjaxRequestTarget target, final TreeNode node) {
            updateTree(target);
        }

        @Override
        protected ITreeState newTreeState() {
            final DefaultTreeState state = new DefaultTreeState();
            treeModel.setTreeState(state);
            state.expandAll();
            return state;
        }

        @Override
        public String renderNode(final TreeNode treeNode, final int level) {
            final String customNodeName = getCustomNodeName((IJcrTreeNode) treeNode);
            return StringUtils.isEmpty(customNodeName) ? super.renderNode(treeNode, level) : customNodeName;
        }

        private String getCustomNodeName(final IJcrTreeNode treeNode) {
            final Node node = treeNode.getNodeModel().getObject();
            if (node != null) {
                try {
                    final String nodeName = node.getName();
                    final String label = CUSTOM_NODE_LABELS.get(nodeName);
                    if (label != null) {
                        return label;
                    }
                } catch (RepositoryException e) {
                    log.error("Failed to render tree node", e);
                }
            }
            return null;
        }

        /**
         * Expand all children of root
         */
        public void expandChildrenOfRoot() {
            final ITreeState state = tree.getTreeState();
            final TreeModel thisTreeModel = (TreeModel) getDefaultModel().getObject();
            final TreeNode root = (TreeNode) thisTreeModel.getRoot();
            if (root != null) {
                Collections.list(root.children()).forEach(state::expandNode);
            }
        }
    }

    public UpdaterPanel(final String componentId, final IPluginContext context) {
        super(componentId);

        this.context = context;

        form = new HippoForm("new-form");
        final AjaxButton newButton = new BrSubmit("new-button") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                newUpdater();
            }
        };

        form.add(newButton);

        add(form);

        final FeedbackPanel feedbackPanel = new FeedbackPanel("feedback", new ContainerFeedbackMessageFilter(UpdaterPanel.this) {
            @Override
            public boolean accept(final FeedbackMessage message1) {
                return !message1.isRendered() && super.accept(message1);
            }
        });
        feedbackPanel.setOutputMarkupId(true);
        feedback = feedbackPanel;
        add(feedback);
        // customize feedbackpanel to display only messages from hippoform
        feedback.setFilter(message -> message.getReporter() == this.form);


        treeModel = new JcrTreeModel(new JcrTreeNode(new JcrNodeModel(UPDATE_PATH), null)) {
            @Override
            protected TreeModelEvent newTreeModelEvent(final Event event) throws RepositoryException {
                if (StringUtils.equals(event.getPath(), getNodePath())) {
                    updateUI();
                }
                return super.newTreeModelEvent(event);
            }
        };
        context.registerService(treeModel, IObserver.class.getName());

        tree = new UpdaterTree("updater-tree", treeModel, newTreeNodeTranslator(), newTreeNodeIconProvider());
        tree.setRootLess(true);
        tree.setOutputMarkupId(true);
        add(tree);

        tree.expandChildrenOfRoot();

        editor = EMPTY_EDITOR;
        editor.setOutputMarkupId(true);
        add(editor);

        title = new Label("updater-title", this::getUpdaterTitle);
        title.setOutputMarkupId(true);
        add(title);

        setOutputMarkupId(true);
    }

    private ITreeNodeIconProvider newTreeNodeIconProvider() {
        return new DefaultTreeNodeIconProvider() {
            @Override
            public HippoIcon getNodeIcon(final String id, final TreeNode treeNode, final ITreeState state) {
                final IModel<Node> nodeModel = ((IJcrTreeNode) treeNode).getNodeModel();
                if (isUpdater(nodeModel.getObject())) {
                    return HippoIcon.fromSprite(id, Icon.FILE_TEXT);
                } else {
                    return super.getNodeIcon(id, treeNode, state);
                }
            }
        };
    }

    private CmsJcrTree.ITreeNodeTranslator newTreeNodeTranslator() {
        return new CmsJcrTree.TreeNodeTranslator();
    }

    private String getUpdaterTitle() {
        if (isQueuedUpdater()) {
            return "Monitoring updater run '" + getUpdaterName() + "'";
        }
        if (isRegisteredUpdater()) {
            return "Editing updater '" + getUpdaterName() + "'";
        }
        if (isArchivedUpdater()) {
            return "Viewing updater run '" + getUpdaterName() + "'";
        }
        return getTitle(null).getObject();
    }

    public IModel<String> getTitle(final Component component) {
        return Model.of(getString("updater-editor-title"));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        updateUI();
    }

    private void updateUI() {
        final Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        target.ifPresent(ajaxRequestTarget -> {
            expandAndSelectNodeInTree(ajaxRequestTarget);
            updateEditor(ajaxRequestTarget);
            ajaxRequestTarget.add(title);
        });

        path = null;
    }

    private void updateEditor(final AjaxRequestTarget target) {
        if (isQueuedUpdater()) {
            replace(editor = new UpdaterQueueEditor(getDefaultModel(), context, this));
        } else if (isRegisteredUpdater()) {
            replace(editor = new UpdaterRegistryEditor(getDefaultModel(), context, this));
        } else if (isArchivedUpdater()) {
            replace(editor = new UpdaterHistoryEditor(getDefaultModel(), context, this));
        } else {
            if (editor != EMPTY_EDITOR) {
                replace(editor = EMPTY_EDITOR);
            }
        }
        target.add(editor);
    }

    private void expandAndSelectNodeInTree(final AjaxRequestTarget target) {
        final JcrNodeModel model = (JcrNodeModel) getDefaultModel();
        final TreePath treePath = treeModel.lookup(model);
        final ITreeState treeState = tree.getTreeState();
        for (final Object n : treePath.getPath()) {
            final TreeNode treeNode = (TreeNode) n;
            if (!treeState.isNodeExpanded(treeNode)) {
                treeState.expandNode(treeNode);
            }
        }
        treeState.selectNode(treePath.getLastPathComponent(), true);
        tree.updateTree(target);
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

    private static boolean isUpdater(final Node node) {
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
        } catch (RepositoryException | IOException e) {
            final String message = "An unexpected error occurred: " + e.getMessage();
            showError(message);
            log.error(message, e);
        }
    }

    private void showError(final String message) {
        form.error(message);
    }


    private Node addUpdater(final Node registry, final int index) throws IOException, RepositoryException {
        try {
            final Node node = registry.addNode("new-" + index, "hipposys:updaterinfo");
            node.setProperty(HIPPOSYS_SCRIPT, IOUtils.toString(UpdaterEditor.class.getResource("UpdaterTemplate.groovy").openStream()));
            node.setProperty(HIPPOSYS_PATH, "/");
            return node;
        } catch (ItemExistsException e) {
            return addUpdater(registry, index + 1);
        }
    }

}
