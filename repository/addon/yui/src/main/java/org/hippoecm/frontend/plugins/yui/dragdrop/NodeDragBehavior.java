package org.hippoecm.frontend.plugins.yui.dragdrop;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class NodeDragBehavior extends DragBehavior {
    private static final long serialVersionUID = 1L;
    
    final private String nodePath;
    
    public NodeDragBehavior(IPluginContext context, IPluginConfig config, String nodePath) {
        super(context, config);
        this.nodePath= nodePath;
    }
    
    @Override
    protected IModel getDragModel() {
        return new JcrNodeModel(nodePath);
    }

}
