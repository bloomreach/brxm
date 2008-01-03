/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugins.admin.editor;

import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;

public class EditorPluginTestPage extends WebPage {
    private static final long serialVersionUID = 1L;

    public EditorPluginTestPage() {
        Application app = (Application) getSession().getApplication();
        Channel outgoing = null;
        PluginDescriptor editorDescriptor = new PluginDescriptor("editorPlugin", null, outgoing);
        add(new EditorPlugin(editorDescriptor, new JcrNodeModel(app.node), null));
    }

}
