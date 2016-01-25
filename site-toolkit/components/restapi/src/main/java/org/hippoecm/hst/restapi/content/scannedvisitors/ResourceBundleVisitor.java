/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.restapi.content.scannedvisitors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.hst.restapi.scanning.PrimaryNodeTypeNodeVisitor;
import org.hippoecm.hst.restapi.content.visitors.HippoPublicationWorkflowDocumentVisitor;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

@PrimaryNodeTypeNodeVisitor
public class ResourceBundleVisitor extends HippoPublicationWorkflowDocumentVisitor {

    protected static final String NT_RESOURCEBUNDLE = "resourcebundle:resourcebundle";
    protected static final String ID = "resourcebundle:id";
    protected static final String KEYS = "resourcebundle:keys";
    protected static final String DESCRIPTIONS = "resourcebundle:descriptions";
    protected static final String MESSAGES = "resourcebundle:messages";

    @Override
    public String getNodeType() {
        return NT_RESOURCEBUNDLE;
    }

    protected void mapValues(final Property property, final String propertyName,
                             final List<Map<String, String>> valuesList)
            throws RepositoryException {
        Value[] values = property.getValues();
        for (int i = 0, len = valuesList.size(); i < len; i++) {
            if (i == values.length) {
                break;
            }
            valuesList.get(i).put(propertyName, values[i].getString());
        }
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);

        response.put("bundleId", node.getProperty(ID).getString());
        if (node.hasProperty(KEYS)) {
            final LinkedHashMap<String, Map<String, String>> keysMap = new LinkedHashMap<>();
            ArrayList<Map<String, String>> valuesList = new ArrayList<>();
            for (Value value : node.getProperty(KEYS).getValues()) {
                LinkedHashMap<String, String> values = new LinkedHashMap<>();
                keysMap.put(value.getString(), values);
                valuesList.add(values);
            }
            if (node.hasProperty(DESCRIPTIONS)) {
                mapValues(node.getProperty(DESCRIPTIONS), "description", valuesList);
            }
            if (node.hasProperty(MESSAGES)) {
                mapValues(node.getProperty(MESSAGES), "default", valuesList);
            }
            for (Property prop : new PropertyIterable(node.getProperties(MESSAGES+"_*"))) {
                String locale = prop.getName().substring(MESSAGES.length()+1);
                mapValues(prop, locale, valuesList);
            }
            response.put("keys", keysMap);
        }
    }

    @Override
    protected void visitLocaleProperty(final ResourceContext context, final Node node, Map<String, Object> response)
            throws RepositoryException {
        // noop
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                   final Property property) throws RepositoryException {
        if (ID.equals(property.getName())) {
            return true;
        }
        if (property.getName().startsWith(MESSAGES+"_")) {
            return true;
        }
        return super.skipProperty(context, propertyType, property);
    }
}
