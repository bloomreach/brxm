/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDefStore;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeDefDiff;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeTypeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QPropertyDefinitionBuilder;

import static org.apache.jackrabbit.spi.commons.name.NameConstants.NT_BASE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.NT_UNSTRUCTURED;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.REP_ROOT;

public class HippoNodeTypeRegistry extends NodeTypeRegistry {

    /**
     * Extended NodeTypeDefDiff implementation to allow more relaxed (or more 'smart') handling and marking of specific
     * node type changes as being 'TRIVIAL' (or ignored), because they are validated and known to be compatible upfront.
     */
    private static class CustomNodeTypeDefDiff extends NodeTypeDefDiff {
        protected CustomNodeTypeDefDiff(final QNodeTypeDefinition oldDef, final QNodeTypeDefinition newDef) {
            super(oldDef, newDef);
        }

        @Override
        public int supertypesDiff() {
            Set<Name> oldSupertypes = new HashSet<>(Arrays.asList(getOldDef().getSupertypes()));
            Set<Name> newSupertypes = new HashSet<>(Arrays.asList(getNewDef().getSupertypes()));

            {
                // ignore adding mixin hippo:identifiable to a nodetype, which is compatible
                Optional<Name> hippoIdentifiable = newSupertypes.stream()
                        .filter(s -> s.getLocalName().equals("identifiable")
                                && s.getNamespaceURI().startsWith("http://www.onehippo.org/jcr/hippo/nt/"))
                        .findFirst();
                if (hippoIdentifiable.isPresent()) {
                    // if mixin is present on the new list of supertypes, remove it from both new and old, to ignore it
                    newSupertypes.remove(hippoIdentifiable.get());
                    oldSupertypes.remove(hippoIdentifiable.get());
                }
            }

            return !oldSupertypes.equals(newSupertypes) ? MAJOR : NONE;
        }
    }

    private final NamespaceRegistry registry;

    private static ThreadLocal<Boolean> ignoreNextConflictingContent = new ThreadLocal<>();
    private static ThreadLocal<Boolean> ignoreNextCheckReferencesInContent = new ThreadLocal<>();
    private static ThreadLocal<Boolean> ignoreNextPersistCustomNodeTypeDefs = new ThreadLocal<>();

