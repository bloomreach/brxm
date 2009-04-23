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
package org.hippoecm.editor.tools;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeStore implements IStore<ITypeDescriptor> {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrTypeStore.class);

    private IPluginContext context;
    private Map<String, JcrTypeDescriptor> types = new HashMap<String, JcrTypeDescriptor>();

    public JcrTypeStore(IPluginContext context) {
        this.context = context;
    }

    public JcrTypeDescriptor getTypeDescriptor(String name) {
        if ("rep:root".equals(name)) {
            // ignore the root node
            return null;
        }
        JcrTypeDescriptor result = types.get(name);
        if (result == null) {
            try {
                Node typeNode = lookupConfigNode(name);
                if (typeNode != null) {
                    result = createTypeDescriptor(typeNode, name);
                    types.put(name, result);
                } else {
                    log.warn("No nodetype description found for " + name);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return result;
    }

    public Map<String,TypeUpdate> getUpdate(String prefix) throws RepositoryException {
        return new NamespaceInfo(prefix).getUpdate();
    }
    
    public void close() {
        context = null;
    }

    public void delete(ITypeDescriptor object) {
    }

    public Iterator<ITypeDescriptor> find(Map<String, Object> criteria) {
        // TODO Auto-generated method stub
        return null;
    }

    public ITypeDescriptor load(String id) {
        return getTypeDescriptor(id);
    }

    public String save(ITypeDescriptor object) throws StoreException {
        if (object instanceof JcrTypeDescriptor) {
            ((JcrTypeDescriptor) object).save();
        } else {
            TypeInfo info = new TypeInfo(object.getType());
            String path = info.getPath();

            HippoSession session = (HippoSession) getJcrSession();
            try {
                if (session.itemExists(path)) {
                    throw new StoreException("type already exists");
                }
                Node nsNode = (Node) session.getItem(info.getNamespaceInfo().getPath());

                WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                Workflow workflow = workflowManager.getWorkflow("editor", nsNode);

                ((NamespaceWorkflow) workflow).addType(info.subType);

                nsNode.refresh(false);

                JcrTypeDescriptor type = getTypeDescriptor(object.getType());
                type.setSuperTypes(object.getSuperTypes());
                type.setIsNode(object.isNode());
                type.setIsMixin(object.isMixin());
                for (Map.Entry<String, IFieldDescriptor> entry : object.getFields().entrySet()) {
                    type.addField(entry.getValue());
                }

                return object.getType();

            } catch (WorkflowException ex) {
                log.error(ex.getMessage());
                throw new StoreException(ex);
            } catch (RemoteException ex) {
                log.error(ex.getMessage());
                throw new StoreException(ex);
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
                throw new StoreException(ex);
            }
        }

        return object.getName();
    }

    // Privates

    private Session getJcrSession() {
        return ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
    }

    private Node lookupConfigNode(String type) throws RepositoryException {
        HippoSession session = (HippoSession) getJcrSession();

        TypeInfo info = new TypeInfo(type);
        String path = info.getPath();
        String uri = info.getNamespaceInfo().getUri();

        if (!session.itemExists(path) || !session.getItem(path).isNode()) {
            return null;
        }
        NodeIterator iter = ((Node) session.getItem(path)).getNode(HippoNodeType.HIPPO_NODETYPE).getNodes(
                HippoNodeType.HIPPO_NODETYPE);

        Node current = null;
        while (iter.hasNext()) {
            Node node = iter.nextNode();
            if (!node.isNodeType(HippoNodeType.NT_REMODEL)) {
                return node;
            } else {
                if (node.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                    current = node;
                }
            }
        }
        return current;
    }

    private JcrTypeDescriptor createTypeDescriptor(Node typeNode, String type) throws RepositoryException {
        try {
            if (typeNode.isNodeType(HippoNodeType.NT_NODETYPE)) {
                return new JcrTypeDescriptor(new JcrNodeModel(typeNode), context);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    private class NamespaceInfo {
        
        String prefix;
        
        NamespaceInfo(String prefix) {
            String uri = getUri(prefix);
            String nsVersion = "_" + uri.substring(uri.lastIndexOf("/") + 1);
            if (prefix.length() > nsVersion.length()
                    && nsVersion.equals(prefix.substring(prefix.length() - nsVersion.length()))) {
                prefix = prefix.substring(0, prefix.length() - nsVersion.length());
            }

            this.prefix = prefix;
        }

        Map<String, TypeUpdate> getUpdate() throws RepositoryException {
            HippoSession session = (HippoSession) getJcrSession();
            Map<String, TypeUpdate> result = new HashMap<String, TypeUpdate>();
            
            String uri = getUri();
            NodeIterator iter = ((Node) session.getItem(getPath())).getNodes();
            while (iter.hasNext()) {
                Node typeNode = iter.nextNode();

                Node draft = null, current = null;
                NodeIterator versions = typeNode.getNodes(HippoNodeType.HIPPO_NODETYPE + "/" + HippoNodeType.HIPPO_NODETYPE);
                while (versions.hasNext()) {
                    Node node = versions.nextNode();
                    if (!node.isNodeType(HippoNodeType.NT_REMODEL)) {
                        draft = node;
                    } else {
                        if (node.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                            current = node;
                        }
                    }
                }

                if (current != null) {
                    ITypeDescriptor currentType = new JcrTypeDescriptor(new JcrNodeModel(current), null);
                    ITypeDescriptor draftType = draft == null ? null : new JcrTypeDescriptor(new JcrNodeModel(draft), null);
                    result.put(typeNode.getName(), new TypeConversion(JcrTypeStore.this, currentType, draftType).getTypeUpdate());
                }
            }
            return result;
        }

        String getPath() {
            return "/" + HippoNodeType.NAMESPACES_PATH + "/" + prefix;
        }

        String getUri() {
            return getUri(prefix);
        }

        private String getUri(String prefix) {
            if ("system".equals(prefix)) {
                return "internal";
            }
            try {
                NamespaceRegistry nsReg = getJcrSession().getWorkspace().getNamespaceRegistry();
                return nsReg.getURI(prefix);
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return null;
        }
    }
    
    private class TypeInfo {
        NamespaceInfo nsInfo;
        String subType;

        TypeInfo(String type) {
            String prefix = "system";
            subType = type;
            if (type.indexOf(':') > 0) {
                prefix = type.substring(0, type.indexOf(':'));
                subType = NodeNameCodec.encode(type.substring(type.indexOf(':') + 1));
            }
            nsInfo = new NamespaceInfo(prefix);
        }

        NamespaceInfo getNamespaceInfo() {
            return nsInfo;
        }
        
        String getPath() {
            return nsInfo.getPath() + "/" + subType;
        }
    }

}