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

import java.util.Iterator;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugins.yui.datetime.AjaxDateTimeField;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public abstract class CompatibilityWorkflowPlugin<T extends Workflow> extends RenderPlugin<WorkflowDescriptor> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: AbstractWorkflowPlugin.java 16815 2009-03-11 16:09:10Z fvlankvelt $";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CompatibilityWorkflowPlugin.class);

    protected CompatibilityWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected void onStart() {
        super.onStart();
        modelChanged();
    }

    @SuppressWarnings("unchecked")
    public IModel<WorkflowDescriptor> getModel() {
        return (IModel<WorkflowDescriptor>) getDefaultModel();
    }

    @SuppressWarnings("unchecked")
    public WorkflowDescriptor getModelObject() {
        return (WorkflowDescriptor) getDefaultModelObject();
    }
    
    @Deprecated
    public class WorkflowAction extends StdWorkflow {
        ResourceReference iconModel;

        public WorkflowAction(String id, String name, ResourceReference iconModel) {
            super(id, name);
            this.iconModel = iconModel;
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

        @Override
        protected IModel initModel() {
            return CompatibilityWorkflowPlugin.this.getDefaultModel();
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
                try {
                    execute();
                } catch (Exception ex) {
                    log.error("Workflow call failed", ex);
                    getPluginContext().getService(IDialogService.class.getName(), IDialogService.class).show(
                            createResponseDialog(ex.getClass().getName() + ": " + ex.getMessage()));
                }
            }
        }

        protected void execute() throws Exception {
            execute((WorkflowDescriptorModel<T>) CompatibilityWorkflowPlugin.this.getDefaultModel());
        }

        protected void execute(WorkflowDescriptorModel<T> model) throws Exception {
            WorkflowDescriptor descriptor = (WorkflowDescriptor) model.getObject();
            WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
            javax.jcr.Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
            session.refresh(true);
            session.save();
            session.refresh(true);
            Workflow workflow = manager.getWorkflow(descriptor);
            String message = execute((T) workflow);
            if (message != null) {
                throw new WorkflowException(message);
            }

            // workflow may have closed existing session
            UserSession us = (UserSession) org.apache.wicket.Session.get();
            session = us.getJcrSession();
            session.refresh(false);
            us.getFacetSearchObserver().broadcastEvents();
        }

        protected String execute(T workflow) throws Exception {
            throw new WorkflowException("unsupported operation");
        }

        @Deprecated
        public class WorkflowDialog extends AbstractDialog {

            private static final long serialVersionUID = 1L;
            private ITranslateService translator;

            public WorkflowDialog() {
                this((IModel) null);
            }

            public WorkflowDialog(IModel message) {
                Label notification = new Label("notification");
                if (message != null) {
                    notification.setDefaultModel(message);
                } else {
                    notification.setVisible(false);
                }
                add(notification);
                notification.add(new AttributeAppender("class", new Model("notification"), " "));
                init();
            }

            protected void init() {
            }

            @Override
            protected void onOk() {
                try {
                    execute();
                } catch (WorkflowException ex) {
                    log.info("Error during workflow execution", ex);
                    error(ex);
                } catch (Exception ex) {
                    log.error("Error during workflow execution", ex);
                    error(ex);
                }
            }

            public IModel getTitle() {
                return new Model("");
            }

            /**
             * This abstract method is called from ok() and should implement
             * the action to be performed when the dialog's ok button is clicked.
             */
            protected final void execute() throws Exception {
                WorkflowAction.this.execute((WorkflowDescriptorModel<T>) CompatibilityWorkflowPlugin.this.getDefaultModel());
            }
        }

        @Deprecated
        public class ConfirmDialog extends WorkflowDialog {
            private static final long serialVersionUID = 1L;
            private IModel title;

            public ConfirmDialog(IModel title, IModel question) {
                this(title, null, null, question);
            }

            public ConfirmDialog(IModel title, IModel intro, IModel text, IModel question) {
                super();
                this.title = title;
                if(intro == null) {
                    Label component;
                    add(component = new Label("intro"));
                    component.setVisible(false);
                } else {
                    add(new Label("intro", intro));
                }
                if(text == null) {
                    Label component;
                    add(component = new Label("text"));
                    component.setVisible(false);
                } else {
                    add(new MultiLineLabel("text", text));
                }
                add(new Label("question", question));
            }

            @Override
            public IModel getTitle() {
                return title;
            }

            @Override
            public IValueMap getProperties() {
                return SMALL;
            }
        }

        @Deprecated
        public class NameDialog extends WorkflowDialog {
            private static final long serialVersionUID = 1L;
            private IModel title;

            public NameDialog(IModel title, IModel question, PropertyModel nameModel) {
                super();
                this.title = title;
                add(new Label("question", question));

                TextFieldWidget textfield;
                add(textfield = new TextFieldWidget("value", nameModel));
                setFocus(textfield.getFocusComponent());
            }

            @Override
            public IModel getTitle() {
                return title;
            }

            @Override
            public IValueMap getProperties() {
                return SMALL;
            }
        }

        @Deprecated
        public class TextDialog extends WorkflowDialog {
            private static final long serialVersionUID = 1L;
            private IModel title;

            public TextDialog(IModel title, IModel question, PropertyModel textModel) {
                super();
                this.title = title;
                add(new Label("question", question));

                TextAreaWidget textfield;
                add(textfield = new TextAreaWidget("value", textModel));
                textfield.setCols("25");
                textfield.setRows("4");
                setFocus(textfield.getFocusComponent());
            }

            @Override
            public IModel getTitle() {
                return title;
            }

            @Override
            public IValueMap getProperties() {
                return MEDIUM;
            }
        }

        @Deprecated
        public class DestinationDialog extends WorkflowDialog {

            private IModel title;
            private IRenderService dialogRenderer;
            private IClusterControl control;

            public DestinationDialog(IModel title, IModel question, PropertyModel nameModel,
                    final NodeModelWrapper destination) {
                super();
                this.title = title;
                add(new Label("question", question));
                add(new TextFieldWidget("name", nameModel));

                final IPluginContext context = CompatibilityWorkflowPlugin.this.getPluginContext();
                IPluginConfig config = CompatibilityWorkflowPlugin.this.getPluginConfig();
                IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                        IPluginConfigService.class);
                IClusterConfig cluster = pluginConfigService.getCluster("cms-pickers/folders");
                control = context.newCluster(cluster, config.getPluginConfig("cluster.options"));
                IClusterConfig decorated = control.getClusterConfig();

                control.start();

                String modelServiceId = decorated.getString("wicket.model.folder");
                context.registerTracker(new ServiceTracker<IModelReference>(IModelReference.class) {
                    
                    IModelReference modelRef;
                    IObserver modelObserver;

                    @Override
                    protected void onServiceAdded(IModelReference service, String name) {
                        super.onServiceAdded(service, name);
                        if (modelRef == null) {
                            modelRef = service;
                            context.registerService(modelObserver = new IObserver<IModelReference>() {
                                private static final long serialVersionUID = 1L;
            
                                public IModelReference getObservable() {
                                    return modelRef;
                                }
            
                                public void onEvent(Iterator<? extends IEvent<IModelReference>> events) {
                                    IModel model = modelRef.getModel();
                                    if (model != null && model instanceof JcrNodeModel && ((JcrNodeModel) model).getNode() != null) {
                                        destination.setChainedModel(model);
                                    }
                                    DestinationDialog.this.setOkEnabled(true);
                                }
                            }, IObserver.class.getName());
                        }
                    }

                    @Override
                    protected void onRemoveService(IModelReference service, String name) {
                        if (service == modelRef) {
                            context.unregisterService(modelObserver, IObserver.class.getName());
                            modelObserver = null;
                            modelRef = null;
                        }
                        super.onRemoveService(service, name);
                    }

                }, modelServiceId);

                dialogRenderer = context.getService(decorated.getString("wicket.id"), IRenderService.class);
                dialogRenderer.bind(null, "picker");
                add(dialogRenderer.getComponent());
                setFocusOnCancel();
                setOkEnabled(false);
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

            @Override
            public IModel getTitle() {
                return title;
            }

            @Override
            public IValueMap getProperties() {
                return LARGE;
            }
        }

        @Deprecated
        public class DateDialog extends WorkflowDialog {

            public DateDialog(IModel question, final PropertyModel dateModel) {
                super();

                add(new Label("question", question));
                add(new AjaxDateTimeField("value", dateModel, true, getPluginContext(), getPluginConfig()));
                setFocusOnCancel();
            }

            @Override
            public IValueMap getProperties() {
                return new ValueMap("width=520,height=200");
            }
        }
    }
}
