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
package org.hippoecm.frontend.editor.editor;

import org.apache.wicket.markup.html.form.Form;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.editor.IFormService;
import org.hippoecm.frontend.editor.resources.EditorResources;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class EditorPlugin extends RenderPlugin implements IFormService {

    private static final long serialVersionUID = 1L;

    private EditorForm form;

    public EditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        String formServiceId = config.getString("service.form");
        if (formServiceId != null) {
            context.registerService(this, formServiceId);
        }

        add(EditorResources.getCss());
    }

    @Override
    protected void onStart() {
        super.onStart();
        add(form = newForm());
    }

    @Override
    public void onModelChanged() {
        if (!form.getModel().equals(getDefaultModel())) {
            form.destroy();
            replace(form = newForm());
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (form != null) {
            form.render(target);
        }
    }

    protected EditorForm newForm() {
        return new EditorForm("form", (JcrNodeModel) getDefaultModel(), this, getPluginContext(), getPluginConfig());
    }

    @Override
    public Form getForm() {
        return form;
    }

}
