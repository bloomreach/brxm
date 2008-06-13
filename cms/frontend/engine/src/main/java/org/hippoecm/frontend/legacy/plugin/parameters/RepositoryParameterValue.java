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
package org.hippoecm.frontend.legacy.plugin.parameters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class RepositoryParameterValue extends ParameterValue implements IModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RepositoryParameterValue.class);

    private JcrNodeModel nodeModel;
    private transient Map<String, ParameterValue> map;
    private transient boolean loaded = false;

    public RepositoryParameterValue(Node node) {
        super((Map<String, ParameterValue>) null);

        nodeModel = new JcrNodeModel(node);
    }

    public Map<String, ParameterValue> getMap() {
        load();
        return map;
    }

    // implement IModel

    public Object getObject() {
        load();
        return map;
    }

    public void setObject(Object object) {
        if (object instanceof Map) {
            try {
                map = null;
                loaded = false;

                Node node = nodeModel.getNode();

                // remove existing parameters

                PropertyIterator propIter = node.getProperties();
                while (propIter.hasNext()) {
                    Property property = propIter.nextProperty();
                    PropertyDefinition definition = property.getDefinition();
                    if (!definition.isProtected()) {
                        property.remove();
                    }
                }

                NodeIterator nodeIter = node.getNodes();
                while (nodeIter.hasNext()) {
                    Node child = nodeIter.nextNode();
                    child.remove();
                }

                // insert new parameters

                for (Map.Entry<String, ParameterValue> entry : map.entrySet()) {
                    switch (entry.getValue().getType()) {
                    case ParameterValue.TYPE_BOOLEAN:
                        node.setProperty(entry.getKey(), entry.getValue().getBoolean());
                        break;
                    case ParameterValue.TYPE_STRING:
                        List<String> paramValues = entry.getValue().getStrings();
                        Value[] values = new Value[paramValues.size()];
                        int i = 0;
                        for (String paramValue : paramValues) {
                            values[i++] = new StringValue(paramValue);
                        }
                        node.setProperty(entry.getKey(), values);
                        break;
                    case ParameterValue.TYPE_MAP:
                        Node child = node.addNode(entry.getKey(), HippoNodeType.NT_PARAMETERS);
                        RepositoryParameterValue value = new RepositoryParameterValue(child);
                        value.setObject(entry.getValue().getMap());
                        break;
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    public void detach() {
        map = null;
        loaded = false;
        nodeModel.detach();
    }

    // privates

    private void load() {
        if (!loaded) {
            map = new HashMap<String, ParameterValue>();
            try {
                Node node = nodeModel.getNode();
                PropertyIterator propIter = node.getProperties();
                while (propIter.hasNext()) {
                    Property property = propIter.nextProperty();
                    PropertyDefinition definition = property.getDefinition();
                    if (!definition.isProtected()) {
                        if (definition.getRequiredType() == PropertyType.STRING) {
                            Value[] values = property.getValues();
                            List<String> list = new LinkedList<String>();
                            for (Value value : values) {
                                list.add(value.getString());
                            }
                            map.put(property.getName(), new ParameterValue(list));
                        } else if (definition.getRequiredType() == PropertyType.BOOLEAN) {
                            map.put(property.getName(), new ParameterValue(property.getValue().getBoolean()));
                        }
                    }
                }
                NodeIterator nodeIter = node.getNodes();
                while (nodeIter.hasNext()) {
                    Node child = nodeIter.nextNode();
                    map.put(child.getName(), new RepositoryParameterValue(child));
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            loaded = true;
        }
    }
}
