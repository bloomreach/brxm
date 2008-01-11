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
package org.hippoecm.cmsprototype.frontend.plugins.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TabsPlugin is an editor-aware container of plugins.  The tabs correspond
 * to child plugins (perspectives).  Tab switching is implemented by handling the
 * "edit" and "focus" requests.  The edit operation is sent as a notification
 * to the perspectives.  If one of those wishes to obtain focus, it can request it
 * by sending a "focus" request.
 *
 */
public class TabsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TabsPlugin.class);
    
    private Map<JcrNodeModel, AbstractTab> editors;
    private ArrayList<Tab> tabs;
    private AjaxTabbedPanel tabbedPanel;

    private String editPerspective;

    public TabsPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        tabs = new ArrayList<Tab>();
        editors = new HashMap<JcrNodeModel, AbstractTab>();

        tabbedPanel = new AjaxTabbedPanel("tabs", tabs);
        add(tabbedPanel);

        List<String> parameter = pluginDescriptor.getParameter("editor");
        if (parameter != null && parameter.size() > 0) {
            editPerspective = parameter.get(0);
        } else {
            editPerspective = null;
        }
    }

    protected Tab createTab(PluginDescriptor descriptor, JcrNodeModel model) {
        descriptor.setWicketId(TabbedPanel.TAB_PANEL_ID);
        PluginFactory pluginFactory = new PluginFactory(getPluginManager());
        final Plugin child = pluginFactory.createPlugin(descriptor, model, this);

        String title = descriptor.getPluginId();
        if(descriptor.getParameter("title") != null) {
            title = descriptor.getParameter("title").get(0);
        }
        return new Tab(child, title);
    }
    
    @Override
    public Plugin addChild(final PluginDescriptor childDescriptor) {
        if (editPerspective == null || !editPerspective.equals(childDescriptor.getPluginId())) {
            Tab tab = createTab(childDescriptor, getNodeModel());
            tabs.add(tab);
            return tab.getPlugin();
        } else {
            return null;
        }
    }

    @Override
    //FIXME: list 'tabs' contains AbstractTab instances, not PluginDescriptors
    public void removeChild(PluginDescriptor childDescriptor) {
        tabs.remove(childDescriptor);
    }

    @Override
    public void handle(Request request) {
        if ("edit".equals(request.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(request.getData());
            if (!editors.containsKey(model)) {
                // create a descriptor for the plugin
                PluginDescriptor descriptor = getPluginManager().getPluginConfig().getPlugin(editPerspective);
                try {
                    List<String> titleParam = new LinkedList<String>();
                    titleParam.add(model.getNode().getName());
                    descriptor.addParameter("title", titleParam);
                } catch(RepositoryException ex) {
                    log.error("Couldn't obtain name of item " + ex.getMessage());
                }

                // add the plugin
                Tab tabbie = createTab(descriptor, model);
                tabs.add(tabbie);
                editors.put(model, tabbie);
                request.getContext().addRefresh(this);

                // HACK: add children before setting the final pluginId.
                // each perspective needs to have a unique Id
                // This should be handled by the plugin factory
                tabbie.getPlugin().addChildren();
                descriptor.setPluginId(editors.size() + ":" + editPerspective);
            }

            // notify children; if tabs should be switched,
            // they should send a focus request.
            Channel channel = getDescriptor().getOutgoing();
            if (channel != null) {
                Notification notification = channel.createNotification(request);
                channel.publish(notification);
            }

            // don't send request to parent
            return;
        } else if ("focus".equals(request.getOperation())) {
            String pluginId = (String) request.getData().get("plugin");
            for (int i = 0; i < tabs.size(); i++) {
                AbstractTab tabbie = tabs.get(i);
                Plugin perspective = (Plugin) tabbie.getPanel(TabbedPanel.TAB_PANEL_ID);
                if (pluginId.equals(perspective.getDescriptor().getPluginId())) {
                    tabbedPanel.setSelectedTab(i);
                    request.getContext().addRefresh(this);

                    // notify children of focus event
                    Channel channel = getDescriptor().getOutgoing();
                    if (channel != null) {
                        Notification notification = channel.createNotification(request);
                        channel.publish(notification);
                    }
                    return;
                }
            }
        }
        // TODO: handle close tab request
        super.handle(request);
    }

    private static class Tab extends AbstractTab {
        private static final long serialVersionUID = 1L;

        private Plugin plugin;

        Tab(Plugin plugin, String title) {
            super(new Model(title));
            this.plugin = plugin;
        }

        @Override
        public Panel getPanel(String panelId) {
            return plugin;
        }

        public Plugin getPlugin() {
            return plugin;
        }
    }
}
