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
package org.hippoecm.frontend.plugins.standards.tabs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.util.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabsPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TabsPlugin.class);

    public static final String TAB_ID = "tabs";

    public static final int MAX_TAB_TITLE_LENGTH = 9;

    private TabbedPanel tabbedPanel;
    private List<Tab> tabs;
    private ServiceTracker tabsTracker;

    public TabsPlugin() {
        tabsTracker = new ServiceTracker(IRenderService.class);
        tabsTracker.addListener(new ServiceTracker.IListener() {
            private static final long serialVersionUID = 1L;

            public void onServiceAdded(String name, Serializable service) {
                // add the plugin
                Tab tabbie = new Tab((IRenderService) service);
                if (tabs.size() == 0) {
                    replace(tabbedPanel);
                }
                tabs.add(tabbie);
                tabbie.select();
                redraw();
            }

            public void onServiceChanged(String name, Serializable service) {
            }

            public void onServiceRemoved(String name, Serializable service) {
                Tab tabbie = findTabbie((IRenderService) service);
                if (tabbie != null) {
                    tabs.remove(tabbie);
                    if (tabs.size() == 0) {
                        replace(new EmptyPanel("tabs"));
                    }
                    redraw();
                }
            }
        });

        add(new EmptyPanel("tabs"));
    }

    @Override
    public void init(PluginContext context, String serviceId, String parentId, String wicketId, String modelId) {
        tabsTracker.open(context, context.getProperty(TAB_ID));

        super.init(context, serviceId, parentId, wicketId, modelId);

        tabs = new ArrayList<Tab>();
        tabbedPanel = new TabbedPanel("tabs", tabs);
    }

    @Override
    public void destroy() {
        tabsTracker.close();
        replace(new EmptyPanel("tabs"));
    }

    @Override
    public void focus(IRenderService child) {
        Tab tabbie = findTabbie(child);
        if (tabbie != null) {
            tabbie.select();
        }
        super.focus(child);
    }

    @Override
    public void onDetach() {
        Iterator<Tab> tabIter = tabs.iterator();
        while (tabIter.hasNext()) {
            Tab tabbie = tabIter.next();
            tabbie.detach();
        }
        super.onDetach();
    }

    private Tab findTabbie(IRenderService service) {
        Iterator<Tab> iter = tabs.iterator();
        while (iter.hasNext()) {
            Tab tabbie = iter.next();
            if (tabbie.renderer == service) {
                return tabbie;
            }
        }
        return null;
    }

    class Tab implements ITab {
        private static final long serialVersionUID = 1L;

        IRenderService renderer;

        Tab(IRenderService renderer) {
            this.renderer = renderer;
        }

        // implement ITab interface

        public Model getTitle() {
            return new Model("title");
        }

        public Panel getPanel(String panelId) {
            assert (panelId.equals(TabbedPanel.TAB_PANEL_ID));

            return (Panel) renderer;
        }

        // package internals

        void select() {
            int selected = tabbedPanel.getSelectedTab();
            if (tabs.indexOf(this) != selected) {
                tabbedPanel.setSelectedTab(tabs.indexOf(this));
                redraw();
            }
        }

        void detach() {
            ((Panel) renderer).detach();
        }
    }

}
