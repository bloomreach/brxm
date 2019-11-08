/*
 * Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.codemirror.CodeMirrorEditor;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.RepoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class UpdaterEditor extends Panel {

    protected final static Logger log = LoggerFactory.getLogger(UpdaterEditor.class);

    protected static final String UPDATE_PATH = "/hippo:configuration/hippo:update";
    protected static final String UPDATE_QUEUE_PATH = UPDATE_PATH + "/hippo:queue";
    protected static final String UPDATE_REGISTRY_PATH = UPDATE_PATH + "/hippo:registry";
    protected static final String UPDATE_HISTORY_PATH = UPDATE_PATH + "/hippo:history";

    private static final long DEFAULT_BATCH_SIZE = 10L;
    private static final long DEFAULT_THROTTLE = 1000L;
    private static final String DEFAULT_METHOD = "path";

    @SuppressWarnings("unchecked")
    private static final Map<String, String> LOG_LEVELS_MAP = Collections.unmodifiableMap(new LinkedHashMap() {
        {
            put(Level.TRACE.toString(), Level.TRACE.toString());
            put(Level.DEBUG.toString(), Level.DEBUG.toString());
            put(Level.INFO.toString(), Level.INFO.toString());
        }
    });

    protected final IPluginContext context;
    protected final Panel container;
    protected final Form form;
    protected final FeedbackPanel feedback;

    protected String script;
    protected String name;
    protected String description;
    protected String visitorPath;
    protected String visitorQuery;
    protected String method = DEFAULT_METHOD;
    protected String parameters;
    protected String batchSize = String.valueOf(DEFAULT_BATCH_SIZE);
    protected String throttle = String.valueOf(DEFAULT_THROTTLE);
    protected boolean dryRun = false;
    protected String logLevel = Level.DEBUG.toString();

    public UpdaterEditor(final IModel<?> model, final IPluginContext context, final Panel container) {
        super("updater-editor", model);
        this.context = context;
        this.container = container;

        form = new HippoForm("updater-form");

        feedback = new FeedbackPanel("feedback", new ContainerFeedbackMessageFilter(this));
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        final AjaxButton executeButton = new AjaxButton("execute-button") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> currentForm) {
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
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> currentForm) {
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
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> currentForm) {
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
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> currentForm) {
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

        final RadioGroup<String> radios = new RadioGroup<>("radios", new PropertyModel<>(this, "method"));
        form.add(radios);

        final LabelledInputFieldTableRow nameField = new LabelledInputFieldTableRow("name", new Model<>("Name"),
                new PropertyModel<>(this, "name")) {
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

        final LabelledTextAreaTableRow descriptionField = new LabelledTextAreaTableRow("description",
                new Model<>("Description"), new PropertyModel<>(this, "description")) {
            @Override
            public boolean isEnabled() {
                return isDescriptionFieldEnabled();
            }

            @Override
            public boolean isVisible() {
                return isDescriptionFieldVisible();
            }
        };
        radios.add(descriptionField);

        radios.add(new Label("select", new Model<>("Select node using")));

        final RadioLabelledInputFieldTableRow pathField = new RadioLabelledInputFieldTableRow("path", radios,
                new Model<>("Repository path"), new PropertyModel<>(this, "visitorPath")) {
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

        final RadioLabelledInputFieldTableRow queryField = new RadioLabelledInputFieldTableRow("query", radios,
                new Model<>("XPath query"), new PropertyModel<>(this, "visitorQuery")) {
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

        final RadioLabelledInputFieldTableRow customField = new RadioLabelledInputFieldTableRow("custom", radios,
                new Model<>("Updater"), new Model<>("")) {
            @Override
            public boolean isEnabled() {
                return isCustomFieldEnabled();
            }

            @Override
            public boolean isVisible() {
                return isCustomFieldVisible();
            }

            @Override
            protected boolean isRadioVisible() {
                return UpdaterEditor.this.isRadioVisible();
            }
        };
        customField.input.setVisible(false);
        radios.add(customField);

        final LabelledTextAreaTableRow parametersField = new LabelledTextAreaTableRow("parameters",
                new Model<>("Parameters"), new PropertyModel<>(this, "parameters")) {
            @Override
            public boolean isEnabled() {
                return isParametersFieldEnabled();
            }

            @Override
            public boolean isVisible() {
                return isParametersFieldVisible();
            }
        };
        radios.add(parametersField);

        final LabelledInputFieldTableRow batchSizeField = new LabelledInputFieldTableRow("batch-size",
                new Model<>("Batch Size"), new PropertyModel<>(this, "batchSize")) {
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

        final LabelledInputFieldTableRow throttleField = new LabelledInputFieldTableRow("throttle",
                new Model<>("Throttle (ms)"), new PropertyModel<>(this, "throttle")) {
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

        final LabelledDropDownFieldTableRow logLevelField = new LabelledDropDownFieldTableRow("log-level",
                new Model<>("Log Level"), new PropertyModel<>(this, "logLevel"), LOG_LEVELS_MAP) {

            @Override
            public boolean isEnabled() {
                return isLogLevelFieldEnabled();
            }

            @Override
            public boolean isVisible() {
                return isLogLevelFieldVisible();
            }
        };
        radios.add(logLevelField);

        final LabelledCheckBoxTableRow dryRunCheckBox = new LabelledCheckBoxTableRow("dryrun", new Model<>("Dry run"),
                new PropertyModel<>(this, "dryRun")) {
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

        final CodeMirrorEditor scriptEditor = new CodeMirrorEditor("script-editor", getEditorName(),
                new PropertyModel<>(this, "script"));
        scriptEditor.setReadOnly(isScriptEditorReadOnly());
        form.add(scriptEditor);

        final Component updaterOutput = createOutputComponent("updater-output");
        form.add(updaterOutput);

        add(form);

        setOutputMarkupId(true);

        loadProperties();
    }

    private void tryRenderFeedback(final AjaxRequestTarget target) {
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
        description = getStringProperty(HippoNodeType.HIPPOSYS_DESCRIPTION, null);
        parameters = getStringProperty(HippoNodeType.HIPPOSYS_PARAMETERS, null);
        visitorPath = getStringProperty(HippoNodeType.HIPPOSYS_PATH, null);
        visitorQuery = getStringProperty(HippoNodeType.HIPPOSYS_QUERY, null);
        batchSize = String.valueOf(getLongProperty(HippoNodeType.HIPPOSYS_BATCHSIZE, DEFAULT_BATCH_SIZE));
        throttle = String.valueOf(getLongProperty(HippoNodeType.HIPPOSYS_THROTTLE, DEFAULT_THROTTLE));
        if (visitorQuery != null) {
            method = "query";
        } else if (visitorPath != null) {
            method = "path";
        } else {
            method = "custom";
        }
        logLevel = getStringProperty(HippoNodeType.HIPPOSYS_LOGLEVEL, Level.DEBUG.toString());
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

    protected final String getStringProperty(final String propertyName, final String defaultValue) {
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

    protected final boolean getBooleanProperty(final String propertyName, final boolean defaultValue) {
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

    protected final Long getLongProperty(final String propertyName, final Long defaultValue) {
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
        final Node node = (Node) getDefaultModelObject();
        if (node != null) {
            try {
                return node.isNodeType("hipposys:updaterinfo");
            } catch (RepositoryException e) {
                log.error("Failed to determine whether node is updater node", e);
            }
        }
        return false;
    }

    protected Component createOutputComponent(final String id) {
        return new Label(id);
    }

    protected void deleteUpdater() {
        final IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
        dialogService.show(new DeleteUpdaterDialog((IModel<Node>) getDefaultModel(), container));
    }

    private boolean saveUpdater() {
        final Node node = (Node) getDefaultModelObject();
        try {
            if (!validateName()) {
                return false;
            }
            if (!validateParameters()) {
                return false;
            }
            if (isCustomMethod()) {
                node.setProperty(HippoNodeType.HIPPOSYS_PATH, (String) null);
                node.setProperty(HippoNodeType.HIPPOSYS_QUERY, (String) null);
            } else if (isPathMethod()) {
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
            node.setProperty(HippoNodeType.HIPPOSYS_BATCHSIZE, Long.parseLong(batchSize));
            if (!validateThrottle()) {
                return false;
            }
            node.setProperty(HippoNodeType.HIPPOSYS_THROTTLE, Long.parseLong(throttle));
            node.setProperty(HippoNodeType.HIPPOSYS_SCRIPT, script);
            if (!node.getName().equals(name)) {
                rename();
            }
            node.setProperty(HippoNodeType.HIPPOSYS_DESCRIPTION, StringUtils.defaultString(description));
            node.setProperty(HippoNodeType.HIPPOSYS_PARAMETERS, StringUtils.defaultString(parameters));
            node.setProperty(HippoNodeType.HIPPOSYS_LOGLEVEL,
                    StringUtils.defaultIfBlank(logLevel, Level.DEBUG.toString()));
            node.getSession().save();
            return true;
        } catch (RepositoryException e) {
            form.error(getExceptionTranslation(e));
            if (log.isDebugEnabled()) {
                log.error("An unexpected error occurred", e);
            } else {
                log.error("An unexpected error occurred: {}", e.getMessage());
            }
        }
        return false;
    }

    private boolean validateName() {
        if (name == null || name.isEmpty()) {
            form.error(getString("name-is-empty"));
            return false;
        }
        return true;
    }

    private boolean validateParameters() {
        if (StringUtils.isBlank(parameters)) {
            return true;
        }
        try {
            final JSONObject json = JSONObject.fromObject(parameters);
            return json != null;
        } catch (JSONException e) {
            form.error(getString("parameters-invalid-json"));
            return false;
        }
    }

    private boolean validateThrottle() {
        try {
            Long.valueOf(throttle);
            return true;
        } catch (NumberFormatException e) {
            form.error(getString("throttle-must-be-positive"));
            return false;
        }
    }

    private boolean validateBatchSize() {
        try {
            Long.valueOf(batchSize);
            return true;
        } catch (NumberFormatException e) {
            form.error(getString("batchsize-must-be-positive"));
            return false;
        }
    }

    private boolean validateVisitorPath() throws RepositoryException {
        if (visitorPath == null || visitorPath.isEmpty()) {
            form.error(getString("path-is-empty"));
            return false;
        }
        final Session session = UserSession.get().getJcrSession();
        try {
            if (!session.nodeExists(visitorPath)) {
                form.error(getString("path-not-exist"));
                return false;
            }
        } catch (RepositoryException e) {
            final String message = getString("path-not-wellformed");
            form.error(message);
            if (log.isDebugEnabled()) {
                log.error(message, e);
            } else {
                log.error(message + ":" + e.getMessage());
            }
            return false;
        }
        return true;
    }

    private boolean validateVisitorQuery() throws RepositoryException {
        if (visitorQuery == null || visitorQuery.isEmpty()) {
            form.error(getString("query-is-empty"));
            return false;
        }
        final Session session = UserSession.get().getJcrSession();
        try {
            session.getWorkspace().getQueryManager().createQuery(RepoUtils.encodeXpath(visitorQuery), Query.XPATH);
        } catch (InvalidQueryException e) {
            final String message = getString("invalid-query");
            form.error(message);
            if (log.isDebugEnabled()) {
                log.error(message, e);
            }
            return false;
        }
        return true;
    }

    private boolean isCustomMethod() {
        return method == null || method.equals("custom");
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
            form.error(getExceptionTranslation(e, name).getObject());

            if (log.isDebugEnabled()) {
                log.error("Failed to rename updater", e);
            } else {
                log.error("Failed to rename updater: {}", e.getMessage());
            }
        }
    }

    private void executeUpdater(final boolean dryRun) {
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
                form.error(message);
                if (log.isDebugEnabled()) {
                    log.error(message, e);
                }
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
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_SKIPPEDCOUNT, -1L);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_FAILED, (Value) null);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_FAILEDCOUNT, -1L);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_LOG, (Value) null);
                    queuedNode.setProperty(HippoNodeType.HIPPOSYS_LOGTAIL, (String) null);
                    session.save();
                    container.setDefaultModel(new JcrNodeModel(queuedNode));
                }
            } catch (RepositoryException e) {
                final String message = "An unexpected error occurred: " + e.getMessage();
                form.error(message);
                if (log.isDebugEnabled()) {
                    log.error(message, e);
                }
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
                final String message = getExceptionTranslation(e).getObject();
                form.error(message);
                if (log.isDebugEnabled()) {
                    log.error(message, e);
                }
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

    protected boolean isDescriptionFieldEnabled() {
        return true;
    }

    protected boolean isDescriptionFieldVisible() {
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

    protected boolean isCustomFieldEnabled() {
        return true;
    }

    protected boolean isCustomFieldVisible() {
        return true;
    }

    protected boolean isQueryFieldVisible() {
        return true;
    }

    protected boolean isParametersFieldEnabled() {
        return true;
    }

    protected boolean isParametersFieldVisible() {
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

    protected boolean isLogLevelFieldEnabled() {
        return false;
    }

    protected boolean isLogLevelFieldVisible() {
        return true;
    }

    protected boolean isDryRunCheckBoxVisible() {
        return true;
    }

    protected boolean isScriptEditorReadOnly() {
        return false;
    }

    protected String getEditorName() {
        return "updater-editor";
    }

    protected IModel<String> getExceptionTranslation(final Throwable t, final Object... parameters) {
        final String key = "exception,type=${type},message=${message}";
        final HashMap<String, String> details = new HashMap<>();
        details.put("type", t.getClass().getName());
        details.put("message", t.getMessage());
        return new StringResourceModel(key, this)
                .setModel(new Model<>(details))
                .setDefaultValue(t.getLocalizedMessage())
                .setParameters(parameters);
    }
}
