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

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.IStringResourceProvider;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
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
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AjaxDateTimeField;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;

public abstract class CompatibilityWorkflowPlugin extends RenderPlugin implements IActivator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: AbstractWorkflowPlugin.java 16815 2009-03-11 16:09:10Z fvlankvelt $";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CompatibilityWorkflowPlugin.class);

    protected CompatibilityWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    public void start() {
        modelChanged();
    }
    
    public void stop() {
    }
    
    public class WorkflowAction extends StdWorkflow {
        ResourceReference iconModel;

        public WorkflowAction(String id, String name, ResourceReference iconModel) {
            super(id, name);
        }

        public WorkflowAction(String id, StringResourceModel name) {
            super(id, (String) name.getObject());
        }

        @Override
        protected ResourceReference getIcon() {
            if (iconModel != null) {
                return iconModel;
            } else {
                return super.getIcon();
            }
        }

        protected Dialog createRequestDialog() {
            return null;
        }

        protected Dialog createResponseDialog(String message) {
            return new ExceptionDialog(message);
        }

        @Override
        public final void invoke() {
            Dialog dialog = createRequestDialog();
            if (dialog != null) {
                getPluginContext().getService(IDialogService.class.getName(), IDialogService.class).show(dialog);
            } else {
                String message = execute();
                if (message != null) {
                    getPluginContext().getService(IDialogService.class.getName(), IDialogService.class).show(createResponseDialog(message));
                }
            }
        }

        protected String execute() {
            return execute((WorkflowDescriptorModel) CompatibilityWorkflowPlugin.this.getModel());
        }

        protected String execute(WorkflowDescriptorModel model) {
            try {
                WorkflowDescriptor descriptor = (WorkflowDescriptor) WorkflowAction.this.getModelObject();
                WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
                javax.jcr.Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
                session.save();
                session.refresh(true);
                Workflow workflow = manager.getWorkflow(descriptor);
                execute(workflow);
                session.refresh(false);
            } catch (RepositoryException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
                return ex.getClass().getName() + ": " + ex.getMessage();
            } catch (WorkflowException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
                return ex.getClass().getName() + ": " + ex.getMessage();
            } catch (RemoteException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
                return ex.getClass().getName() + ": " + ex.getMessage();
            } catch (Exception ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
                return ex.getClass().getName() + ": " + ex.getMessage();
            }
            return null;
        }

        protected String execute(Workflow workflow) throws Exception {
            throw new WorkflowException("unsupported operation");
        }

        public class WorkflowDialog extends AbstractDialog implements IStringResourceProvider {

            private static final long serialVersionUID = 1L;
            private ITranslateService translator;

            public WorkflowDialog() {
                this((IModel) null);
            }

            public WorkflowDialog(IModel message) {
                Label notification = new Label("notification");
                if (message != null) {
                    notification.setModel(message);
                } else {
                    notification.setVisible(false);
                }
                add(notification);

                // FIXME: refactor the plugin so that we can use a service instead here
                IPluginContext context = getPluginContext();
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

            public IModel getTitle() {
                return new Model("");
            }

            /**
             * This abstract method is called from ok() and should implement
             * the action to be performed when the dialog's ok button is clicked.
             */
            protected final String execute() {
                return WorkflowAction.this.execute((WorkflowDescriptorModel) WorkflowAction.this.getModel());
            }
        }

        public abstract class NameDialog extends WorkflowDialog {

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

        public abstract class DestinationDialog extends WorkflowDialog {

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
                        if (model != null && model instanceof JcrNodeModel && ((JcrNodeModel) model).getNode() != null) {
                            destination = (JcrNodeModel) model;
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

        public abstract class DateDialog extends WorkflowDialog {

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
}
