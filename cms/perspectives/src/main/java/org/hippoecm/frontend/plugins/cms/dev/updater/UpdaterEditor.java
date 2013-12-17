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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.dev.codemirror.CodeMirrorEditor;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdaterEditor extends Panel {

    protected final static Logger log = LoggerFactory.getLogger(UpdaterEditor.class);

    protected static final String UPDATE_PATH = "/hippo:configuration/hippo:update";
    protected static final String UPDATE_QUEUE_PATH = UPDATE_PATH + "/hippo:queue";
    protected static final String UPDATE_REGISTRY_PATH = UPDATE_PATH + "/hippo:registry";
    protected static final String UPDATE_HISTORY_PATH = UPDATE_PATH + "/hippo:history";

    private static final long DEFAULT_BATCH_SIZE = 10l;
    private static final long DEFAULT_THOTTLE = 1000l;
    private static final String DEFAULT_METHOD = "path";

    protected final IPluginContext context;
    protected final Panel container;
    protected final Form form;
    protected final FeedbackPanel feedback;
    protected String script;

    protected String name;
    protected String visitorPath;
    protected String visitorQuery;
    protected String method = DEFAULT_METHOD;
    protected String batchSize = String.valueOf(DEFAULT_BATCH_SIZE);
    protected String throttle = String.valueOf(DEFAULT_THOTTLE);
    protected boolean dryRun = false;

    public UpdaterEditor(IModel<?> model, final IPluginContext context, Panel container) {
        super("updater-editor", model);
        this.context = context;
        this.container = container;

        form = new Form("updater-form");

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        final AjaxButton executeButton = new AjaxButton("execute-button") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> currentForm) {
                executeUpdater(false);
                tryRenderFeedback(target);
            }

            @Override
            public boolean isEnabled() {
                return isExecuteButtonEnabled();
            }

            @Override
            public boolean isVisible() {
                return isExecuteButtonVisible();
            }
        };
        executeButton.setOutputMarkupId(true);
        form.add(executeButton);

        final AjaxButton undoButton = new AjaxButton("undo-button") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> currentForm) {
                executeUndo();
                tryRenderFeedback(target);
            }

            @Override
            public boolean isEnabled() {
                return isUndoButtonEnabled();
            }

            @Override
            public boolean isVisible() {
                return isUndoButtonVisible();
            }
        };
        undoButton.setOutputMarkupId(true);
        form.add(undoButton);

        final AjaxButton dryRunButton = new AjaxButton("dryrun-button") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> currentForm) {
                executeUpdater(true);
                tryRenderFeedback(target);
            }

            @Override
            public boolean isEnabled() {
                return isExecuteButtonEnabled();
            }

            @Override
            public boolean isVisible() {
                return isExecuteButtonVisible();
            }
        };
        dryRunButton.setOutputMarkupId(true);
        form.add(dryRunButton);

        final AjaxButton saveButton = new AjaxButton("save-button") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                saveUpdater();
                target.add(executeButton);
                target.add(dryRunButton);
                tryRenderFeedback(target);
            }

            @Override
            public boolean isEnabled() {
                return isSaveButtonEnabled();
            }

            @Override
            public boolean isVisible() {
                return isSaveButtonVisible();
            }
        };
        form.add(saveButton);

        final AjaxButton stopButton = new AjaxButton("stop-button") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> currentForm) {
                stopUpdater();
                tryRenderFeedback(target);
            }

            @Override
            public boolean isEnabled() {
                return isStopButtonEnabled();
            }

            @Override
            public boolean isVisible() {
                return isStopButtonVisible();
            }
        };
        form.add(stopButton);

        final AjaxButton deleteButton = new AjaxButton("delete-button") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                deleteUpdater();
            }

            @Override
            public boolean isEnabled() {
                return isDeleteButtonEnabled();
            }

            @Override
            public boolean isVisible() {
                return isDeleteButtonVisible();
            }
        };
        form.add(deleteButton);

        final RadioGroup<String> radios = new RadioGroup<String>("radios", new PropertyModel<String>(this, "method"));
        form.add(radios);

        final LabelledInputFieldTableRow nameField = new LabelledInputFieldTableRow("name", new Model<String>("Name"), new PropertyModel<String>(this, "name")) {
            @Override
            public boolean isEnabled() {
                return isNameFieldEnabled();
            }

            @Override
            public boolean isVisible() {
                return isNameFieldVisible();
            }
        };
        radios.add(nameField);

        final RadioLabelledInputFieldTableRow pathField = new RadioLabelledInputFieldTableRow("path", radios, new Model<String>("Path"), new PropertyModel<String>(this, "visitorPath")) {
            @Override
            public boolean isEnabled() {
                return isPathFieldEnabled();
            }

            @Override
            public boolean isVisible() {
                return isPathFieldVisible();
            }

            @Override
            protected boolean isRadioVisible() {
                return UpdaterEditor.this.isRadioVisible();
            }
        };
        radios.add(pathField);

        final RadioLabelledInputFieldTableRow queryField = new RadioLabelledInputFieldTableRow("query", radios, new Model<String>("Query"), new PropertyModel<String>(this, "visitorQuery")) {
            @Override
            public boolean isEnabled() {
                return isQueryFieldEnabled();
            }

            @Override
            public boolean isVisible() {
                return isQueryFieldVisible();
            }

            @Override
            protected boolean isRadioVisible() {
                return UpdaterEditor.this.isRadioVisible();
            }
        };
        radios.add(queryField);

        final LabelledInputFieldTableRow batchSizeField = new LabelledInputFieldTableRow("batch-size", new Model<String>("Batch Size"), new PropertyModel<String>(this, "batchSize")) {
            @Override
            public boolean isEnabled() {
                return isBatchSizeFieldEnabled();
            }

            @Override
            public boolean isVisible() {
                return isBatchSizeFieldVisible();
            }

        };
        radios.add(batchSizeField);

        final LabelledInputFieldTableRow throttleField = new LabelledInputFieldTableRow("throttle", new Model<String>("Throttle (ms)"), new PropertyModel<String>(this, "throttle")) {
            @Override
            public boolean isEnabled() {
                return isThrottleFieldEnabled();
            }

            @Override
            public boolean isVisible() {
                return isThrottleFieldVisible();
            }
        };
        radios.add(throttleField);

        final LabelledCheckBoxTableRow dryRunCheckBox = new LabelledCheckBoxTableRow("dryrun", new Model<String>("Dry run"), new PropertyModel<Boolean>(this, "dryRun")) {
            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public boolean isVisible() {
                return isDryRunCheckBoxVisible();
            }
        };
        radios.add(dryRunCheckBox);

        script = getStringProperty(HippoNodeType.HIPPOSYS_SCRIPT, null);

        final TextArea<String> scriptEditor = new CodeMirrorEditor("script-editor", getEditorName(), new PropertyModel<String>(this, "script"));
        form.add(scriptEditor);

        final Component updaterOutput = createOutputComponent("updater-output");
        form.add(updaterOutput);

        add(form);

        setOutputMarkupId(true);

        loadProperties();
    }

    private void tryRenderFeedback(AjaxRequestTarget target) {
        if (feedback.findParent(Page.class) != null) {
            target.add(feedback);
        }
    }

    @Override
    protected void onDetach() {
        form.detach();
        super.onDetach();
    }

    protected void loadProperties() {
        script = getStringProperty(HippoNodeType.HIPPOSYS_SCRIPT, null);
        if (isUpdater()) {
            name = getName();
        } else {
            name = null;
        }
        visitorPath = getStringProperty(HippoNodeType.HIPPOSYS_PATH, null);
        visitorQuery = getStringProperty(HippoNodeType.HIPPOSYS_QUERY, null);
        batchSize = String.valueOf(getLongProperty(HippoNodeType.HIPPOSYS_BATCHSIZE, DEFAULT_BATCH_SIZE));
        throttle = String.valueOf(getLongProperty(HippoNodeType.HIPPOSYS_THROTTLE, DEFAULT_THOTTLE));
        if (visitorQuery != null) {
            method = "query";
        }
        if (visitorPath != null) {
            method = "path";
        }
        dryRun = getBooleanProperty(HippoNodeType.HIPPOSYS_DRYRUN, false);
    }

    private String getName() {
        final Node node = (Node) getDefaultModelObject();
        if (node != null) {
            try {
                return node.getName();
            } catch (RepositoryException e) {
                log.error("Failed to retrieve node name", e);
            }
        }
        return null;
    }

    protected final String getStringProperty(String propertyName, String defaultValue) {
        final Node node = (Node) getDefaultModelObject();
        if (node != null) {
            try {
                return JcrUtils.getStringProperty(node, propertyName, defaultValue);
            } catch (RepositoryException e) {
                log.error("Failed to retrieve property {}", propertyName, e);
            }
        }
        return defaultValue;
    }

    protected final boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        final Node node = (Node) getDefaultModelObject();
        if (node != null) {
            try {
                return JcrUtils.getBooleanProperty(node, propertyName, defaultValue);
            } catch (RepositoryException e) {
                log.error("Failed to retrieve property {}", propertyName, e);
            }
        }
        return defaultValue;
    }

    protected final Long getLongProperty(String propertyName, Long defaultValue) {
        final Node node = (Node) getDefaultModelObject();
        if (node != null) {
            try {
                return JcrUtils.getLongProperty(node, propertyName, defaultValue);
            } catch (RepositoryException e) {
                log.error("Failed to retrieve property {}", propertyName, e);
            }
        }
        return defaultValue;
    }

    protected boolean isUpdater() {
        Node node = (Node) getDefaultModelObject();
        if (node != null) {
            try {
                return node.isNodeType("hipposys:updaterinfo");
            } catch (RepositoryException e) {
                log.error("Failed to determine whether node is updater node", e);
            }
        }
        return false;
    }

    protected Component createOutputComponent(String id) {
        return new Label(id);
    }

    protected void deleteUpdater() {
        IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
        dialogService.show(new DeleteUpdaterDialog(getDefaultModel(), container));
    }

    private boolean saveUpdater() {
        final Node node = (Node) getDefaultModelObject();
        try {
            if (!validateName()) {
                return false;
            }
            if (isPathMethod()) {
                if (!validateVisitorPath()) {
                    return false;
                }
                node.setProperty(HippoNodeType.HIPPOSYS_PATH, visitorPath);
                node.setProperty(HippoNodeType.HIPPOSYS_QUERY, (String) null);
            } else {
                if (!validateVisitorQuery()) {
                    return false;
                }
                node.setProperty(HippoNodeType.HIPPOSYS_QUERY, visitorQuery);
                node.setProperty(HippoNodeType.HIPPOSYS_PATH, (String) null);
            }
            node.setProperty(HippoNodeType.HIPPOSYS_DRYRUN, dryRun);
            if (!validateBatchSize()) {
                return false;
            }
            node.setProperty(HippoNodeType.HIPPOSYS_BATCHSIZE, Long.valueOf(batchSize));
            if (!validateThrottle()) {
                return false;
            }
            node.setProperty(HippoNodeType.HIPPOSYS_THROTTLE, Long.valueOf(throttle));
            node.setProperty(HippoNodeType.HIPPOSYS_SCRIPT, script);
            if (!node.getName().equals(name)) {
                rename();
            }
            node.getSession().save();
            return true;
        } catch (RepositoryException e) {
            final String message = "An unexpected error occurred: " + e.getMessage();
            error(message);
            log.error(message, e);
        }
        return false;
    }

    private boolean validateName() {
        if (name == null || name.isEmpty()) {
            error("Name is empty");
            return false;
        }
        return true;
    }

    private boolean validateThrottle() {
        try {
            Long.valueOf(throttle);
            return true;
        } catch (NumberFormatException e) {
            error("Throttle must be a positive integer");
            return false;
        }
    }

    private boolean validateBatchSize() {
        try {
            Long.valueOf(batchSize);
            return true;
        } catch(NumberFormatException e) {
            error("Batch size must be a positive integer");
            return false;
        }
    }

    private boolean validateVisitorPath() throws RepositoryException {
        if (visitorPath == null || visitorPath.isEmpty()) {
            error("Path is empty");
            return false;
        }
        final Session session = UserSession.get().getJcrSession();
        try {
            if (!session.nodeExists(visitorPath)) {
                final String message = "The path does not exist";
                error(message);
                return false;
            }
        } catch (RepositoryException e) {
            final String message = "The path is not well-formed";
            error(message);
            log.error(message, e);
            return false;
        }
        return true;
    }

    private boolean validateVisitorQuery() throws RepositoryException {
        if (visitorQuery == null || visitorQuery.isEmpty()) {
            error("Query is empty");
            return false;
        }
        final Session session = UserSession.get().getJcrSession();
        try {
            session.getWorkspace().getQueryManager().createQuery(visitorQuery, Query.XPATH);
        } catch (InvalidQueryException e) {
            final String message = "The query that is provided is not a valid xpath query";
            error(message);
            log.error(message, e);
            return false;
        }
        return true;
    }

    private boolean isPathMethod() {
        return method != null && method.equals("path");
    }

    private void rename() {
        final Session session = UserSession.get().getJcrSession();
        try {
            final Node node = (Node) getDefaultModelObject();
            final String destAbsPath = UPDATE_REGISTRY_PATH + "/" + name;
            session.move(node.getPath(), destAbsPath);
            container.setDefaultModel(new JcrNodeModel(session.getNode(destAbsPath)));
        } catch (RepositoryException e) {
            log.error("Failed to rename updater", e);
        }
    }

    private void executeUpdater(boolean dryRun) {
        this.dryRun = dryRun;
        if (!saveUpdater()) {
            return;
        }
        final Node node = (Node) getDefaultModelObject();
        final Session session = UserSession.get().getJcrSession();
        if (node != null) {
            try {
                final String srcPath = node.getPath();
                if (srcPath.startsWith(UPDATE_REGISTRY_PATH)) {
                    final String destPath = UPDATE_QUEUE_PATH + "/" + node.getName();
                    JcrUtils.copy(session, srcPath, destPath);
                    final Node queuedNode = session.getNode(destPath);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_STARTEDBY, session.getUserID());
                    session.save();
                    container.setDefaultModel(new JcrNodeModel(queuedNode));
                }
            } catch (RepositoryException e) {
                final String message = "An unexpected error occurred: " + e.getMessage();
                error(message);
                log.error(message, e);
            }
        }
    }

    private void executeUndo() {
        final Node node = (Node) getDefaultModelObject();
        final Session session = UserSession.get().getJcrSession();
        if (node != null) {
            try {
                final String srcPath = node.getPath();
                if (srcPath.startsWith(UPDATE_HISTORY_PATH)) {
                    final String destPath = UPDATE_QUEUE_PATH + "/undo-" + node.getName();
                    JcrUtils.copy(session, srcPath, destPath);
                    final Node queuedNode = session.getNode(destPath);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_STARTEDBY, session.getUserID());
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_REVERT, true);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_SKIPPED, (Value) null);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_SKIPPEDCOUNT, -1l);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_FAILED, (Value) null);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_FAILEDCOUNT, -1l);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_LOG, (Value) null);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_LOGTAIL, (String) null);
                    session.save();
                    container.setDefaultModel(new JcrNodeModel(queuedNode));
                }
            } catch (RepositoryException e) {
                final String message = "An unexpected error occurred: " + e.getMessage();
                error(message);
                log.error(message, e);
            }
        }
    }

    private void stopUpdater() {
        final Node node = (Node) getDefaultModelObject();
        final Session session = UserSession.get().getJcrSession();
        if (node != null) {
            try {
                node.setProperty(HippoNodeType.HIPPOSYS_CANCELLED, true);
                node.setProperty(HippoNodeType.HIPPOSYS_CANCELLEDBY, session.getUserID());
                session.save();
            } catch (RepositoryException e) {
                final String message = "An unexpected error occurred: " + e.getMessage();
                error(message);
                log.error(message, e);
            }
        }
    }

    protected boolean isStopButtonVisible() {
        return true;
    }

    protected boolean isSaveButtonVisible() {
        return true;
    }

    protected boolean isExecuteButtonVisible() {
        return true;
    }

    protected boolean isUndoButtonVisible() {
        return true;
    }

    protected boolean isDeleteButtonVisible() {
        return true;
    }

    protected boolean isStopButtonEnabled() {
        return true;
    }

    protected boolean isSaveButtonEnabled() {
        return true;
    }

    protected boolean isExecuteButtonEnabled() {
        return true;
    }

    protected boolean isUndoButtonEnabled() {
        return true;
    }

    protected boolean isDeleteButtonEnabled() {
        return true;
    }

    protected boolean isNameFieldEnabled() {
        return true;
    }

    protected boolean isNameFieldVisible() {
        return true;
    }

    protected boolean isPathFieldEnabled() {
        return true;
    }

    protected boolean isPathFieldVisible() {
        return true;
    }

    protected boolean isQueryFieldEnabled() {
        return true;
    }

    protected boolean isQueryFieldVisible() {
        return true;
    }

    protected boolean isBatchSizeFieldEnabled() {
        return true;
    }

    protected boolean isBatchSizeFieldVisible() {
        return true;
    }

    protected boolean isThrottleFieldEnabled() {
        return true;
    }

    protected boolean isThrottleFieldVisible() {
        return true;
    }

    protected boolean isRadioVisible() {
        return true;
    }

    protected boolean isDryRunCheckBoxVisible() {
        return true;
    }

    protected String getEditorName() {
        return "updater-editor";
    }

}
