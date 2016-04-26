/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.editor;

import javax.jcr.Node;

import org.apache.wicket.markup.html.form.Form;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.editor.IFormService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorPlugin extends RenderPlugin implements IFormService {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(EditorPlugin.class);

    private EditorForm form;

    public EditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        String formServiceId = config.getString("service.form");
        if (formServiceId != null) {
            context.registerService(this, formServiceId);
        }
    }

    @Override
    protected void onStart() {
        Task onStartTask = null;

        try {
            if (HDC.isStarted()) {
                onStartTask = HDC.getCurrentTask().startSubtask("EditorPlugin.onStart");
                addModelInfoToDiagnosticsTaskAttributes(HDC.getCurrentTask());
            }

            super.onStart();
            add(form = newForm());
        } finally {
            if (onStartTask != null) {
                onStartTask.stop();
            }
        }
    }

    @Override
    public void onModelChanged() {
        Task onModelChangedTask = null;

        try {
            if (HDC.isStarted()) {
                onModelChangedTask = HDC.getCurrentTask().startSubtask("EditorPlugin.onModelChanged");
                addModelInfoToDiagnosticsTaskAttributes(HDC.getCurrentTask());
            }

            if (!form.getModel().equals(getDefaultModel())) {
                form.destroy();
                replace(form = newForm());
            }
        } finally {
            if (onModelChangedTask != null) {
                onModelChangedTask.stop();
            }
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        Task renderTask = null;

        try {
            if (HDC.isStarted()) {
                renderTask = HDC.getCurrentTask().startSubtask("EditorPlugin.render");
                addModelInfoToDiagnosticsTaskAttributes(HDC.getCurrentTask());
            }

            super.render(target);

            if (form != null) {
                form.render(target);
            }
        } finally {
            if (renderTask != null) {
                renderTask.stop();
            }
        }
    }

    protected EditorForm newForm() {
        return new EditorForm("form", (JcrNodeModel) getDefaultModel(), this, getPluginContext(), getPluginConfig());
    }

    @Override
    public Form getForm() {
        return form;
    }

    private void addModelInfoToDiagnosticsTaskAttributes(final Task task) {
        final JcrNodeModel model = (JcrNodeModel) getDefaultModel();

        if (model != null) {
            try {
                final Node node = model.getNode();
                if (node != null) {
                    task.setAttribute("editorModelType", node.getPrimaryNodeType().getName());
                    task.setAttribute("editorModelPath", node.getPath());
                }
            } catch (Exception e) {
                log.error("Failed to get model info of the EditorForm.", e);
            }
        }
    }
}
