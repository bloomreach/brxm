/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Session;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.BasicRequestWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CancelWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    private final DateFormat dateFormatFull = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, getSession().getLocale());

    private String state = "unknown";
    private Date schedule = null;

    public CancelWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("info", "info") {

            @Override
            protected IModel getTitle() {
                final String resourceKey = "state-"+state;
                final String parameter = schedule!=null ? dateFormatFull.format(schedule) : "??";
                return new StringResourceModel(resourceKey, this, null, new Object[] { parameter }, "state-unknown");
            }

            @Override
            protected void invoke() {
            }
        });

        final StdWorkflow cancelAction;
        add(cancelAction = new StdWorkflow("cancel", new StringResourceModel("cancel-request", this, null), getModel()) {

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

        WorkflowDescriptorModel model = getModel();
        schedule = null;
        if (model != null) {
            try {
                Node jobNode = model.getNode();
                final String refId = JcrUtils.getStringProperty(jobNode, "hippostdpubwf:refId", null);
                if (refId != null) {
                    final Session session = jobNode.getSession();
                    jobNode = session.getNodeByIdentifier(refId);
                }
                state = JcrUtils.getStringProperty(jobNode, "hipposched:methodName", null);
                if (state == null) {
                    state = deprecatedStateLookupMethod(jobNode);
                }
                if (state == null) {
                    state = "unknown";
                }
                if(jobNode.hasProperty("hipposched:triggers/default/hipposched:fireTime")) {
                    schedule = jobNode.getProperty("hipposched:triggers/default/hipposched:fireTime").getDate().getTime();
                } else if (jobNode.hasProperty("hippostdpubwf:reqdate")) {
                    schedule = new Date(jobNode.getProperty("hippostdpubwf:reqdate").getLong());
                }
                final Map<String, Serializable> hints = model.getObject().hints();
                if (hints.containsKey("cancelRequest") && !(Boolean) hints.get("cancelRequest")) {
                    cancelAction.setVisible(false);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }

    @Deprecated
    private String deprecatedStateLookupMethod(Node jobNode) throws RepositoryException {
        // Pre 7.8 backwards compatibility
        String data = jobNode.getProperty("hipposched:data").getString();
        if(data.contains("depublish")) {
            return "depublish";
        } else if(data.contains("publish")) {
            return "publish";
        } else {
            return null;
        }
    }
}
