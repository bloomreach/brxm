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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.ResourceReference;

import org.apache.wicket.model.IModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.reviewedactions.BasicRequestWorkflow;

public class CancelWorkflowPlugin extends CompatibilityWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    private String state = "unknown";
    private Date schedule = null;
    private boolean cancelable = true;

    WorkflowAction cancelAction;

    public CancelWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                return new StringResourceModel("state-"+state, this, null,
                    new Object[] {  (schedule!=null ? dateFormatFull.format(schedule) : "??") }, "unknown");
            }
            @Override
            protected void invoke() {
            }
        });

        add(cancelAction = new WorkflowAction("cancel", new StringResourceModel("cancel-request", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "delete-16.png");
            }
            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicRequestWorkflow workflow = (BasicRequestWorkflow) wf;
                workflow.cancelRequest();
                return null;
            }
        });
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
        schedule = null;
        if (model != null) {
            try {
                Node node = model.getNode();
                if (node.hasProperty("hipposched:triggers/default/hipposched:fireTime")) {
                    //FIXME: Instead of looking for clues in the serialized-object string, we need to add this information
                    //thipposched:trigger
                    String data = node.getProperty("hipposched:data").getString();
                    if(data.contains("depublish")) {
                        state = "depublish";
                    } else if(data.contains("publish")) {
                        state = "publish";
                    } else {
                        state = "unknown";
                    }
                } else {
                    state = "unknown";
                }
                if(node.hasProperty("hipposched:triggers/default/hipposched:fireTime")) {
                    schedule = node.getProperty("hipposched:triggers/default/hipposched:fireTime").getDate().getTime();
                } else if (node.hasProperty("hippostdpubwf:reqdate")) {
                    schedule = new Date(node.getProperty("hippostdpubwf:reqdate").getLong());
                }
                Map<String, Serializable> hints = ((WorkflowDescriptor)model.getObject()).hints();
                if (hints.containsKey("cancelRequest") && !((Boolean)hints.get("cancelRequest")).booleanValue()) {
                    cancelAction.setVisible(false);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }
}
