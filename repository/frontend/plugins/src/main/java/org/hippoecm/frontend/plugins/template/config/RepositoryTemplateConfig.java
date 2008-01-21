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
package org.hippoecm.frontend.plugins.template.config;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

public class RepositoryTemplateConfig implements TemplateConfig {

    private static final long serialVersionUID = 1L;

    public RepositoryTemplateConfig() {
    }

    public TemplateDescriptor getTemplate(String name) {
        if (name == null) {
            return null;
        }

        try {
            Node node = lookupConfigNode(name);
            if (node == null) {
                return new TemplateDescriptor(name, getNodeTypeDefined(name));
            }

            Set<String> explicit = new HashSet<String>();
            LinkedList<FieldDescriptor> children = new LinkedList<FieldDescriptor>();
            NodeIterator iter = node.getNodes();
            while (iter.hasNext()) {
                Node child = iter.nextNode();
                if (child.isNodeType("hippo:field")) {
                    String path = child.getProperty("hippo:path").getString();
                    if (!path.equals("*")) {
                        explicit.add(path);
                    }

                    FieldDescriptor descriptor = new FieldDescriptor(child.getName(), path);

                    if (child.hasProperty("hippo:template")) {
                        descriptor.setType(child.getProperty("hippo:template").getString());
                    }

                    if (child.hasProperty("hippo:renderer")) {
                        descriptor.setRenderer(child.getProperty("hippo:renderer").getString());
                    }

                    if (child.hasProperty("hippo:multiple")) {
                        descriptor.setMultiple(child.getProperty("hippo:multiple").getBoolean());
                    }
                    children.addLast(descriptor);
                }
            }

            Iterator<FieldDescriptor> fieldIter = children.iterator();
            while (fieldIter.hasNext()) {
                FieldDescriptor field = fieldIter.next();
                if (field.getPath().equals("*")) {
                    field.setExcluded(explicit);
                }
            }

            return new TemplateDescriptor(name, children);
        } catch (RepositoryException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Privates

    private Node lookupConfigNode(String template) throws RepositoryException {
        UserSession session = (UserSession) Session.get();

        String xpath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH + "/" + session.getHippo()
                + "/*/" + HippoNodeType.HIPPO_TEMPLATES + "/" + template;

        QueryManager queryManager = session.getJcrSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        NodeIterator iter = result.getNodes();
        if (iter.getSize() > 1) {
            throw new IllegalStateException("Multiple templates defined for type " + template);
        }
        return iter.hasNext() ? iter.nextNode() : null;
    }

    private List<FieldDescriptor> getNodeTypeDefined(String name) throws RepositoryException {
        List<FieldDescriptor> children = new LinkedList<FieldDescriptor>();

        // create a descriptor based on the node type
        UserSession session = (UserSession) Session.get();
        NodeTypeManager ntMgr = session.getJcrSession().getWorkspace().getNodeTypeManager();

        // throws NoSuchNodeTypeException if type doesn't exist
        NodeType nt = ntMgr.getNodeType(name);
        for (PropertyDefinition pd : nt.getPropertyDefinitions()) {
            children.add(new FieldDescriptor(pd));
        }
        return children;
    }
}
