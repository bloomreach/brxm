/*
 * Copyright 2012 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.editor.builder;

import org.apache.wicket.Component;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class LayoutPluginEditorPlugin extends RenderPluginEditorPlugin {

    public LayoutPluginEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        
        Component removeLink = get("head:remove");
        if(removeLink != null) {
            removeLink.setVisible(false);
        }
    }
}
