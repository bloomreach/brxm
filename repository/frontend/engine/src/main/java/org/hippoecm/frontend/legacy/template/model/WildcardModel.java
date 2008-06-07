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
package org.hippoecm.frontend.legacy.template.model;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.Session;
import org.hippoecm.frontend.legacy.template.FieldDescriptor;
import org.hippoecm.frontend.legacy.template.ItemDescriptor;
import org.hippoecm.frontend.legacy.template.TypeDescriptor;
import org.hippoecm.frontend.legacy.template.config.TypeConfig;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class WildcardModel extends ItemModel {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(WildcardModel.class);

    private int id;
    private TypeDescriptor type;
    private FieldDescriptor field;

    public WildcardModel(ItemDescriptor descriptor, TypeConfig config, JcrNodeModel parent, String path, int id) {
        super(descriptor, parent);

        field = descriptor.getTemplate().getTypeDescriptor().getField(descriptor.getField()).clone();
        field.setPath(path);

        this.type = config.getTypeDescriptor(field.getType());
        this.id = id;
    }

    public String getPath() {
        return field.getPath();
    }

    public void setPath(String path) {
        try {
            Node node = getNodeModel().getNode();
            if (type.isNode()) {
                if (field.getPath() == null) {
                    node.addNode(path, type.getType());
                } else if (field.getPath() != path) {
                    javax.jcr.Session jcrSession = ((UserSession) Session.get()).getJcrSession();
                    jcrSession.move(node.getPath() + "/" + field.getPath(), node.getPath() + "/" + path);
                }
            } else {
                if (field.getPath() == null) {
                    Value value = type.createValue();
                    if (field.isMultiple()) {
                        node.setProperty(path, new Value[] { value });
                    } else {
                        node.setProperty(path, value);
                    }
                } else if (field.getPath() != path) {
                    Property prop = node.getProperty(field.getPath());
                    if (field.isMultiple()) {
                        Value[] values = prop.getValues();
                        node.setProperty(path, values);
                        prop.remove();
                    } else {
                        Value value = prop.getValue();
                        node.setProperty(path, value);
                        prop.remove();
                    }
                }
            }
            field.setPath(path);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof WildcardModel)) {
            return false;
        }
        if (object == this) {
            return true;
        }
        return super.equals(object) && ((WildcardModel) object).id == this.id;
    }
}
