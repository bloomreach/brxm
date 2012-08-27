/**
 * Copyright (C) 2012 Hippo
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
package org.hippoecm.frontend.plugins.cms.dev.panels;

import java.io.IOException;
import java.util.Iterator;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.tree.DefaultTreeState;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.model.tree.ObservableTreeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.dev.codemirror.CodeMirrorEditor;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbPanel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.JcrTree;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updater editor
 */
public class UpdaterPanel extends PanelPluginBreadCrumbPanel {

    private final static Logger log = LoggerFactory.getLogger(UpdaterPanel.class);

    private static final String UPDATE_PATH = "/hippo:configuration/hippo:update";
    private static final String UPDATE_QUEUE_PATH = UPDATE_PATH + "/hippo:queue";
    private static final String UPDATE_REGISTRY_PATH = UPDATE_PATH + "/hippo:registry";
    private static final String UPDATE_HISTORY_PATH = UPDATE_PATH + "/hippo:history";

    private static final long DEFAULT_BATCH_SIZE = 10l;
    private static final long DEFAULT_THOTTLE = 1000l;

    private String visitorPath;
    private String visitorQuery;
    private Long batchSize = DEFAULT_BATCH_SIZE;
    private Long throttle = DEFAULT_THOTTLE;
    private Boolean dryRun;
    private String script;

    private final JcrTree tree;
    private final JcrTreeModel treeModel;
    private final Form form;

