package org.hippoecm.frontend.yui.dragdrop.node;

import java.util.Map;

import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.yui.dragdrop.PluginDragDropBehavior;

public abstract class NodeDragDropBehavior extends PluginDragDropBehavior {

    public static final String NODE_DRAGDROP_GROUP = "drag_node";

    private String nodePath;
    
    public NodeDragDropBehavior() {
        this(null);
    }
    
    public NodeDragDropBehavior(String nodePath) {
        super(NODE_DRAGDROP_GROUP);
        this.nodePath = nodePath;
    }

    public String getLabel() {
        return getNodePath();
    }
    
    public String getNodePath() {
        if(nodePath == null)
            nodePath = (String) getPlugin().getPluginModel().getMapRepresentation().get("node");
        return nodePath;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response); //render yahoo javascript dependencies first
        response.renderJavascriptReference(new JavascriptResourceReference(getHeaderContributorClass(), "DDNode.js"));
    }

    @Override
    protected Class<? extends IBehavior> getHeaderContributorClass() {
        return NodeDragDropBehavior.class;
    }
    
    @Override
    protected Map<String, Object> getHeaderContributorVariables() {
        Map<String, Object> variables = super.getHeaderContributorVariables();
        Map<String, Object> newVariables = new MiniMap(variables.size() + 1);
        newVariables.putAll(variables);
        newVariables.put("label", getLabel());
        return newVariables;
    }

}
