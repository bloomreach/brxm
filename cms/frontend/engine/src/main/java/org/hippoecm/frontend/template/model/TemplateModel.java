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

import javax.jcr.Node;
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
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class TemplateModel extends NodeModelWrapper implements IPluginModel {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateModel.class);

    private TemplateDescriptor template;
    private String name;
    private int index;

    //  Constructor
    public TemplateModel(TemplateDescriptor descriptor, JcrNodeModel model, String name, int index) {
        super(model);
        this.template = descriptor;
        this.name = name;
        this.index = index;
    }

    public TemplateModel(IPluginModel model) {
        super(new JcrNodeModel(model));
        Map<String, Object> map = model.getMapRepresentation();
//        this.template = new TemplateDescriptor((Map) map.get("template"));
        this.template = (TemplateDescriptor) map.get("template");
        this.name = (String) map.get("name");
        this.index = ((Integer) map.get("index")).intValue();
    }

    // implement IPluginModel

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = getNodeModel().getMapRepresentation();
//        map.put("template", template.getMapRepresentation());
        map.put("template", template);
        map.put("name", name);
        map.put("index", new Integer(index));
        return map;
    }

    public TemplateDescriptor getTemplateDescriptor() {
        return template;
    }

    public String getPath() {
        return name + "[" + index + "]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        try {
            Node parent = getNodeModel().getNode();
            if (template.getTypeDescriptor().isNode()) {
                if (this.name != name) {
                    javax.jcr.Session jcrSession = ((UserSession) Session.get()).getJcrSession();
                    jcrSession.move(parent.getPath() + "/" + getPath(), parent.getPath() + "/" + name);
                    this.name = name;
                    this.index = (int) parent.getNodes(name).getSize();
                }
            } else {
                log.warn("renaming values in properties is not supported");
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isNew() {
        return (name == null);
    }

    public JcrNodeModel getJcrNodeModel() {
        return new JcrNodeModel(new JcrItemModel(getNodeModel().getItemModel().getPath() + "/" + getPath()));
    }

    public JcrPropertyValueModel getJcrPropertyValueModel() {
        JcrPropertyModel propertyModel = new JcrPropertyModel(getNodeModel().getItemModel().getPath() + "/" + getName());
        try {
            int index = getIndex();
            if (index == JcrPropertyValueModel.NO_INDEX) {
                return new JcrPropertyValueModel(index, propertyModel.getProperty().getValue(), propertyModel);
            } else {
                Value[] values = propertyModel.getProperty().getValues();
                if (index >= values.length || index < 0) {
                    log.error("invalid index " + index + ", should be >= 0 and < " + values.length);
                    index = 0;
                }
                return new JcrPropertyValueModel(index, values[index], propertyModel);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    // override Object methods

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("descriptor", template)
                .append("node", getNodeModel()).append("name", name).append("index", index).toString();
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
        return new EqualsBuilder().append(template, templateModel.template).append(
                nodeModel.getItemModel(), templateModel.nodeModel.getItemModel()).append(name, templateModel.name)
                .append(index, templateModel.index).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(71, 67).append(template).append(nodeModel.getItemModel()).append(name)
                .append(index).toHashCode();
    }
}