    /**
     * <p>
     * Modify the Jackrabbit builtin nodetype definition for rep:root to remove SNS and orderable children support
     * which it inherits from extending nt:unstructured.
     * As result of the below changes, rep:root will no longer extend nt:unstructured but instead get's the
     * same residual child node and property definition from it added <em>without</em> the orderable and sns.
     * </p>
     * <p>
     * While technically these changes might be easier to 'implement' or apply by customizing the builtin_nodetypes.cnd
     * as bundled within the jackrabbit-core module in our hippo version of it, but as Jackrabbit itself has several
     * unit tests which assume sns and/or orderable children of the root node, those tests then also would need to be
     * modified (which is too invasive).
     * </p>
     * <p>
     * The <em>Jackrabbit</em> provided rep:root nodetype is based upon the following definitions (from builtin_nodetypes.cnd):
     * <pre>
         [rep:root] > nt:unstructured
         + jcr:system (rep:system) = rep:system mandatory IGNORE

         [nt:unstructured]
         orderable
         - * (UNDEFINED) multiple
         - * (UNDEFINED)
         + * (nt:base) = nt:unstructured sns VERSION
     * </pre>
     * which gets modified dynamically by this method to the following definition:
     * <pre>
         [rep:root] > nt:base
         + jcr:system (rep:system) = rep:system mandatory IGNORE
         - * (UNDEFINED) multiple
         - * (UNDEFINED)
         + * (nt:base) = nt:unstructured VERSION
     * </pre>
     * </p>
     *
     * <p>
     * Note: this changes are not (and don't need to be) persisted as the builtin nodetypes as only read and
     * never written (back).
     * </p>
     * @param store The {@link NodeTypeDefStore} into which the node type
     *              definitions are loaded.
     * @throws RepositoryException If an error occurs while loading the
     *                             built-in node type definitions.
     */
    protected void loadBuiltInNodeTypeDefs(NodeTypeDefStore store)
            throws RepositoryException {
        super.loadBuiltInNodeTypeDefs(store);
        QNodeTypeDefinitionImpl currentRepRootNodeTypeDef = (QNodeTypeDefinitionImpl)store.get(REP_ROOT);

        // create new rep:root NodeTypeDefinition
        QNodeTypeDefinitionBuilder repRootNodeTypeDefBuilder = new QNodeTypeDefinitionBuilder();
        repRootNodeTypeDefBuilder.setName(REP_ROOT);
        repRootNodeTypeDefBuilder.setSupertypes(new Name[]{NT_BASE});

        // we'll add 2 property and 2 child node definitions
        QPropertyDefinition[] propDefs = new QPropertyDefinition[2];
        QNodeDefinition[] childNodeDefs = new QNodeDefinition[2];

        // reuse current (and only child) rep:system child node definition from current rep:root node type definition
        childNodeDefs[0] = currentRepRootNodeTypeDef.getChildNodeDefs()[0];

        // create new rep:root residiual child node def for nt:base (default nt:unstructured) children
        QNodeDefinitionBuilder ntBaseResidualNodeDefBuilder = new QNodeDefinitionBuilder();
        ntBaseResidualNodeDefBuilder.setDeclaringNodeType(REP_ROOT);
        ntBaseResidualNodeDefBuilder.addRequiredPrimaryType(NT_BASE);
        ntBaseResidualNodeDefBuilder.setDefaultPrimaryType(NT_UNSTRUCTURED);
        ntBaseResidualNodeDefBuilder.setOnParentVersion(OnParentVersionAction.VERSION);
        // add the new nt:base residiual child node definition
        childNodeDefs[1] = ntBaseResidualNodeDefBuilder.build();

        QPropertyDefinitionBuilder undefinedResidualPropDefBuilder = new QPropertyDefinitionBuilder();
        undefinedResidualPropDefBuilder.setDeclaringNodeType(REP_ROOT);
        undefinedResidualPropDefBuilder.setRequiredType(PropertyType.UNDEFINED);
        // create new single UNDEFINED (type) residual property
        propDefs[0] = undefinedResidualPropDefBuilder.build();

        undefinedResidualPropDefBuilder.setMultiple(true);
        // create new multiple UNDEFINED (type) residual property
        propDefs[1] = undefinedResidualPropDefBuilder.build();

        // add the property and child node definitions to the new rep:root node type definition
        repRootNodeTypeDefBuilder.setPropertyDefs(propDefs);
        repRootNodeTypeDefBuilder.setChildNodeDefs(childNodeDefs);

        // now remove the predefined rep:root definition
        store.remove(REP_ROOT);
        // and replace it with ours
        store.add(repRootNodeTypeDefBuilder.build());
    }

    public HippoNodeTypeRegistry(NamespaceRegistry registry, FileSystem fileSystem) throws RepositoryException {
        super(registry, fileSystem);
        this.registry = registry;
    }

