package org.hippoecm.frontend.plugins.template.field;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeNamePlugin extends Plugin {
    private static final long serialVersionUID = 1L;
    
    static final Logger log = LoggerFactory.getLogger(NodeNamePlugin.class);

    public NodeNamePlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        
        List<String> captions = pluginDescriptor.getParameter("caption").getStrings();
        if(captions != null && captions.size() > 0) {
            add(new Label("name", captions.get(0)));
        } else {
            add(new Label("name", ""));
        }
        
        JcrNodeModel nodeModel = new JcrNodeModel(model);
        try {
            add(new Label("field", nodeModel.getNode().getName()));
        } catch (RepositoryException e) {
            log.error("Could not retrieve name of node (" + nodeModel.getItemModel().getPath() + ")", e);
        }
    }

}
