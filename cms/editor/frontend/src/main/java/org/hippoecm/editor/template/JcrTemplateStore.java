/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.editor.template;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.editor.EditorNodeType;
import org.hippoecm.editor.model.JcrNamespace;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTemplateStore implements ITemplateStore, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrTemplateStore.class);

    private ITypeLocator typeLocator;

    public JcrTemplateStore(ITypeLocator typeStore) {
        this.typeLocator = typeStore;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<String> getMetadataEditors() {
        List<String> mixins = new LinkedList<String>();

        try {
            javax.jcr.Session session = UserSession.get().getJcrSession();
            QueryManager qMgr = session.getWorkspace().getQueryManager();
            Query query = qMgr.createQuery("//element(*, " + HippoNodeType.NT_NODETYPE + ")[@"
                    + HippoNodeType.HIPPO_MIXIN + "='true']", Query.XPATH);
            NodeIterator iter = query.execute().getNodes();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                log.debug("search result: {}", node.getPath());

                node = node.getParent();
                if (!node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    log.debug("invalid parent");
                    continue;
                }
                node = node.getParent();
                if (!node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                    log.debug("invalid ancestor");
                    continue;
                }

                if (!node.isNodeType(EditorNodeType.NT_EDITABLE)) {
                    log.debug("no template present");
                    continue;
                }

                Node parent = node.getParent();
                if (!parent.isNodeType(HippoNodeType.NT_NAMESPACE)) {
                    log.debug("invalid great ancestor");
                    continue;
                }

                String name;
                if ("system".equals(parent.getName())) {
                    name = node.getName();
                } else {
                    name = parent.getName() + ":" + node.getName();
                }
                log.debug("found: " + name);
                mixins.add(name);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

        return mixins;
    }

    @Override
    public Iterator<IClusterConfig> find(Map<String, Object> criteria) {
        if (criteria.containsKey("type")) {
            List<IClusterConfig> list = new ArrayList<IClusterConfig>(1);
            try {
                list.add(getTemplate((ITypeDescriptor) criteria.get("type")));
            } catch (StoreException ex) {
                // ignore
            }
            return list.iterator();
        }
        return new ArrayList<IClusterConfig>(0).iterator();
    }

    @Override
    public IClusterConfig load(String id) throws StoreException {
        JcrNodeModel nodeModel = new JcrNodeModel(id);
        if (nodeModel.getNode() == null) {
            throw new StoreException("Unknown template " + id);
        }
        return new JcrClusterConfig(nodeModel);
    }

    @SuppressWarnings("deprecation")
    @Override
    public String save(IClusterConfig cluster) throws StoreException {
        if (cluster instanceof JcrClusterConfig) {
            try {
                Node node = ((JcrClusterConfig) cluster).getNodeModel().getNode();
                node.save();
                return node.getPath();
            } catch (RepositoryException ex) {
                throw new StoreException(ex);
            }
        } else {
            throw new StoreException("Can only store clusters with a type");
        }
    }

    @Override
    public String save(IClusterConfig cluster, ITypeDescriptor typeDescriptor) throws StoreException {
        try {
            Node node = getTemplateNode(typeDescriptor, true);

            JcrClusterConfig jcrConfig = new JcrClusterConfig(new JcrNodeModel(node));
            for (Map.Entry<String, Object> entry : cluster.entrySet()) {
                jcrConfig.put(entry.getKey(), entry.getValue());
            }

            for (IPluginConfig plugin : cluster.getPlugins()) {
                Node child = node.addNode(plugin.getName(), FrontendNodeType.NT_PLUGIN);
                JcrPluginConfig pluginConfig = new JcrPluginConfig(new JcrNodeModel(child));
                for (Map.Entry<String, Object> entry : plugin.entrySet()) {
                    pluginConfig.put(entry.getKey(), entry.getValue());
                }
            }

            node.setProperty(FrontendNodeType.FRONTEND_SERVICES, getValues(cluster.getServices()));
            node.setProperty(FrontendNodeType.FRONTEND_REFERENCES, getValues(cluster.getReferences()));
            node.setProperty(FrontendNodeType.FRONTEND_PROPERTIES, getValues(cluster.getProperties()));

            node.save();
            return node.getPath();
        } catch (RepositoryException ex) {
            throw new StoreException(ex);
        }
    }

    @Override
    public void delete(IClusterConfig object) {
    }

    /**
     * Retrieve the node that contains the template cluster.  Throws an exception when no such
     * node can be found.
     */
    @SuppressWarnings("deprecation")
    protected Node getTemplateNode(ITypeDescriptor type, boolean create) throws RepositoryException, StoreException {
        javax.jcr.Session session = UserSession.get().getJcrSession();

        String typeName = type.getName();
        String prefix;
        String subType;
        if (typeName.indexOf(':') > 0) {
            prefix = typeName.substring(0, typeName.indexOf(':'));
            subType = typeName.substring(typeName.indexOf(':') + 1);
        } else {
            prefix = "system";
            subType = typeName;
        }
        String path = new JcrNamespace(session, prefix).getPath() + "/" + subType;
        if (!session.itemExists(path)) {
            throw new StoreException("No template type node exists");
        }
        Node typeNode = (Node) session.getItem(path);

        boolean save = false;
        try {
            Node templateSetNode;
            if (!typeNode.isNodeType(EditorNodeType.NT_EDITABLE)) {
                if (create) {
                    typeNode.addMixin(EditorNodeType.NT_EDITABLE);
                    templateSetNode = typeNode.addNode(EditorNodeType.EDITOR_TEMPLATES, EditorNodeType.NT_TEMPLATESET);
                    save = true;
                } else {
                    throw new StoreException("Type " + type + " is not editable");
                }
            } else {
                if (typeNode.hasNode(EditorNodeType.EDITOR_TEMPLATES)) {
                    templateSetNode = typeNode.getNode(EditorNodeType.EDITOR_TEMPLATES);
                } else {
                    if (create) {
                        templateSetNode = typeNode.addNode(EditorNodeType.EDITOR_TEMPLATES, EditorNodeType.NT_TEMPLATESET);
                        save = true;
                    } else {
                        throw new StoreException("No editor cluster found for type " + type + ", even though it was marked editable");
                    }
                }
            }

            // use the first available template
            Node templateNode = null;
            NodeIterator nodes = templateSetNode.getNodes();
            while (nodes.hasNext()) {
                Node template = nodes.nextNode();
                if (template != null && template.isNodeType(FrontendNodeType.NT_PLUGINCLUSTER)) {
                    templateNode = template;
                    break;
                }
            }
            if (templateNode == null) {
                if (create) {
                    templateNode = templateSetNode.addNode("_default_", FrontendNodeType.NT_PLUGINCLUSTER);
                    save = true;
                } else {
                    throw new StoreException("No template found for " + type);
                }
            }
            return templateNode;
        } finally {
            if (save) {
                typeNode.save();
            }
        }
    }

    protected IClusterConfig getTemplate(ITypeDescriptor type) throws StoreException {
        try {
            Node node = getTemplateNode(type, false);
            return new JcrClusterConfig(new JcrNodeModel(node));
        } catch (RepositoryException ex) {
            throw new StoreException("Error while fetching template for type: " + type, ex);
        }
    }

    @Override
    public void detach() {
        if (typeLocator != null) {
            typeLocator.detach();
        }
    }

    private Value[] getValues(List<String> list) throws RepositoryException {
        Value[] values = new Value[list.size()];
        int i = 0;
        for (String override : list) {
            values[i++] = UserSession.get().getJcrSession().getValueFactory()
                    .createValue(override);
        }
        return values;
    }

}
