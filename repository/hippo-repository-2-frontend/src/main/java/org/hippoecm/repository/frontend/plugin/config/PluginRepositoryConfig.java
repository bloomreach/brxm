package org.hippoecm.repository.frontend.plugin.config;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.repository.frontend.UserSession;
import org.hippoecm.repository.frontend.model.JcrNodeModel;

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

    public Map getPluginMap() {
        Map result = new HashMap();
        try {
            PropertyIterator it = pluginConfig.getNode().getProperties("*Panel");
            while (it.hasNext()) {
                Property prop = it.nextProperty();
                result.put(prop.getName(), prop.getString());
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

}
