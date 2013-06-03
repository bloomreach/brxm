/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.contenttype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;

class EffectiveNodeTypesCache extends Sealable implements EffectiveNodeTypes {

    private volatile static long versionSequence = 0;

    private final long version = ++versionSequence;

    private Map<String, EffectiveNodeTypeImpl> types = new TreeMap<String, EffectiveNodeTypeImpl>();
    private SortedMap<String, Set<EffectiveNodeType>> prefixesMap;

    public EffectiveNodeTypesCache(Session serviceSession) throws RepositoryException {
        loadEffectiveNodeTypes(serviceSession, true);
    }

    private void loadEffectiveNodeTypes(Session session, boolean allowRetry) throws RepositoryException {
        try {
            NodeTypeIterator nodeTypes = session.getWorkspace().getNodeTypeManager().getAllNodeTypes();

            // load all jcr node types (recursively if needed)
            while (nodeTypes.hasNext()) {
                loadEffectiveNodeType(nodeTypes.nextNodeType());
            }
        }
        catch (RepositoryException re) {
            if (allowRetry) {
                // for now only do and support retrying once
                loadEffectiveNodeTypes(session, false);
            }
            throw re;
        }

        // lock down
        seal();
    }

    private EffectiveNodeTypeImpl loadEffectiveNodeType(NodeType nodeType) throws RepositoryException {
        EffectiveNodeTypeImpl ent = types.get(nodeType.getName());
        if (ent == null) {
            ent = new EffectiveNodeTypeImpl(nodeType.getName(), version);

            ent.setMixin(nodeType.isMixin());
            ent.setAbstract(nodeType.isAbstract());
            ent.setOrdered(nodeType.hasOrderableChildNodes());
            ent.setPrimaryItemName(nodeType.getPrimaryItemName());

            types.put(ent.getName(), ent);

            // ensure all super types are also loaded
            for (NodeType superType : nodeType.getSupertypes()) {
                ent.getSuperTypes().add(loadEffectiveNodeType(superType).getName());
            }

            loadChildNodeDefinitions(nodeType, ent);
            loadPropertyDefinitions(nodeType, ent);
        }
        return ent;
    }

    private void loadChildNodeDefinitions(NodeType nodeType, EffectiveNodeTypeImpl ent) throws RepositoryException {
        for (NodeDefinition nd : nodeType.getChildNodeDefinitions()) {
            EffectiveNodeTypeChildImpl child =
                    // ensure child definition declaring type is also loaded
                    new EffectiveNodeTypeChildImpl(nd.getName(), loadEffectiveNodeType(nd.getDeclaringNodeType()).getName());

            for (NodeType childType : nd.getRequiredPrimaryTypes()) {
                // ensure all possible child types are also loaded
                child.getRequiredPrimaryTypes().add(loadEffectiveNodeType(childType).getName());
            }

            if (nd.getDefaultPrimaryType() != null) {
                // ensure possible primary type is also loaded
                child.setDefaultPrimaryType(loadEffectiveNodeType(nd.getDefaultPrimaryType()).getName());
            }
            child.setMandatory(nd.isMandatory());
            child.setAutoCreated(nd.isAutoCreated());
            child.setMultiple(nd.allowsSameNameSiblings());
            child.setProtected(nd.isProtected());

            // each child definition is maintained in a list by name
            List<EffectiveNodeTypeChild> childList = ent.getChildren().get(child.getName());
            if (childList == null) {
                childList = new ArrayList<EffectiveNodeTypeChild>();
                ent.getChildren().put(child.getName(), childList);
            }
            childList.add(child);
        }
    }

    private void loadPropertyDefinitions(NodeType nodeType, EffectiveNodeTypeImpl ent) throws RepositoryException {
        for (PropertyDefinition pd : nodeType.getPropertyDefinitions()) {
            EffectiveNodeTypePropertyImpl property =
                    // ensure property definition declaring type is also loaded
                    new EffectiveNodeTypePropertyImpl(pd.getName(), loadEffectiveNodeType(pd.getDeclaringNodeType()).getName(), pd.getRequiredType());

            property.setMandatory(pd.isMandatory());
            property.setAutoCreated(pd.isAutoCreated());
            property.setMultiple(pd.isMultiple());
            property.setProtected(pd.isProtected());

            String[] valueConstraints = pd.getValueConstraints();
            if (valueConstraints != null) {
                for (String s : valueConstraints) {
                    if (s != null) {
                        property.getValueConstraints().add(s);
                    }
                }
            }

            Value[] defaultValues = pd.getDefaultValues();
            if (defaultValues != null) {
                for (Value value : defaultValues) {
                    // skip/ignore BINARY type values (unsupported)
                    if (value.getType() != PropertyType.BINARY) {
                        property.getDefaultValues().add(value.getString());
                    }
                }
            }

            // each property definition is maintained in a list by name
            List<EffectiveNodeTypeProperty> propertyList = ent.getProperties().get(property.getName());
            if (propertyList == null) {
                propertyList = new ArrayList<EffectiveNodeTypeProperty>();
                ent.getProperties().put(property.getName(), propertyList);
            }
            propertyList.add(property);
        }
    }

    protected void doSeal() {
        for (Sealable s : types.values()) {
            s.seal();
        }

        types = Collections.unmodifiableMap(types);

        prefixesMap = new TreeMap<String, Set<EffectiveNodeType>>();
        for (Map.Entry<String, EffectiveNodeTypeImpl> entry : types.entrySet()) {
            Set<EffectiveNodeType> entries = prefixesMap.get(entry.getValue().getPrefix());
            if (entries == null) {
                entries = new LinkedHashSet<EffectiveNodeType>();
                prefixesMap.put(entry.getValue().getPrefix(), entries);
            }
            entries.add(entry.getValue());
        }
        for (Map.Entry<String, Set<EffectiveNodeType>> entry : prefixesMap.entrySet()) {
            entry.setValue(Collections.unmodifiableSet(entry.getValue()));
        }

        prefixesMap = Collections.unmodifiableSortedMap(prefixesMap);
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public EffectiveNodeType getType(String name) {
        return types.get(name);
    }

    @Override
    public SortedMap<String, Set<EffectiveNodeType>> getTypesByPrefix() {
        return prefixesMap;
    }

    public Map<String, EffectiveNodeTypeImpl> getTypes() {
        return types;
    }
}
