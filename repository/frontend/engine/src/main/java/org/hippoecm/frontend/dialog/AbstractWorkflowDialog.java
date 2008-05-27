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
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.sa.* instead
 */
@Deprecated
public abstract class AbstractWorkflowDialog extends AbstractDialog {

    static protected Logger log = LoggerFactory.getLogger(AbstractWorkflowDialog.class);

    public AbstractWorkflowDialog(DialogWindow dialogWindow, String title) {
        super(dialogWindow);
        dialogWindow.setTitle(title);
        if (dialogWindow.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    protected Workflow getWorkflow() {
        Plugin owningPlugin = getPlugin();
        Workflow workflow = null;
        IPluginModel model = owningPlugin.getPluginModel();
        if (model instanceof WorkflowsModel) {
            try {
                WorkflowsModel workflowModel = (WorkflowsModel) owningPlugin.getPluginModel();
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                return manager.getWorkflow(workflowModel.getWorkflowDescriptor());
            } catch (MappingException e) {
                log.error(e.getMessage());
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        } else {
            /* FIXME: The else part is in fact a workaround to support legacy
             * workflow plugin implementations.
             */
            try {
                JcrNodeModel nodeModel = new JcrNodeModel(model);
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                return manager.getWorkflow("internal", nodeModel.getNode());
            } catch (MappingException e) {
                log.error(e.getMessage());
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return workflow;
    }

    
    @Override
    protected void ok() throws Exception {
        JcrNodeModel handle = getDialogWindow().getNodeModel();
        while (handle.getParentModel() != null && !handle.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
            handle = handle.getParentModel();
        }
        handle.getNode().save();
        ((UserSession) Session.get()).getJcrSession().refresh(true);
        
        execute();
        ((UserSession) Session.get()).getJcrSession().refresh(true);
         
        Channel channel = getChannel();
        if (channel != null) {
            Request request = channel.createRequest("select", (IPluginModel) getDialogWindow().getModel());
            channel.send(request);
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