    private String nodePath;

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        script = "";
        visitorPath = "";
        visitorQuery = "";
        batchSize = 0l;
        throttle = 0l;
        dryRun = false;
        final Node node = (Node) getDefaultModelObject();
        try {
            if (node != null) {
                if (node.isNodeType("hipposys:updaterinfo")) {
                    script = JcrUtils.getStringProperty(node, "hipposys:script", "");
                    visitorPath = JcrUtils.getStringProperty(node, "hipposys:path", "");
                    visitorQuery = JcrUtils.getStringProperty(node, "hipposys:query", "");
                    batchSize = JcrUtils.getLongProperty(node, "hipposys:batchsize", DEFAULT_BATCH_SIZE);
                    throttle = JcrUtils.getLongProperty(node, "hipposys:throttle", DEFAULT_THOTTLE);
                    dryRun = JcrUtils.getBooleanProperty(node, "hipposys:dryrun", false);
                }
                nodePath = node.getPath();
            }
        } catch (RepositoryException e) {
            final String message = "An unexpected error occurred: " + e.getMessage();
            error(message);
            log.error(message, e);
        }

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
        AjaxRequestTarget.get().addComponent(form);
    }

    public UpdaterPanel(final String componentId, final IBreadCrumbModel breadCrumbModel, final IPluginContext context) {
        super(componentId, breadCrumbModel);

        form = new Form("updater-form");
        form.setMultiPart(true);
        form.setOutputMarkupId(true);

        final TextArea<String> scriptEditor = new CodeMirrorEditor("script-editor", new PropertyModel<String>(this, "script"));
        form.add(scriptEditor);

        final TextField<String> pathField = new TextField<String>("path", new PropertyModel<String>(this, "visitorPath"));
        form.add(pathField);

        final TextField<String> queryField = new TextField<String>("query", new PropertyModel<String>(this, "visitorQuery"));
        form.add(queryField);

        final TextField<String> batchSizeField = new TextField<String>("batch-size", new PropertyModel<String>(this, "batchSize"));
        form.add(batchSizeField);

        final TextField<String> throttleField = new TextField<String>("throttle", new PropertyModel<String>(this, "throttle"));
        form.add(throttleField);

        final CheckBox dryRunCheckBox = new CheckBox("dry-run", new PropertyModel<Boolean>(this, "dryRun"));
        form.add(dryRunCheckBox);

        final FileUploadField fileUploadField = new FileUploadField("file-upload");
        form.add(fileUploadField);

        final AjaxButton uploadButton = new AjaxButton("upload-button", form) {
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                uploadScript(fileUploadField);
                target.addComponent(form);
            }
        };
        form.add(uploadButton);

        final Label output = new Label("output", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                final Object o = UpdaterPanel.this.getDefaultModelObject();
                if (o != null) {
                    final Node node = (Node) o;
                    try {
                        final Binary fullLog = JcrUtils.getBinaryProperty(node, "hipposys:log", null);
                        if (fullLog != null) {
                            return IOUtils.toString(fullLog.getStream());
                        } else {
                            return JcrUtils.getStringProperty(node, "hipposys:logtail", "");
                        }
                    } catch (RepositoryException e) {
                        return "Cannot read log: " + e.getMessage();
                    } catch (IOException e) {
                        return "Cannot read log: " + e.getMessage();
                    }
                }
                return "";
            }
        });
        output.setOutputMarkupId(true);
        output.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5)));
        form.add(output);

        final JcrTreeNode jcrTreeNode = new JcrTreeNode(new JcrNodeModel(UPDATE_PATH), null);
        treeModel = new JcrTreeModel(jcrTreeNode) {
            @Override
            public void onEvent(final Iterator<? extends IEvent<ObservableTreeModel>> iter) {
                super.onEvent(iter);
                AjaxRequestTarget.get().addComponent(tree);
            }

            @Override
            protected TreeModelEvent newTreeModelEvent(final Event event) throws RepositoryException {
                // FIXME: hack to observe the jcr events
                // what we do here is to follow the updater node as it is moved from the queue to the history
                // by the updater executor when it is finished executing the updater
                if (event.getType() == Event.NODE_MOVED) {
                    final Object srcAbsPath = event.getInfo().get("srcAbsPath");
                    if (srcAbsPath.equals(nodePath)) {
                        final Session session = UserSession.get().getJcrSession();
                        try {
                            final Node node = session.getNode((String) event.getInfo().get("destAbsPath"));
                            UpdaterPanel.this.setDefaultModel(new JcrNodeModel(node));
                        } catch (RepositoryException e) {
                            log.error(e.getClass().getName() + " : " + e.getMessage());
                        }
                    }
                }
                return super.newTreeModelEvent(event);
            }
        };
        context.registerService(treeModel, IObserver.class.getName());
        tree = new JcrTree("updater-tree", treeModel) {

            @Override
            protected void onNodeLinkClicked(final AjaxRequestTarget target, final TreeNode clickedNode) {
                UpdaterPanel.this.setDefaultModel(((JcrTreeNode) clickedNode).getChainedModel());
            }

            @Override
            protected ITreeState newTreeState() {
                DefaultTreeState state = new DefaultTreeState();
                JcrTreeModel model = (JcrTreeModel) getModelObject();
                model.setTreeState(state);
                return state;
            }

            @Override
            public void onBeforeRender() {
                super.onBeforeRender();
                updateTree();
            }
        };
        tree.setOutputMarkupId(true);
        add(tree);

        form.add(new AjaxButton("new-button", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> currentForm) {
                newUpdater();
                target.addComponent(tree);
            }
        });

        final AjaxButton executeButton = new AjaxButton("execute-button", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> currentForm) {
                executeUpdater();
                target.addComponent(tree);
            }
        };
        form.add(executeButton);

        final AjaxButton pauseResumeButton = new AjaxButton("pause-resume-button", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return isPaused() ? "Resume" : "Pause";
            }
        }, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> currentForm) {
                pauseOrResumeUpdater();
                target.addComponent(form);
            }
        };
        form.add(pauseResumeButton);

        final AjaxButton stopButton = new AjaxButton("stop-button", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> currentForm) {
                cancelUpdater();
                target.addComponent(tree);
            }
        };
        form.add(stopButton);

        final AjaxButton saveButton = new AjaxButton("save-button", form) {
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                saveUpdater();
            }
        };
        form.add(saveButton);

        final AjaxButton deleteButton = new AjaxButton("delete-button", form) {
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                deleteUpdater();
                target.addComponent(tree);
            }
        };
        form.add(deleteButton);

        add(form);

    }

    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("updater-panel-title");
    }

    private void uploadScript(final FileUploadField fileUploadField) {
        final FileUpload upload = fileUploadField.getFileUpload();
        if (upload != null) {
            try {
                script = IOUtils.toString(upload.getInputStream());
            } catch (IOException e) {
                final String message = "An unexpected error occurred: " + e.getMessage();
                error(message);
                log.error(message, e);
            }
        }
    }

    private void newUpdater() {
        final Session session = UserSession.get().getJcrSession();
        try {
            final Node registry = session.getNode(UPDATE_REGISTRY_PATH);
            final Node node = registry.addNode("new", "hipposys:updaterinfo");
            node.setProperty("hipposys:script", IOUtils.toString(UpdaterPanel.class.getResource("UpdaterTemplate.groovy").openStream()));
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

    private void executeUpdater() {
        final Node node = (Node) getDefaultModelObject();
        final Session session = UserSession.get().getJcrSession();
        if (node != null) {
            try {
                final String srcPath = node.getPath();
                if (srcPath.startsWith(UPDATE_REGISTRY_PATH)) {
                    final String destPath = UPDATE_QUEUE_PATH + "/" + node.getName();
                    JcrUtils.copy(session, srcPath, destPath);
                    final Node queuedNode = session.getNode(destPath);
                    queuedNode.setProperty("hipposys:startedby", session.getUserID());
                    session.save();
                    setDefaultModel(new JcrNodeModel(queuedNode));
                }
            } catch (RepositoryException e) {
                final String message = "An unexpected error occurred: " + e.getMessage();
                error(message);
                log.error(message, e);
            }
        }
    }

    private void pauseOrResumeUpdater() {
        final Node node = (Node) getDefaultModelObject();
        final Session session = UserSession.get().getJcrSession();
        if (node != null) {
            try {
                final String path = node.getPath();
                if (path.startsWith(UPDATE_QUEUE_PATH)) {
                    boolean isPaused = JcrUtils.getBooleanProperty(node, "hipposys:paused", false);
                    node.setProperty("hipposys:paused", !isPaused);
                    session.save();
                }
            } catch (RepositoryException e) {
                final String message = "An unexpected error occurred: " + e.getMessage();
                error(message);
                log.error(message, e);
            }
        }
    }

    private boolean isPaused() {
        final Node node = (Node) getDefaultModelObject();
        if (node != null) {
            try {
                final String path = node.getPath();
                if (path.startsWith(UPDATE_QUEUE_PATH)) {
                    return JcrUtils.getBooleanProperty(node, "hipposys:paused", false);
                }
            } catch (RepositoryException e) {
                final String message = "An unexpected error occurred: " + e.getMessage();
                error(message);
                log.error(message, e);
            }
        }
        return false;
    }

    private void cancelUpdater() {
        final Node node = (Node) getDefaultModelObject();
        final Session session = UserSession.get().getJcrSession();
        if (node != null) {
            try {
                node.setProperty("hipposys:cancelled", true);
                node.setProperty("hipposys:cancelledby", session.getUserID());
                session.save();
            } catch (RepositoryException e) {
                final String message = "An unexpected error occurred: " + e.getMessage();
                error(message);
                log.error(message, e);
            }
        }
    }

    private void deleteUpdater() {
        final Node node = (Node) getDefaultModelObject();
        final Session session = UserSession.get().getJcrSession();
        if (node != null) {
            try {
                final Node siblingOrParent = getSiblingOrParent(node);
                node.remove();
                session.save();
                setDefaultModel(new JcrNodeModel(siblingOrParent));
            } catch (RepositoryException e) {
                final String message = "An unexpected error occurred: " + e.getMessage();
                error(message);
                log.error(message, e);
            }
        }
    }

    private void saveUpdater() {
        final Node node = (Node) getDefaultModelObject();
        try {
            node.setProperty("hipposys:path", visitorPath);
            node.setProperty("hipposys:query", visitorQuery);
            node.setProperty("hipposys:batchsize", batchSize);
            node.setProperty("hipposys:throttle", throttle);
            node.setProperty("hipposys:dryrun", dryRun);
            node.setProperty("hipposys:script", script);
            node.getSession().save();
        } catch (RepositoryException e) {
            final String message = "An unexpected error occurred: " + e.getMessage();
            error(message);
            log.error(message, e);
        }
    }

    private Node getSiblingOrParent(Node node) throws RepositoryException {
        final Node parent = node.getParent();
        final NodeIterator nodes = parent.getNodes();
        Node sibling = null;
        while (nodes.hasNext()) {
            final Node nextNode = nodes.nextNode();
            if (node.isSame(nextNode)) {
                if (nodes.hasNext()) {
                    return nodes.nextNode();
                } else if (sibling != null) {
                    return sibling;
                }
            }
            sibling = nextNode;
        }
        return parent;
    }
}
