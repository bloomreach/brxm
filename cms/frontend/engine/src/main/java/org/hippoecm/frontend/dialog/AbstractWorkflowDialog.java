/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.dialog;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog operating in a workflow context. Each workflow action should
 * extend this class and implement the doOk() method.
 *
 */
public abstract class AbstractWorkflowDialog extends AbstractDialog {

    static protected Logger log = LoggerFactory.getLogger(AbstractWorkflowDialog.class);

    private String title;
    private WorkflowsModel model;
    private IServiceReference<AbstractWorkflowPlugin> pluginRef;
    private IServiceReference<IJcrService> jcrServiceRef;

    public AbstractWorkflowDialog(AbstractWorkflowPlugin plugin, IDialogService dialogWindow, String title) {
        super(plugin.getPluginContext(), dialogWindow);

        IPluginContext context = plugin.getPluginContext();
        this.title = title;
        this.model = (WorkflowsModel) plugin.getModel();
        this.pluginRef = context.getReference(plugin);

        IJcrService service = context.getService(IJcrService.class.getName(), IJcrService.class);
        jcrServiceRef = context.getReference(service);

        if (model.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    public String getTitle() {
        return title;
    }

    protected AbstractWorkflowPlugin getPlugin() {
        return pluginRef.getService();
    }

    protected Workflow getWorkflow() {
        try {
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            return manager.getWorkflow(model.getWorkflowDescriptor());
        } catch (MappingException e) {
            log.error(e.getMessage());
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    protected void ok() throws Exception {
        JcrNodeModel handle = model.getNodeModel();
        while (handle != null && !handle.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
            handle = handle.getParentModel();
        }
        if (handle == null) {
            handle = model.getNodeModel().getParentModel();
            if (handle == null) {
                handle = model.getNodeModel();
            }
        }
        handle.getNode().save();
        execute();

        ((UserSession) Session.get()).getJcrSession().refresh(true);

        IJcrService jcrService = jcrServiceRef.getService();
        if (jcrService != null) {
            jcrService.flush(handle);
        }
    }

    @Override
    public void cancel() {
    }

    /**
     * This abstract method is called from ok() and should implement
     * the action to be performed when the dialog's ok button is clicked.
     */
    protected abstract void execute() throws Exception;

}
