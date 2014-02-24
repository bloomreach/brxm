/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StdWorkflow<T extends Workflow> extends ActionDescription {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CompatibilityWorkflowPlugin.class);

    private IModel<String> name;
    private ResourceReference iconModel;
    private IPluginContext pluginContext;

    /**
     * @deprecated Old-style constructor
     *    Use a constructor with explicit model argument.
     *    The WorkflowDescriptorModel is available in workflow plugin constructor..
     */
    @Deprecated
    public StdWorkflow(String id, String name, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, Model.of(name), null, pluginContext, (WorkflowDescriptorModel) enclosingPlugin.getModel());
    }

    /**
     * @deprecated Old-style constructor
     *    Use a constructor with explicit model argument.
     *    The WorkflowDescriptorModel is available in workflow plugin constructor..
     */
    @Deprecated
    public StdWorkflow(String id, StringResourceModel name, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, name, null, pluginContext, (WorkflowDescriptorModel) enclosingPlugin.getModel());
    }

    /**
     * @deprecated Old-style constructor
     *    Use a constructor with explicit model argument.
     *    The WorkflowDescriptorModel is available in workflow plugin constructor..
     */
    @Deprecated
    public StdWorkflow(String id, StringResourceModel name, ResourceReference iconModel, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, name, iconModel, pluginContext, (WorkflowDescriptorModel) enclosingPlugin.getModel());
    }

    /**
     * @deprecated Old-style constructor
     *    Use a constructor with explicit model argument.
     *    The WorkflowDescriptorModel is available in workflow plugin constructor..
     */
    @Deprecated
    public StdWorkflow(String id, String name, ResourceReference iconModel, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, Model.of(name), iconModel, pluginContext, (WorkflowDescriptorModel) enclosingPlugin.getModel());
    }

    public StdWorkflow(String id, String name) {
        this(id, Model.of(name));
    }

    public StdWorkflow(String id, IModel<String> name) {
        this(id, name, null, null, null);
    }

    public StdWorkflow(String id, IModel<String> name, WorkflowDescriptorModel model) {
        this(id, name, null, null, model);
    }

    public StdWorkflow(String id, IModel<String> name, ResourceReference iconModel, WorkflowDescriptorModel model) {
        this(id, name, iconModel, null, model);
    }

    public StdWorkflow(String id, IModel<String> name, IPluginContext pluginContext, WorkflowDescriptorModel model) {
        this(id, name, null, pluginContext, model);
    }

    public StdWorkflow(String id, IModel<String> name, ResourceReference iconModel, IPluginContext pluginContext, WorkflowDescriptorModel model) {
        super(id, model);

        this.iconModel = iconModel;
        this.pluginContext = pluginContext;

        this.name = name;

        add(new ActionDisplay("text") {
            @Override
            protected void initialize() {
                IModel<String> title = getTitle();
                Label titleLabel = new Label("text", title);
                titleLabel.add(new AttributeModifier("title", true, getTooltip()));
                add(titleLabel);
            }
        });

        add(new ActionDisplay("icon") {
            @Override
            protected void initialize() {
                add(new Image("icon", new LoadableDetachableModel<ResourceReference>() {
                    @Override
                    protected ResourceReference load() {
                        return getIcon();
                    }
                }));
            }
        });

        add(new ActionDisplay("panel") {
            @Override
            protected void initialize() {
            }
        });
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) super.getDefaultModel();
    }

    protected final String getName() {
        return name.getObject();
    }

    protected IModel getTitle() {
        return new StringResourceModel(getName(), this, null, getName());
    }

    protected IModel getTooltip() {
        return getTitle();
    }

    protected ResourceReference getIcon() {
        if (iconModel != null) {
            return iconModel;
        } else {
            return new PackageResourceReference(StdWorkflow.class, "workflow-16.png");
        }
    }

    protected Dialog createRequestDialog() {
        return null;
    }

    protected Dialog createResponseDialog(String message) {
        return new ExceptionDialog(message);
    }

    protected Dialog createResponseDialog(Exception ex) {
        return new ExceptionDialog(ex);
    }

    @Override
    protected void invoke() {
        Dialog dialog = createRequestDialog();
        if (dialog != null) {
            pluginContext.getService(IDialogService.class.getName(), IDialogService.class).show(dialog);
        } else {
            Exception exception = null;
            try {
                execute();
            } catch (WorkflowException ex) {
                log.info("Workflow call failed", ex);
                exception = ex;
            } catch (Exception ex) {
                log.info("Workflow call failed", ex);
                exception = ex;
            }
            if (exception != null && pluginContext != null) {
                pluginContext.getService(IDialogService.class.getName(), IDialogService.class).show(
                        createResponseDialog(exception));
            }
        }
    }

    protected void execute() throws Exception {
        execute((WorkflowDescriptorModel) getDefaultModel());
    }

    protected void execute(WorkflowDescriptorModel model) throws Exception {
        javax.jcr.Session session = UserSession.get().getJcrSession();
        session.save();

        T workflow = model.getWorkflow();
        if (workflow == null) {
            throw new MappingException("action no longer valid");
        }

        String message = execute(workflow);
        if (message != null) {
            throw new WorkflowException(message);
        }

        // invalidate all virtual nodes & notify virtual node listeners
        session.refresh(false);
        UserSession us = UserSession.get();
        us.getFacetRootsObserver().broadcastEvents();
    }

    protected String execute(T workflow) throws Exception {
        throw new WorkflowException("unsupported operation");
    }

    @Override
    public void invokeWorkflow() throws Exception {
        execute();
    }

    @Override
    protected void onDetach() {
        name.detach();
        super.onDetach();
    }

}
