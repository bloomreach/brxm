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
package org.hippoecm.frontend.template.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.config.PluginRepositoryConfig;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryTemplateConfig extends PluginRepositoryConfig implements TemplateConfig {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RepositoryTemplateConfig.class);

    public RepositoryTemplateConfig(JcrSessionModel sessionModel) {
        super(sessionModel, "");
    }

    public TemplateDescriptor getTemplate(String name) {
        PluginDescriptor descriptor = getPlugin(name);
        if (descriptor != null) {
            return ((Descriptor) descriptor).template;
        } else {
            return null;
        }
    }

    public PluginDescriptor getPlugin(String pluginId) {
        PluginDescriptor result = null;
        try {
            Node pluginNode = lookupConfigNode(pluginId);
            if (pluginNode != null) {
                result = nodeToDescriptor(pluginNode);
            } else {
                log.error("No plugin node found for " + pluginId);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    public List<TemplateDescriptor> getTemplates() {
        return getTemplates("*");
    }

    public List<TemplateDescriptor> getTemplates(String namespace) {
        Session session = getJcrSession();

        List<TemplateDescriptor> list = new LinkedList<TemplateDescriptor>();
        try {
            String xpath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.NAMESPACES_PATH + "/" + namespace
                    + "/*";

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(xpath, Query.XPATH);
            QueryResult result = query.execute();
            NodeIterator iter = result.getNodes();
            while (iter.hasNext()) {
                Node pluginNode = iter.nextNode();
                PluginDescriptor descriptor = createDescriptor(pluginNode, pluginNode.getName(), null, null);
                TemplateDescriptor template = ((Descriptor) descriptor).template;
                list.add(template);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return list;
    }


    // Privates

    private Node lookupConfigNode(String template) throws RepositoryException {
        Session session = getJcrSession();

        String xpath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.NAMESPACES_PATH + "/*/" + template;

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        NodeIterator iter = result.getNodes();
        if (iter.getSize() > 1) {
            throw new IllegalStateException("Multiple templates defined for type " + template);
        }
        return iter.hasNext() ? iter.nextNode() : null;
    }

    @Override
    protected PluginDescriptor nodeToDescriptor(Node pluginNode) throws RepositoryException {
        if (pluginNode.hasProperty(HippoNodeType.HIPPO_RENDERER)) {
            return super.nodeToDescriptor(pluginNode);
        }
        return new Descriptor(pluginNode, pluginNode.getName(), null, null);
    }

    @Override
    protected PluginDescriptor createDescriptor(Node node, String pluginId, String className, Channel outgoing) {
        return new Descriptor(node, pluginId, className, outgoing);
    }

    // IPluginConfigContext implementation
    // It explicitly uses the fact that the outer class extends the class that
    // defines the delegate.  Thus, nodeToDescriptor is able to override the descriptors
    // that are returned.
    protected class Descriptor extends PluginRepositoryConfig.Descriptor {
        private static final long serialVersionUID = 1L;

        FieldDescriptor field;
        TemplateDescriptor template;

        Descriptor(Node pluginNode, String pluginId, String className, Channel outgoing) {
            super(pluginNode, pluginId, className, outgoing);

            try {
                if (pluginNode.isNodeType(HippoNodeType.NT_TEMPLATE)) {
                    String type = pluginNode.getProperty(HippoNodeType.HIPPO_TYPE).getString();
                    template = new RepositoryTemplateDescriptor(pluginNode, pluginNode.getName(), type, this);
                } else if (pluginNode.isNodeType(HippoNodeType.NT_FIELD)) {
                    String path = null;
                    if (pluginNode.hasProperty(HippoNodeType.HIPPO_PATH)) {
                        path = pluginNode.getProperty(HippoNodeType.HIPPO_PATH).getString();
                    }

                    String name = "";
                    if (pluginNode.hasProperty(HippoNodeType.HIPPO_NAME)) {
                        name = pluginNode.getProperty(HippoNodeType.HIPPO_NAME).getString();
                    }

                    field = new RepositoryFieldDescriptor(pluginNode, name, path, this);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        @Override
        public List<PluginDescriptor> getChildren() {
            // filter list of children.  fields and templates are manages by the template engine.
            List<PluginDescriptor> list = new ArrayList<PluginDescriptor>();
            Iterator<PluginDescriptor> iterator = super.getChildren().iterator();
            while (iterator.hasNext()) {
                Descriptor plugin = (Descriptor) iterator.next();
                if (plugin.field == null && plugin.template == null) {
                    list.add(plugin);
                }
            }
            return list;
        }

        public List<FieldDescriptor> getFields() {
            List<FieldDescriptor> list = new LinkedList<FieldDescriptor>();
            Iterator<PluginDescriptor> iterator = super.getChildren().iterator();
            while (iterator.hasNext()) {
                Descriptor plugin = (Descriptor) iterator.next();
                if (plugin.field != null) {
                    list.add(plugin.field);
                }
            }
            return list;
        }

        public List<TemplateDescriptor> getTemplates() {
            List<TemplateDescriptor> list = new LinkedList<TemplateDescriptor>();
            Iterator<PluginDescriptor> iterator = super.getChildren().iterator();
            while (iterator.hasNext()) {
                Descriptor plugin = (Descriptor) iterator.next();
                if (plugin.template != null) {
                    list.add(plugin.template);
                }
            }
            return list;
        }

        @Override
        protected Node getNode() throws RepositoryException {
            return super.getNode();
        }
    }

    protected class RepositoryTemplateDescriptor extends TemplateDescriptor {
        private static final long serialVersionUID = 1L;

        public RepositoryTemplateDescriptor(Node node, String name, String type, Descriptor plugin) {
            super(name, type, plugin);

            try {
                if (node.hasProperty(HippoNodeType.HIPPO_NODE)) {
                    setIsNode(node.getProperty(HippoNodeType.HIPPO_NODE).getBoolean());
                }
                if (node.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                    setSuperType(node.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getString());
                }
                if (node.hasProperty(HippoNodeType.HIPPO_MIXINTYPES)) {
                    List<String> mixins = new LinkedList<String>();
                    Value[] values = node.getProperty(HippoNodeType.HIPPO_MIXINTYPES).getValues();
                    for(int i = 0; i < values.length; i++) {
                        mixins.add(values[i].getString());
                    }
                    setMixinTypes(mixins);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        @Override
        public List<FieldDescriptor> getFields() {
            Descriptor plugin = (Descriptor) getPlugin();

            List<FieldDescriptor> fields = plugin.getFields();
            Set<String> explicit = new HashSet<String>();
            Iterator<FieldDescriptor> iterator = fields.iterator();
            while (iterator.hasNext()) {
                FieldDescriptor field = iterator.next();
                if (!field.getPath().equals("*")) {
                    explicit.add(field.getPath());
                }
            }

            iterator = fields.iterator();
            while (iterator.hasNext()) {
                FieldDescriptor field = iterator.next();
                if (field.getPath().equals("*")) {
                    field.setExcluded(explicit);
                }
            }
            return fields;
        }
    }

    protected class RepositoryFieldDescriptor extends FieldDescriptor {
        private static final long serialVersionUID = 1L;

        public RepositoryFieldDescriptor(Node pluginNode, String name, String path, Descriptor plugin) {
            super(name, path, plugin);

            try {
                if (pluginNode.hasProperty(HippoNodeType.HIPPO_MULTIPLE)) {
                    boolean multiple = pluginNode.getProperty(HippoNodeType.HIPPO_MULTIPLE).getBoolean();
                    setIsMultiple(multiple);
                }

                if (pluginNode.hasProperty(HippoNodeType.HIPPO_MANDATORY)) {
                    boolean mandatory = pluginNode.getProperty(HippoNodeType.HIPPO_MANDATORY).getBoolean();
                    setMandatory(mandatory);
                }

                if (pluginNode.hasProperty(HippoNodeType.HIPPO_ORDERED)) {
                    setIsOrdered(pluginNode.getProperty(HippoNodeType.HIPPO_ORDERED).getBoolean());
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        @Override
        public FieldDescriptor getField() {
            Descriptor plugin = (Descriptor) getPlugin();
            List<FieldDescriptor> fields = plugin.getFields();
            if (fields.size() > 1) {
                log.warn("more than one field wrapped by field descriptor");
            } else if (fields.size() == 0) {
                return null;
            }
            return fields.get(0);
        }

        @Override
        public TemplateDescriptor getTemplate() {
            Descriptor plugin = (Descriptor) getPlugin();
            List<TemplateDescriptor> templates = plugin.getTemplates();
            if (templates.size() > 1) {
                log.warn("more than one field wrapped by field descriptor");
                return templates.get(0);
            } else if (templates.size() == 1) {
                return templates.get(0);
            } else {
                try {
                    Node pluginNode = plugin.getNode();
                    if (pluginNode.hasProperty(HippoNodeType.HIPPO_TEMPLATE)) {
                        String template = pluginNode.getProperty(HippoNodeType.HIPPO_TEMPLATE).getString();
                        return RepositoryTemplateConfig.this.getTemplate(template);
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
                return null;
            }
        }
    }
}
