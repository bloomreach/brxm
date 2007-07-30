package org.hippocms.repository.frontend.plugin.config;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippocms.repository.frontend.UserSession;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public class PluginRepositoryConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    private String pluginConfigPath = "configuration/plugins";
    private JcrNodeModel pluginConfig;
    
    public PluginRepositoryConfig() {
        try {
            UserSession session = (UserSession) Session.get();
            Node rootNode = session.getJcrSession().getRootNode();
            pluginConfig = new JcrNodeModel(rootNode.getNode(pluginConfigPath));
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String pluginClassname(String id) {
        String result;
        try {
            result = pluginConfig.getNode().getProperty(id).getString();
        } catch (RepositoryException e) {
            result = null;
        }
        return result;
    }

}
