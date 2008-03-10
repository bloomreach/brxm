package org.hippoecm.frontend.plugins.reporting;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.empty.EmptyPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ReportPlugin.class);

    public ReportPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        Node node = new JcrNodeModel(model).getNode();
        add(createPlugin("report", pluginDescriptor, node));
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getModel());
            if (!model.equals(getModel())) {
                setPluginModel(model);
                replace(createPlugin("report", getDescriptor(), model.getNode()));
                notification.getContext().addRefresh(this);
            }
        }
        super.receive(notification);
    }

    // privates

    private Plugin createPlugin(String id, PluginDescriptor pluginDescriptor, Node node) {
        Plugin result;
        try {
            if (node.isNodeType(ReportingNodeTypes.NT_REPORT)) {
                result = createReport(id, node);
            } else {
                result = createDefault(id, pluginDescriptor, node);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            result = createDefault(id, pluginDescriptor, node);
        }
        return result;
    }

    private Plugin createReport(String id, Node reportNode) {
        String rendererClass = EmptyPlugin.class.getName();
        try {
            NodeIterator it = reportNode.getNodes();
            while (it.hasNext()) {
                Node node = it.nextNode();
                if (node.isNodeType(ReportingNodeTypes.NT_PLUGIN)) {
                    rendererClass = node.getProperty(ReportingNodeTypes.RENDERER).getString();
                    break;
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        PluginDescriptor reportRenderer = new PluginDescriptor(id, rendererClass);
        ReportModel reportModel = new ReportModel(new JcrNodeModel(reportNode));

        PluginFactory pluginFactory = new PluginFactory(getPluginManager());
        return pluginFactory.createPlugin(reportRenderer, reportModel, this);
    }

    private Plugin createDefault(String id, PluginDescriptor pluginDescriptor, Node node) {
        String rendererClass = EmptyPlugin.class.getName();
        List<String> parameter = pluginDescriptor.getParameter(ReportingNodeTypes.RENDERER);
        if (parameter != null && parameter.size() > 0) {
            rendererClass = parameter.get(0);
        } else {
            rendererClass = null;
        }

        PluginDescriptor reportRenderer = new PluginDescriptor(id, rendererClass);
        PluginFactory pluginFactory = new PluginFactory(getPluginManager());
        return pluginFactory.createPlugin(reportRenderer, new JcrNodeModel(node), this);
    }

}
