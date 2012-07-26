/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.editor.type;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.editor.model.JcrTypeInfo;
import org.hippoecm.editor.model.JcrTypeVersion;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.TypeException;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJcrTypeStore implements IStore<ITypeDescriptor> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractJcrTypeStore.class);

    private ITypeLocator locator;
    protected Map<String, ITypeDescriptor> types = new HashMap<String, ITypeDescriptor>();
    protected Map<String, ITypeDescriptor> currentTypes = new HashMap<String, ITypeDescriptor>();

    @SuppressWarnings("unchecked")
    public AbstractJcrTypeStore() {
        BuiltinTypeStore builtinTypeStore = new BuiltinTypeStore();
        locator = new TypeLocator(new IStore[] { this, builtinTypeStore });
        builtinTypeStore.setTypeLocator(locator);
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

    public ITypeDescriptor getTypeDescriptor(String name) {
        if ("rep:root".equals(name)) {
            // ignore the root node
            return null;
        }
        ITypeDescriptor result = types.get(name);
        if (result == null) {
            try {
                JcrTypeVersion version = new JcrTypeVersion(getJcrSession(), name);
                Node typeNode = version.getTypeNode();
                if (typeNode != null) {
                    result = createTypeDescriptor(typeNode, name);
                    types.put(name, result);
                } else {
                    log.debug("No nodetype description found for " + name);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            } catch (StoreException e) {
                log.error(e.getMessage());
            }
        }
        return result;
    }

    @Override
    public void delete(ITypeDescriptor object) {
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public Iterator<ITypeDescriptor> find(Map<String, Object> criteria) throws StoreException {
        Map<String, ITypeDescriptor> results = new TreeMap<String, ITypeDescriptor>();
        if (criteria.containsKey("supertype")) {
            if (!(criteria.get("supertype") instanceof List)) {
                throw new StoreException("Invalid criteria; supertype should be of type List<String>");
            }
            List<String> supertype = (List<String>) criteria.get("supertype");
            if (supertype.size() == 0) {
                throw new StoreException("No supertypes specified");
            }
            Session session = getJcrSession();
            try {
                QueryManager qm = session.getWorkspace().getQueryManager();
                StringBuilder sb = new StringBuilder();
                sb.append("//element(*,");
                sb.append(HippoNodeType.NT_NODETYPE);
                sb.append(")[@");
                sb.append(HippoNodeType.HIPPO_SUPERTYPE);
                sb.append("='");
                boolean first = true;
                for (String type : supertype) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append("' or @");
                        sb.append(HippoNodeType.HIPPO_SUPERTYPE);
                        sb.append("='");
                    }
                    sb.append(type);
                }
                sb.append("']");
                String queryStr = sb.toString();
                log.debug("Query for supertypes: {}", queryStr);
                Query query = qm.createQuery(queryStr, Query.XPATH);
                QueryResult result = query.execute();
                for (NodeIterator nodes = result.getNodes(); nodes.hasNext();) {
                    Node node = nodes.nextNode();
                    String realType = null;
                    if (node.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                        realType = node.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString();
                    }
                    // assume /hippo:namespaces/<prefix>/<subType>/hipposysedit:nodetype/hipposysedit:nodetype
                    if (node.getDepth() < 5) {
                        continue;
                    }
                    node = node.getParent();
                    if (!node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        continue;
                    }
                    node = node.getParent();
                    if (!node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                        continue;
                    }
                    String subType = node.getName();
                    node = node.getParent();
                    if (!node.isNodeType(HippoNodeType.NT_NAMESPACE)) {
                        continue;
                    }
                    String prefix = node.getName();
                    if ("system".equals(prefix)) {
                        continue;
                    }
                    String subTypeName = prefix + ":" + subType;
                    if (realType != null && !realType.equals(subTypeName)) {
                        continue;
                    }
                    if (!results.containsKey(subTypeName)) {
                        try {
                            ITypeDescriptor type = locator.locate(subTypeName);
                            results.put(subTypeName, type);
                        } catch (StoreException ex) {
                            // not found; continue
                            continue;
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error searching for subtypes of " + supertype, e);
            }
        }
        return results.values().iterator();
    }

    @Override
    public ITypeDescriptor load(String id) throws StoreException {
        ITypeDescriptor result = getTypeDescriptor(id);
        if (result == null) {
            throw new StoreException("Could not find type " + id);
        }
        return result;
    }

    @Override
    public String save(ITypeDescriptor object) throws StoreException {
        if (object instanceof JcrTypeDescriptor) {
            ((JcrTypeDescriptor) object).save();
        } else {
            JcrTypeInfo info;
            try {
                info = new JcrTypeInfo(getJcrSession(), object.getType());
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

                ((NamespaceWorkflow) workflow).addCompoundType(info.getTypeName());

                nsNode.refresh(false);

                ITypeDescriptor type = getDraftType(object.getType());
                if (type == null) {
                    throw new StoreException("Could not find newly created draft");
                }
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
            } catch (TypeException ex) {
                log.error(ex.getMessage());
                throw new StoreException(ex);
            }
        }

        return object.getName();
    }

    public ITypeDescriptor getDraftType(String name) throws StoreException {
        ITypeDescriptor result = currentTypes.get(name);
        if (result == null) {
            try {
                JcrTypeVersion version = new JcrTypeVersion(getJcrSession(), name);
                JcrTypeInfo info = version.getTypeInfo();
                JcrTypeVersion current = info.getDraft();
                Node typeNode = current.getTypeNode();
                if (typeNode != null) {
                    result = createTypeDescriptor(typeNode, name);
                    currentTypes.put(name, result);
                } else {
                    log.debug("No nodetype description found for " + name);
                }
            } catch (RepositoryException e) {
                throw new StoreException("Could not find draft type descriptor", e);
            }
        }
        return result;
    }

    private ITypeDescriptor createTypeDescriptor(Node typeNode, String type) throws RepositoryException, StoreException {
        if (typeNode.isNodeType(HippoNodeType.NT_NODETYPE)) {
            if (typeNode.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                String realType = typeNode.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString();
                if (!realType.equals(type)) {
                    return new PseudoTypeDescriptor(type, locator.locate(realType));
                }
            }
            return createJcrTypeDescriptor(typeNode);
        }
        return null;
    }

    // Protected methods to be overridden in concrete classes

    protected JcrTypeDescriptor createJcrTypeDescriptor(Node typeNode) throws RepositoryException {
        return new JcrTypeDescriptor(new JcrNodeModel(typeNode), locator);
    }

    protected abstract Session getJcrSession();

}
