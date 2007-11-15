package org.hippoecm.frontend.model;

import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;

public abstract class NodeModelWrapper implements IChainingModel {

    protected JcrNodeModel nodeModel;

    public NodeModelWrapper(JcrNodeModel itemModel) {
        this.nodeModel = itemModel;
    }
    
    public IModel getChainedModel() {
        return nodeModel;
    }

    public void setChainedModel(IModel model) {
        if (model instanceof JcrNodeModel) {
            nodeModel = (JcrNodeModel) model;
        }
    }

    public Object getObject() {
        return nodeModel.getObject();
    }

    public void setObject(Object object) {
        nodeModel.setObject(object);
    }

    public void detach() {
        nodeModel.detach();
    }
}
