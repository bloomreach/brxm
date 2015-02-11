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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.tabs.TabbedPanel;
import org.hippoecm.frontend.plugins.standards.tabs.TabsPlugin;

public class EditorTabsPlugin extends TabsPlugin {

    public EditorTabsPlugin(final IPluginContext context, final IPluginConfig properties) {
        super(context, properties);
    }

    @Override
    protected TabbedPanel newTabbedPanel(String id, List<TabsPlugin.Tab> tabs, MarkupContainer tabsContainer) {
        return new TabbedPanel(id, EditorTabsPlugin.this, tabs, tabsContainer) {

            @Override
            protected Form getPanelContainerForm() {
                return (Form) get("panel-container");
            }

            @Override
            protected WebMarkupContainer newPanelContainer(final String id) {
                return new Form(id);
            }
        };
    }

}
