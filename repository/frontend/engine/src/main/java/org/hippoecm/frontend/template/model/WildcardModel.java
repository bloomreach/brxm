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
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WildcardModel extends FieldModel {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(WildcardModel.class);

    public WildcardModel(FieldDescriptor descriptor, JcrNodeModel parent) {
        super(descriptor, parent);
    }

    public String getPath() {
        return getDescriptor().getPath();
    }

    public void setPath(String path) {
        try {
            Node node = getNodeModel().getNode();
            FieldDescriptor descriptor = getDescriptor();
            if (descriptor.isNode()) {
                if (descriptor.getPath() == null) {
                    node.addNode(path, descriptor.getTemplate().getType());
                } else if (descriptor.getPath() != path) {
                    javax.jcr.Session jcrSession = ((UserSession) Session.get()).getJcrSession();
                    jcrSession.move(node.getPath() + "/" + descriptor.getPath(), node.getPath() + "/" + path);
                }
            } else {
                TemplateDescriptor templateDescriptor = descriptor.getTemplate();
                if (descriptor.getPath() == null) {
                    Value value = templateDescriptor.createValue("");
                    if (templateDescriptor.isMultiple()) {
                        node.setProperty(path, new Value[] { value });
                    } else {
                        node.setProperty(path, value);
                    }
                } else if (descriptor.getPath() != path) {
                    Property prop = node.getProperty(descriptor.getPath());
                    if (templateDescriptor.isMultiple()) {
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
}
