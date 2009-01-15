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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin implements IValidateService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(EditingReviewedActionsWorkflowPlugin.class);

    private transient boolean validated = false;
    private transient boolean isvalid = true;

    public EditingReviewedActionsWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString(IValidateService.VALIDATE_ID) != null) {
            context.registerService(this, config.getString(IValidateService.VALIDATE_ID));
        } else {
            log.warn("No validator id {} defined", IValidateService.VALIDATE_ID);
        }

        addWorkflowAction("save", new StringResourceModel("save", this, null), new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.commitEditableInstance();
                close();
            }
        });
        addWorkflowAction("revert", new StringResourceModel("revert", this, null), new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean validateSession(List<IValidateService> validators) {
                return true;
            }

            @Override
            protected void prepareSession(JcrNodeModel handleModel) throws RepositoryException {
                Node handleNode = handleModel.getNode();
                handleNode.refresh(false);
                handleNode.getSession().refresh(true);
            }

            @Override
            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.disposeEditableInstance();
                close();
            }
        });
    }

    private void close() {
        IPluginContext context = getPluginContext();
        IEditService editor = context.getService(getPluginConfig().getString(IEditService.EDITOR_ID),
                IEditService.class);
        if (editor != null) {
            editor.close(((WorkflowsModel) getModel()).getNodeModel());
        } else {
            log.warn("No editor service found");
        }
    }

    public boolean hasError() {
        if (!validated) {
            validate();
        }
        return !isvalid;
    }

    public void validate() {
        isvalid = true;
        try {
            Node handle = ((WorkflowsModel) getModel()).getNodeModel().getNode();
            String currentUser = handle.getSession().getUserID();
            NodeIterator variants = handle.getNodes(handle.getName());
            Node toBeValidated = null;
            while (variants.hasNext()) {
                Node variant = variants.nextNode();
                if (variant.hasProperty("hippostd:state")
                        && variant.getProperty("hippostd:state").getString().equals("draft")) {
                    if (variant.hasProperty("hippostd:holder")
                            && variant.getProperty("hippostd:holder").getString().equals(currentUser)) {
                        toBeValidated = variant;
                        break;
                    }
                }
            }
            if (toBeValidated != null) {
                PropertyIterator properties = toBeValidated.getProperties();
                while (properties.hasNext()) {
                    PropertyDefinition propertyDefinition = properties.nextProperty().getDefinition();
                    if (propertyDefinition.isMandatory()) {
                        String propName = propertyDefinition.getName();
                        if (toBeValidated.hasProperty(propName)) {
                            Property mandatory = toBeValidated.getProperty(propName);
                            if (mandatory.getDefinition().isMultiple()) {
                                if (mandatory.getLengths().length == 0) {
                                    isvalid = false;
                                    error("Mandatory field " + propName + " has no value.");
                                    break;
                                } else {
                                    for (Value value : mandatory.getValues()) {
                                        if (value.getString() == null || value.getString().equals("")) {
                                            isvalid = false;
                                            error("Mandatory field " + propName + " has no value.");
                                            break;
                                        }
                                    }
                                }
                            } else {
                                Value value = mandatory.getValue();
                                if (value == null || value.getString() == null || value.getString().equals("")) {
                                    isvalid = false;
                                    error("Mandatory field " + propName + " has no value.");
                                    break;
                                }
                            }
                        } else {
                            isvalid = false;
                            error("Mandatory field " + propName + " has no value.");
                            break;
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            error("Problem while validating: " + ex.getMessage());
            isvalid = false;
        }
        validated = true;
    }
}
