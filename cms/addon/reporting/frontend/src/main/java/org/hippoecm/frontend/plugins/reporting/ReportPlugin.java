package org.hippoecm.frontend.plugins.reporting;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.config.PluginConfig;
import org.hippoecm.frontend.plugin.config.PluginRepositoryConfig;
import org.hippoecm.frontend.plugin.empty.EmptyPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ReportPlugin.class);

    public ReportPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        String reportId = getDescriptor().getParameter("report").getStrings().get(0);
        Node node;
        if (reportId != null) {
            Session session = ((UserSession) getSession()).getJcrSession();
            try {
                node = session.getNodeByUUID(reportId);
            } catch (RepositoryException e) {
                node = new JcrNodeModel(model).getNode();
            }
        } else {
            node = new JcrNodeModel(model).getNode();
        }
        add(createReport("report", node));
    }

//    @Override
//    public void receive(Notification notification) {
//        if ("select".equals(notification.getOperation())) {
//            JcrNodeModel model = new JcrNodeModel(notification.getModel());
//            if (!model.equals(getModel())) {
//                setPluginModel(model);
//                replace(createReport("report", model.getNode()));
//                notification.getContext().addRefresh(this);
//            }
//        }
//        super.receive(notification);
//    }

    // privates

    private Plugin createReport(String id, Node reportNode) {
        PluginDescriptor pluginDescriptor = null;
        try {
            if (reportNode.isNodeType(ReportingNodeTypes.NT_REPORT)) {
                String basePath = reportNode.getPath().substring(1);
                PluginConfig pluginConfig = new PluginRepositoryConfig(basePath);
                pluginDescriptor = pluginConfig.getPlugin(ReportingNodeTypes.PLUGIN);
                pluginDescriptor.setWicketId(id);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        if (pluginDescriptor == null) {
            pluginDescriptor = new PluginDescriptor(id, EmptyPlugin.class.getName());
        }
        ReportModel reportModel = new ReportModel(new JcrNodeModel(reportNode));

        PluginFactory pluginFactory = new PluginFactory(getPluginManager());
        return pluginFactory.createPlugin(pluginDescriptor, reportModel, this);
    }

}
