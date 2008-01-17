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
package org.hippoecm.frontend.plugins.template.model;

import java.util.Iterator;
import java.util.LinkedList;

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
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.template.config.FieldDescriptor;
import org.hippoecm.frontend.plugins.template.config.TemplateDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides FieldModel instances based on a template descriptor.
 */
public class FieldProvider extends AbstractProvider implements IDataProvider {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(FieldProvider.class);

    private TemplateDescriptor descriptor;

    // Constructor

    public FieldProvider(TemplateDescriptor descriptor, JcrNodeModel nodeModel) {
        super(nodeModel);
        this.descriptor = descriptor;
    }

    public void setDescriptor(TemplateDescriptor descriptor) {
        this.descriptor = descriptor;
        detach();
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("descriptor", descriptor.toString())
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof FieldProvider == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        FieldProvider fieldProvider = (FieldProvider) object;
        return new EqualsBuilder().append(descriptor, fieldProvider.descriptor)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(descriptor).toHashCode();
    }

    // handle wildcard expansion

    private void expandNodeWildcard(Node node) throws RepositoryException {
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
                FieldDescriptor desc = new FieldDescriptor(next.getName(), next.getName());
                desc.setType(template);
                addField(desc, new JcrItemModel(next));
            }
        }
    }

    private void expandPropertyWildcard(Node node) throws RepositoryException {
        PropertyIterator iterator = node.getProperties();
        while (iterator.hasNext()) {
            Property next = iterator.nextProperty();
            if (!descriptor.hasField(next.getName())) {
                FieldDescriptor desc = new FieldDescriptor(next.getName(), next.getName());
                addField(desc, new JcrItemModel(next));
            }
        }
    }

    private void loadField(FieldDescriptor field, Node node) throws RepositoryException {
        JcrItemModel model = null;
        model = new JcrItemModel(node);
        addField(field, model);
    }

    private void addField(FieldDescriptor field, JcrItemModel model) {
        fields.addLast(new FieldModel(field, model));
    }

    // internal (lazy) loading of fields

    @Override
    protected void load() {
        if (fields != null) {
            return;
        }

        Node node = getNodeModel().getNode();
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
                    log.error(ex.getMessage());
                }
            }
        }
    }
}
