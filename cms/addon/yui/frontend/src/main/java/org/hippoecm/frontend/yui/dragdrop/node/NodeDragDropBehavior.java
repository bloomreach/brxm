package org.hippoecm.frontend.yui.dragdrop.node;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.yui.dragdrop.PluginDragDropBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NodeDragDropBehavior extends PluginDragDropBehavior {

    private static final Logger log = LoggerFactory.getLogger(NodeDragDropBehavior.class);

    public static final String NODE_DRAGDROP_GROUP = "drag_node";

    private JcrNodeModel nodeModel;

    public NodeDragDropBehavior() {
        this(null);
    }

    public NodeDragDropBehavior(JcrNodeModel nodeModel) {
        this(nodeModel, NODE_DRAGDROP_GROUP);
    }

    public NodeDragDropBehavior(JcrNodeModel nodeModel, String... groups) {
        super(groups);
        this.nodeModel = nodeModel;
    }

    public String getNodePath() {
        return getNodeModel().getItemModel().getPath();
    }

    protected JcrNodeModel getNodeModel() {
        if (nodeModel == null) {
            IPluginModel pluginModel = getPlugin().getPluginModel();
            if(pluginModel.getMapRepresentation().get("node") != null)
                nodeModel = new JcrNodeModel(getPlugin().getPluginModel());
        }
        return nodeModel;
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

    public String getLabel() {
        if (getNodeModel() != null) {
            try {
                return getNodeModel().getNode().getDisplayName();
            } catch (RepositoryException e) {
                log.error("Error getting displayName for node [" + getNodePath() + "]", e);
            }
        }
        return null;
    }

}
