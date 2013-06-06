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

class ContentTypesCache extends Sealable implements ContentTypes {

    static final Logger log = LoggerFactory.getLogger(ContentTypesCache.class);

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
    private final AggregatedContentTypesCache actCache = new AggregatedContentTypesCache();
    private Map<String, String> propertyTypeMappings = new HashMap<String, String>(jcrPropertyTypesMap);
    private Map<String, ContentTypeImpl> types = new TreeMap<String, ContentTypeImpl>();
    private SortedMap<String, Set<ContentType>> prefixesMap;

    public ContentTypesCache(Session session, EffectiveNodeTypesCache entCache) throws RepositoryException {
        this.entCache = entCache;
        loadContentTypes(session, true);
    }

    private void loadContentTypes(Session session, boolean allowRetry) throws RepositoryException {
        try {

            // 1st pass: load shallow versions of all ContentTypes
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
                                        ContentTypeImpl ct = new ContentTypeImpl(namespacePrefix.getName(), typeTemplate.getName(), version);
                                        if (entCache.getType(ct.getName()) == null) {
                                            log.error("No corresponding Effective NodeType found for defined ContentType named {}. Type ignored.", ct.getName());
                                        }
                                        else {
                                            types.put(ct.getName(), ct);
                                            typeNodes.put(ct.getName(), typeNode);
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
                loadContentType(typeName, typeNodes.get(typeName));
            }

            /* TODO
               not resolving 'extended' field properties like captions etc. because of the complicated and many different 'mapping' use-cases
               furthermore the order of processing is complicated as well as inherited fields are only resolved 2 steps down while matching to
               these extended field properties needs to have all fields resolved first which complicates the proper moment for sealing

            for (String typeName : typeNodes.keySet()) {
                loadContentTypeItemProperties(types.get(typeName), typeNodes.get(typeName));
            }
            */

            // 3rd pass: create derived ContentTypes for all effective node types without explicit ContentType definition
            //             and initialize the explicitly defined ContentTypes with their underlying (base) effective node type
            for (Map.Entry<String, EffectiveNodeTypeImpl> entry : entCache.getTypes().entrySet()) {
                ContentTypeImpl ct = types.get(entry.getKey());
                EffectiveNodeTypeImpl ent = new EffectiveNodeTypeImpl(entry.getValue());
                if (ct == null) {
                    types.put(entry.getKey(), new ContentTypeImpl(ent, version));
                }
                else {
                    ct.setEffectiveNodeType(ent);
                }
            }

            // 4th pass: resolve and cache all ContentTypes
            for (String name : types.keySet()) {
                resolveContentType(name);
            }

            // 5th pass: resolve all ContentTypeItems and seal all types
            for (AggregatedContentTypesCache.Key key : actCache.getKeys()) {
                resolveContentTypeItemsAndSeal(actCache.get(key));
            }

            //lock down the cache itself
            seal();
        }

        catch (RepositoryException re) {
            if (allowRetry) {
                loadContentTypes(session, false);
            }
            throw re;
        }
    }

