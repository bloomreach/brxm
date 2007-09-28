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
import java.util.Iterator;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.hippoecm.repository.api.HippoNode;

public class JcrNodeModel implements IWrapModel, IDataProvider {
    private static final long serialVersionUID = 1L;

    // The Item model that is wrapped by this model using the IWrapmodel interface
    private JcrItemModel itemModel;
    
    private JcrNodeModelState state;

    // Constructor

    public JcrNodeModel(Node node) {
        itemModel = new JcrItemModel(node);
        state = new JcrNodeModelState(JcrNodeModelState.UNCHANGED);
    }

    // The wrapped jcr Node object, convenience methods and not part of an api

    public HippoNode getNode() {
        HippoNode result = (HippoNode) itemModel.getObject();
        
        boolean sessionClosed = false;
        try {
            if (result == null || result.getSession() == null || !result.getSession().isLive()) {
                sessionClosed = true;
            }
        } catch (RepositoryException e) {
           sessionClosed = true;
        }
        if (sessionClosed) {
            itemModel = new JcrItemModel(itemModel.path);
        }
        
        return result;
    }

    public void setNode(HippoNode node) {
        if (node != null) {
            itemModel = new JcrItemModel(node);
        }
    }
    
    public String toString() {
        try {
            return getNode().getPath();
        } catch (RepositoryException e) {
            return null;
        }
    }
    
    
    public JcrNodeModelState getState() {
        return this.state;
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

    public boolean equals(JcrNodeModel jcrNodeModel) {
        if (jcrNodeModel == null) {
        	// nothing to compare
            return false;
        }
        if (getNode() == null) {
        	// null is null is null
            return jcrNodeModel.getNode() == null;
        }
        return getNode().equals(jcrNodeModel.getNode());
    }
    
}
