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
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.application.PluginRequestTarget;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITitleDecorator;
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
    private int selectCount;

    public TabsPlugin() {
        tabsTracker = new ServiceTracker(IRenderService.class);
        tabsTracker.addListener(new ServiceTracker.IListener() {
            private static final long serialVersionUID = 1L;

            public void onServiceAdded(String name, Serializable service) {
                // add the plugin
                ((IRenderService) service).bind(TabsPlugin.this, TabbedPanel.TAB_PANEL_ID);
                Tab tabbie = new Tab((IRenderService) service);
                if (tabs.size() == 0) {
                    tabbedPanel = new TabbedPanel("tabs", TabsPlugin.this, tabs);
                    replace(tabbedPanel);
                }
                tabs.add(tabbie);
                tabbie.select(true);
                redraw();
            }

            public void onServiceChanged(String name, Serializable service) {
            }

            public void onRemoveService(String name, Serializable service) {
                Tab tabbie = findTabbie((IRenderService) service);
                if (tabbie != null) {
                    tabs.remove(tabbie);
                    tabbie.destroy();
                    ((IRenderService) service).unbind();
                    if (tabs.size() == 0) {
                        replace(new EmptyPanel("tabs"));
                        tabbedPanel = null;
                    }
                    redraw();
                }
            }
        });

        tabs = new ArrayList<Tab>();
        add(new EmptyPanel("tabs"));

        selectCount = 0;
    }

    @Override
    public void init(PluginContext context, Map<String, ParameterValue> properties) {
        tabsTracker.open(context, properties.get(TAB_ID).getStrings().get(0));

        if (tabs.size() > 0) {
            tabbedPanel = new TabbedPanel("tabs", this, tabs);
            replace(tabbedPanel);
        }

        super.init(context, properties);
    }

    @Override
    public void destroy() {
        super.destroy();
        tabsTracker.close();
        if (tabs.size() > 0) {
            replace(new EmptyPanel("tabs"));
            tabbedPanel = null;
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
        if(tabbie.factoryTracker.getServices().size() > 0) {
            IFactoryService factory = tabbie.factoryTracker.getServices().get(0);
            factory.delete(tabbie.renderer);
        }
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
        ServiceTracker titleTracker;
        ServiceTracker<IFactoryService> factoryTracker;
        int lastSelected;

        Tab(IRenderService renderer) {
            this.renderer = renderer;
            titleTracker = new ServiceTracker(ITitleDecorator.class);
            titleTracker.open(getPluginContext(), renderer.getServiceId() + ".decorator");
            factoryTracker = new ServiceTracker<IFactoryService>(IFactoryService.class);
            factoryTracker.open(getPluginContext(), renderer.getServiceId() + ".factory");
        }

        void destroy() {
            factoryTracker.close();
            titleTracker.close();

            // look for previously selected tab
            int lastCount = 0;
            Tab lastTab = null;
            Iterator<Tab> tabs = tabbedPanel.getTabs().iterator();
            while (tabs.hasNext()) {
                Tab tabbie = tabs.next();
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
            List<Serializable> titles = titleTracker.getServices();
            if (titles.size() > 0) {
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
