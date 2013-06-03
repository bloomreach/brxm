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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DocumentTypesCache extends Sealable implements DocumentTypes {

    static final Logger log = LoggerFactory.getLogger(DocumentTypesCache.class);

    private volatile static long versionSequence = 0;

    private final long version = ++versionSequence;

    private static Map<String, String> jcrPropertyTypesMap = new HashMap<String, String>();
    static {
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_BINARY, PropertyType.TYPENAME_BINARY);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_BOOLEAN, PropertyType.TYPENAME_BOOLEAN);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_DATE, PropertyType.TYPENAME_DATE);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_DECIMAL, PropertyType.TYPENAME_DECIMAL);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_DOUBLE, PropertyType.TYPENAME_DOUBLE);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_LONG, PropertyType.TYPENAME_LONG);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_NAME, PropertyType.TYPENAME_NAME);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_PATH, PropertyType.TYPENAME_PATH);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_REFERENCE, PropertyType.TYPENAME_REFERENCE);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_STRING, PropertyType.TYPENAME_STRING);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_URI, PropertyType.TYPENAME_URI);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_WEAKREFERENCE, PropertyType.TYPENAME_WEAKREFERENCE);
        jcrPropertyTypesMap = Collections.unmodifiableMap(jcrPropertyTypesMap);
    }

    private final EffectiveNodeTypesCache entCache;
    private final AggregatedDocumentTypesCache adtCache = new AggregatedDocumentTypesCache();
    private Map<String, String> propertyTypeMappings = new HashMap<String, String>(jcrPropertyTypesMap);
    private Map<String, DocumentTypeImpl> types = new TreeMap<String, DocumentTypeImpl>();
    private SortedMap<String, Set<DocumentType>> prefixesMap;

    public DocumentTypesCache(Session session, EffectiveNodeTypesCache entCache) throws RepositoryException {
        this.entCache = entCache;
        loadDocumentTypes(session, true);
    }

    private void loadDocumentTypes(Session session, boolean allowRetry) throws RepositoryException {
        try {

            // 1st pass: load shallow versions of all document types
            Map<String, Node> typeNodes = new HashMap<String, Node>();

            Node namespacePrefixes = session.getRootNode().getNode(HippoNodeType.NAMESPACES_PATH);
            for (NodeIterator prefixIterator = namespacePrefixes.getNodes(); prefixIterator.hasNext(); ) {
                Node namespacePrefix = prefixIterator.nextNode();

                if (namespacePrefix.hasNodes()) {

                    for (NodeIterator templatesIterator = namespacePrefix.getNodes(); templatesIterator.hasNext(); ) {

                        Node typeTemplate = templatesIterator.nextNode();
                        if (typeTemplate.hasNode(HippoNodeType.NT_NODETYPE)) {

                            Node handle = typeTemplate.getNode(HippoNodeType.NT_NODETYPE);
                            Node typeNode = null;

                            for (NodeIterator typeIterator = handle.getNodes(HippoNodeType.NT_NODETYPE); typeIterator.hasNext(); ) {
                                typeNode = typeIterator.nextNode();
                                if (typeNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                                    // use-case: hippo:namespaces/hippo:document doesn't have NIPPO_NODE property
                                    if (!JcrUtils.getBooleanProperty(typeNode, HippoNodeType.HIPPO_NODE, true)) {
                                        String typeAlias = ("system".equals(namespacePrefix.getName()) ? "" : namespacePrefix.getName()+":") + typeTemplate.getName();
                                        String jcrType = JcrUtils.getStringProperty(typeNode, HippoNodeType.HIPPOSYSEDIT_TYPE, typeTemplate.getName());
                                        try {
                                            PropertyType.valueFromName(jcrType);
                                            propertyTypeMappings.put(typeAlias, jcrType);
                                        }
                                        catch (IllegalArgumentException iae) {
                                            log.error("Illegal JCR property type {} defined for property type alias {}. Property alias ignored.", jcrType, typeAlias);
                                        }
                                    }
                                    else if (typeNode.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                                        // TODO: for now don't support document/compound type aliasing: those are assumed only used/needed for custom editors, not a different type
                                    }
                                    else {
                                        DocumentTypeImpl dt = new DocumentTypeImpl(namespacePrefix.getName(), typeTemplate.getName(), version);
                                        if (entCache.getType(dt.getName()) == null) {
                                            log.error("No corresponding Effective NodeType found for defined Document Type named {}. Type ignored.", dt.getName());
                                        }
                                        else {
                                            types.put(dt.getName(), dt);
                                            typeNodes.put(dt.getName(), typeNode);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2nd pass: load each type
            for (String typeName : typeNodes.keySet()) {
                loadDocumentType(typeName, typeNodes.get(typeName));
            }

            /* TODO
               not resolving 'extended' field properties like captions etc. because of the complicated and many different 'mapping' use-cases
               furthermore the order of processing is complicated as well as inherited fields are only resolved 2 steps down while matching to
               these extended field properties needs to have all fields resolved first which complicates the proper moment for sealing

            for (String typeName : typeNodes.keySet()) {
                loadDocumentTypeFieldProperties((DocumentTypeImpl)dtCache.getType(typeName), typeNodes.get(typeName), dtCache);
            }
            */

            Map<String, DocumentTypeImpl> allDocumentTypes = new HashMap<String, DocumentTypeImpl>(types);

            // 3rd pass: create derived document types for all effective node types without explicit document type definition
            //             and initialize the concrete document types with their underlying (base) effective node type
            for (Map.Entry<String, EffectiveNodeTypeImpl> entry : entCache.getTypes().entrySet()) {
                DocumentTypeImpl dt = allDocumentTypes.get(entry.getKey());
                EffectiveNodeTypeImpl ent = new EffectiveNodeTypeImpl(entry.getValue());
                if (dt == null) {
                    allDocumentTypes.put(entry.getKey(), new DocumentTypeImpl(ent, version));
                }
                else {
                    dt.setEffectiveNodeType(ent);
                }
            }

            // 4th pass: resolve and cache all document types
            for (String name : allDocumentTypes.keySet()) {
                resolveDocumentType(name, allDocumentTypes);
            }

            // update cache map of defined document types
            for (Map.Entry<String, DocumentTypeImpl> entry : allDocumentTypes.entrySet()) {
                if (!entry.getValue().isDerivedType()) {
                    types.put(entry.getKey(), entry.getValue());
                }
            }

            // 5th pass: resolve all document type fields and seal all types
            for (AggregatedDocumentTypesCache.Key key : adtCache.getKeys()) {
                resolveDocumentTypeFieldsAndSeal(adtCache.get(key));
            }

            //lock down the cache itself
            seal();
        }

        catch (RepositoryException re) {
            if (allowRetry) {
                loadDocumentTypes(session, false);
            }
            throw re;
        }
    }

    private void loadDocumentType(String typeName, Node typeNode) throws RepositoryException {
        DocumentTypeImpl dt = types.get(typeName);
        dt.setMixin(JcrUtils.getBooleanProperty(typeNode, HippoNodeType.HIPPO_MIXIN, false));

        if (typeNode.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
            Value[] values = typeNode.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
            for (Value value : values) {
                String superType = value.getString();
                if (superType.length() == 0 || (types.get(superType) == null && entCache.getType(superType) == null)) {
                    // TODO: log warn invalid/unknown supertype
                    continue;
                }
                else {
                    dt.getSuperTypes().add(superType);
                }
            }
        }
        dt.setCascadeValidate(JcrUtils.getBooleanProperty(typeNode, HippoNodeType.HIPPO_CASCADEVALIDATION, false));

        if (typeNode.hasNodes()) {
            for (NodeIterator fieldsIterator = typeNode.getNodes(); fieldsIterator.hasNext(); ) {
                Node field = fieldsIterator.nextNode();

                if (field.isNodeType(HippoNodeType.NT_FIELD)) {

                    DocumentTypeFieldImpl df;
                    String fieldType;

                    String fieldName = field.getProperty(HippoNodeType.HIPPO_PATH).getString();

                    if ("*".equals(fieldName)) {
                        // DocumentType model doesn't support residual fields
                        continue;
                    }

                    fieldType = JcrUtils.getStringProperty(field, HippoNodeType.HIPPOSYSEDIT_TYPE, PropertyType.TYPENAME_STRING);

                    if (propertyTypeMappings.containsKey(fieldType)) {
                        df = new DocumentTypeFieldImpl(dt.getName(), fieldName, fieldType, propertyTypeMappings.get(fieldType));
                    }
                    else if (types.containsKey(fieldType)) {
                        df = new DocumentTypeFieldImpl(dt.getName(), fieldName, fieldType);
                    }
                    else if (entCache.getTypes().containsKey(fieldType)) {
                        df = new DocumentTypeFieldImpl(dt.getName(), fieldName, fieldType);
                    }
                    else {
                        // TODO: log warn unknown fieldType value
                        continue;
                    }
                    df.setMandatory(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_MANDATORY, false));
                    df.setAutoCreated(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_AUTOCREATED, false));
                    df.setMultiple(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_MULTIPLE, false));
                    df.setOrdered(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_ORDERED, false));
                    df.setProtected(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_PROTECTED, false));
                    df.setPrimaryField(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_PRIMARY, false));

                    if (field.hasProperty(HippoNodeType.HIPPO_VALIDATORS)) {
                        Value[] values = field.getProperty(HippoNodeType.HIPPO_VALIDATORS).getValues();
                        for (Value value : values) {
                            String validator = value.getString();
                            if (validator.length() > 0) {
                                df.getValidators().add(validator);
                            }
                        }
                    }
                    dt.getFields().put(df.getName(), df);
                }
            }
        }
    }

    private void resolveDocumentType(String name, Map<String, DocumentTypeImpl> types) {
        DocumentTypeImpl dt = types.get(name);

        // skip already resolved types
        if (adtCache.get(name) != null) {
            return;
        }

        Set<String> mixins = new TreeSet<String>();

        for (String superType : dt.getSuperTypes()) {
            // ensure inherited document types are resolved first
            resolveDocumentType(superType, types);
        }

        // for non-derived types check if all super types are really defined in the underlying node type
        // this might not be the case for mixins added after initial definition/creation of the document type
        // See also: CMS7-7070
        if (!dt.isDerivedType()) {
            for (Iterator<String> iter = dt.getSuperTypes().iterator(); iter.hasNext();) {
                String superType = iter.next();
                if (!dt.getEffectiveNodeType().getSuperTypes().contains(superType)) {
                    // found a superType not enforced by the underlying node type, so not really a superType but a mixin
                    iter.remove();
                    // save it temporarily to create an aggregated variant later
                    mixins.add(superType);
                }
                else {
                    // check if superType itself contains aggregated mixins, then those also need/should be aggregated
                    // NOTE: the DocumentTypeEditor itself doesn't do this (yet)
                    DocumentTypeImpl sdt = types.get(superType);
                    if (!sdt.isDerivedType() && sdt.getAggregatedTypes().size() > 1) {
                        for (String mixin : sdt.getAggregatedTypes()) {
                            if (!mixin.equals(superType) // skip superType itself
                                    && !dt.getEffectiveNodeType().getSuperTypes().contains(mixin)) {
                                mixins.add(mixin);
                            }
                        }
                    }
                }
            }
        }

        if (!dt.getSuperTypes().isEmpty()) {
            DocumentTypeImpl sdt = getAggregatedDocumentType(dt.getSuperTypes());
            dt.merge(sdt, true);
        }

        dt = adtCache.put(dt);

        if (!mixins.isEmpty()) {
            // we've got a document type with optional/extra mixin(s) specified as super type
            // drop mixins which are already contained
            for (Iterator<String> mixinIterator = mixins.iterator(); mixinIterator.hasNext(); ) {
                if (dt.isDocumentType(mixinIterator.next())) {
                    mixinIterator.remove();
                }
            }
            if (!mixins.isEmpty()) {
                // create an aggregated (default) variant
                dt = new DocumentTypeImpl(dt);
                dt.merge(getAggregatedDocumentType(mixins), false);
                dt = adtCache.put(dt);
            }
        }
        // update dt in types map to be picked up during the recursive resolving
        types.put(name, dt);
    }

    private DocumentTypeImpl getAggregatedDocumentType(Set<String> names) {
        if (names.size() == 1) {
            // must already be in adtCache
            return adtCache.get(names.iterator().next());
        }

        AggregatedDocumentTypesCache.Key key = adtCache.getKey(names);
        DocumentTypeImpl result = adtCache.get(key);
        if (result != null) {
            return result;
        }

        boolean newAggregate = false;
        while (!key.getNames().isEmpty()) {
            AggregatedDocumentTypesCache.Key subKey = adtCache.findBest(key);
            DocumentTypeImpl dt = adtCache.get(subKey);
            if (result == null) {
                result = new DocumentTypeImpl(dt); // clone as aggregate
            }
            else {
                if (result.merge(dt, false)) {
                    newAggregate = true;
                }
            }
            key = key.subtract(subKey);
        }
        if (newAggregate) {
            result = adtCache.put(result);
        }

        return result;
    }

    private void resolveDocumentTypeFieldsAndSeal(DocumentTypeImpl dt) {
        if (!dt.isSealed()) {
            for (String s : dt.getSuperTypes()) {
                resolveDocumentTypeFieldsAndSeal(adtCache.get(s));
            }
            dt.resolveFields(this);
            dt.seal();
        }
    }

    @Override
    protected void doSeal() {
        propertyTypeMappings = Collections.unmodifiableMap(propertyTypeMappings);

        for (Sealable s : types.values()) {
            s.seal();
        }

        types = Collections.unmodifiableMap(types);

        // create the prefixesMap for accessing the predefined types by their prefix
        prefixesMap = new TreeMap<String, Set<DocumentType>>();
        for (Map.Entry<String, DocumentTypeImpl> entry : types.entrySet()) {
            // recreate the prefix as the DocumentType itself may have been 'upgraded' to an aggregate without a (single) prefix
            String prefix = entry.getKey().substring(0, entry.getKey().indexOf(":"));
            Set<DocumentType> entries = prefixesMap.get(prefix);
            if (entries == null) {
                entries = new LinkedHashSet<DocumentType>();
                prefixesMap.put(prefix, entries);
            }
            entries.add(entry.getValue());
        }
        for (Map.Entry<String, Set<DocumentType>> entry : prefixesMap.entrySet()) {
            entry.setValue(Collections.unmodifiableSet(entry.getValue()));
        }
        prefixesMap = Collections.unmodifiableSortedMap(prefixesMap);
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public DocumentTypeImpl getType(String name) {
        return types.get(name);
    }

    @Override
    public SortedMap<String, Set<DocumentType>> getTypesByPrefix() {
        return prefixesMap;
    }

    @Override
    public EffectiveNodeTypesCache getEffectiveNodeTypes() {
        return entCache;
    }

    public AggregatedDocumentTypesCache getAdtCache() {
        return adtCache;
    }
    @Override
    public DocumentType getDocumentTypeForNode(Node node) throws RepositoryException {
        DocumentType dt = null;
        Set<String> names = new HashSet<String>();
        names.add(node.getPrimaryNodeType().getName());
        for (NodeType mixin : node.getMixinNodeTypes()) {
            names.add(mixin.getName());
        }
        return getAggregatedDocumentType(names);
    }

    @Override
    public DocumentType getDocumentTypeForNodeByUuid(Session session, String uuid) throws RepositoryException {
        return getDocumentTypeForNode(session.getNodeByIdentifier(uuid));
    }

    @Override
    public DocumentType getDocumentTypeForNodeByPath(Session session, String path) throws RepositoryException {
        return getDocumentTypeForNode(session.getNode(path));
    }
}
