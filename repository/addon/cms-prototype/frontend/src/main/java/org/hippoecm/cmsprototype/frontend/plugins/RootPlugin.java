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
package org.hippoecm.cmsprototype.frontend.plugins;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugins.admin.browser.BrowserPlugin;

public class RootPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public RootPlugin(String id, final JcrNodeModel model) {
        super(id, model);

        // create a list of ITab objects used to feed the tabbed panel
        List tabs = new ArrayList();
        tabs.add(new AbstractTab(new Model("Browse"))
        {
            public Panel getPanel(String panelId)
            {
                return new TabPanel1(panelId, model);
            }
        });

        tabs.add(new AbstractTab(new Model("Edit"))
        {
            public Panel getPanel(String panelId)
            {
                return new TabPanel2(panelId, model);
            }
        });

        add(new AjaxTabbedPanel("tabs", tabs));
    
    
    }

    public void update(final AjaxRequestTarget target, JcrEvent jcrEvent) {
    }

    /**
     * Panel representing the content panel for the first tab.
     */
    private static class TabPanel1 extends Panel
    {
        /**
         * Constructor
         * 
         * @param id
         *            component id
         */
        public TabPanel1(String id, JcrNodeModel model)
        {
            super(id);
            
            add(new BrowserPlugin("tree", model));
        }
    };

    /**
     * Panel representing the content panel for the second tab.
     */
    private static class TabPanel2 extends Panel
    {
        /**
         * Constructor
         * 
         * @param id
         *            component id
         */
        public TabPanel2(String id, JcrNodeModel model)
        {
            super(id);
        }
    };
    
}
