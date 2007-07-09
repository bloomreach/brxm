package org.hippocms.repository.webapp.model;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;

public class JcrNodeModel extends DefaultMutableTreeNode implements IWrapModel {
    private static final long serialVersionUID = 1L;

    private JcrItemModel itemModel;

    // Constructors

    public JcrNodeModel() {
        itemModel = new JcrItemModel();
    }

    public JcrNodeModel(String path) {
        itemModel = new JcrItemModel(path);
    }

    public JcrNodeModel(Node node) {
        itemModel = new JcrItemModel(node);
    }

    // The wrapped jcr Node object

    public Node getNode() {
        return (Node) itemModel.getObject();
    }

    public void setNode(Node node) {
        try {
            itemModel = new JcrItemModel(node.getPath());
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // IWrapModel

    public IModel getWrappedModel() {
        return itemModel;
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
