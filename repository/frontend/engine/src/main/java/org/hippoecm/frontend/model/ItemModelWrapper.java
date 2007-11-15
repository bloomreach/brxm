package org.hippoecm.frontend.model;

import javax.jcr.Item;

import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;

public abstract class ItemModelWrapper implements IChainingModel {

    protected JcrItemModel itemModel;

    public ItemModelWrapper(Item item) {
        itemModel = new JcrItemModel(item);
    }
    
    public IModel getChainedModel() {
        return itemModel;
    }

    public void setChainedModel(IModel model) {
        if (model instanceof JcrItemModel) {
            itemModel = (JcrItemModel) model;
        }
    }

    public Object getObject() {
        return itemModel.getObject();
    }

    public void setObject(Object object) {
        itemModel.setObject(object);
    }

    public void detach() {
        itemModel.detach();
    }
}
