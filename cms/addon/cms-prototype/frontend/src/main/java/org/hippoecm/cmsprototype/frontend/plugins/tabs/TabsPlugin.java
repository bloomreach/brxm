package org.hippoecm.cmsprototype.frontend.plugins.tabs;

import java.util.ArrayList;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;

public class TabsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private ArrayList tabs;

    public TabsPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        tabs = new ArrayList();
        add(new AjaxTabbedPanel("tabs", tabs));
        
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

}
