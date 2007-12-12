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
import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.EventChannel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.frontend.plugin.PluginFactory;

public class TabsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private ArrayList<AbstractTab> tabs;
    private AjaxTabbedPanel tabbedPanel;

    public TabsPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        tabs = new ArrayList<AbstractTab>();
        tabbedPanel = new AjaxTabbedPanel("tabs", tabs); 
        add(tabbedPanel);
    }

    @Override
    public Plugin addChild(PluginDescriptor childDescriptor) {
        childDescriptor.setWicketId(TabbedPanel.TAB_PANEL_ID);
        PluginFactory pluginFactory = new PluginFactory(getPluginManager());
        final Plugin child = pluginFactory.createPlugin(childDescriptor, getNodeModel(), this);

        AbstractTab tab = new AbstractTab(new Model(childDescriptor.getPluginId())) {
            private static final long serialVersionUID = 1L;
            @Override
            public Panel getPanel(String panelId) {
                return child;
            }
        };
        
        tabs.add(tab);
        return child;
    }
    
    @Override
    //FIXME: list 'tabs' contains AbstractTab instances, not PluginDescriptors
    public void removeChild(PluginDescriptor childDescriptor) {
        tabs.remove(childDescriptor);
    }

    @Override
    public void update(AjaxRequestTarget target, PluginEvent event) {
        
        // check all perspectives to see if their incoming channel matches that
        // on which the event was broadcasted
        // on the first match, the corresponding perspective/tab is made active
        
        Set<EventChannel> eventChannels = event.getChannels();
        AbstractTab selectMe = null;
        int i = 0;
        while (i < tabs.size() && selectMe == null) {
            AbstractTab tabbie = tabs.get(i); 
            Plugin perspective = (Plugin) tabbie.getPanel(TabbedPanel.TAB_PANEL_ID);
            Set<EventChannel> perspectiveChannels = new HashSet<EventChannel>(perspective.getDescriptor().getIncoming());
            perspectiveChannels.retainAll(eventChannels);
            
            if (!perspectiveChannels.isEmpty()) {
                // an event has been broadcasted on this perspective's incoming channel
                // -> the tab this perspective belongs to should be made active
                selectMe = tabbie;
            }
            i++;
        }
        
        if (selectMe != null) {
            tabbedPanel.setSelectedTab(tabs.indexOf(selectMe));
            if (target != null && findPage() != null) {
                target.addComponent(this);
            }
        }
    }
    
    

}
