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
package org.hippoecm.frontend.editor.plugins;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextTemplatePlugin extends RenderPlugin<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TextTemplatePlugin.class);

    public TextTemplatePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IModel<String> valueModel = getModel();
        IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode", "view"));
        if (IEditor.Mode.EDIT == mode) {
            // IModel<String> encodedStringModel  = new EncodedStringModel(valueModel);
            // TextAreaWidget widget = new TextAreaWidget("value", encodedStringModel);
            TextAreaWidget widget = new TextAreaWidget("value", valueModel);
            if (config.getString("rows") != null) {
                widget.setRows(config.getString("rows"));
            }
            if (config.getString("cols") != null) {
                widget.setCols(config.getString("cols"));
            }
            add(widget);
        } else {
            add(new Label("value", valueModel));
        }
    }

}
