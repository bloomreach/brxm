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
package org.hippoecm.frontend.editor.plugins;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BooleanValueTemplatePlugin extends RenderPlugin<Boolean> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BooleanValueTemplatePlugin.class);
    public static final String CHECKED_SYMBOL = "&#9745;";
    public static final String UNCHECKED_SYMBOL = "&#9744;";

    public BooleanValueTemplatePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IModel<Boolean> valueModel = getModel();
        final IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.VIEW);
        if (mode == IEditor.Mode.EDIT) {
            add(new BooleanFieldWidget("value", valueModel));
        } else {
            Fragment fragment = new Fragment("value", "view", this);
            add(fragment);

            Component checkbox = new Label("checkbox", new LoadableDetachableModel<String>() {
                @Override
                protected String load() {
                    return valueModel.getObject() ? CHECKED_SYMBOL : UNCHECKED_SYMBOL;
                }
            }).setEscapeModelStrings(false);
            fragment.add(checkbox);
        }
        setOutputMarkupId(true);
    }

}
