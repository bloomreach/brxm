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
package org.hippoecm.frontend.plugins.template;

import java.util.Iterator;
import java.util.LinkedList;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateProvider extends JcrNodeModel implements IDataProvider {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TemplateProvider.class);

    private TemplateDescriptor descriptor;
    private TemplateEngine engine;
    private LinkedList<FieldModel> fields;

    // Constructor

    public TemplateProvider(TemplateDescriptor descriptor, Node node, TemplateEngine engine) {
        super(node);
        this.descriptor = descriptor;
        this.engine = engine;
    }

    public void setDescriptor(TemplateDescriptor descriptor) {
        this.descriptor = descriptor;
        fields = null;
    }

    @Override
    public void setChainedModel(IModel model) {
        fields = null;
        super.setChainedModel(model);
    }

    // IDataProvider implementation, provides the fields of the chained itemModel

    public Iterator<FieldModel> iterator(int first, int count) {
        load();
        return fields.subList(first, first + count).iterator();
    }

    public IModel model(Object object) {
        FieldModel model = (FieldModel) object;
        return model;
    }

    public int size() {
        load();
        return fields.size();
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("descriptor", descriptor.toString())
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof TemplateProvider == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        TemplateProvider fieldProvider = (TemplateProvider) object;
        return new EqualsBuilder().append(engine, fieldProvider.engine).append(descriptor, fieldProvider.descriptor)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(engine).append(descriptor).toHashCode();
    }

    // handle wildcard expansion

    protected void expandNodeWildcard(Node node) throws RepositoryException {
        // FIXME: a separate template should be loaded, i.e.
        // the FieldModel should be able to describe "multiple"
        NodeIterator iterator = node.getNodes();
        while (iterator.hasNext()) {
            Node next = iterator.nextNode();
            if (!descriptor.hasField(next.getName())) {
                String template = null;
                if (next.getPrimaryNodeType() != null) {
                    template = next.getPrimaryNodeType().getName();
                }
                FieldDescriptor desc = new FieldDescriptor(next.getName(), next.getName(), template, null);
                addField(desc, next);
            }
        }
    }

    protected void expandPropertyWildcard(Node node) throws RepositoryException {
        PropertyIterator iterator = node.getProperties();
        while (iterator.hasNext()) {
            Property next = iterator.nextProperty();
            if (!descriptor.hasField(next.getName())) {
                FieldDescriptor desc = new FieldDescriptor(next.getName(), next.getName(), null, null);
                addField(desc, next);
            }
        }
    }

    protected void loadField(FieldDescriptor field, Node node) throws RepositoryException {
        Item item = null;
        if (field.isNode()) {
            if (!node.hasNode(field.getPath())) {
                item = node.addNode(field.getPath(), field.getType());
            } else {
                item = node.getNode(field.getPath());
            }
        } else {
            if (!node.hasProperty(field.getPath())) {
                addField(field, node.getPath() + "/" + field.getPath());
            } else {
                item = node.getProperty(field.getPath());
            }
        }
        if (item != null) {
            addField(field, item);
        }
    }

    private void addField(FieldDescriptor field, Item item) {
        fields.addLast(new FieldModel(field, item));
    }

    private void addField(FieldDescriptor field, String path) {
        fields.addLast(new FieldModel(field, path));
    }

    // internal (lazy) loading of fields

    private void load() {
        if (fields != null) {
            return;
        }

        Node node = getNode();
        fields = new LinkedList<FieldModel>();
        if (descriptor != null) {
            Iterator<FieldDescriptor> iter = descriptor.getFieldIterator();
            while (iter.hasNext()) {
                FieldDescriptor field = iter.next();
                try {
                    if (field.getPath().equals("*")) {
                        if (field.isNode()) {
                            expandNodeWildcard(node);
                        } else {
                            expandPropertyWildcard(node);
                        }
                    } else {
                        loadField(field, node);
                    }
                } catch (RepositoryException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
