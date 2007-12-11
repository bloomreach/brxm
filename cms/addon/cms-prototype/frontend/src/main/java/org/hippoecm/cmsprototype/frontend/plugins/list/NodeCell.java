package org.hippoecm.cmsprototype.frontend.plugins.list;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.frontend.plugin.PluginManager;

public class NodeCell extends Panel {
    private static final long serialVersionUID = 1L;

    public NodeCell(String id, JcrNodeModel model) {
        super(id, model);
        AjaxLink link = new AjaxLink("link", model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Plugin owningPlugin = (Plugin)findParent(Plugin.class);
                PluginManager pluginManager = owningPlugin.getPluginManager();      
                PluginEvent event = new PluginEvent(owningPlugin, JcrEvent.NEW_MODEL, (JcrNodeModel) this.getModel());
                pluginManager.update(target, event); 
            }
        
        };
        add(link);
        link.add(new Label("label", new PropertyModel(model, "name")));
    }


}
