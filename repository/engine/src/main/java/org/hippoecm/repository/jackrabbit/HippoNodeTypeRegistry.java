/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeDefDiff;

public class HippoNodeTypeRegistry extends NodeTypeRegistry {

    private final NamespaceRegistry registry;

    private static ThreadLocal<Boolean> ignoreNextConflictingContent = new ThreadLocal<>();
    private static ThreadLocal<Boolean> ignoreNextCheckReferencesInContent = new ThreadLocal<>();

    public HippoNodeTypeRegistry(NamespaceRegistry registry, FileSystem fileSystem) throws RepositoryException {
        super(registry, fileSystem);
        this.registry = registry;
    }

    public void ignoreNextConflictingContent() {
        ignoreNextConflictingContent.set(true);
    }

    public void ignoreNextCheckReferencesInContent() {
        ignoreNextCheckReferencesInContent.set(true);
    }

    /**
     * Skip checks for changes in the hippo namespaces. "Trust me, I know what I'm doing".
     * Also may fixup a diff when the type is only trivially extended through additional trivial supertypes.
     *
     * @param ntd  The node type definition replacing the former node type definition of the same name.
     * @param diff The diff of the node type definition with the currently registered type
     * @throws javax.jcr.RepositoryException
     */
    @Override
    protected void checkForConflictingContent(final QNodeTypeDefinition ntd, NodeTypeDefDiff diff) throws RepositoryException {
        if (ignoreNextConflictingContent.get() != null) {
            ignoreNextConflictingContent.remove();
            return;
        }
        final Name name = ntd.getName();
        final String prefix = registry.getPrefix(name.getNamespaceURI());
        final String[] systemPrefixes = {"hippo", "hipposys", "hipposysedit", "hippofacnav", "hipposched"};
        for (String systemPrefix : systemPrefixes) {
            if (prefix.equals(systemPrefix)) {
                return;
            }
        }
        if (diff.isMajor() && diff.supertypesDiff() == NodeTypeDefDiff.MAJOR) {
            diff = fixupTrivialSuperTypesDiff(ntd, diff);
            if (!diff.isModified()) {
                return;
            }
        }
        super.checkForConflictingContent(ntd, diff);
    }

    @Override
    protected void checkForReferencesInContent(Name nodeTypeName)
            throws RepositoryException {
        if (ignoreNextCheckReferencesInContent.get() != null) {
            ignoreNextCheckReferencesInContent.remove();
            return;
        }
        super.checkForReferencesInContent(nodeTypeName);
    }

    /**
     * Checks and possible fixup a NodeTypeDefDiff of type MAJOR caused by a difference in supertypes.

     * If the new nodetypedef only adds trivial supertype(s) without any additional type 'constraints'
     * (properties, child node types, etc.) then these are effectively harmless and can be ignored.
     * The returned NodeTypeDefDiff then is 'fixed up' by using a modified nodetypedef with the original supertypes.
     *
     * @param ntd the new nodetypediff
     * @param diff the nodetypedefdiff of type MAJOR caused by a difference in supertype
     * @return either the original nodetypedefdiff (still of type MAJOR) or a fixed up modified instance (which still MAY be of type MAJOR for other reasons)
     * @throws RepositoryException
     */
    protected NodeTypeDefDiff fixupTrivialSuperTypesDiff(final QNodeTypeDefinition ntd, final NodeTypeDefDiff diff) throws RepositoryException {
        QNodeTypeDefinition ntdOld = getNodeTypeDef(ntd.getName());

        Map<Name, QNodeTypeDefinition> superTypesMapOld = buildSuperTypesMap(new HashMap<>(), ntdOld.getSupertypes());
        Map<Name, QNodeTypeDefinition> superTypesMap = buildSuperTypesMap(new HashMap<>(), ntd.getSupertypes());

        for (Name superType : superTypesMapOld.keySet()) {
            if (superTypesMap.remove(superType) == null) {
                // non-trivial supertype removal, no fixup
                return diff;
            }
        }
        if (!superTypesMap.isEmpty()) {
            // should be
            for (QNodeTypeDefinition def : superTypesMap.values()) {
                if (!isTrivialTypeDef(def)) {
                    // non-trivial additional supertype, no fixup
                    return diff;
                }
            }
            QNodeTypeDefinition ntdFixup = new QNodeTypeDefinitionImpl(ntd.getName(),
                    ntdOld.getSupertypes(),
                    ntd.getSupportedMixinTypes(), ntd.isMixin(),
                    ntd.isAbstract(), ntd.isQueryable(),
                    ntd.hasOrderableChildNodes(), ntd.getPrimaryItemName(),
                    ntd.getPropertyDefs(), ntd.getChildNodeDefs());
            return NodeTypeDefDiff.create(ntdOld, ntdFixup);
        }
        return diff;
    }

    /**
     * Builds and returns the recursively determined map of superType (Name) with their QNodeTypeDefinition
     * @param superTypesMap the map to fill
     * @param superTypes the superTypes to map
     * @return the mapped superTypes
     * @throws RepositoryException
     */
    protected Map<Name, QNodeTypeDefinition> buildSuperTypesMap(Map<Name, QNodeTypeDefinition> superTypesMap, Name[] superTypes) throws RepositoryException {
        for (Name name : superTypes) {
            if (!(NameConstants.NT_BASE.equals(name) || superTypesMap.containsKey(name))) {
                QNodeTypeDefinition def = getNodeTypeDef(name);
                superTypesMap.put(name, def);
                buildSuperTypesMap(superTypesMap, def.getSupertypes());
            }
        }
        return superTypesMap;
    }

    /**
     * Determine if a type definition itself is trivial: adding no additional type constraints other than its 'marker' name.
     *
     * Note: this ignores 'inherited' constraints from its supertypes because the check is done against all added supertype
     * already, including those inherited.
     *
     * @param def the type definition to check
     * @return true if the type definition (itself) is trivial
     */
    protected boolean isTrivialTypeDef(QNodeTypeDefinition def) {
        return !def.hasOrderableChildNodes() &&
                def.getPrimaryItemName() == null &&
                def.getPropertyDefs().length == 0 &&
                def.getChildNodeDefs().length == 0;
    }
}