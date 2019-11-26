/*
 * Copyright 2013-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentTypeImpl extends Sealable implements ContentType {

    static final Logger log = LoggerFactory.getLogger(ContentTypeImpl.class);

    private final long version;
    private boolean aggregate;
    private boolean derivedType;
    private EffectiveNodeTypeImpl ent;
    private String name;
    private String prefix;
    private SortedSet<String> superTypes = new TreeSet<>();
    private SortedSet<String> aggregatedTypes = new TreeSet<>();
    private boolean documentType;
    private boolean compoundType;
    private boolean mixin;
    private boolean cascadeValidate;
    private Map<String, ContentTypeProperty> properties = new LinkedHashMap<>();
    private Map<String, ContentTypeChild> children = new LinkedHashMap<>();
    private List<String> validators = new ArrayList<>();

    public ContentTypeImpl(final String prefix, final String name, final long contentTypesVersion) {
        this.version = contentTypesVersion;
        this.aggregate = false;
        this.derivedType = false;
        this.prefix = prefix;
        this.name = prefix + ":" + name;
        aggregatedTypes.add(this.name);
    }

    public ContentTypeImpl(final EffectiveNodeTypeImpl ent, final long contentTypesVersion) {
        this.version = contentTypesVersion;
        aggregate = false;
        this.derivedType = true;
        this.name = ent.getName();
        this.prefix = ent.getPrefix();
        this.ent = ent;
        documentType = false;
        compoundType = false;
        mixin = ent.isMixin();
        superTypes.addAll(ent.getSuperTypes());
        aggregatedTypes.addAll(ent.getAggregatedTypes());
        cascadeValidate = false;
    }

    public ContentTypeImpl(final ContentTypeImpl other) {
        this.version = other.version;
        aggregate = other.aggregate;
        this.derivedType = other.derivedType;
        prefix = other.prefix;
        this.ent = new EffectiveNodeTypeImpl(other.ent); // clone
        name = other.name;
        documentType = other.documentType;
        compoundType = other.compoundType;
        mixin = other.mixin;
        superTypes.addAll(other.superTypes);
        aggregatedTypes.addAll(other.aggregatedTypes);
        cascadeValidate = other.cascadeValidate;
        for (final Map.Entry<String, ContentTypeProperty> entry : other.properties.entrySet()) {
            properties.put(entry.getKey(), new ContentTypePropertyImpl((ContentTypePropertyImpl)entry.getValue()));
        }
        for (final Map.Entry<String, ContentTypeChild> entry : other.children.entrySet()) {
            children.put(entry.getKey(), new ContentTypeChildImpl((ContentTypeChildImpl) entry.getValue()));
        }
        validators.addAll(other.validators);
    }

    @Override
    protected void doSeal() {
        ent.seal();
        superTypes = Collections.unmodifiableSortedSet(superTypes);
        aggregatedTypes = Collections.unmodifiableSortedSet(aggregatedTypes);
        for (final ContentTypeItem cti : properties.values() ) {
            ((Sealable)cti).seal();
        }
        properties = Collections.unmodifiableMap(properties);
        for (final ContentTypeItem cti : children.values() ) {
            ((Sealable)cti).seal();
        }
        children = Collections.unmodifiableMap(children);
        validators = Collections.unmodifiableList(validators);
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public boolean isDerivedType() {
        return derivedType;
    }

    @Override
    public boolean isAggregate() {
        return aggregate;
    }

    @Override
    public EffectiveNodeTypeImpl getEffectiveNodeType() {
        return ent;
    }

    public void setEffectiveNodeType(final EffectiveNodeTypeImpl ent) {
        checkSealed();
        this.ent = ent;
        superTypes.addAll(ent.getSuperTypes());
        aggregatedTypes.addAll(ent.getAggregatedTypes());
        this.name = null; // reset possible cached derived name
    }

    @Override
    public String getName() {
        if (name == null) {
            final Iterator<String> iterator = aggregatedTypes.iterator();
            final StringBuilder sb = new StringBuilder(iterator.next());
            while (iterator.hasNext()) {
                sb.append(',');
                sb.append(iterator.next());
            }
            name = sb.toString();
        }
        return name;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public SortedSet<String> getSuperTypes() {
        return superTypes;
    }

    public SortedSet<String> getAggregatedTypes() {
        return aggregatedTypes;
    }

    @Override
    public boolean isContentType(final String contentTypeName) {
        return aggregatedTypes.contains(contentTypeName) || superTypes.contains(contentTypeName);
    }

    @Override
    public boolean isDocumentType() {
        return documentType;
    }

    public void setDocumentType(final boolean documentType) {
        checkSealed();
        this.documentType = documentType;
    }

    @Override
    public boolean isCompoundType() {
        return compoundType;
    }

    public void setCompoundType(final boolean compoundType) {
        checkSealed();
        this.compoundType = compoundType;
    }

    @Override
    public boolean isMixin() {
        return mixin;
    }

    public void setMixin(final boolean mixin) {
        checkSealed();
        this.mixin = mixin;
    }

    @Override
    public boolean isCascadeValidate() {
        return cascadeValidate;
    }

    public void setCascadeValidate(final boolean cascadeValidate) {
        checkSealed();
        this.cascadeValidate = cascadeValidate;
    }

    @Override
    public ContentTypeItem getItem(final String name) {
        final ContentTypeItem item = children.get(name);
        return item != null ? item : properties.get(name);
    }

    @Override
    public List<String> getValidators() {
        return validators;
    }

    @Override
    public Map<String, ContentTypeProperty> getProperties() {
        return properties;
    }

    @Override
    public Map<String, ContentTypeChild> getChildren() {
        return children;
    }

    public int hashCode() {
        return isSealed() ? getName().hashCode() : super.hashCode();
    }

    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ContentTypeImpl && this.isSealed() && ((Sealable)obj).isSealed()) {
            return this.getName().equals(((ContentTypeImpl)obj).getName());
        }
        return false;
    }

    public boolean contains(final ContentTypeImpl other) {
        for (final String s : other.superTypes) {
            if (!isContentType(s)) {
                return false;
            }
        }
        for (final String s : other.aggregatedTypes) {
            if (!isContentType(s)) {
                return false;
            }
        }
        return true;
    }

    public boolean merge(final ContentTypeImpl other, final boolean superType) {
        checkSealed();
        if (!ent.merge(other.getEffectiveNodeType(), superType) && contains(other)) {
            return false;
        }

        aggregate = true;
        name = null;
        prefix = null;

        if (!compoundType) {
            compoundType = other.isCompoundType();
        }

        if (!other.isDerivedType()) {
            derivedType = false;
        }

        if (other.isCascadeValidate()) {
            cascadeValidate = true;
        }

        ContentTypeItemImpl cti;

        for (final Map.Entry<String, ContentTypeChild> entry : other.getChildren().entrySet()) {
            if (!isContentType(entry.getValue().getDefiningType())) {
                cti = (ContentTypeItemImpl)children.get(entry.getKey());
                if (cti != null) {
                    // duplicate child name
                    if (cti.isMultiple() != entry.getValue().isMultiple() ||
                            cti.getItemType().equals(entry.getValue().getItemType())) {
                        log.error("Conflicting ContentType child named {} encountered while merging ContentType {} into {}. Incoming child ignored."
                                , cti.getName(), other.getName(), getName());
                    }
                    else {
                        log.warn("Duplicate ContentType child named {} encountered while merging ContentType {} into {}. Incoming child ignored."
                                , cti.getName(), other.getName(), getName());
                    }
                }
                else {
                    if (!isDerivedType() && properties.containsKey(entry.getKey())) {
                        // non-derived types may not have property & child by same name: child takes precedence
                        cti = (ContentTypeItemImpl)properties.remove(entry.getKey());
                        log.warn("Duplicate ContentType child and property named {} encountered while merging ContentType {} into non-derived type {}. Dropping existing property."
                                , cti.getName(), other.getName(), getName());
                    }
                    children.put(entry.getKey(), new ContentTypeChildImpl((ContentTypeChildImpl)entry.getValue()));
                }
            }
        }

        for (final Map.Entry<String, ContentTypeProperty> entry : other.getProperties().entrySet()) {
            if (!isContentType(entry.getValue().getDefiningType())) {
                cti = (ContentTypeItemImpl)properties.get(entry.getKey());
                if (cti != null) {
                    // duplicate property name
                    if (cti.isMultiple() != entry.getValue().isMultiple() ||
                            !cti.getEffectiveType().equals(entry.getValue().getEffectiveType())) {
                        log.error("Conflicting ContentType property named {} encountered while merging ContentType {} into {}. Incoming property ignored."
                                , cti.getName(), other.getName(), getName());
                    }
                    else {
                        log.warn("Duplicate ContentType property named {} encountered while merging ContentType {} into {}. Incoming property ignored."
                               , cti.getName(), other.getName(), getName());
                    }
                }
                else if (!isDerivedType() && children.containsKey(entry.getKey())) {
                    // non-derived types may not have property & child by same name: child takes precedence
                    cti = (ContentTypeItemImpl)children.get(entry.getKey());
                    log.warn("Duplicate ContentType child and property named {} encountered while merging ContentType {} into non-derived type {}. Incoming property ignored."
                            , cti.getName(), other.getName(), getName());
                }
                else {
                    properties.put(entry.getKey(), new ContentTypePropertyImpl((ContentTypePropertyImpl)entry.getValue()));
                }
            }
        }

        superTypes.addAll(other.superTypes);

        if (superType) {
            superTypes.addAll(other.aggregatedTypes);
        }
        else {
            aggregatedTypes.addAll(other.aggregatedTypes);
        }

        return true;
    }

    public void resolveItems(final ContentTypesCache ctCache) {
        checkSealed();
        mergeInheritedItems(ctCache);
        mapEffectiveItems(ctCache);
        resolveUnmappedItems(ctCache);
    }

    private void mergeInheritedItems(final ContentTypesCache ctCache) {
        for (final String s : superTypes) {
            final ContentTypeImpl ct = ctCache.getActCache().get(s);
            for (final Map.Entry<String, ContentTypeChild> entry : ct.children.entrySet()) {
                if (!children.containsKey(entry.getKey())) {
                    if (!isDerivedType() && properties.remove(entry.getKey()) != null) {
                        log.warn("Duplicate ContentType child and property named {} encountered while merging super type {} items into non-derived type {}. Dropping existing property."
                                , entry.getKey(), ct.getName(), getName());
                    }
                    children.put(entry.getKey(), entry.getValue());
                }
            }
            for (final Map.Entry<String, ContentTypeProperty> entry : ct.properties.entrySet()) {
                if (!properties.containsKey(entry.getKey())) {
                    if (!isDerivedType() && children.containsKey(entry.getKey())) {
                        log.warn("Duplicate ContentType child and property named {} encountered while merging super type {} items into non-derived type {}. Ignoring super type property."
                                , entry.getKey(), ct.getName(), getName());
                    }
                    else {
                        properties.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    private void mapEffectiveItems(final ContentTypesCache ctCache) {
        final Set<String> removedItems = new HashSet<>();

        for (final Map.Entry<String,List<EffectiveNodeTypeChild>> entry : ent.getChildren().entrySet()) {
            if (!"*".equals(entry.getKey())) {
                ContentTypeChildImpl cti = (ContentTypeChildImpl)children.get(entry.getKey());
                if (cti == null) {
                    if (!isDerivedType() && properties.containsKey(entry.getKey())) {
                        log.error("Effective NodeType {} defines a child node named {} which conflicts with an non-derived ContentType {} property. "
                                 + "The ContentType property is removed and the NodeType child will be hidden."
                                 , ent.getName(), entry.getKey(), getName());
                        properties.remove(entry.getKey());
                        removedItems.add(entry.getKey());
                    }
                    else {
                        cti = new ContentTypeChildImpl(entry.getValue().get(0));
                        children.put(entry.getKey(), cti);
                        if (entry.getValue().size() > 1) {
                            cti.setMultiChildTypes(entry.getValue());
                        }
                    }
                }
                else if (cti.isSealed()) {
                    // skip already processed inherited children
                    continue;
                }
                else {
                    // first check predefined ContentTypes cache: it might contain an aggregated (with optional mixins) ContentType
                    ContentTypeImpl ct = ctCache.getType(cti.getEffectiveType());
                    if (ct == null) {
                        // not an aggregated ContentType: get it from the aggregated ContentType cache (which also contains every non-aggregated type)
                        ct = ctCache.getActCache().get(cti.getEffectiveType());
                    }
                    if (ct == null) {
                        log.error("Effective NodeType {} defines a child named {} with a matching child in ContentType {} but with unknown type {}. "
                                + "The NodeType child will be hidden and the ContentType child removed.",
                                ent.getName(), entry.getKey(), getName(), cti.getEffectiveType());
                        children.remove(entry.getKey());
                        removedItems.add(entry.getKey());
                    }
                    else {
                        EffectiveNodeTypeChild matchingChild = null;
                        boolean mismatch = false;
                        for (final EffectiveNodeTypeChild c : entry.getValue()) {
                            if (matchingChild == null && (!cti.isMultiple() || c.isMultiple())) {
                                boolean match = true;
                                for (final String requiredType : c.getRequiredPrimaryTypes()) {
                                    if (!ct.isContentType(requiredType)) {
                                        match = false;
                                        break;
                                    }
                                }
                                if (match) {
                                    matchingChild = c;
                                }
                                else {
                                    mismatch = true;
                                }
                            }
                            else {
                                mismatch = true;
                            }
                        }
                        if (matchingChild == null) {
                            if (mismatch) {
                                log.error("Effective NodeType {} defines multiple children named {} but not with a matching type {} or multiplicity for its corresponding child in ContentType {}. "
                                        + "The NodeType children will be hidden and the ContentType child removed."
                                        , ent.getName(), entry.getKey(), cti.getEffectiveType(), getName());
                            }
                            else {
                                log.error("Effective NodeType {} defines a child named {} but not with matching type {} or multiplicity for its corresponding child in ContentType {}. "
                                        + "The NodeType child will be hidden and the ContentType child removed."
                                        , ent.getName(), entry.getKey(), cti.getEffectiveType(), getName());
                            }
                            children.remove(entry.getKey());
                            removedItems.add(entry.getKey());
                        }
                        else {
                            if (mismatch) {
                                log.warn("Effective NodeType {} defines multiple children named {} for its corresponding child of type {} in ContentType {}. "
                                        + "Other NodeType children will be hidden."
                                        , ent.getName(), entry.getKey(), cti.getEffectiveType(), getName());
                            }
                            if (matchingChild.isAutoCreated() && !cti.isAutoCreated()) {
                                log.warn("Effective NodeType {} child named {} is autoCreated while its corresponding child in ContentType {} is not. "
                                        + "ContentType child is corrected to be autoCreated."
                                        , ent.getName(), entry.getKey(), getName());
                                cti.setAutoCreated(true);
                            }
                            if (matchingChild.isMandatory() && !cti.isMandatory()) {
                                log.warn("Effective NodeType {} child node named {} is mandatory while its corresponding child in ContentType {} is not. "
                                        + "ContentType child is corrected to be mandatory."
                                        , ent.getName(), entry.getKey(), getName());
                                cti.setMandatory(true);
                            }
                            if (matchingChild.isProtected() && !cti.isProtected()) {
                                log.warn("Effective NodeType {} child node named {} is protected while its corresponding child in ContentType {} is not. "
                                        + "ContentType child is corrected to be protected."
                                        , ent.getName(), entry.getKey(), getName());
                                cti.setProtected(true);
                            }
                            cti.setEffectiveNodeTypeItem(matchingChild);
                        }
                    }
                }
            }
        }

        for (final Map.Entry<String,List<EffectiveNodeTypeProperty>> entry : ent.getProperties().entrySet()) {
            if (!"*".equals(entry.getKey())) {
                ContentTypePropertyImpl cti = (ContentTypePropertyImpl)properties.get(entry.getKey());
                if (cti == null) {
                    if (isDerivedType() || !removedItems.contains(entry.getKey())) {
                        if (!isDerivedType() && children.containsKey(entry.getKey())) {
                            log.warn("Effective NodeType {} defines a property named {} which conflicts with a equally named child in non-derived ContentType {}. "
                                    + "NodeType property will be hidden."
                                    , ent.getName(), entry.getKey(), getName());
                        }
                        // create new derived property
                        cti = new ContentTypePropertyImpl(entry.getValue().get(0));
                        properties.put(entry.getKey(), cti);
                        if (entry.getValue().size() > 1) {
                            cti.setMultiPropertyTypes(entry.getValue());
                        }
                    }
                }
                else if (cti.isSealed()) {
                    // skip already processed inherited properties
                    continue;
                }
                else {
                    EffectiveNodeTypeProperty matchingProperty = null;
                    boolean mismatch = false;
                    for (final EffectiveNodeTypeProperty p : entry.getValue()) {
                        if (matchingProperty == null && p.isMultiple() == cti.isMultiple() && p.getType().equals(cti.getEffectiveType())) {
                            matchingProperty = p;
                        }
                        else {
                            mismatch = true;
                        }
                    }
                    if (matchingProperty == null) {
                        if (mismatch) {
                            log.error("Effective NodeType {} defines multiple properties named {} but not of required type {} or multiplicity for its corresponding property in ContentType {}. "
                                    + "The NodeType properties will be hidden and the ContentType property removed."
                                    , ent.getName(), entry.getKey(), cti.getItemType(), getName());
                        }
                        else {
                            log.error("Effective NodeType {} defines a property named {} but not of required type {} or multiplicity for its corresponding property in ContentType {}. "
                                    + "The NodeType property will be hidden and the ContentType property removed."
                                    , ent.getName(), entry.getKey(), cti.getItemType(), getName());
                        }
                        properties.remove(entry.getKey());
                    }
                    else {
                        if (mismatch) {
                            log.warn("Effective NodeType {} defines multiple properties named {} for its corresponding property of type {} in ContentType {}. "
                                    + "Other NodeType properties will be hidden."
                                    , ent.getName(), entry.getKey(), cti.getItemType(), getName());
                        }
                        if (matchingProperty.isAutoCreated() && !cti.isAutoCreated()) {
                            log.warn("Effective NodeType {} property named {} is autoCreated while its corresponding property in ContentType {} is not. "
                                    + "ContentType property is corrected to be autoCreated."
                                    , ent.getName(), entry.getKey(), getName());
                            cti.setAutoCreated(true);
                        }
                        if (matchingProperty.isMandatory() && !cti.isMandatory()) {
                            log.warn("Effective NodeType {} property named {} is mandatory while its corresponding property in ContentType {} is not. "
                                    + "ContentType property is corrected to be mandatory."
                                    , ent.getName(), entry.getKey(), getName());
                            cti.setMandatory(true);
                        }
                        if (matchingProperty.isProtected() && !cti.isProtected()) {
                            log.warn("Effective NodeType {} property named {} is protected while its corresponding property in ContentType {} is not. "
                                    + "ContentType property is corrected to be protected."
                                    , ent.getName(), entry.getKey(), getName());
                            cti.setProtected(true);
                        }
                        cti.setEffectiveNodeTypeItem(matchingProperty);
                    }
                }
            }
        }
    }

    private void resolveUnmappedItems(final ContentTypesCache ctCache) {
        for (final Iterator<String> itemNameIterator = children.keySet().iterator(); itemNameIterator.hasNext(); ) {
            final ContentTypeChildImpl cti = (ContentTypeChildImpl)children.get(itemNameIterator.next());
            if (cti.isSealed()) {
                // skip already processed inherited children
                continue;
            }
            if (cti.getEffectiveNodeTypeItem() == null) {
                // first check predefined ContentTypes cache: it might contain an aggregated (with optional mixins) ContentType
                ContentTypeImpl ct = ctCache.getType(cti.getEffectiveType());
                if (ct == null) {
                    // not an aggregated ContentType: get it from the aggregated ContentType cache (which also contains every non-aggregated type)
                    ct = ctCache.getActCache().get(cti.getEffectiveType());
                }
                if (ct == null) {
                    log.error("ContentType {} defines child named {} with unresolved type {}. "
                            + "Child is removed.",
                            getName(), cti.getName(), cti.getEffectiveType());
                    itemNameIterator.remove();
                }
                else {
                    final List<EffectiveNodeTypeChild> children = ent.getChildren().get("*");
                    if (children != null) {
                        for (final EffectiveNodeTypeChild c : children) {
                            if (!cti.isMultiple() || c.isMultiple()) {
                                boolean match = true;
                                for (final String requiredType : c.getRequiredPrimaryTypes()) {
                                    if (!ct.isContentType(requiredType)) {
                                        match = false;
                                        break;
                                    }
                                }
                                if (match) {
                                    if (c.isAutoCreated() && !cti.isAutoCreated()) {
                                        log.warn("Matching residual Effective NodeType {} child named {} is autoCreated while its corresponding child in ContentType {} is not. "
                                                + "ContentType child is corrected to be autoCreated."
                                                , ent.getName(), cti.getName(), getName());
                                        cti.setAutoCreated(true);
                                    }
                                    if (c.isMandatory() && !cti.isMandatory()) {
                                        log.warn("Matching residual Effective NodeType {} child named {} is mandatory while its corresponding child in ContentType {} is not. "
                                                + "ContentType child is corrected to be autoCreated."
                                                , ent.getName(), cti.getName(), getName());
                                        cti.setMandatory(true);
                                    }
                                    if (c.isProtected() && !cti.isProtected()) {
                                        log.warn("Matching residual Effective NodeType {} child named {} is protected while its corresponding child in ContentType {} is not. "
                                                + "ContentType child is corrected to be autoCreated."
                                                , ent.getName(), cti.getName(), getName());
                                        cti.setProtected(true);
                                    }
                                    cti.setEffectiveNodeTypeItem(c);
                                    break;
                                }
                            }
                        }
                    }
                    if (cti.getEffectiveNodeTypeItem() == null) {
                        log.error("ContentType {} defines child named {} without matching named or residual child in its Effective NodeType {}. "
                                + "ContentType child is removed.",
                                getName(), cti.getName(), ent.getName());
                        itemNameIterator.remove();
                    }
                }
            }
        }

        for (final Iterator<String> itemNameIterator = properties.keySet().iterator(); itemNameIterator.hasNext(); ) {
            final ContentTypePropertyImpl cti = (ContentTypePropertyImpl)properties.get(itemNameIterator.next());
            if (cti.isSealed()) {
                // skip already processed inherited properties
                continue;
            }
            if (cti.getEffectiveNodeTypeItem() == null) {
                final List<EffectiveNodeTypeProperty> props = ent.getProperties().get("*");
                if (props != null) {
                    for (final EffectiveNodeTypeProperty p : props) {
                        if (p.getType().equals(cti.getEffectiveType()) && p.isMultiple() == cti.isMultiple()) {
                            cti.setEffectiveNodeTypeItem(p);
                            if (p.isAutoCreated() && !cti.isAutoCreated()) {
                                log.warn("Matching residual Effective NodeType {} property named {} is autoCreated while its corresponding property in ContentType {} is not. "
                                        + "ContentType property is corrected to be autoCreated."
                                        , ent.getName(), cti.getName(), getName());
                                cti.setAutoCreated(true);
                            }
                            if (p.isMandatory() && !cti.isMandatory()) {
                                log.warn("Matching residual Effective NodeType {} property named {} is mandatory while its corresponding property in ContentType {} is not. "
                                        + "ContentType property is corrected to be mandatory."
                                        , ent.getName(), cti.getName(), getName());
                                cti.setMandatory(true);
                            }
                            if (p.isProtected() && !cti.isProtected()) {
                                log.warn("Matching residual Effective NodeType {} property named {} is protected while its corresponding property in ContentType {} is not. "
                                        + "ContentType property is corrected to be protected."
                                        , ent.getName(), cti.getName(), getName());
                                cti.setProtected(true);
                            }
                            break;
                        }
                    }
                }
                if (cti.getEffectiveNodeTypeItem() == null) {
                    log.error("ContentType {} defines property named {} without matching named or residual property in its Effective NodeType {}. "
                            + "ContentType property is removed."
                            , getName(), cti.getName(), ent.getName());
                    itemNameIterator.remove();
                }
            }
        }
    }
}
