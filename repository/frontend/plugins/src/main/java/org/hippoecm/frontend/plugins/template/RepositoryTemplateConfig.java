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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.Session;
import org.hippoecm.frontend.UserSession;
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

            List<FieldDescriptor> children = new ArrayList<FieldDescriptor>();
            NodeIterator iter = node.getNodes();
            while (iter.hasNext()) {
                Node child = iter.nextNode();
                if (child.isNodeType("hippo:field")) {
                    String path = child.getProperty("hippo:path").getString();

                    String template = null;
                    if (child.hasProperty("hippo:template")) {
                        template = child.getProperty("hippo:template").getString();
                    }

                    String renderer = null;
                    if (child.hasProperty("hippo:renderer")) {
                        renderer = child.getProperty("hippo:renderer").getString();
                    }
                    children.add(new FieldDescriptor(child.getName(), path, template, renderer));
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

        String path = HippoNodeType.CONFIGURATION_PATH + "/hippo:frontend/hippo:cms-prototype/hippo:templates/"
                + template;
        if (session.getRootNode().hasNode(path)) {
            return session.getRootNode().getNode(path);
        }
        return null;
    }

    private List<FieldDescriptor> getNodeTypeDefined(String name) throws RepositoryException {
        List<FieldDescriptor> children = new ArrayList<FieldDescriptor>();

        // create a descriptor based on the node type
        UserSession session = (UserSession) Session.get();
        NodeTypeManager ntMgr = session.getJcrSession().getWorkspace().getNodeTypeManager();

        // throws NoSuchNodeTypeException if type doesn't exist
        NodeType nt = ntMgr.getNodeType(name);
        for (NodeDefinition nd : nt.getChildNodeDefinitions()) {
            children.add(new FieldDescriptor(nd));
        }
        for (PropertyDefinition pd : nt.getPropertyDefinitions()) {
            children.add(new FieldDescriptor(pd));
        }
        return children;
    }
}
