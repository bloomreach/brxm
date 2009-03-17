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
package org.hippoecm.frontend.plugin.workflow;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IActivator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated 
 */
public abstract class AbstractWorkflowPlugin extends RenderPlugin implements IActivator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AbstractWorkflowPlugin.class);

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

    public AbstractWorkflowPlugin(IPluginContext context, IPluginConfig config) {
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
        DialogLink link = new DialogLink(dialogName, dialogLink, new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {
                return new AbstractWorkflowDialog(AbstractWorkflowPlugin.this, text) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void execute() throws Exception {
                        action.execute(getWorkflow());
                    }

                    public IModel getTitle() {
                        return dialogTitle;
                    }
                };
            }
        }, getDialogService());
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
        AjaxLink link = new AjaxLink(linkName) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<IValidateService> validators = null;
                IPluginConfig config = getPluginConfig();
                if (config.getString(IValidateService.VALIDATE_ID) != null) {
                    validators = getPluginContext().getServices(config.getString(IValidateService.VALIDATE_ID),
                            IValidateService.class);
                    if (validators != null && !action.validateSession(validators)) {
                        return;
                    }
                }
                execute(action);
            }
        };
        add(link);
        link.add(new Label(linkName + "-label", linkText));
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

    protected void execute(WorkflowAction action) {
        // before saving (which possibly means deleting), find the handle
        final WorkflowsModel workflowModel = (WorkflowsModel) getModel();
        JcrNodeModel handle = workflowModel.getNodeModel();
        try {
            while (handle.getParentModel() != null && !handle.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                handle = handle.getParentModel();
            }
            action.prepareSession(handle);
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            Workflow workflow = manager.getWorkflow(workflowModel.getWorkflowDescriptor());
            action.execute(workflow);
        } catch (MappingException e) {
            log.error("MappingException while getting workflow: " + e.getMessage(), e);
            showException(e);
        } catch (RepositoryException e) {
            log.error("RepositoryException while getting workflow: " + e.getMessage(), e);
            showException(e);
        } catch (Exception e) {
            log.error("Exception while getting workflow: " + e.getMessage(), e);
            showException(e);
        } finally {
            try {
                ((UserSession) Session.get()).getJcrSession().refresh(true);
            } catch (RepositoryException e) {
                log.error("Failed to refresh session: " + e.getMessage(), e);
            }
        }

    }

}
