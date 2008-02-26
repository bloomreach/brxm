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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WildcardModel extends ItemModel {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(WildcardModel.class);

    private int id;
    private TypeDescriptor type;

    public WildcardModel(FieldDescriptor descriptor, TypeConfig config, JcrNodeModel parent, int id) {
        super(descriptor, parent);
        this.type = config.getTypeDescriptor(descriptor.getType());
        this.id = id;
    }

    public String getPath() {
        return ((FieldDescriptor) getDescriptor()).getPath();
    }

    public void setPath(String path) {
        try {
            Node node = getNodeModel().getNode();
            FieldDescriptor descriptor = (FieldDescriptor) getDescriptor();
            if (type.isNode()) {
                if (descriptor.getPath() == null) {
                    node.addNode(path, type.getType());
                } else if (descriptor.getPath() != path) {
                    javax.jcr.Session jcrSession = ((UserSession) Session.get()).getJcrSession();
                    jcrSession.move(node.getPath() + "/" + descriptor.getPath(), node.getPath() + "/" + path);
                }
            } else {
                if (descriptor.getPath() == null) {
                    Value value = type.createValue("");
                    if (descriptor.isMultiple()) {
                        node.setProperty(path, new Value[] { value });
                    } else {
                        node.setProperty(path, value);
                    }
                } else if (descriptor.getPath() != path) {
                    Property prop = node.getProperty(descriptor.getPath());
                    if (descriptor.isMultiple()) {
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
            descriptor.setPath(path);
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
