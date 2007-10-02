package org.hippoecm.frontend.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;

public class JcrLazyTreeNode extends LazyTreeNode implements Serializable {
    private static final long serialVersionUID = 1L;

    public JcrLazyTreeNode(JcrLazyTreeNode parent, JcrNodeModel jcrNodeModel) {
        super(parent, jcrNodeModel);
    }

    protected LazyTreeNode createNode(Object o) {
        JcrNodeModel jcrNodeModel = (JcrNodeModel) o; 
        JcrLazyTreeNode newTreeNode = new JcrLazyTreeNode(this, jcrNodeModel);
        return newTreeNode;
    }

    protected int getChildObjectCount() {
        int childCount = 0;
        JcrNodeModel jcrNodeModel = (JcrNodeModel) getUserObject();
        Node jcrNode = jcrNodeModel.getNode();
        try {
            NodeIterator jcrChildren = jcrNode.getNodes();
            childCount = (int) jcrChildren.getSize();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return childCount;
    }

    protected Collection getChildObjects() {
        JcrNodeModel jcrNodeModel = (JcrNodeModel) getUserObject();
        Node jcrNode = jcrNodeModel.getNode();
        Collection childObjects = new ArrayList();
        try {
            NodeIterator jcrChildren = jcrNode.getNodes();
            while (jcrChildren.hasNext()) {
                Node jcrChild = jcrChildren.nextNode();
                if (jcrChild != null) {
                    JcrNodeModel newJcrNodeModel = new JcrNodeModel(jcrChild);
                    childObjects.add(newJcrNodeModel);
                }
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return childObjects;
    }

    protected Comparator getComparator() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public JcrNodeModel getJcrNodeModel() {
        JcrNodeModel jcrNodeModel = (JcrNodeModel) getUserObject();
        return jcrNodeModel;
    }
    
}
