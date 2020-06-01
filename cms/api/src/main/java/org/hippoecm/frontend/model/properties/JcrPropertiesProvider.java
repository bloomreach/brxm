/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPropertiesProvider extends NodeModelWrapper<Void> implements IDataProvider<Property> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrPropertiesProvider.class);

    // Constructor

    public JcrPropertiesProvider(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    // IDataProvider implementation, provides the properties of the chained nodeModel

    public Iterator<Property> iterator(long first, long count) {
        List<Property> list = new ArrayList<Property>();
        try {
            if (nodeModel.getObject() != null) {
                PropertyIterator it = nodeModel.getObject().getProperties();
                if (it.getSize() > 0) {
                    it.skip(first);
                    for (int i = 0; i < count && it.hasNext(); i++) {
                        Property prop = it.nextProperty();
//                        boolean isPrimaryType = prop.getName().equals("jcr:primaryType");
//                        boolean isMixinTypes = prop.getName().equals("jcr:mixinTypes");
//                        if (!isPrimaryType && !isMixinTypes) {
                            list.add(prop);
//                        }
                    }
                }
            }
        } catch (InvalidItemStateException e) {
            // This can happen after a node has been deleted and the tree hasn't been refreshed yet
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return list.iterator();
    }

    public IModel<Property> model(Property object) {
        return new JcrPropertyModel(object);
    }

    public long size() {
        long result = 0;
        try {
            if (nodeModel.getObject() != null) {
                PropertyIterator it = nodeModel.getObject().getProperties();
                result = it.getSize();

//                result = result - 1; // For jcr:primaryType
//                result = nodeModel.getNode().hasProperty("jcr:mixinTypes") ? result -1 : result;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("nodeModel", nodeModel.toString())
            .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrPropertiesProvider == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrPropertiesProvider propertiesProvider = (JcrPropertiesProvider) object;
        return new EqualsBuilder()
            .append(nodeModel, propertiesProvider.nodeModel)
            .isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(nodeModel)
            .toHashCode();
    }

}
