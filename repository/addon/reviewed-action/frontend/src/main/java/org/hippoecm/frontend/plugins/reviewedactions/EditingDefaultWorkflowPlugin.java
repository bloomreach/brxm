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

import javax.jcr.RepositoryException;

import org.apache.wicket.model.StringResourceModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.repository.api.Workflow;

public class EditingDefaultWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(EditingDefaultWorkflowPlugin.class);

    public EditingDefaultWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        IEditor editor = context.getService(getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditor.class);
        context.registerService(new IEditorFilter() {
            public void postClose(Object object) {
                // nothing to do
            }
            public Object preClose() {
                try {
                    ((WorkflowDescriptorModel) getDefaultModel()).getNode().save();
                    return new Object();
                } catch (RepositoryException ex) {
                    log.info(ex.getMessage());
                }
                return null;
            }
        }, context.getReference(editor).getServiceId());

        add(new WorkflowAction("save", new StringResourceModel("save", this, null, "Save").getString(), null) {
            @Override
            protected String execute(Workflow wf) throws Exception {
                ((WorkflowDescriptorModel) getDefaultModel()).getNode().save();
                return null;
            }
        });
    }
}
