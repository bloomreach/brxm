/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.template.model;

import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Session;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateModel extends NodeModelWrapper implements IPluginModel {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateModel.class);

    private TemplateDescriptor templateDescriptor;
    private String path;

    //  Constructor
    public TemplateModel(TemplateDescriptor descriptor, JcrNodeModel model, String path) {
        super(model);
        this.templateDescriptor = descriptor;
        this.path = path;
    }

    public TemplateModel(IPluginModel model, TemplateEngine engine) {
        super(new JcrNodeModel(model));
        Map<String, Object> map = model.getMapRepresentation();
        this.templateDescriptor = new TemplateDescriptor((Map) map.get("template"), engine);
        this.path = (String) map.get("path");
    }

    // implement IPluginModel

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = getNodeModel().getMapRepresentation();
        map.put("template", templateDescriptor.getMapRepresentation());
        map.put("path", path);
        return map;
    }

    public TemplateDescriptor getTemplateDescriptor() {
        return templateDescriptor;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        try {
            Node node = getNodeModel().getNode();
            if (templateDescriptor.isNode()) {
                if (this.path == null) {
                    Node child = node.addNode(path, templateDescriptor.getType());
                    this.path = child.getName() + "[" + child.getIndex() + "]";
                } else if (this.path != path) {
                    javax.jcr.Session jcrSession = ((UserSession) Session.get()).getJcrSession();
                    jcrSession.move(node.getPath() + "/" + this.path, node.getPath() + "/" + path);
                    this.path = path;
                }
            } else {
                if (this.path == null) {
                    Value value = templateDescriptor.createValue("");
                    if (templateDescriptor.isMultiple()) {
                        node.setProperty(path, new Value[] { value });
                    } else {
                        node.setProperty(path, value);
                    }
                    this.path = path;
                } else if (this.path != path) {
                    Property prop = node.getProperty(this.path);
                    if (templateDescriptor.isMultiple()) {
                        Value[] values = prop.getValues();
                        node.setProperty(path, values);
                        prop.remove();
                    } else {
                        Value value = prop.getValue();
                        node.setProperty(path, value);
                        prop.remove();
                    }
                    this.path = path;
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public boolean isNew() {
        return (path == null);
    }

    public void remove() {
        Node node = getNodeModel().getNode();
        try {
            JcrItemModel itemModel = new JcrItemModel(node.getPath() + "/" + path);

            if (itemModel.exists()) {
                Item item = (Item) itemModel.getObject();

                // remove the item
                log.info("removing item " + item.getPath());
                item.remove();
            } else {
                log.info("item " + itemModel.getPath() + " does not exist");
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    // override Object methods

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("descriptor", templateDescriptor)
                .append("node", getNodeModel()).append("path", path).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof TemplateModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        TemplateModel templateModel = (TemplateModel) object;
        return new EqualsBuilder().append(templateDescriptor, templateModel.templateDescriptor).append(nodeModel.getItemModel(),
                templateModel.nodeModel.getItemModel()).append(path, templateModel.path).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(71, 67).append(templateDescriptor).append(nodeModel.getItemModel()).append(path).toHashCode();
    }
}
