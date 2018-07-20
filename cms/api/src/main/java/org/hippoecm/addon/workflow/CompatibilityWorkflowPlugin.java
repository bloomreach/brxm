/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
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
import org.hippoecm.frontend.plugins.yui.datetime.YuiDateTimeField;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Please directly extend from RenderPlugin.
   In case you use the getModel and/or getModelObject methods, you should use the 
   Wicket getDefaultModel/getDefaultModelObject methods and use generics or cast
   to IModel<WorkflowDescriptor> or WorkflowDescriptor, respectively.
   Additionally some implementations might need the method
   <pre>
    protected void onStart() {
        super.onStart();
        modelChanged();
    }
   </pre>
   to be present, though this should be avoided.
 * @author berry
 * @param <T> 
 */
@Deprecated
public abstract class CompatibilityWorkflowPlugin<T extends Workflow> extends RenderPlugin<WorkflowDescriptor> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CompatibilityWorkflowPlugin.class);

    /** Date formatter for internalionzed dates */
    protected final DateFormat dateFormatFull;

    protected CompatibilityWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        dateFormatFull = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, getSession().getLocale());
    }

    @Override
    protected void onStart() {
        super.onStart();
        modelChanged();
    }

    @SuppressWarnings("unchecked")
    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }

    @SuppressWarnings("unchecked")
    public WorkflowDescriptor getModelObject() {
        return (WorkflowDescriptor) getDefaultModelObject();
    }

    /**
     * @deprecated Please directly extend from StdWorkflow, passing the enclosing RenderPlugin
     * and it's plugin context as final parameters to the constructor.
     */
    @Deprecated
    public class WorkflowAction extends StdWorkflow<T> {

        public WorkflowAction(String id, String name, ResourceReference iconModel) {
            super(id, name, iconModel, CompatibilityWorkflowPlugin.this.getPluginContext(), CompatibilityWorkflowPlugin.this);
        }

        @Deprecated
        public WorkflowAction(String id, StringResourceModel name) {
            super(id, name, CompatibilityWorkflowPlugin.this.getPluginContext(), CompatibilityWorkflowPlugin.this.getModel());
        }

        public WorkflowAction(String id, IModel<String> name) {
            super(id, name, CompatibilityWorkflowPlugin.this.getPluginContext(), CompatibilityWorkflowPlugin.this.getModel());
        }

        /** @deprecated Please extend directly from AbstractDialog */
        @Deprecated
        public class WorkflowDialog extends AbstractDialog {

            private static final long serialVersionUID = 1L;

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
                    log.info("Error during workflow execution", ex);
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
                WorkflowAction.this.execute((WorkflowDescriptorModel) CompatibilityWorkflowPlugin.this.getDefaultModel());
            }
        }

        /** @deprecated Either implement a dialog extending directly from WorkflowDialog or use a standard dialog from the CMS API.*/
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
                return DialogConstants.SMALL;
            }
        }

        /** @deprecated Either implement a dialog extending directly from WorkflowDialog or use a standard dialog from the CMS API.*/
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
                return DialogConstants.SMALL;
            }
        }

        /** @deprecated Either implement a dialog extending directly from WorkflowDialog or use a standard dialog from the CMS API.*/
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
                textfield.addBehaviourOnFormComponent(new AttributeAppender("class", Model.of("text-dialog-textarea"), " "));
                setFocus(textfield.getFocusComponent());
            }

            @Override
            public IModel getTitle() {
                return title;
            }

            @Override
            public IValueMap getProperties() {
                return DialogConstants.MEDIUM;
            }
        }

        /** @deprecated Either implement a dialog extending directly from WorkflowDialog or use a standard dialog from the CMS API.*/
        @Deprecated
        public class DestinationDialog extends WorkflowDialog {

            private IModel title;
            private IRenderService dialogRenderer;
            private IClusterControl control;
            private String modelServiceId;
            private ServiceTracker tracker;

            public DestinationDialog(IModel title, IModel question, PropertyModel nameModel,
                    final NodeModelWrapper destination) {
                super();
                this.title = title;
                if (question != null) {
                    add(new Label("question", question));
                } else {
                    Label dummy = new Label("question");
                    dummy.setVisible(false);
                    add(dummy);
                }
                if (nameModel != null) {
                    add(new TextFieldWidget("name", nameModel));
                } else {
                    Label dummy = new Label("name");
                    dummy.setVisible(false);
                    add(dummy);
                }

                final IPluginContext context = CompatibilityWorkflowPlugin.this.getPluginContext();
                IPluginConfig config = CompatibilityWorkflowPlugin.this.getPluginConfig();
                IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                        IPluginConfigService.class);
                IClusterConfig cluster = pluginConfigService.getCluster("cms-pickers/folders");
                control = context.newCluster(cluster, config.getPluginConfig("cluster.options"));
                IClusterConfig decorated = control.getClusterConfig();

                control.start();

                modelServiceId = decorated.getString("model.folder");
                tracker = new ServiceTracker<IModelReference>(IModelReference.class) {
                    
                    IModelReference modelRef;
                    IObserver modelObserver;

                    @Override
                    protected void onServiceAdded(IModelReference service, String name) {
                        super.onServiceAdded(service, name);
                        if (modelRef == null) {
                            modelRef = service;
                            modelRef.setModel(destination.getChainedModel());
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

                };
                context.registerTracker(tracker, modelServiceId);

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
                CompatibilityWorkflowPlugin.this.getPluginContext().unregisterTracker(tracker, modelServiceId);
                tracker = null;
            }

            @Override
            public IModel getTitle() {
                return title;
            }

            @Override
            public IValueMap getProperties() {
                return DialogConstants.LARGE;
            }
        }

        /** @deprecated Either implement a dialog extending directly from WorkflowDialog or use a standard dialog from the CMS API.*/
        @Deprecated
        public class DateDialog extends WorkflowDialog {

            public DateDialog(IModel question, final PropertyModel<Date> dateModel) {
                super();
                Calendar minimum = Calendar.getInstance();
                minimum.setTime(dateModel.getObject());
                minimum.set(Calendar.SECOND, 0);
                minimum.set(Calendar.MILLISECOND, 0);
                // if you want to round upwards, the following ought to be executed: minimum.add(Calendar.MINUTE, 1);
                dateModel.setObject(minimum.getTime());
                add(new Label("question", question));
                YuiDateTimeField ydtf = new YuiDateTimeField("value", dateModel);
                ydtf.add(new FutureDateValidator());
                add(ydtf);
                setFocusOnCancel();
            }

            @Override
            public IValueMap getProperties() {
                return new ValueMap("width=520,height=200");
            }
        }
    }
}
