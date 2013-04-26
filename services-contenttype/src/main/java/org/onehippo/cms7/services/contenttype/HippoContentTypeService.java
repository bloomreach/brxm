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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoContentTypeService implements ContentTypeService {

    static final Logger log = LoggerFactory.getLogger(HippoContentTypeService.class);

    private Session serviceSession;
    private EffectiveNodeTypesCache entCache;
    private AggregatedDocumentTypesCache adtCache;

    private EventListener nodeTypesChangeListener = new EventListener() {
        @Override
        public void onEvent(final EventIterator events) {
            synchronized (HippoContentTypeService.this) {
                // delete caches to be rebuild again on next invocation
                entCache = null;
                adtCache = null;
            }
        }
    };

    private EventListener documentTypesChangeListener = new EventListener() {
        @Override
        public void onEvent(final EventIterator events) {
            // TODO: make it more finegrained by only reacting to changes of 'committed' document types
            synchronized (HippoContentTypeService.this) {
                // delete caches to be rebuild again on next invocation
                adtCache = null;
            }
        }
    };

    public HippoContentTypeService(Session serviceSession) throws RepositoryException {
        this.serviceSession = serviceSession;

        // register our nodeTypesChangeListener
        serviceSession.getWorkspace().getObservationManager().addEventListener(nodeTypesChangeListener,
                Event.NODE_ADDED|Event.NODE_REMOVED|Event.NODE_MOVED|Event.PROPERTY_ADDED|Event.PROPERTY_CHANGED|Event.PROPERTY_REMOVED,
                "/jcr:system/jcr:nodeTypes", true, null, null, false);

        // register our documentTypesChangeListener
        serviceSession.getWorkspace().getObservationManager().addEventListener(documentTypesChangeListener,
                Event.NODE_ADDED|Event.NODE_REMOVED|Event.NODE_MOVED|Event.PROPERTY_ADDED|Event.PROPERTY_CHANGED|Event.PROPERTY_REMOVED,
                "/hippo:namespaces", true, null, null, false);
    }

    public synchronized void shutdown() {
        try {
            serviceSession.getWorkspace().getObservationManager().removeEventListener(documentTypesChangeListener);
            serviceSession.getWorkspace().getObservationManager().removeEventListener(nodeTypesChangeListener);
        } catch (RepositoryException e) {
            // ignore
        }
        adtCache = null;
        entCache = null;
        serviceSession = null;
    }

    @Override
    public synchronized EffectiveNodeTypesCache getEffectiveNodeTypes() throws RepositoryException {
        if (entCache == null) {
            entCache = loadEffectiveNodeTypes(serviceSession, true);
            // TODO: check if not already changed again
        }
        return entCache;
    }

    private synchronized AggregatedDocumentTypesCache getAggregatedDocumentTypes() throws RepositoryException {
        if (adtCache == null) {
            adtCache = loadDocumentTypes(serviceSession, true);
            // TODO: check if not already changed again
        }
        return adtCache;
    }

    @Override
    public DocumentTypesCache getDocumentTypes() throws RepositoryException {
        return getAggregatedDocumentTypes().getDocumentTypesCache();
    }

    //@Override
    public DocumentType getDocumentType(String nodeTypeName) throws RepositoryException {
        AggregatedDocumentTypesCache adtCache = getAggregatedDocumentTypes();
        // TODO
        return null;
    }

    //@Override
    public DocumentType getDocumentType(String[] nodeTypeNames) throws RepositoryException {
        // TODO
        return null;
    }

    //@Override
    public DocumentType getDocumentType(Node node) throws RepositoryException {
        // TODO
        return  null;
    }

    //@Override
    public DocumentType getDocumentType(Session session, String path) throws RepositoryException {
        // TODO
        return null;
    }

    //@Override
    public DocumentType getDocumentType(String uuid, Session session) throws RepositoryException {
        // TODO
        return null;
    }

    //@Override
    public Iterator<DocumentType> getDocumentTypes(Iterator<Node> nodes) throws RepositoryException {
        // TODO
        return null;
    }

    //@Override
    public Iterator<DocumentType> getDocumentTypes(Session session, Iterator<String> paths) throws RepositoryException {
        // TODO
        return null;
    }

    //@Override
    public Iterator<DocumentType> getDocumentTypes(Iterator<String> uuids, Session session) throws RepositoryException {
        // TODO
        return null;
    }

    private EffectiveNodeTypesCache loadEffectiveNodeTypes(Session session, boolean allowRetry) throws RepositoryException {
        EffectiveNodeTypesCache entCache = new EffectiveNodeTypesCache();

        try {
            NodeTypeIterator nodeTypes = session.getWorkspace().getNodeTypeManager().getAllNodeTypes();

            // load all jcr node types (recursively if needed)
            while (nodeTypes.hasNext()) {
                loadEffectiveNodeType(nodeTypes.nextNodeType(), entCache);
            }
        }
        catch (RepositoryException re) {
            if (allowRetry) {
              // for now only do and support retrying once
              return loadEffectiveNodeTypes(session, false);
            }
            throw re;
        }

        // lock down
        entCache.seal();

        return entCache;
    }

    private EffectiveNodeTypeImpl loadEffectiveNodeType(NodeType nodeType, EffectiveNodeTypesCache entCache) throws RepositoryException {
        EffectiveNodeTypeImpl ent = entCache.getTypes().get(nodeType.getName());
        if (ent == null) {
            ent = new EffectiveNodeTypeImpl(nodeType.getName(), entCache.version());

            ent.setMixin(nodeType.isMixin());
            ent.setAbstract(nodeType.isAbstract());
            ent.setOrdered(nodeType.hasOrderableChildNodes());
            ent.setPrimaryItemName(nodeType.getPrimaryItemName());

            entCache.getTypes().put(ent.getName(), ent);

            // ensure all super types are also loaded
            for (NodeType superType : nodeType.getSupertypes()) {
                ent.getSuperTypes().add(loadEffectiveNodeType(superType, entCache).getName());
            }

            loadChildNodeDefinitions(nodeType, ent, entCache);
            loadPropertyDefinitions(nodeType, ent, entCache);
        }
        return ent;
    }

    private void loadChildNodeDefinitions(NodeType nodeType, EffectiveNodeTypeImpl ent, EffectiveNodeTypesCache entCache) throws RepositoryException {
        for (NodeDefinition nd : nodeType.getChildNodeDefinitions()) {
            EffectiveNodeTypeChildImpl child =
                    // ensure child definition declaring type is also loaded
                    new EffectiveNodeTypeChildImpl(nd.getName(), loadEffectiveNodeType(nd.getDeclaringNodeType(), entCache).getName());

            for (NodeType childType : nd.getRequiredPrimaryTypes()) {
                // ensure all possible child types are also loaded
                child.getRequiredPrimaryTypes().add(loadEffectiveNodeType(childType, entCache).getName());
            }

            if (nd.getDefaultPrimaryType() != null) {
                // ensure possible primary type is also loaded
                child.setDefaultPrimaryType(loadEffectiveNodeType(nd.getDefaultPrimaryType(), entCache).getName());
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

    private void loadPropertyDefinitions(NodeType nodeType, EffectiveNodeTypeImpl ent, EffectiveNodeTypesCache entCache) throws RepositoryException {
        for (PropertyDefinition pd : nodeType.getPropertyDefinitions()) {
            EffectiveNodeTypePropertyImpl property =
                    // ensure property definition declaring type is also loaded
                    new EffectiveNodeTypePropertyImpl(pd.getName(), loadEffectiveNodeType(pd.getDeclaringNodeType(), entCache).getName(), pd.getRequiredType());

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

    private AggregatedDocumentTypesCache loadDocumentTypes(Session session, boolean allowRetry) throws RepositoryException {
        try {
            DocumentTypesCache dtCache = new DocumentTypesCache(getEffectiveNodeTypes());
            AggregatedDocumentTypesCache adtCache = new AggregatedDocumentTypesCache(dtCache);

            // 1st pass: load shallow versions of all document types
            Map<String, Node> typeNodes = new HashMap<String, Node>();

            Node namespacePrefixes = session.getNode("/" + HippoNodeType.NAMESPACES_PATH);
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
                                    if (typeNode.hasProperty(HippoNodeType.HIPPO_NODE) // use-case: hippo:namespaces/hippo:document doesn't have NIPPO_NODE property
                                            && !typeNode.getProperty(HippoNodeType.HIPPO_NODE).getBoolean()) {
                                        String typeAlias = ("system".equals(namespacePrefix.getName()) ? "" : namespacePrefix.getName()+":") + typeTemplate.getName();
                                        String jcrType = typeNode.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)
                                                ? typeNode.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString() : typeTemplate.getName();
                                        try {
                                            PropertyType.valueFromName(jcrType);
                                            dtCache.getPropertyTypeMappings().put(typeAlias, jcrType);
                                        }
                                        catch (IllegalArgumentException iae) {
                                            log.error("Illegal JCR property type {} defined for property type alias {}. Property alias ignored.", jcrType, typeAlias);
                                        }
                                    }
                                    else if (typeNode.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                                        // TODO: for now don't support document/compound type aliasing: those are assumed only used/needed for custom editors, not a different type
                                    }
                                    else {
                                        DocumentTypeImpl dt = new DocumentTypeImpl(namespacePrefix.getName(), typeTemplate.getName(), dtCache.version());
                                        if (dtCache.getEffectiveNodeTypes().getType(dt.getName()) == null) {
                                            log.error("No corresponding Effective NodeType found for defined Document Type named {}. Type ignored.", dt.getName());
                                        }
                                        else {
                                            dtCache.getTypes().put(dt.getName(), dt);
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
                loadDocumentType(typeName, typeNodes.get(typeName), dtCache);
            }

            /* TODO
               not resolving 'extended' field properties like captions etc. because of the complicated and many different 'mapping' use-cases
               furthermore the order of processing is complicated as well as inherited fields are only resolved 2 steps down while matching to
               these extended field properties needs to have all fields resolved first which complicates the proper moment for sealing

            for (String typeName : typeNodes.keySet()) {
                loadDocumentTypeFieldProperties((DocumentTypeImpl)dtCache.getType(typeName), typeNodes.get(typeName), dtCache);
            }
            */

            Map<String, DocumentTypeImpl> allDocumentTypes = new HashMap<String, DocumentTypeImpl>(dtCache.getTypes());

            // 3rd pass: create derived document types for all effective node types without explicit document type definition
            //             and initialize the concrete document types with their underlying (base) effective node type
            for (Map.Entry<String, EffectiveNodeTypeImpl> entry : adtCache.getDocumentTypesCache().getEffectiveNodeTypes().getTypes().entrySet()) {
                DocumentTypeImpl dt = allDocumentTypes.get(entry.getKey());
                EffectiveNodeTypeImpl ent = new EffectiveNodeTypeImpl(entry.getValue());
                if (dt == null) {
                    allDocumentTypes.put(entry.getKey(), new DocumentTypeImpl(ent, dtCache.version()));
                }
                else {
                    dt.setEffectiveNodeType(ent);
                }
            }

            // 4th pass: resolve and cache all document types
            for (String name : allDocumentTypes.keySet()) {
                resolveDocumentType(name, allDocumentTypes, adtCache);
            }

            // update cache map of defined document types
            for (Map.Entry<String, DocumentTypeImpl> entry : allDocumentTypes.entrySet()) {
                if (!entry.getValue().isDerivedType()) {
                    adtCache.getDocumentTypesCache().getTypes().put(entry.getKey(), entry.getValue());
                }
            }

            // 5th pass: resolve all document type fields and seal all types
            for (AggregatedDocumentTypesCache.Key key : adtCache.getKeys()) {
                DocumentTypeImpl dt = adtCache.get(key);
                dt.resolveFields(adtCache);
                dt.seal();
            }

            //lock down the cache itself
            dtCache.seal();

            return adtCache;
        }

        catch (RepositoryException re) {
            if (allowRetry) {
                return loadDocumentTypes(session, false);
            }
            throw re;
        }
    }

    private void loadDocumentType(String typeName, Node typeNode, DocumentTypesCache dtCache) throws RepositoryException {
        DocumentTypeImpl dt = dtCache.getTypes().get(typeName);
        if (typeNode.hasProperty(HippoNodeType.HIPPO_MIXIN)) {
            dt.setMixin(typeNode.getProperty(HippoNodeType.HIPPO_MIXIN).getBoolean());
        }
        if (typeNode.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
            Value[] values = typeNode.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
            for (Value value : values) {
                String superType = value.getString();
                if (superType.length() == 0 || (dtCache.getType(superType) == null && dtCache.getEffectiveNodeTypes().getType(superType) == null)) {
                    // TODO: log warn invalid/unknown supertype
                    continue;
                }
                else {
                    dt.getSuperTypes().add(superType);
                }
            }
        }
        if (typeNode.hasProperty(HippoNodeType.HIPPO_CASCADEVALIDATION)) {
            dt.setCascadeValidate(typeNode.getProperty(HippoNodeType.HIPPO_CASCADEVALIDATION).getBoolean());
        }

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
                    if (!field.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                        // TODO: log warn invalid/undefined field type
                        fieldType = PropertyType.TYPENAME_STRING;
                    }
                    else {
                        fieldType = field.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString();
                    }
                    if (dtCache.getPropertyTypeMappings().containsKey(fieldType)) {
                        df = new DocumentTypeFieldImpl(dt.getName(), fieldName, fieldType, dtCache.getPropertyTypeMappings().get(fieldType));
                    }
                    else if (dtCache.getTypes().containsKey(fieldType)) {
                        df = new DocumentTypeFieldImpl(dt.getName(), fieldName, fieldType);
                    }
                    else if (dtCache.getEffectiveNodeTypes().getTypes().containsKey(fieldType)) {
                        df = new DocumentTypeFieldImpl(dt.getName(), fieldName, fieldType);
                    }
                    else {
                        // TODO: log warn unknown fieldType value
                        continue;
                    }
                    if (field.hasProperty(HippoNodeType.HIPPO_MANDATORY)) {
                        df.setMandatory(field.getProperty(HippoNodeType.HIPPO_MANDATORY).getBoolean());
                    }
                    if (field.hasProperty(HippoNodeType.HIPPO_AUTOCREATED)) {
                        df.setAutoCreated(field.getProperty(HippoNodeType.HIPPO_AUTOCREATED).getBoolean());
                    }
                    if (field.hasProperty(HippoNodeType.HIPPO_MULTIPLE)) {
                        df.setMultiple(field.getProperty(HippoNodeType.HIPPO_MULTIPLE).getBoolean());
                    }
                    if (field.hasProperty(HippoNodeType.HIPPO_ORDERED)) {
                        df.setOrdered(field.getProperty(HippoNodeType.HIPPO_ORDERED).getBoolean());
                    }
                    if (field.hasProperty(HippoNodeType.HIPPO_PROTECTED)) {
                        df.setProtected(field.getProperty(HippoNodeType.HIPPO_PROTECTED).getBoolean());
                    }
                    if (field.hasProperty(HippoNodeType.HIPPO_PRIMARY)) {
                        df.setPrimaryField(field.getProperty(HippoNodeType.HIPPO_PRIMARY).getBoolean());
                    }
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

    private void resolveDocumentType(String name, Map<String, DocumentTypeImpl> types, AggregatedDocumentTypesCache adtCache) {
        DocumentTypeImpl dt = types.get(name);

        // skip already resolved types
        if (adtCache.get(name) != null) {
            return;
        }

        Set<String> mixins = new TreeSet<String>();

        for (String superType : dt.getSuperTypes()) {
            // ensure inherited document types are resolved first
            resolveDocumentType(superType, types, adtCache);
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
            DocumentTypeImpl sdt = getAggregatedDocumentType(dt.getSuperTypes(), types, adtCache);
            dt.merge(sdt, true);
        }

        dt = adtCache.put(dt);

        if (!mixins.isEmpty()) {
            // we've got a document type with optional/extra mixin(s) specified as super type
            // create an aggregated (default) variant
            dt = new DocumentTypeImpl(dt);
            dt.merge(getAggregatedDocumentType(mixins, types, adtCache), false);
            dt = adtCache.put(dt);
        }
        // update dt in types map to be picked up during the recursive resolving
        types.put(name, dt);
    }

    private DocumentTypeImpl getAggregatedDocumentType(Set<String> names, Map<String, DocumentTypeImpl> types, AggregatedDocumentTypesCache adtCache) {
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

    // not yet used, but should be
    private void loadDocumentTypeFieldProperties(DocumentTypeImpl dt, Node typeNode, DocumentTypesCache dtCache) throws RepositoryException {
        Node typeTemplate = typeNode.getParent().getParent();
        if (typeTemplate.hasNode("editor:templates/_default_")) {
            Node editorTemplate = typeTemplate.getNode("editor:templates/_default_");
            // TODO: this won't resolve mixin fields like taxonomy:classifiable property keys as its configuration is stored in the mixin configuration itself
            for (DocumentTypeField value : dt.getFields().values()) {
                DocumentTypeFieldImpl df = (DocumentTypeFieldImpl)value;
                // TODO: this won't match inherited fields as their namespace/name/path is indirectly encoded in the child nodes, not by node name ...
                // TODO: this doesn't work anyway as df.getName() is based on the path property of the hipposysedit:field node, not its name (the latter is 'lost' when arriving here)
                if (editorTemplate.hasNode(df.getName())) {
                    Node fieldEditor = editorTemplate.getNode(df.getName());
                    if (fieldEditor.hasProperty("caption")) {
                        df.setCaption(fieldEditor.getProperty("caption").getString());
                    }
                    if (fieldEditor.hasNode("cluster.options")) {
                        Node clusterOptions = fieldEditor.getNode("cluster.options");
                        for (PropertyIterator pi = clusterOptions.getProperties(); pi.hasNext(); ) {
                            Property p = pi.nextProperty();
                            ArrayList valList = new ArrayList();
                            if (p.getType() == PropertyType.STRING) {
                                if (p.isMultiple()) {
                                    for (Value v : p.getValues()) {
                                        valList.add(v.getString());
                                    }
                                }
                                else {
                                    valList.add(p.getString());
                                }
                                df.getFieldProperties().put(p.getName(), valList);
                            }
                        }
                    }
                }
            }
        }
    }
}
