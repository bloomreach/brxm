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
    
    //IDataProvider
    
    public Iterator iterator(int first, int count) {
        List list = new ArrayList();
        try {
            PropertyIterator it = getNode().getProperties();
            it.skip(first);            
            for (int i = 0; i < count; i++) {
                list.add(it.nextProperty());
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return list.iterator();
    }

    public IModel model(Object object) {
        Property prop = (Property) object;
        JcrPropertyModel result = null;
        try {
            result = new JcrPropertyModel(prop.getPath());
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public int size() {
        int result = 0;
        try {
            result = new Long(getNode().getProperties().getSize()).intValue();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

}
