package org.hippoecm.frontend.plugins.reporting.simple;

import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.reporting.ReportModel;

public class SimpleReportPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public SimpleReportPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        String message = "Query:\n" + new JcrNodeModel(model).toString() + "\n";
        message += "\n\nResultSet:\n";
        
        if(model instanceof ReportModel) {
            ReportModel reportModel = (ReportModel)model;
            for(JcrNodeModel node : reportModel.getResultSet()) {
                message += node.toString() + "\n";
            }
        }
        
        message += "\n\n";
        message += getDescriptor().getParameters();
        
        add(new MultiLineLabel("result", new Model(message)));
    }

}