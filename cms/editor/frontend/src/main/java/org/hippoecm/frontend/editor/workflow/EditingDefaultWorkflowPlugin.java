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
package org.hippoecm.frontend.editor.workflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingDefaultWorkflowPlugin extends RenderPlugin {

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
                    getModel().getNode().save();
                    return new Object();
                } catch (RepositoryException ex) {
                    log.info(ex.getMessage());
                }
                return null;
            }
        }, context.getReference(editor).getServiceId());

        add(new StdWorkflow("save", new StringResourceModel("save", this, null, "Save"),
                new PackageResourceReference(EditingDefaultWorkflowPlugin.class, "document-save-16.png"), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                getModel().getNode().save();
                return null;
            }
        });

        add(new StdWorkflow("done", new StringResourceModel("done", this, null, "Done"),
                new PackageResourceReference(getClass(), "document-saveclose-16.png"), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                Node docNode = ((WorkflowDescriptorModel) EditingDefaultWorkflowPlugin.this.getDefaultModel())
                        .getNode();
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    JcrNodeModel docModel = new JcrNodeModel(docNode);
                    IEditor editor = editorMgr.getEditor(docModel);
                    if (editor == null) {
                        editorMgr.openEditor(docModel);
                    } else {
                        editor.setMode(Mode.VIEW);
                    }
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
                return null;
            }
        });
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }

}
