/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.sa.template.editor;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.impl.RenderPlugin;
import org.hippoecm.frontend.sa.service.IViewService;
import org.hippoecm.frontend.sa.service.PluginRequestTarget;

public class EditorPlugin extends RenderPlugin implements IViewService {
    private static final long serialVersionUID = 1L;

    private EditorForm form;

    public EditorPlugin() {
        add(new Form("form"));
        form = null;
    }

    @Override
    public void init(IPluginContext context, IPluginConfig properties) {
        super.init(context, properties);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    public void view(IModel model) {
        setModel(model);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        if (form != null) {
            form.destroy();
        }
        replace(form = new EditorForm("form", (JcrNodeModel) getModel(), this, getPluginContext(), getPluginConfig()));
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if(form != null) {
            form.render(target);
        }
    }
}
