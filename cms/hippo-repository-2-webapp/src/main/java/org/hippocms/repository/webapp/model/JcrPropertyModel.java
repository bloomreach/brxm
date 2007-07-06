package org.hippocms.repository.webapp.model;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;

public class JcrPropertyModel implements IWrapModel {
    private static final long serialVersionUID = 1L;

    private JcrItemModel itemModel;

    public JcrPropertyModel(String path) {
        itemModel = new JcrItemModel(path);
    }
    
    // The wrapped jcr Property object
    
    public Property getProperty() {
        return (Property) itemModel.getObject();
    }
    
    public void setProperty(Property property) {
        try {
            itemModel = new JcrItemModel(property.getPath());
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }         
    }

    //  IWrapModel

    public IModel getWrappedModel() {
        return itemModel;
    }

    public Object getObject() {
        String result = null;
        try {
            Property prop = getProperty();
            result = prop.getValue().getString();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public void setObject(Object object) {
        try {
            Property prop = getProperty();
            prop.setValue(object.toString());
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void detach() {
        itemModel.detach();      
    }

}
