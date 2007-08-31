package org.hippoecm.repository.frontend.plugin.config;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.repository.frontend.UserSession;
import org.hippoecm.repository.frontend.model.JcrNodeModel;

public class PluginRepositoryConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    private static final String pluginConfigNodePath = "configuration/frontend";
    private static final String rootPluginId = "rootPanel";
    private static final String pluginImplProperty = "renderer";

    private JcrNodeModel pluginConfigNodeModel;

    public PluginRepositoryConfig() {
        try {
            UserSession session = (UserSession) Session.get();
            Node rootNode = session.getJcrSession().getRootNode();
            pluginConfigNodeModel = new JcrNodeModel(rootNode.getNode(pluginConfigNodePath));
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public PluginDescriptor getRoot() {
        try {
            return getPluginDescriptor(pluginConfigNodeModel.getNode().getNode(rootPluginId));
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public List getChildren(PluginDescriptor pluginDescriptor) {
        List result = new ArrayList();
        try {
            Node pluginNode = getPluginNode(pluginDescriptor);
            NodeIterator it = pluginNode.getNodes();
            while (it.hasNext()) {
                Node child = it.nextNode();
                result.add(getPluginDescriptor(child));
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private Node getPluginNode(PluginDescriptor pluginDescriptor) throws RepositoryException {
        String path = pluginDescriptor.getPath().substring(2);
        path = path.replaceAll(":", "/");
        return pluginConfigNodeModel.getNode().getNode(path);
    }

    private PluginDescriptor getPluginDescriptor(Node pluginNode) throws RepositoryException {
        String classname = pluginNode.getProperty(pluginImplProperty).getString();
        String path = pluginNode.getPath().substring(1);
        path = path.replaceAll(pluginConfigNodePath, "0");
        path = path.replaceAll("/", ":");
        return new PluginDescriptor(path, classname);
    }

}
