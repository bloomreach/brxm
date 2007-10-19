/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.repository.api.HippoNode;

public class JcrNodeModel implements TreeNode, IWrapModel, IDataProvider {
    private static final long serialVersionUID = 1L;

    // The Item model that is wrapped by this model using the IWrapmodel interface
    private JcrItemModel itemModel;
    
    // Parent node, null if this node represents a root node
    private JcrNodeModel parent;

    // Constructor

    public JcrNodeModel(JcrNodeModel parent, Node node) {
        this.parent = parent;
        itemModel = new JcrItemModel(node);
    }

    // Rootnode factory

    public static JcrNodeModel getRootModel() {
        JcrNodeModel result;
        try {
            UserSession wicketSession = (UserSession) org.apache.wicket.Session.get();
            javax.jcr.Session jcrSession = wicketSession.getJcrSession();
            if (jcrSession == null) {
                result = null;
            } else {
                result = new JcrNodeModel(null, jcrSession.getRootNode());
            }
        } catch (RepositoryException e) {
            result = null;
        }
        return result;
    }

    // Convenience methods, not part of an api

    public HippoNode getNode() {
        return (HippoNode) itemModel.getObject();
    }

    public void impersonate(JcrNodeModel model) {
        if (model != null) {
            itemModel = new JcrItemModel(model.getNode());
            parent = model.parent;
        }
    }
    
    // TreeNode implementation for use in trees

    public Enumeration children() {
        return Collections.enumeration(getChildren());
    }

    public TreeNode getChildAt(int i) {
        return (TreeNode) getChildren().get(i);
    }

    public int getChildCount() {
        return getChildren().size();
    }

    public int getIndex(TreeNode node) {
        return getChildren().indexOf(node);
    }

    public boolean isLeaf() {
        return getChildren().size() == 0;
    }

    public TreeNode getParent() {
        return parent;
    }

    public boolean getAllowsChildren() {
        return true;
    }


    // Implement IWrapModel, all IModel calls done by wicket components 
    // (subclasses of org.apache.wicket.Component) are redirected to this wrapped model. 

    public IModel getWrappedModel() {
        return itemModel;
    }

    // This takes care that bean property calls on the wrapped model are wired through
    public Object getObject() {
        return itemModel.getObject();
    }

    // IDataProvider implementation for use in DataViews 
    // (subclasses of org.apache.wicket.markup.repeater.data.DataViewBase)
    //  TODO this is for properties, need the same thing for mixintypes

    public Iterator iterator(int first, int count) {
        List list = new ArrayList();
        try {
            if (getNode() != null) {
                PropertyIterator it = getNode().getProperties();
                if (it.getSize() > 0) {
                    it.skip(first);
                    for (int i = 0; i < count; i++) {
                        Property prop = it.nextProperty();
                        if (prop != null) {
                            list.add(prop);
                        }
                    }
                }
            }
        } catch (InvalidItemStateException e) {
            // This can happen after a node has been deleted and the tree hasn't been refreshed yet
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return list.iterator();
    }

    public IModel model(Object object) {
        Property prop = (Property) object;
        return new JcrPropertyModel(this, prop);
    }

    public int size() {
        int result = 0;
        try {
            if (getNode() != null) {
                result = new Long(getNode().getProperties().getSize()).intValue();
            }
        } catch (InvalidItemStateException e) {
            // This can happen after a node has been deleted and the tree hasn't been refreshed yet
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return result;
    }

    // Empty implementation of the 'write' methods of IModel,
    // all model calls are redirected to the wrapped model. 

    public void setObject(Object object) {
    }

    public void detach() {
    }

    // override Object
    // TODO: add properties to this

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("item", itemModel).toString();
    }

    public boolean equals(Object object) {
        if (object instanceof JcrNodeModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrNodeModel nodeModel = (JcrNodeModel) object;
        return new EqualsBuilder().append(itemModel, nodeModel.itemModel).isEquals();

    }

    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(itemModel).toHashCode();
    }

    
    // privates
    
    private List getChildren() {
        Node jcrNode = getNode();
        List children = new ArrayList();
        try {
            NodeIterator jcrChildren = jcrNode.getNodes();
            while (jcrChildren.hasNext()) {
                Node jcrChild = jcrChildren.nextNode();
                if (jcrChild != null) {
                    children.add(new JcrNodeModel(this, jcrChild));
                }
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return children;
    }

}
