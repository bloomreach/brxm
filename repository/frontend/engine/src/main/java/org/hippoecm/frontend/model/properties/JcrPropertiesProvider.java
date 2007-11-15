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

public class JcrPropertiesProvider extends NodeModelWrapper implements IDataProvider {
    private static final long serialVersionUID = 1L;

    // Constructor

    public JcrPropertiesProvider(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    // IDataProvider implementation, provides the properties of the chained nodeModel

    public Iterator iterator(int first, int count) {
        List list = new ArrayList();
        try {
            if (nodeModel.getNode() != null) {
                PropertyIterator it = nodeModel.getNode().getProperties();
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
            if (nodeModel.getNode() != null) {
                PropertyIterator it = nodeModel.getNode().getProperties();
                result = new Long(it.getSize()).intValue();
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return result;
    }

    // override Object

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("nodeModel", nodeModel.toString())
            .toString();
    }

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

    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(nodeModel)
            .toHashCode();
    }

}
