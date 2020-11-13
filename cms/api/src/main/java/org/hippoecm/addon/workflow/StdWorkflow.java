/*
 *  Copyright 2009-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StdWorkflow<T extends Workflow> extends ActionDescription {

    private static final Logger log = LoggerFactory.getLogger(StdWorkflow.class);

    private static final String ICON_ID = "icon";

    private IModel<String> name;
    private ResourceReference iconReference;
    private IPluginContext pluginContext;

    /**
     * @deprecated Old-style constructor Use a constructor with explicit model argument. The WorkflowDescriptorModel is
     * available in workflow plugin constructor.
     */
    @Deprecated
    public StdWorkflow(String id, String name, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, Model.of(name), null, pluginContext, (WorkflowDescriptorModel) enclosingPlugin.getModel());
    }

    /**
     * @deprecated Old-style constructor Use a constructor with explicit model argument. The WorkflowDescriptorModel is
     * available in workflow plugin constructor..
     */
    @Deprecated
    public StdWorkflow(String id, StringResourceModel name, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, name, null, pluginContext, (WorkflowDescriptorModel) enclosingPlugin.getModel());
    }

    /**
     * @deprecated Old-style constructor Use a constructor with explicit model argument. The WorkflowDescriptorModel is
     * available in workflow plugin constructor..
     */
    @Deprecated
    public StdWorkflow(String id, StringResourceModel name, ResourceReference iconReference, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, name, iconReference, pluginContext, (WorkflowDescriptorModel) enclosingPlugin.getModel());
    }

    /**
     * @deprecated Old-style constructor Use a constructor with explicit model argument. The WorkflowDescriptorModel is
     * available in workflow plugin constructor..
     */
    @Deprecated
    public StdWorkflow(String id, String name, ResourceReference iconReference, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, Model.of(name), iconReference, pluginContext, (WorkflowDescriptorModel) enclosingPlugin.getModel());
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

    public StdWorkflow(String id, IModel<String> name, IPluginContext pluginContext, WorkflowDescriptorModel model) {
        this(id, name, null, pluginContext, model);
    }

    public StdWorkflow(String id, IModel<String> name, ResourceReference iconReference, WorkflowDescriptorModel model) {
        this(id, name, iconReference, null, model);
    }

    public StdWorkflow(String id, IModel<String> name, ResourceReference iconReference, IPluginContext pluginContext, WorkflowDescriptorModel model) {
        super(id, model);

        this.name = name;
        this.iconReference = iconReference;
        this.pluginContext = pluginContext;

        add(new ActionDisplay("text") {
            @Override
            protected void initialize() {
                IModel<String> title = getTitle();
                Label titleLabel = new Label("text", title);
                titleLabel.add(TitleAttribute.set(getTooltip()));
                add(titleLabel);
            }
        });

        add(new ActionDisplay("icon") {
            @Override
            protected void initialize() {

                add(ClassAttribute.append(() -> StdWorkflow.this.isEnabled()
                        ? "icon-enabled"
                        : "icon-disabled"));

                Component icon = getIcon(ICON_ID);
                if (icon == null) {
                    if (getIcon() != null) {
                        // Legacy custom override
                        icon = HippoIcon.fromResourceModel(ICON_ID, new LoadableDetachableModel<ResourceReference>() {
                            @Override
                            protected ResourceReference load() {
                                return getIcon();
                            }
                        });
                    } else {
                        icon = HippoIcon.fromSprite(ICON_ID, Icon.GEAR);
                    }
                }
                add(icon);
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

    protected IModel<String> getTitle() {
        return new StringResourceModel(getName(), this)
                .setDefaultValue(getName());
    }

    protected IModel<String> getTooltip() {
        return getTitle();
    }

    /**
     * @deprecated This method is deprecated in favor of {@link StdWorkflow#getIcon(String id)} which gives the
     * developer the freedom to return a component like a {@link HippoIcon} if desired.
     */
    @Deprecated
    protected ResourceReference getIcon() {
        return null;
    }

    protected Component getIcon(final String id) {
        if (iconReference != null) {
            return HippoIcon.fromResource(id, iconReference);
        }
        return null;
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

    protected boolean invokeOnFormError() {
        return false;
    }

    @Override
    protected void invoke() {
        final Dialog dialog = createRequestDialog();
        if (dialog != null) {
            pluginContext.getService(IDialogService.class.getName(), IDialogService.class).show(dialog);
        } else {
            Exception exception = null;
            try {
                execute();
                resolve(null);
            } catch (Exception ex) {
                log.info("Workflow call failed", ex);
                exception = ex;
                reject(exception.getMessage());
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
        try {
            execute();
            resolve(null);
        } catch (final Exception e) {
            reject(e.getMessage());
            throw e;
        }
    }

    public void invokeWorkflowNoReject() throws Exception {
        try {
            execute();
            resolve(null);
        } catch (final Exception e) {
            throw e;
        }
    }

    @Override
    protected void onDetach() {
        name.detach();
        super.onDetach();
    }

}
