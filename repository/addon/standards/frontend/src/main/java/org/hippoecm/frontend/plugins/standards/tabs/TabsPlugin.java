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
package org.hippoecm.frontend.plugins.standards.tabs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabsPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TabsPlugin.class);

    public static final String TAB_ID = "tabs";
    public static final String MAX_TAB_TITLE_LENGTH = "title.maxlength";

    private int maxTabLength;
    private TabbedPanel tabbedPanel;
    private RenderService emptyPanel;
    private List<Tab> tabs;
    private ServiceTracker<IRenderService> tabsTracker;
    private int selectCount;
    private boolean openleft = false;

    public TabsPlugin(IPluginContext context, IPluginConfig properties) {
        super(context, properties);

        maxTabLength = properties.getInt(MAX_TAB_TITLE_LENGTH, 12);

        tabs = new ArrayList<Tab>();
        add(tabbedPanel = new TabbedPanel("tabs", TabsPlugin.this, tabs));

        setOutputMarkupId(true);

        IPluginConfig panelConfig = new JavaPluginConfig();
        panelConfig.put("wicket.id", properties.getString(TAB_ID));
        panelConfig.put("wicket.behavior", properties.getString("tabbedpanel.behavior"));
        
        if(properties.containsKey("tabbedpanel.openleft")) {
            openleft = properties.getBoolean("tabbedpanel.openleft");
        }

        emptyPanel = new RenderService(context, panelConfig);
        context.registerService(emptyPanel, properties.getString(TAB_ID));

        selectCount = 0;
        tabsTracker = new ServiceTracker<IRenderService>(IRenderService.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onServiceAdded(IRenderService service, String name) {
                // add the plugin
                service.bind(TabsPlugin.this, TabbedPanel.TAB_PANEL_ID);
                if (service != emptyPanel) {
                    Tab tabbie = new Tab(service);
                    if (openleft) {
                        tabs.add(0, tabbie);
                        tabbedPanel.setSelectedTab(0);
                    } else {
                        tabs.add(tabbie);
                        if (tabs.size() == 1) {
                            tabbedPanel.setSelectedTab(0);
                        }
                    }
                }
                redraw();
            }

            @Override
            public void onRemoveService(IRenderService service, String name) {
                Tab tabbie = findTabbie(service);
                if (tabbie != null) {
                    tabs.remove(tabbie);
                    tabbie.destroy();
                    if (tabs.size() == 0) {
                        tabbedPanel.setSelectedTab(-1);
                    }
                    service.unbind();
                    redraw();
                }
            }
        };
        context.registerTracker(tabsTracker, properties.getString(TAB_ID));
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        tabbedPanel.render(target);
        for (Tab tabbie : tabs) {
            tabbie.renderer.render(target);
        }
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

    Panel getEmptyPanel() {
        return emptyPanel;
    }

    void onSelect(Tab tabbie, AjaxRequestTarget target) {
        tabbie.renderer.focus(null);
    }

    void onClose(Tab tabbie, AjaxRequestTarget target) {
        IServiceReference<IRenderService> reference = getPluginContext().getReference(tabbie.renderer);
        IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);
        try {
            editor.close();
        } catch (EditorException ex) {
            log.info("Failed to close editor: " + ex.getMessage());
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

    class Tab implements ITab, IObserver {
        private static final long serialVersionUID = 1L;

        ServiceTracker<ITitleDecorator> decoratorTracker;
        ITitleDecorator decorator;
        IModel titleModel;
        IRenderService renderer;
        int lastSelected;

        Tab(IRenderService renderer) {
            this.renderer = renderer;

            IPluginContext context = getPluginContext();
            String serviceId = context.getReference(renderer).getServiceId();
            decoratorTracker = new ServiceTracker<ITitleDecorator>(ITitleDecorator.class) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onServiceAdded(ITitleDecorator service, String name) {
                    decorator = service;
                    if (titleModel instanceof IObservable) {
                        getPluginContext().unregisterService(Tab.this, IObserver.class.getName());
                        titleModel = null;
                    }
                    tabbedPanel.redraw();
                }

                @Override
                protected void onRemoveService(ITitleDecorator service, String name) {
                    if (decorator == service) {
                        if (titleModel instanceof IObservable) {
                            getPluginContext().unregisterService(Tab.this, IObserver.class.getName());
                            titleModel = null;
                        }
                        decorator = null;
                        tabbedPanel.redraw();
                    }
                }
            };
            context.registerTracker(decoratorTracker, serviceId);
        }

        void destroy() {
            IPluginContext context = getPluginContext();
            String serviceId = context.getReference(renderer).getServiceId();
            context.unregisterTracker(decoratorTracker, serviceId);

            if (tabs.size() > 0) {
                // look for previously selected tab
                int lastCount = 0;
                Tab lastTab = tabs.get(0);
                Iterator<Tab> tabIterator = tabbedPanel.getTabs().iterator();
                while (tabIterator.hasNext()) {
                    Tab tabbie = tabIterator.next();
                    if (tabbie.lastSelected > lastCount) {
                        lastCount = tabbie.lastSelected;
                        lastTab = tabbie;
                    }
                }
                tabbedPanel.setSelectedTab(tabs.indexOf(lastTab));
                lastTab.lastSelected = ++TabsPlugin.this.selectCount;
                lastTab.renderer.focus(null);
                redraw();
            }
        }

        public IObservable getObservable() {
            return (IObservable) titleModel;
        }

        public void onEvent(Iterator<? extends IEvent> events) {
            tabbedPanel.redraw();
        }

        // implement ITab interface

        public IModel getTitle() {
            if (titleModel == null && decorator != null) {
                titleModel = decorator.getTitle();
                if (titleModel instanceof IObservable) {
                    IPluginContext context = getPluginContext();
                    context.registerService(this, IObserver.class.getName());
                }
            }
            if (titleModel != null) {
                String title = (String) titleModel.getObject();
                if(title.length() > maxTabLength) {
                    title = title.substring(0, maxTabLength-2) + ".."; // leave space for two .. then add them
                }
                return new Model(title);
            }
            return new Model("title");
        }

        public Panel getPanel(String panelId) {
            assert (panelId.equals(TabbedPanel.TAB_PANEL_ID));

            return (Panel) renderer;
        }

        // package internals

        boolean canClose() {
            IServiceReference<IRenderService> reference = getPluginContext().getReference(renderer);
            IEditor editor = getPluginContext().getService(reference.getServiceId(), IEditor.class);
            if (editor != null) {
                return true;
            }
            return false;
        }

        void select() {
            if (tabs.indexOf(this) != tabbedPanel.getSelectedTab()) {
                tabbedPanel.setSelectedTab(tabs.indexOf(this));
                lastSelected = ++TabsPlugin.this.selectCount;
                redraw();
            }
        }

        void detach() {
            ((Panel) renderer).detach();
            if (titleModel != null) {
                titleModel.detach();
            }
        }
    }
}
