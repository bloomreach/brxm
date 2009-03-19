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
package org.hippoecm.addon.workflow;

import org.hippoecm.frontend.plugin.workflow.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.IStringResourceProvider;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.i18n.SearchingTranslatorPlugin;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IActivator;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AjaxDateTimeField;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public abstract class CompatibilityWorkflowPlugin extends RenderPlugin implements IActivator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: AbstractWorkflowPlugin.java 16815 2009-03-11 16:09:10Z fvlankvelt $";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CompatibilityWorkflowPlugin.class);

    protected interface Visibility extends Serializable {
        boolean isVisible();
    }

    static class Action implements Serializable {
        private static final long serialVersionUID = 1L;

        Component component;
        Visibility visible;

        Action(Component comp, Visibility vis) {
            component = comp;
            visible = vis;
        }
    }

    private Map<String, Action> actions;

    public CompatibilityWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        actions = new HashMap<String, Action>();
    }

    public void start() {
        modelChanged();
    }
    
    public void stop() {
    }
    
    @Override
    public IPluginContext getPluginContext() {
        return super.getPluginContext();
    }

    @Override
    public IPluginConfig getPluginConfig() {
        return super.getPluginConfig();
    }

    protected void addWorkflowDialog(final String dialogName, final IModel dialogLink, final IModel dialogTitle,
            final IModel text, final Visibility visible, final WorkflowAction action) {
        StdWorkflow link = new StdWorkflow.Compatibility(dialogName, (String)dialogLink.getObject(), this) {
            @Override
            protected void execute(Workflow wf) throws Exception {
                CompatibilityWorkflowPlugin.this.getPluginContext().getService(IDialogService.class.getName(), IDialogService.class).show(new Dialog() {
                            protected String execute() {
                                return CompatibilityWorkflowPlugin.this.execute(action, false);
                            }

                            public IModel getTitle() {
                                return dialogTitle;
                            }
                        });
                    }};
        add(link);
        actions.put(dialogName, new Action(link, visible));

        updateActions();
    }

    protected void addWorkflowDialog(final String dialogName, final IModel dialogLink, final Visibility visible,
            IDialogFactory dialogFactory) {
        DialogLink link = new DialogLink(dialogName, dialogLink, dialogFactory, getDialogService());
        add(link);
        actions.put(dialogName, new Action(link, visible));
        updateActions();
    }

    protected void addWorkflowAction(final String linkName, IModel linkText, Visibility visible,
            final WorkflowAction action) {
    StdWorkflow link = new StdWorkflow.Compatibility(linkName, (String) linkText.getObject(), this) {

            @Override
            protected void execute(Workflow wf) throws Exception {
                List<IValidateService> validators = null;
                IPluginConfig config = getPluginConfig();
                if (config.getString(IValidateService.VALIDATE_ID) != null) {
                    validators = getPluginContext().getServices(config.getString(IValidateService.VALIDATE_ID),
                            IValidateService.class);
                    if (validators != null && !action.validateSession(validators)) {
                        return;
                    }
                }
                CompatibilityWorkflowPlugin.this.execute(action, true);
            }
        };
        add(link);
        actions.put(linkName, new Action(link, visible));

        updateActions();
    }

    protected void addWorkflowAction(final String linkName, IModel linkText, final WorkflowAction action) {
        addWorkflowAction(linkName, linkText, new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return true;
            }
        }, action);
    }

    protected void addWorkflowDialog(final String dialogName, final IModel dialogLink, final IModel dialogTitle,
            final WorkflowAction action) {
        addWorkflowDialog(dialogName, dialogLink, dialogTitle, (IModel) null, action);
    }

    protected void addWorkflowDialog(final String dialogName, final IModel dialogLink, final IModel dialogTitle,
            final IModel text, final WorkflowAction action) {
        addWorkflowDialog(dialogName, dialogLink, dialogTitle, text, new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return true;
            }
        }, action);
    }

    protected void updateActions() {
        for (Map.Entry<String, Action> entry : actions.entrySet()) {
            entry.getValue().component.setVisible(entry.getValue().visible.isVisible());
        }
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        updateActions();
    }

    protected void showException(Exception ex) {
        IDialogService dialogService = getPluginContext().getService(IDialogService.class.getName(),
                IDialogService.class);
        if (dialogService != null) {
            dialogService.show(new ExceptionDialog(ex));
        }
    }

    protected String execute(WorkflowAction action, boolean showError) {
        // before saving (which possibly means deleting), find the handle
        final WorkflowDescriptorModel workflowModel = (WorkflowDescriptorModel)CompatibilityWorkflowPlugin.this.getModel();
        try {
            Node handle = workflowModel.getNode();
            while (handle.getDepth() > 0 && !handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                handle = handle.getParent();
            }
            action.prepareSession(new JcrNodeModel(handle));
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            Workflow workflow = manager.getWorkflow((WorkflowDescriptor)(workflowModel.getObject()));
            action.execute(workflow);
            return null;
        } catch (MappingException e) {
            log.error("MappingException while getting workflow: " + e.getMessage(), e);
            if(showError)
                showException(e);
            return e.getClass().getName()+": "+e.getMessage();
        } catch (RepositoryException e) {
            log.error("RepositoryException while getting workflow: " + e.getMessage(), e);
            if(showError)
                showException(e);
            return e.getClass().getName()+": "+e.getMessage();
        } catch (Exception e) {
            log.error("Exception while getting workflow: " + e.getMessage(), e);
            if(showError)
                showException(e);
            return e.getClass().getName()+": "+e.getMessage();
        } finally {
            try {
                ((UserSession) Session.get()).getJcrSession().refresh(true);
            } catch (RepositoryException e) {
                log.error("Failed to refresh session: " + e.getMessage(), e);
            }
        }
    }

    public Workflow getWorkflow() throws MappingException, RepositoryException {
        final WorkflowDescriptorModel workflowModel = (WorkflowDescriptorModel)CompatibilityWorkflowPlugin.this.getModel();
        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
        return manager.getWorkflow((WorkflowDescriptor)(workflowModel.getObject()));
    }
    
    public abstract class Dialog extends AbstractDialog implements IStringResourceProvider {
        private static final long serialVersionUID = 1L;

        private ITranslateService translator;

        public Dialog() {
            this(null);
        }

        public Dialog(IModel message) {
            Label notification = new Label("notification");
            if (message != null) {
                notification.setModel(message);
            } else {
                notification.setVisible(false);
            }
            add(notification);

            // FIXME: refactor the plugin so that we can use a service instead here
            IPluginContext context = CompatibilityWorkflowPlugin.this.getPluginContext();
            translator = new SearchingTranslatorPlugin(context, null);
        }

        public String getString(Map<String, String> criteria) {
            return translator.translate(criteria);
        }

        @Override
        protected void onOk() {
            String errorMessage = execute();
            if (errorMessage != null) {
                error(errorMessage);
            }
        }

        /**
         * This abstract method is called from ok() and should implement
         * the action to be performed when the dialog's ok button is clicked.
         */
        protected abstract String execute();
    }

    public abstract class NameDialog extends Dialog {
        @SuppressWarnings("unused")
        private final static String SVN_ID = "$Id: AbstractNameDialog.java 15465 2008-12-19 15:50:41Z jtietema $";
        private static final long serialVersionUID = 1L;
        protected String name;
        private IModel title;

        public NameDialog(IModel title, IModel question, String name) {
            super();
            this.name = name;
            this.title = title;
            add(new Label("question", question));
            add(new TextFieldWidget("value", new PropertyModel(this, "name")));
        }

        public IModel getTitle() {
            return title;
        }
    }

    public abstract class DestinationDialog extends Dialog {
        protected JcrNodeModel destination;
        protected String name;
        private IModel title;
        private IRenderService dialogRenderer;
        private IClusterControl control;

        public DestinationDialog(IModel title, IModel question) {
            super();
            this.title = title;
            this.destination = null;
            add(new Label("question", question));
            add(new TextFieldWidget("name", new PropertyModel(this, "name")));

            IPluginContext context = CompatibilityWorkflowPlugin.this.getPluginContext();
            IPluginConfig config = CompatibilityWorkflowPlugin.this.getPluginConfig();
            IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(), IPluginConfigService.class);
            IClusterConfig cluster = pluginConfigService.getCluster("cms-pickers/folders");
            control = context.newCluster(cluster, config.getPluginConfig("cluster.options"));
            IClusterConfig decorated = control.getClusterConfig();
            String modelServiceId = decorated.getString("wicket.model.folder");
            ModelReference modelService;
            modelService = new ModelReference<IModel>(modelServiceId, getModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void setModel(IModel model) {
                    DestinationDialog.this.destination = null;
                    if (model != null && model instanceof JcrNodeModel && ((JcrNodeModel)model).getNode() != null) {
                        destination = (JcrNodeModel)model;
                    }
                    super.setModel(model);
                }
            };
            modelService.init(context);

            control.start();

            dialogRenderer = context.getService(decorated.getString("wicket.id"), IRenderService.class);
            dialogRenderer.bind(null, "picker");
            add(dialogRenderer.getComponent());
        }

        @Override
        public void render(PluginRequestTarget target) {
            if (dialogRenderer != null) {
                dialogRenderer.render(target);
            }
            super.render(target);
        }

        @Override
        public final void onClose() {
            super.onClose();
            dialogRenderer.unbind();
            dialogRenderer = null;
            control.stop();
        }

        public IModel getTitle() {
            return title;
        }

        @Override
        public void onDetach() {
            if (destination != null) {
                destination.detach();
            }
            super.onDetach();
        }
    }

    public abstract class DateDialog extends Dialog {

        protected Date date;

        protected Button now;

        public DateDialog(IModel question, Date date) {
            super();
            this.date = date;

            add(new Label("question", question));

            add(new AjaxDateTimeField("value", new PropertyModel(this, "date")));

            now = new AjaxButton(getButtonId(), this) {
                    private static final long serialVersionUID = 1L;
                    
                    @Override
                        public void onSubmit(AjaxRequestTarget target, Form form) {
                        DateDialog.this.date = null;
                        onOk();
                        if (!hasError()) {
                            closeDialog();
                        }
                    }
                }.setDefaultFormProcessing(false);
            now.add(new Label("label", new ResourceModel("now", "Now")));
            addButton(now);
        }
    }
}
