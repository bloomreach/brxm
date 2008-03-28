package org.hippoecm.frontend.plugins.cms.dashboard;

import java.util.List;

import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class TodoPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public TodoPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        
//        String message = "Todo:\n\n";        
//        List<String> entries = (List<String>)model.getMapRepresentation().get("entries");
//        for(String entry: entries) {
//            message += entry + "\n";
//        }
        String message = "";
        add(new MultiLineLabel("result", new Model(message)));
    }

}
