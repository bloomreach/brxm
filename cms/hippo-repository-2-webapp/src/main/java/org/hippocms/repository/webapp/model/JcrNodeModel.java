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
package org.hippocms.repository.webapp.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;

public class JcrNodeModel extends DefaultMutableTreeNode implements IWrapModel, IDataProvider {
    private static final long serialVersionUID = 1L;

    // The Item model that is wrapped by this model using the IWrapmodel interface
    private JcrItemModel itemModel;

    // Constructor

    public JcrNodeModel(Node node) {
        itemModel = new JcrItemModel(node);
    }

    // The wrapped jcr Node object, convenience methods and not part of an api

    public Node getNode() {
        return (Node) itemModel.getObject();
    }

    public void setNode(Node node) {
        if (node != null) {
            itemModel = new JcrItemModel(node);
        }
    }

    // Override DefaultMutableTreeNode, called when used as a TreeNode
    // by a subclass of org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree
    
    public boolean isLeaf() {
        boolean result = true;
        if (getNode() != null) {
            try {
                result = !getNode().hasNodes();
            } catch (InvalidItemStateException e) {
                // This can happen after a node has been deleted and the tree hasn't been refreshed yet
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
        return result;
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

    public Iterator iterator(int first, int count) {
        List list = new ArrayList();
        try {
            if (getNode() != null) {
                PropertyIterator it = getNode().getProperties();
                if (it.getSize() > 0) {
                    it.skip(first);
                    for (int i = 0; i < count; i++) {
                        list.add(it.nextProperty());
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
        return new JcrPropertyModel(prop);
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

}
