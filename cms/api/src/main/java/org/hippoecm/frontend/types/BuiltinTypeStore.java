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
package org.hippoecm.frontend.types;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.session.UserSession;

public class BuiltinTypeStore implements ITypeStore {

    private static final long serialVersionUID = 1L;

    private ITypeLocator locator;

    @SuppressWarnings("unchecked")
    public BuiltinTypeStore() {
        locator = new TypeLocator(new IStore[] { this });
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

    public ITypeDescriptor getTypeDescriptor(String type) {
        BuiltinTypeDescriptor result;
        try {
            result = new BuiltinTypeDescriptor(type, locator);
        } catch (StoreException e) {
            return null;
        }
        if (result.isValid()) {
            return result;
        }
        return null;
    }

    public List<ITypeDescriptor> getTypes(String namespace) {
        return null;
    }

    public void delete(ITypeDescriptor object) {
        throw new UnsupportedOperationException();
    }

    public Iterator<ITypeDescriptor> find(Map<String, Object> criteria) throws StoreException {
        List<ITypeDescriptor> result = new LinkedList<ITypeDescriptor>();
        if (criteria.containsKey("namespace")) {
            return getTypes((String) criteria.get("namespace")).iterator();
        }
        if (criteria.containsKey("supertype")) {
            Set<String> types = new HashSet<String>((List<String>) criteria.get("supertype"));
            try {
                Session session = UserSession.get().getJcrSession();
                NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                NodeTypeIterator ntIter = ntMgr.getAllNodeTypes();
                while (ntIter.hasNext()) {
                    NodeType nt = ntIter.nextNodeType();
                    for (NodeType superType : nt.getDeclaredSupertypes()) {
                        String name = superType.getName();
                        if (types.contains(name)) {
                            result.add(locator.locate(nt.getName()));
                            break;
                        }
                    }
                }
            } catch (RepositoryException e) {
                throw new StoreException("Could not find supertypes", e);
            }
        }
        return result.iterator();
    }

    public ITypeDescriptor load(String id) throws StoreException {
        ITypeDescriptor result = getTypeDescriptor(id);
        if (result == null) {
            throw new StoreException("Could not find type " + id);
        }
        return result;
    }

    public String save(ITypeDescriptor object) throws StoreException {
        throw new StoreException("Builtin type store is read-only");
    }

}
