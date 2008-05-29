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
package org.hippoecm.frontend.plugins.standards.sa.tabs;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.sa.service.ITitleDecorator;
import org.hippoecm.frontend.sa.service.PluginRequestTarget;
import org.hippoecm.frontend.sa.service.ServiceTracker;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabsPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TabsPlugin.class);

    public static final String TAB_ID = "tabs";
    public static final int MAX_TAB_TITLE_LENGTH = 9;

    private TabbedPanel tabbedPanel;
    private List<Tab> tabs;
    private ServiceTracker<IRenderService> tabsTracker;
    private int selectCount;

    public TabsPlugin(IPluginContext context, IPluginConfig properties) {
        super(context, properties);

        tabs = new ArrayList<Tab>();
        add(new EmptyPanel("tabs"));

        selectCount = 0;

        tabsTracker = new ServiceTracker<IRenderService>(IRenderService.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onServiceAdded(IRenderService service, String name) {
                // add the plugin
                service.bind(TabsPlugin.this, TabbedPanel.TAB_PANEL_ID);
                Tab tabbie = new Tab(service);
                if (tabs.size() == 0) {
                    tabbedPanel = new TabbedPanel("tabs", TabsPlugin.this, tabs);
                    replace(tabbedPanel);
                }
                tabs.add(tabbie);
                redraw();
            }

            @Override
            public void onRemoveService(IRenderService service, String name) {
                Tab tabbie = findTabbie(service);
                if (tabbie != null) {
                    tabs.remove(tabbie);
                    tabbie.destroy();
                    service.unbind();
                    if (tabs.size() == 0) {
                        replace(new EmptyPanel("tabs"));
                        tabbedPanel = null;
                    }
                    redraw();
                }
            }
        };
        context.registerTracker(tabsTracker, properties.getString(TAB_ID));

        if (tabs.size() > 0) {
            tabbedPanel = new TabbedPanel("tabs", this, tabs);
            replace(tabbedPanel);
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        for (Tab tabbie : tabs) {
            tabbie.renderer.render(target);
        }
    }

    @Override
    public void focus(IRenderService child) {
        Tab tabbie = findTabbie(child);
        if (tabbie != null) {
            tabbie.select(false);
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

    void onSelect(Tab tabbie, AjaxRequestTarget target) {
        tabbie.select(true);
        /*        IFactoryService factory = tabbie.factoryTracker.getService();
         if (factory != null) {
         factory.delete(tabbie.renderer);
         } */
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
        int lastSelected;

        Tab(IRenderService renderer) {
            this.renderer = renderer;
        }

        void destroy() {
            // look for previously selected tab
            int lastCount = 0;
            Tab lastTab = null;
            Iterator<Tab> tabIterator = tabbedPanel.getTabs().iterator();
            while (tabIterator.hasNext()) {
                Tab tabbie = tabIterator.next();
                if (tabbie.lastSelected > lastCount) {
                    lastCount = tabbie.lastSelected;
                    lastTab = tabbie;
                }
            }
            if (lastTab != null) {
                lastTab.select(true);
            }
        }

        // implement ITab interface

        public Model getTitle() {
            List<IClusterable> titles = getPluginContext().getServices(renderer.getServiceId() + ".decorator");
            if (titles != null && titles.size() > 0) {
                String fulltitle = ((ITitleDecorator) titles.get(0)).getTitle();
                int length = fulltitle.length();
                String appendix = (length < (MAX_TAB_TITLE_LENGTH + 1) ? "" : "..");
                length = (length < MAX_TAB_TITLE_LENGTH ? length : MAX_TAB_TITLE_LENGTH);
                String title = fulltitle.substring(0, length) + appendix;
                return new Model(title);
            }
            return new Model("title");
        }

        public Panel getPanel(String panelId) {
            assert (panelId.equals(TabbedPanel.TAB_PANEL_ID));

            return (Panel) renderer;
        }

        // package internals

        void select(boolean force) {
            if (force || tabs.indexOf(this) != tabbedPanel.getSelectedTab()) {
                tabbedPanel.setSelectedTab(tabs.indexOf(this));
                lastSelected = ++TabsPlugin.this.selectCount;
                redraw();
            }
        }

        void detach() {
            ((Panel) renderer).detach();
        }
    }

}
