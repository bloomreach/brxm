/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.instructions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.onehippo.repository.bootstrap.InitializeInstruction;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.PostStartupTask;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPSET;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

public class ContentPropSetInstruction extends InitializeInstruction {

    public ContentPropSetInstruction(final InitializeItem item, final Session session) {
        super(item, session);
    }

    @Override
    protected String getName() {
        return HIPPO_CONTENTPROPSET;
    }

    @Override
    public PostStartupTask execute() throws RepositoryException {
        Property contentSetProperty = item.getContentPropSetProperty();
        String contentRoot = item.getContentRoot();

        if (session.propertyExists(contentRoot)) {
            final Property property = session.getProperty(contentRoot);
            if (property.isMultiple()) {
                property.setValue(contentSetProperty.getValues());
            } else {
                final Value[] values = contentSetProperty.getValues();
                if (values.length == 0) {
                    property.remove();
                } else if (values.length == 1) {
                    property.setValue(values[0]);
                } else {
                    log.warn("Invalid content prop set item {}: Cannot set multiple values on a single valued property", item.getName());
                }
            }
        } else {
            final int offset = contentRoot.lastIndexOf('/');
            final String targetNodePath = offset == 0 ? "/" : contentRoot.substring(0, offset);
            final String propertyName = contentRoot.substring(offset+1);
            final Value[] values = contentSetProperty.getValues();
            if (values.length == 0) {
                log.warn("Invalid content prop set item {}: No property value(s) specified");
            } else if (values.length == 1) {
                final Node target = session.getNode(targetNodePath);
                if (isMultiple(target, propertyName)) {
                    target.setProperty(propertyName, values);
                } else {
                    target.setProperty(propertyName, values[0]);
                }
            } else {
                session.getNode(targetNodePath).setProperty(propertyName, values);
            }
        }
        return null;
    }

    private static boolean isMultiple(final Node target, final String propertyName) throws RepositoryException {
        final List<NodeType> nodeTypes = new ArrayList<>(Arrays.asList(target.getMixinNodeTypes()));
        nodeTypes.add(target.getPrimaryNodeType());
        for (NodeType nodeType : nodeTypes) {
            for (PropertyDefinition propertyDefinition : nodeType.getPropertyDefinitions()) {
                if (propertyDefinition.getName().equals("*") || propertyDefinition.getName().equals(propertyName)) {
                    return propertyDefinition.isMultiple();
                }
            }
        }
        return false;
    }

}
