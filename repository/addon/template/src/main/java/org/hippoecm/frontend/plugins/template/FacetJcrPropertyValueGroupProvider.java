/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.template;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// SA port: this class doesn't seem to be used; low prio
public class FacetJcrPropertyValueGroupProvider extends NodeModelWrapper implements IDataProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(FacetJcrPropertyValueGroupProvider.class);

    private static final long serialVersionUID = 1L;
    private String[] properties;

    public FacetJcrPropertyValueGroupProvider(JcrNodeModel nodeModel, String[] properties) {
        super(nodeModel);
        this.properties = properties;
    }

    public Iterator<PropertyValueGroup> iterator(int first, int count) {
        List<PropertyValueGroup> list = new ArrayList<PropertyValueGroup>();
        try {
            if (nodeModel.getNode() != null && properties.length > 0) {
                Node node = nodeModel.getNode();

                Iterator<PropertyValueGroup> it = getValueGroup(node).iterator();

                while(it.hasNext()) {
                    PropertyValueGroup pvg = it.next();
                    if(first != 0) {
                        first--;
                    } else {
                        if(count == 0) {
                          break;
                        }
                        list.add(pvg);
                        count--;
                    }
                }
            }
        }
        catch (PropertyValueGroupException e) {
            log.error(e.getMessage());
        }
        catch (InvalidItemStateException e) {
            // This can happen after a node has been deleted and the tree hasn't been refreshed yet
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return list.iterator();
    }


    public IModel model(Object object) {
        PropertyValueGroup propertyValueGroup = (PropertyValueGroup) object;
        return propertyValueGroup;
    }

    public int size() {
        try {
            return getValueGroup(nodeModel.getNode()).size();
        } catch (Exception e) {

        }
        return 0;
    }

    class PropertyValueGroup implements IModel{

        private static final long serialVersionUID = 1L;
        private Property[] propertyGroup;
        private int index;

        public PropertyValueGroup(Property[] propertyGroup, int index) {
            this.propertyGroup = propertyGroup;
            this.index = index;
        }

        public Property[] getPropertyGroup() {
            return propertyGroup;
        }

        public int getIndex() {
            return index;
        }

        public Object getObject() {
            return this;
        }

        public void setObject(Object object) {
        }

        public void detach() {
            propertyGroup = null;
        }
    }

    private List<PropertyValueGroup> getValueGroup(Node node) throws RepositoryException, PropertyValueGroupException {
        List<PropertyValueGroup> propertyValueGroups = new ArrayList<PropertyValueGroup>();
        try {
            int numberOfValues = node.getProperty(properties[0]).getValues().length;
            for(int j = 0; j < numberOfValues ; j++) {
                Property[] propertyArray = new Property[properties.length];
                for(int i = 0 ; i < properties.length; i++) {
                    propertyArray[i] = node.getProperty(properties[i]);
                    if( i > 0) {
                        if( node.getProperty(properties[i]).getValues().length != node.getProperty(properties[i-1]).getValues().length ){
                            throw new PropertyValueGroupException(" multi valued properties of different length are not allowed ");
                        }
                    }
                }
                propertyValueGroups.add(new PropertyValueGroup(propertyArray, j));
            }

        } catch (PathNotFoundException e) {
            throw new PropertyValueGroupException("properties for PropertyGroup must all be present " + e.getMessage());

        } catch (ValueFormatException e) {
            throw new PropertyValueGroupException("properties for PropertyGroup must all be multi-valued " + e.getMessage());
        }
        return propertyValueGroups;
    }

    class PropertyValueGroupException extends Exception {

        private static final long serialVersionUID = 1L;

        public PropertyValueGroupException(String message) {
            super(message);
        }

    }

}