    private void loadContentType(String typeName, Node typeNode) throws RepositoryException {
        ContentTypeImpl ct = types.get(typeName);
        ct.setMixin(JcrUtils.getBooleanProperty(typeNode, HippoNodeType.HIPPO_MIXIN, false));

        if (typeNode.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
            Value[] values = typeNode.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
            for (Value value : values) {
                String superType = value.getString();
                if (superType.length() == 0 || (types.get(superType) == null && entCache.getType(superType) == null)) {
                    // TODO: log warn invalid/unknown supertype
                    continue;
                }
                else {
                    ct.getSuperTypes().add(superType);
                }
            }
        }
        ct.setCascadeValidate(JcrUtils.getBooleanProperty(typeNode, HippoNodeType.HIPPO_CASCADEVALIDATION, false));

        if (typeNode.hasNodes()) {
            for (NodeIterator fieldsIterator = typeNode.getNodes(); fieldsIterator.hasNext(); ) {
                Node field = fieldsIterator.nextNode();

                if (field.isNodeType(HippoNodeType.NT_FIELD)) {

                    ContentTypeItemImpl cti;
                    String itemType;

                    String fieldName = field.getProperty(HippoNodeType.HIPPO_PATH).getString();

                    if ("*".equals(fieldName)) {
                        // ContentType model doesn't support residual fields
                        continue;
                    }

                    itemType = JcrUtils.getStringProperty(field, HippoNodeType.HIPPOSYSEDIT_TYPE, PropertyType.TYPENAME_STRING);

                    if (propertyTypeMappings.containsKey(itemType)) {
                        cti = new ContentTypePropertyImpl(ct.getName(), fieldName, itemType, propertyTypeMappings.get(itemType));
                    }
                    else if (types.containsKey(itemType)) {
                        cti = new ContentTypeChildImpl(ct.getName(), fieldName, itemType);
                    }
                    else if (entCache.getTypes().containsKey(itemType)) {
                        cti = new ContentTypeChildImpl(ct.getName(), fieldName, itemType);
                    }
                    else {
                        // TODO: log warn unknown itemType value
                        continue;
                    }
                    cti.setMandatory(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_MANDATORY, false));
                    cti.setAutoCreated(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_AUTOCREATED, false));
                    cti.setMultiple(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_MULTIPLE, false));
                    cti.setOrdered(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_ORDERED, false));
                    cti.setProtected(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_PROTECTED, false));
                    cti.setPrimaryItem(JcrUtils.getBooleanProperty(field, HippoNodeType.HIPPO_PRIMARY, false));

                    if (field.hasProperty(HippoNodeType.HIPPO_VALIDATORS)) {
                        Value[] values = field.getProperty(HippoNodeType.HIPPO_VALIDATORS).getValues();
                        for (Value value : values) {
                            String validator = value.getString();
                            if (validator.length() > 0) {
                                cti.getValidators().add(validator);
                            }
                        }
                    }
                    if (cti.isProperty()) {
                        ct.getProperties().put(cti.getName(), (ContentTypeProperty)cti);
                    }
                    else {
                        ct.getChildren().put(cti.getName(), (ContentTypeChild)cti);
                    }
                }
            }
        }
    }

    private void resolveContentType(String name) {
        ContentTypeImpl ct = types.get(name);

        // skip already resolved types
        if (actCache.get(name) != null) {
            return;
        }

        Set<String> mixins = new TreeSet<String>();

        for (String superType : ct.getSuperTypes()) {
            // ensure inherited ContentTypes are resolved first
            resolveContentType(superType);
        }

        // for non-derived types check if all super types are really defined in the underlying node type
        // this might not be the case for mixins added after initial definition/creation of the ContentType
        // See also: CMS7-7070
        if (!ct.isDerivedType()) {
            for (Iterator<String> iter = ct.getSuperTypes().iterator(); iter.hasNext();) {
                String superType = iter.next();
                if (!ct.getEffectiveNodeType().getSuperTypes().contains(superType)) {
                    // found a superType not enforced by the underlying node type, so not really a superType but a mixin
                    iter.remove();
                    // save it temporarily to create an aggregated variant later
                    mixins.add(superType);
                }
                else {
                    // check if superType itself contains aggregated mixins, then those also need/should be aggregated
                    // NOTE: the CMS DocumentTypeEditor itself doesn't do this (yet)
                    ContentTypeImpl sct = types.get(superType);
                    if (!sct.isDerivedType() && sct.getAggregatedTypes().size() > 1) {
                        for (String mixin : sct.getAggregatedTypes()) {
                            if (!mixin.equals(superType) // skip superType itself
                                    && !ct.getEffectiveNodeType().getSuperTypes().contains(mixin)) {
                                mixins.add(mixin);
                            }
                        }
                    }
                }
            }
        }

        if (!ct.getSuperTypes().isEmpty()) {
            ContentTypeImpl sct = getAggregatedContentType(ct.getSuperTypes());
            ct.merge(sct, true);
        }

        ct = actCache.put(ct);

        if (!ct.isDerivedType() && !ct.isMixin()) {
            if (ct.isContentType(HippoNodeType.NT_DOCUMENT)) {
                ct.setDocumentType(true);
            }
            // TODO: hippo:compound not defined as HippoNodeType constant
            else if (ct.isContentType("hippo:compound")) {
                ct.setCompoundType(true);
            }
        }

        if (!mixins.isEmpty()) {
            // we've got a ContentType with optional/extra mixin(s) specified as super type
            // drop mixins which are already contained
            for (Iterator<String> mixinIterator = mixins.iterator(); mixinIterator.hasNext(); ) {
                if (ct.isContentType(mixinIterator.next())) {
                    mixinIterator.remove();
                }
            }
            if (!mixins.isEmpty()) {
                // create an aggregated (default) variant
                ct = new ContentTypeImpl(ct);
                ct.merge(getAggregatedContentType(mixins), false);
                ct = actCache.put(ct);
            }
        }
        // update ct in types map to be picked up during the recursive resolving
        types.put(name, ct);
    }

    private ContentTypeImpl getAggregatedContentType(Set<String> names) {
        if (names.size() == 1) {
            // must already be in actCache
            return actCache.get(names.iterator().next());
        }

        AggregatedContentTypesCache.Key key = actCache.getKey(names);
        ContentTypeImpl result = actCache.get(key);
        if (result != null) {
            return result;
        }

        boolean newAggregate = false;
        while (!key.getNames().isEmpty()) {
            AggregatedContentTypesCache.Key subKey = actCache.findBest(key);
            ContentTypeImpl ct = actCache.get(subKey);
            if (result == null) {
                result = new ContentTypeImpl(ct); // clone as aggregate
            }
            else {
                if (result.merge(ct, false)) {
                    newAggregate = true;
                }
            }
            key = key.subtract(subKey);
        }
        if (newAggregate) {
            result = actCache.put(result);
        }

        return result;
    }

    private void resolveContentTypeItemsAndSeal(ContentTypeImpl ct) {
        if (!ct.isSealed()) {
            for (String s : ct.getSuperTypes()) {
                resolveContentTypeItemsAndSeal(actCache.get(s));
            }
            ct.resolveItems(this);
            ct.seal();
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
        prefixesMap = new TreeMap<String, Set<ContentType>>();
        for (Map.Entry<String, ContentTypeImpl> entry : types.entrySet()) {
            // recreate the prefix as the ContentType itself may have been 'upgraded' to an aggregate without a (single) prefix
            String prefix = entry.getKey().substring(0, entry.getKey().indexOf(":"));
            Set<ContentType> entries = prefixesMap.get(prefix);
            if (entries == null) {
                entries = new LinkedHashSet<ContentType>();
                prefixesMap.put(prefix, entries);
            }
            entries.add(entry.getValue());
        }
        for (Map.Entry<String, Set<ContentType>> entry : prefixesMap.entrySet()) {
            entry.setValue(Collections.unmodifiableSet(entry.getValue()));
        }
        prefixesMap = Collections.unmodifiableSortedMap(prefixesMap);
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public ContentTypeImpl getType(String name) {
        return types.get(name);
    }

    @Override
    public SortedMap<String, Set<ContentType>> getTypesByPrefix() {
        return prefixesMap;
    }

    @Override
    public EffectiveNodeTypesCache getEffectiveNodeTypes() {
        return entCache;
    }

    public AggregatedContentTypesCache getActCache() {
        return actCache;
    }
    @Override
    public ContentType getContentTypeForNode(Node node) throws RepositoryException {
        Set<String> names = new HashSet<String>();
        names.add(node.getPrimaryNodeType().getName());
        for (NodeType mixin : node.getMixinNodeTypes()) {
            names.add(mixin.getName());
        }
        return getAggregatedContentType(names);
    }

    @Override
    public ContentType getContentTypeForNodeByUuid(Session session, String uuid) throws RepositoryException {
        return getContentTypeForNode(session.getNodeByIdentifier(uuid));
    }

    @Override
    public ContentType getContentTypeForNodeByPath(Session session, String path) throws RepositoryException {
        return getContentTypeForNode(session.getNode(path));
    }
}
