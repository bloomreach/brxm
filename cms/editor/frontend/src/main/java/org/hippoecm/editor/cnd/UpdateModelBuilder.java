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
package org.hippoecm.editor.cnd;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.editor.model.JcrNamespace;
import org.hippoecm.editor.type.JcrTypeDescriptor;
import org.hippoecm.editor.type.JcrTypeStore;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateModelBuilder {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: CndSerializer.java 18973 2009-07-23 10:01:26Z fvlankvelt $";

    private static Logger log = LoggerFactory.getLogger(UpdateModelBuilder.class);

    class TypeEntry {
        Node oldType;
        Node newType;

        TypeEntry(Node oldType, Node newType) {
            this.oldType = oldType;
            this.newType = newType;
        }

        ITypeDescriptor getOldType() throws StoreException {
            try {
                return new JcrTypeDescriptor(new JcrNodeModel(oldType), oldTypeLocator);
            } catch (RepositoryException e) {
                throw new StoreException(e);
            }
        }

        ITypeDescriptor getNewType() throws StoreException {
            try {
                // FIXME: it should be possible to delete types.
                if (newType == null) {
                    return new JcrTypeDescriptor(new JcrNodeModel(oldType), newTypeLocator);
                }
                return new JcrTypeDescriptor(new JcrNodeModel(newType), newTypeLocator);
            } catch (RepositoryException e) {
                throw new StoreException(e);
            }
        }

        TypeUpdate getUpdate() throws StoreException {
            if (oldType == null) {
                return null;
            }

            TypeUpdate update = new TypeUpdate();

            ITypeDescriptor oldTypeDescriptor = getOldType();
            ITypeDescriptor newTypeDescriptor = getNewType();

            if (newType != null) {
                update.newName = newTypeDescriptor.getName();
            } else {
                update.newName = oldTypeDescriptor.getName();
            }

            update.renames = new HashMap<FieldIdentifier, FieldIdentifier>();
            for (Map.Entry<String, IFieldDescriptor> entry : oldTypeDescriptor.getFields().entrySet()) {
                IFieldDescriptor origField = entry.getValue();
                FieldIdentifier oldId = new FieldIdentifier();
                oldId.path = origField.getPath();
                oldId.type = origField.getTypeDescriptor().getType();

                if (newType != null) {
                    IFieldDescriptor newField = newTypeDescriptor.getField(entry.getKey());
                    if (newField != null) {
                        FieldIdentifier newId = new FieldIdentifier();
                        newId.path = newField.getPath();
                        newId.type = newField.getTypeDescriptor().getType();

                        update.renames.put(oldId, newId);
                    }
                } else {
                    update.renames.put(oldId, oldId);
                }
            }
            return update;
        }
    }

    private Session jcrSession;
    private LinkedHashMap<String, TypeEntry> types;
    private HashMap<String, String> pseudoTypes;
    private ITypeLocator oldTypeLocator;
    private ITypeLocator newTypeLocator;
    private final CndSerializer serializer;

    public UpdateModelBuilder(Session session, final String namespace) throws StoreException {
        this.jcrSession = session;

        final JcrTypeStore jcrTypeStore = new JcrTypeStore();
        IStore<ITypeDescriptor> oldBuiltinTypeStore = new BuiltinTypeStore();
        oldTypeLocator = new TypeLocator(new IStore[] { jcrTypeStore, oldBuiltinTypeStore });
        jcrTypeStore.setTypeLocator(oldTypeLocator);

        JcrTypeStore newJcrTypeStore = new JcrTypeStore();
        final BuiltinTypeStore newBuiltinTypeStore = new BuiltinTypeStore();
        final ITypeLocator newLocator = new TypeLocator(new IStore[] { newJcrTypeStore, newBuiltinTypeStore });
        newTypeLocator = new ITypeLocator() {
            private static final long serialVersionUID = 1L;

            public ITypeDescriptor locate(String type) throws StoreException {
                if (type.indexOf(':') > 0 && namespace.equals(type.substring(0, type.indexOf(':')))) {
                    if (types.containsKey(type)) {
                        return types.get(type).getNewType();
                    } else if (pseudoTypes.containsKey(type)) {
                        return locate(pseudoTypes.get(type));
                    } else {
                        return newBuiltinTypeStore.load(type);
                    }
                }
                return newLocator.locate(type);
            }

            public List<ITypeDescriptor> getSubTypes(String type) throws StoreException {
                throw new StoreException("sub-types are not supported in cnd serializer");
            }

            public void detach() {
            }

        };
        newBuiltinTypeStore.setTypeLocator(newTypeLocator);
        newJcrTypeStore.setTypeLocator(newTypeLocator);

        this.serializer = new CndSerializer(jcrSession);
        addNamespace(namespace);

        initTypes(namespace);

        versionNamespace(namespace);
    }

    public Map<String, TypeUpdate> getUpdate() throws StoreException {
        Map<String, TypeUpdate> result = new HashMap<String, TypeUpdate>();
        for (Map.Entry<String, TypeEntry> entry : types.entrySet()) {
            TypeUpdate update = entry.getValue().getUpdate();
            if (update != null) {
                result.put(entry.getKey(), entry.getValue().getUpdate());
            }
        }
        return result;
    }

    public CndSerializer getSerializer() {
        return serializer;
    }

    private void initTypes(String namespace) throws StoreException {
        types = new LinkedHashMap<String, TypeEntry>();
        pseudoTypes = new HashMap<String, String>();

        try {
            JcrNamespace jcrNamespace = new JcrNamespace(jcrSession, namespace);
            String uri = jcrNamespace.getCurrentUri();
            Node nsNode = jcrSession.getRootNode().getNode(jcrNamespace.getPath().substring(1));

            NodeIterator typeIter = nsNode.getNodes();
            while (typeIter.hasNext()) {
                Node templateTypeNode = typeIter.nextNode();
                String pseudoName = namespace + ":" + templateTypeNode.getName();

                Node oldType = null, newType = null;

                boolean isPseudoType = false;
                Node ntNode = templateTypeNode.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                NodeIterator versions = ntNode.getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                while (versions.hasNext()) {
                    Node version = versions.nextNode();
                    if (version.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                        if (!pseudoName.equals(version.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString())) {
                            isPseudoType = true;
                            oldType = version;
                            break;
                        }
                    }
                    if (version.isNodeType(HippoNodeType.NT_REMODEL)) {
                        if (version.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                            oldType = version;
                        }
                    } else {
                        newType = version;
                    }
                }
                if (oldType == null && newType == null) {
                    throw new StoreException("No description found for either old or new version of type " + pseudoName);
                }
                if (!isPseudoType) {
                    TypeEntry entry = new TypeEntry(oldType, newType);
                    types.put(pseudoName, entry);
                    getSerializer().addType(entry.getNewType());
                } else {
                    pseudoTypes.put(pseudoName, oldType.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString());
                }
            }
        } catch (RepositoryException ex) {
            throw new StoreException("Error retrieving namespace description from repository");
        }
    }

    private void addNamespace(String prefix) {
        getSerializer().addNamespace(prefix);
    }

    private void versionNamespace(String prefix) {
        String namespace = getSerializer().getNamespace(prefix);
        String last = namespace;
        int pos = namespace.lastIndexOf('/');
        try {
            for (String registered : jcrSession.getNamespacePrefixes()) {
                String uri = jcrSession.getNamespaceURI(registered);
                if (uri.startsWith(namespace.substring(0, pos + 1))) {
                    if (isLater(uri, last)) {
                        last = uri;
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            return;
        }

        int minorPos = last.lastIndexOf('.');
        if (minorPos > pos) {
            int minor = Integer.parseInt(last.substring(minorPos + 1));
            namespace = last.substring(0, minorPos + 1) + new Integer(minor + 1).toString();
            getSerializer().remap(prefix, namespace);
        } else {
            log.warn("namespace for " + prefix + " does not conform to versionable format");
        }
    }

    private static boolean isLater(String one, String two) {
        int pos = one.lastIndexOf('/');
        String[] oneVersions = one.substring(pos + 1).split("\\.");
        String[] twoVersions = two.substring(pos + 1).split("\\.");
        for (int i = 0; i < oneVersions.length; i++) {
            if (i < twoVersions.length) {
                int oneVersion = Integer.parseInt(oneVersions[i]);
                int twoVersion = Integer.parseInt(twoVersions[i]);
                if (oneVersion > twoVersion) {
                    return true;
                } else if (oneVersion < twoVersion) {
                    return false;
                }
            } else {
                return true;
            }
        }
        return false;
    }

}
