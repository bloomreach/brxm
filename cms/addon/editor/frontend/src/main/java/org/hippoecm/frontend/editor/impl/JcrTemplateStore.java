/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.Session;
import org.hippoecm.editor.EditorNodeType;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTemplateStore implements IStore<IClusterConfig> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrTemplateStore.class);

    private IStore<ITypeDescriptor> typeStore;

    public JcrTemplateStore(IStore<ITypeDescriptor> typeStore) {
        this.typeStore = typeStore;
    }

    public List<String> getAvailableMixins() {
        List<String> mixins = new LinkedList<String>();

        try {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            QueryManager qMgr = session.getWorkspace().getQueryManager();
            Query query = qMgr.createQuery("//element(*, " + HippoNodeType.NT_NODETYPE + ")[@hippo:mixin='true']", Query.XPATH);
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

    public IClusterConfig load(String id) throws StoreException {
        JcrNodeModel nodeModel = new JcrNodeModel(id);
        if (nodeModel.getNode() == null) {
            throw new StoreException("Unknown template " + id);
        }
        return new JcrClusterConfig(nodeModel);
    }

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
            String type = cluster.getString("type");
            if (type == null) {
                throw new StoreException("Can only store clusters with a type");
            }
            try {
                Node node = getTemplateNode(typeStore.load(type), true);

                JcrClusterConfig jcrConfig = new JcrClusterConfig(new JcrNodeModel(node));
                for (Map.Entry entry : (Set<Map.Entry>) ((Map) cluster).entrySet()) {
                    jcrConfig.put(entry.getKey(), entry.getValue());
                }

                for (IPluginConfig plugin : cluster.getPlugins()) {
                    String name = UUID.randomUUID().toString();
                    Node child = node.addNode(name, FrontendNodeType.NT_PLUGIN);
                    JcrPluginConfig pluginConfig = new JcrPluginConfig(new JcrNodeModel(child));
                    for (Map.Entry entry : (Set<Map.Entry>) ((Map) plugin).entrySet()) {
                        pluginConfig.put(entry.getKey(), entry.getValue());
                    }
                }

                node.setProperty(FrontendNodeType.FRONTEND_SERVICES, getValues(cluster.getServices()));
                node.setProperty(FrontendNodeType.FRONTEND_REFERENCES, getValues(cluster.getReferences()));
                node.setProperty(FrontendNodeType.FRONTEND_PROPERTIES, getValues(cluster.getProperties()));

                return node.getPath();
            } catch (RepositoryException ex) {
                throw new StoreException(ex);
            }
        }
    }

    public void close() {
    }

    public void delete(IClusterConfig object) {
    }

    protected Node getTemplateNode(ITypeDescriptor type, boolean create) throws RepositoryException {
        String typeName = type.getName();
        String path = "/hippo:namespaces/";
        if (typeName.indexOf(':') > 0) {
            path += typeName.replace(':', '/');
        } else {
            path += "system/" + typeName;
        }
        Node typeNode = (Node) ((UserSession) Session.get()).getJcrSession().getItem(path);

        Node node;
        if (!typeNode.isNodeType(HippoNodeType.HIPPO_TEMPLATE)) {
            if (create) {
                typeNode.addMixin(EditorNodeType.NT_EDITABLE);
                node = typeNode.addNode(EditorNodeType.EDITOR_TEMPLATES, EditorNodeType.NT_TEMPLATESET);
            } else {
                return null;
            }
        } else {
            node = typeNode.getNode(HippoNodeType.HIPPO_TEMPLATE);
        }
        if (!node.hasNode(HippoNodeType.HIPPO_TEMPLATE)) {
            if (create) {
                node = node.addNode(HippoNodeType.HIPPO_TEMPLATE, FrontendNodeType.NT_PLUGINCLUSTER);
            } else {
                return null;
            }
        } else {
            node = node.getNode(HippoNodeType.HIPPO_TEMPLATE);
        }
        return node;
    }

    protected IClusterConfig getTemplate(ITypeDescriptor type) throws StoreException {
        try {
            Node node = getTemplateNode(type, false);
            if (node == null) {
                throw new StoreException("No template found for " + type);
            } else {
                return new JcrClusterConfig(new JcrNodeModel(node));
            }
        } catch (RepositoryException ex) {
            log.error("Error while fetching template for type: " + type, ex);
        }
        return null;
    }

    private Value[] getValues(List<String> list) throws RepositoryException {
        Value[] values = new Value[list.size()];
        int i = 0;
        for (String override : list) {
            values[i++] = ((UserSession) org.apache.wicket.Session.get()).getJcrSession().getValueFactory()
                    .createValue(override);
        }
        return values;
    }

}