    @Override
    protected NodeTypeDefDiff createNodeTypeDefDiff(final QNodeTypeDefinition oldDef, final QNodeTypeDefinition newDef) {
        if (oldDef == null || newDef == null) {
            throw new IllegalArgumentException("arguments can not be null");
        }
        if (!oldDef.getName().equals(newDef.getName())) {
            throw new IllegalArgumentException("at least node type names must be matching");
        }
        // use our custom NodeTypeDefDiff validator instead of the more limited/restricted one provided by Jackrabbit
        return new CustomNodeTypeDefDiff(oldDef, newDef);
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
     * @throws RepositoryException If there is conflicting content or if the
     *                             check failed for some other reason.
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
     * @throws RepositoryException repositoryException
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
     * @throws RepositoryException repositoryException
     */
    protected Map<Name, QNodeTypeDefinition> buildSuperTypesMap(Map<Name, QNodeTypeDefinition> superTypesMap, Name[] superTypes) throws RepositoryException {
        for (Name name : superTypes) {
            if (!(NT_BASE.equals(name) || superTypesMap.containsKey(name))) {
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

    /**
     * Skip persisting external (already processed and persisted) nodetype registration again!
     * @param ntDefs node type definitions
     * @throws RepositoryException if an error occurs
     * @throws InvalidNodeTypeDefException if the node type definition is invalid
     */
    public void externalRegistered(Collection<QNodeTypeDefinition> ntDefs) throws RepositoryException, InvalidNodeTypeDefException {
        // save current threadlocal value
        Boolean currentIgnoreNextPersistCustomNodeTypeDefs = ignoreNextPersistCustomNodeTypeDefs.get();
        try {
            ignoreNextPersistCustomNodeTypeDefs.set(true);
            super.externalRegistered(ntDefs);
        } finally {
            // restore threadlocal value
            ignoreNextPersistCustomNodeTypeDefs.set(currentIgnoreNextPersistCustomNodeTypeDefs);
        }
    }

    /**
     * Allow all external (already processed) nodetype reregistration: no need to verify them against possible
     * incompatible changes. Furthermore skip persisting such (already processed and persisted) changes again!
     * @param ntDef node type definition
     * @throws RepositoryException if an error occurs
     * @throws NoSuchNodeTypeException if the node type had not yet been registered
     * @throws InvalidNodeTypeDefException if the node type definition is invalid
     */
    @Override
    public void externalReregistered(QNodeTypeDefinition ntDef)
            throws NoSuchNodeTypeException, InvalidNodeTypeDefException,
            RepositoryException {
        // save current threadlocal values
        Boolean currentIgnoreNextConflictingContent = ignoreNextConflictingContent.get();
        Boolean currentIgnoreNextPersistCustomNodeTypeDefs = ignoreNextPersistCustomNodeTypeDefs.get();
        try {
            ignoreNextConflictingContent.set(true);
            ignoreNextPersistCustomNodeTypeDefs.set(true);
            super.externalReregistered(ntDef);
        } finally {
            // restore threadlocal values
            ignoreNextConflictingContent.set(currentIgnoreNextConflictingContent);
            ignoreNextPersistCustomNodeTypeDefs.set(currentIgnoreNextPersistCustomNodeTypeDefs);
        }
    }

    /**
     * Allow all external (already processed) nodetype unregistration: no need to check references in context.
     * Furthermore skip persisting such (already processed and persisted) changes again!
     * @param ntNames node type qnames
     * @throws RepositoryException if an error occurs
     * @throws NoSuchNodeTypeException if a node type is already unregistered
     */
    @Override
    public void externalUnregistered(Collection<Name> ntNames) throws RepositoryException, NoSuchNodeTypeException {
        // save current threadlocal values
        Boolean currentIgnoreNextCheckReferencesInContent = ignoreNextCheckReferencesInContent.get();
        Boolean currentIgnoreNextPersistCustomNodeTypeDefs = ignoreNextPersistCustomNodeTypeDefs.get();
        try {
            ignoreNextCheckReferencesInContent.set(true);
            ignoreNextPersistCustomNodeTypeDefs.set(true);
            super.externalUnregistered(ntNames);
        } finally {
            // restore threadlocal values
            ignoreNextCheckReferencesInContent.set(currentIgnoreNextCheckReferencesInContent);
            ignoreNextPersistCustomNodeTypeDefs.set(currentIgnoreNextPersistCustomNodeTypeDefs);
        }
    }

    /**
     * Ignore persisting of custom node types if unnecessary for the current thread (once)
     * @param store The {@link NodeTypeDefStore} containing the definitions to
     *              be persisted.
     * @throws RepositoryException If an error occurs while persisting the
     *                             custom node type definitions.
     */
    @Override
    protected void persistCustomNodeTypeDefs(NodeTypeDefStore store) throws RepositoryException {
        if (ignoreNextPersistCustomNodeTypeDefs.get() != null) {
            ignoreNextPersistCustomNodeTypeDefs.remove();
            return;
        }
        super.persistCustomNodeTypeDefs(store);
    }
}