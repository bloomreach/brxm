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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeStore implements IStore<ITypeDescriptor>, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrTypeStore.class);

    private ITypeLocator locator;
    private Map<String, JcrTypeDescriptor> types = new HashMap<String, JcrTypeDescriptor>();

    @SuppressWarnings("unchecked")
    public JcrTypeStore() {
        IStore<ITypeDescriptor> builtinTypeStore = new BuiltinTypeStore();
        locator = new TypeLocator(new IStore[] { this, builtinTypeStore });
    }

    public ITypeLocator getTypeLocator() {
        return this.locator;
    }

    /**
     * Set the type locator that will be used by type descriptors to resolve super
     * types.
     * @param locator
     */
    public void setTypeLocator(ITypeLocator locator) {
        this.locator = locator;
    }

    public JcrTypeDescriptor getTypeDescriptor(String name) {
        if ("rep:root".equals(name)) {
            // ignore the root node
            return null;
        }
        JcrTypeDescriptor result = types.get(name);
        if (result == null) {
            try {
                TypeInfo info = new TypeInfo(name);
                TypeVersion version = info.getLatest();
                Node typeNode = version.getTypeNode(getJcrSession());
                if (typeNode != null) {
                    result = createTypeDescriptor(typeNode, name);
                    // do validation on type
                    if (WebApplication.get() != null
                            && "development".equals(WebApplication.get().getConfigurationType())) {
                        result.validate();
                    }
                    types.put(name, result);
                } else {
                    log.debug("No nodetype description found for " + name);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return result;
    }

    public void delete(ITypeDescriptor object) {
    }

    public Iterator<ITypeDescriptor> find(Map<String, Object> criteria) {
        // TODO Auto-generated method stub
        return null;
    }

    public ITypeDescriptor load(String id) throws StoreException {
        ITypeDescriptor result = getTypeDescriptor(id);
        if (result == null) {
            throw new StoreException("Could not find type " + id);
        }
        return result;
    }

    public String save(ITypeDescriptor object) throws StoreException {
        if (object instanceof JcrTypeDescriptor) {
            ((JcrTypeDescriptor) object).save();
        } else {
            TypeInfo info;
            try {
                info = new TypeInfo(object.getType());
            } catch (RepositoryException ex) {
                throw new StoreException("unable to store type", ex);
            }
            String path = info.getPath();

            HippoSession session = (HippoSession) getJcrSession();
            try {
                if (session.itemExists(path)) {
                    throw new StoreException("type already exists");
                }
                Node nsNode = (Node) session.getItem(info.getNamespace().getPath());

                WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                Workflow workflow = workflowManager.getWorkflow("editor", nsNode);

                ((NamespaceWorkflow) workflow).addType("compound", info.subType);

                nsNode.refresh(false);

                JcrTypeDescriptor type = getTypeDescriptor(object.getType());
                type.setSuperTypes(object.getSuperTypes());
                type.setIsNode(object.isNode());
                type.setIsMixin(object.isMixin());
                type.setIsValidationCascaded(object.isValidationCascaded());
                for (Map.Entry<String, IFieldDescriptor> entry : object.getFields().entrySet()) {
                    type.addField(entry.getValue());
                }

                return object.getName();

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

    public void detach() {
        for (JcrTypeDescriptor type : types.values()) {
            type.detach();
        }
    }

    public ITypeDescriptor getCurrentType(String type) throws StoreException {
        try {
            TypeInfo info = new TypeInfo(type);
            TypeVersion current = info.getCurrent();
            return createTypeDescriptor(current.getTypeNode(getJcrSession()), type);
        } catch (RepositoryException e) {
            throw new StoreException("Could not find current type descriptor", e);
        }
    }

    // Privates

    private Session getJcrSession() {
        return ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
    }

    private JcrTypeDescriptor createTypeDescriptor(Node typeNode, String type) throws RepositoryException {
        try {
            if (typeNode.isNodeType(HippoNodeType.NT_NODETYPE)) {
                return new JcrTypeDescriptor(new JcrNodeModel(typeNode), locator);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    private class TypeInfo {
        JcrNamespace nsInfo;
        String subType;
        String type;

        TypeInfo(String type) throws RepositoryException {
            this.type = type;
            String prefix = "system";
            subType = type;
            if (type.indexOf(':') > 0) {
                prefix = type.substring(0, type.indexOf(':'));
                subType = NodeNameCodec.encode(type.substring(type.indexOf(':') + 1));
            }
            nsInfo = new JcrNamespace(getJcrSession(), prefix);
        }

        JcrNamespace getNamespace() {
            return nsInfo;
        }

        String getPath() {
            return nsInfo.getPath() + "/" + subType;
        }

        TypeVersion getLatest() throws RepositoryException {
            boolean latest = true;
            String uri = getNamespace().getCurrentUri();
            String prefix;
            if (type.indexOf(':') > 0) {
                prefix = type.substring(0, type.indexOf(':'));
                String typeUri = getJcrSession().getWorkspace().getNamespaceRegistry().getURI(prefix);
                if (!typeUri.equals(uri)) {
                    latest = false;
                    uri = typeUri;
                }
            } else {
                prefix = "system";
            }

            return new TypeVersion(this, latest, uri);
        }

        TypeVersion getCurrent() throws RepositoryException {
            String prefix = "system";
            String uri = null;
            if (type.indexOf(':') > 0) {
                prefix = type.substring(0, type.indexOf(':'));
                uri = getJcrSession().getWorkspace().getNamespaceRegistry().getURI(prefix);
            }
            return new TypeVersion(this, false, uri);
        }
    }

    private class TypeVersion {

        TypeInfo info;
        boolean latest;
        String uri;

        public TypeVersion(TypeInfo info, boolean latest, String uri) {
            this.info = info;
            this.latest = latest;
            this.uri = uri;
        }

        Node getTypeNode(Session session) throws RepositoryException {
            String path = info.getPath();
            if (!session.itemExists(path) || !session.getItem(path).isNode()) {
                return null;
            }

            NodeIterator iter = ((Node) session.getItem(path)).getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes(
                    HippoNodeType.HIPPOSYSEDIT_NODETYPE);

            Node current = null;
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (latest && !node.isNodeType(HippoNodeType.NT_REMODEL)) {
                    return node;
                } else {
                    if (node.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                        current = node;
                    }
                }
            }
            return current;
            
        }
    }
}
