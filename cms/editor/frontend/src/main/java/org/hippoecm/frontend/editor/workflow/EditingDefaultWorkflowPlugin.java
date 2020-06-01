/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.buttons.ButtonStyle;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingDefaultWorkflowPlugin extends RenderPlugin {

    private static final Logger log = LoggerFactory.getLogger(EditingDefaultWorkflowPlugin.class);

    public EditingDefaultWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final String editorId = config.getString(IEditorManager.EDITOR_ID);
        final IEditor editor = context.getService(editorId, IEditor.class);
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

        add(new StdWorkflow("save", new StringResourceModel("save", this).setDefaultValue("Save"), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.FLOPPY);
            }

            @Override
            public String getCssClass() {
                return ButtonStyle.SECONDARY.getCssClass();
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

        add(new StdWorkflow("done", new StringResourceModel("done", this).setDefaultValue("Done"), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.inline(id, CmsIcon.FLOPPY_TIMES_CIRCLE);
            }

            @Override
            public String getCssClass() {
                return ButtonStyle.PRIMARY.getCssClass();
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                final String editorId = getPluginConfig().getString(IEditorManager.EDITOR_ID);
                final IEditorManager editorMgr = getPluginContext().getService(editorId, IEditorManager.class);

                final Node documentNode = getModel().getNode();
                if (editorMgr == null) {
                    log.warn("No editor found to edit {}", documentNode.getPath());
                    return null;
                }

                final JcrNodeModel documentModel = new JcrNodeModel(documentNode);
                final IEditor editor = editorMgr.getEditor(documentModel);
                if (editor == null) {
                    editorMgr.openEditor(documentModel);
                } else {
                    editor.setMode(Mode.VIEW);
                }
                return null;
            }
        });
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }

}
