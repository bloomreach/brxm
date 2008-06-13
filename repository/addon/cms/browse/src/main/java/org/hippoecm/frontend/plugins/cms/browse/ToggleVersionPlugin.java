package org.hippoecm.frontend.plugins.cms.browse;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class ToggleVersionPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;
    
    public ToggleVersionPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        add(new Label("toggleLabel"));
        if(getModel() != null) {
            onModelChanged();
        }
    }
    
    @Override
    protected void onModelChanged() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        try {
            Node node = model.getNode().getCanonicalNode();
            if(node.isNodeType(HippoNodeType.NT_HANDLE)) {
                replace(new ToggleVersionPanel("toggleLabel"));
            } else {
                replace(new Label("toggleLabel"));
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }
    
}
