package org.hippoecm.cmsprototype.frontend.plugins.tabs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.config.PluginConfig;

public class TabsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;
    
    ArrayList tabs;
    
    public TabsPlugin(String id, JcrNodeModel model) {
        super(id, model);
        tabs = new ArrayList();
    }

    public void addChildren(PluginConfig pluginConfig) {
        List children = pluginConfig.getChildren(new PluginDescriptor(this));
        Iterator it = children.iterator();
        List plugins = new ArrayList();
        while (it.hasNext()) {
            PluginDescriptor childDescriptor = (PluginDescriptor) it.next();
            PluginDescriptor tabDescriptor = new PluginDescriptor(childDescriptor.getPath(), childDescriptor.getClassName(),
                    TabbedPanel.TAB_PANEL_ID);
            final Plugin child = new PluginFactory(tabDescriptor).getPlugin((JcrNodeModel) getModel());
            
            tabs.add(new AbstractTab(new Model(childDescriptor.getId())) {
                private static final long serialVersionUID = 1L;

                public Panel getPanel(String panelId) {
                    return child;
                }
            });
            plugins.add(child);
        }
        add(new AjaxTabbedPanel("tabs", tabs));
//        it= plugins.iterator();
//        while(it.hasNext()) {
//            Plugin child = (Plugin) it.next();
//            child.addChildren(pluginConfig);
//        }
    }

    @Override
    public void update(AjaxRequestTarget target, JcrEvent model) {
        
    }

}
