/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.versioning;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(VersionWorkflowPlugin.class);

    WorkflowAction restoreAction;

    public VersionWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                return new StringResourceModel("created", this, null, new Object[] { new IModel() {

                    public Object getObject() {
                        try {
                            Node frozenNode = ((WorkflowDescriptorModel) VersionWorkflowPlugin.this.getModel())
                                    .getNode();
                            Node versionNode = frozenNode.getParent();
                            Calendar calendar = versionNode.getProperty("jcr:created").getDate();
                            return calendar.getTime();
                        } catch (ValueFormatException e) {
                            log.error("Value is not a date", e);
                        } catch (PathNotFoundException e) {
                            log.error("Could not find node", e);
                        } catch (RepositoryException e) {
                            log.error("Repository error", e);
                        }
                        return null;
                    }

                    public void setObject(Object object) {
                        // TODO Auto-generated method stub

                    }

                    public void detach() {
                        // TODO Auto-generated method stub

                    }

                } }, "unknown");
            }

            @Override
            protected void invoke() {
            }
        });

        add(restoreAction = new WorkflowAction("restore", new StringResourceModel("restore", this, null).getString(),
                null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "restore-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                Node frozenNode = ((WorkflowDescriptorModel) getModel()).getNode();
                Node versionNode = frozenNode.getParent();
                Calendar created = versionNode.getProperty("jcr:created").getDate();
                VersionWorkflow workflow = (VersionWorkflow) wf;
                workflow.restore(created);
                return null;
            }
        });

        onModelChanged();
    }

}
