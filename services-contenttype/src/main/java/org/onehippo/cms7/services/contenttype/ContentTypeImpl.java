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
    private SortedSet<String> superTypes = new TreeSet<String>();
    private SortedSet<String> aggregatedTypes = new TreeSet<String>();
    private boolean documentType;
    private boolean compoundType;
    private boolean mixin;
    private boolean templateType;
    private boolean cascadeValidate;
    private Map<String, ContentTypeField> fields = new LinkedHashMap<String, ContentTypeField>();

    public ContentTypeImpl(String prefix, String name, long contentTypesVersion) {
        this.version = contentTypesVersion;
        this.aggregate = false;
        this.derivedType = false;
        this.prefix = prefix;
        this.name = prefix + ":" + name;
        aggregatedTypes.add(this.name);
    }

    public ContentTypeImpl(EffectiveNodeTypeImpl ent, long contentTypesVersion) {
        this.version = contentTypesVersion;
        aggregate = false;
        this.derivedType = true;
        this.name = ent.getName();
        this.prefix = ent.getPrefix();
        this.ent = ent;
        documentType = false;
        compoundType = false;
        mixin = ent.isMixin();
        templateType = false;
        superTypes.addAll(ent.getSuperTypes());
        aggregatedTypes.addAll(ent.getAggregatedTypes());
        cascadeValidate = false;
    }

    public ContentTypeImpl(ContentTypeImpl other) {
        this.version = other.version;
        aggregate = other.aggregate;
        this.derivedType = other.derivedType;
        prefix = other.prefix;
        this.ent = new EffectiveNodeTypeImpl(other.ent); // clone
        name = other.name;
        documentType = other.documentType;
        compoundType = other.compoundType;
        mixin = other.mixin;
        templateType = other.templateType;
        superTypes.addAll(other.superTypes);
        aggregatedTypes.addAll(other.aggregatedTypes);
        cascadeValidate = other.cascadeValidate;
        for (String name : other.fields.keySet()) {
            fields.put(name, new ContentTypeFieldImpl((ContentTypeFieldImpl)other.fields.get(name)));
        }
    }

    @Override
    protected void doSeal() {
        ent.seal();
        superTypes = Collections.unmodifiableSortedSet(superTypes);
        aggregatedTypes = Collections.unmodifiableSortedSet(aggregatedTypes);
        for (ContentTypeField df : fields.values() ) {
            ((Sealable)df).seal();
        }
        fields = Collections.unmodifiableMap(fields);
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

    public void setEffectiveNodeType(EffectiveNodeTypeImpl ent) {
        checkSealed();
        this.ent = ent;
        superTypes.addAll(ent.getSuperTypes());
        aggregatedTypes.addAll(ent.getAggregatedTypes());
    }

    @Override
    public String getName() {
        if (name == null) {
            Iterator<String> iterator = aggregatedTypes.iterator();
            StringBuilder sb = new StringBuilder(iterator.next());
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

    public void setDocumentType(boolean documentType) {
        checkSealed();
        this.documentType = documentType;
    }

    @Override
    public boolean isCompoundType() {
        return compoundType;
    }

    public void setCompoundType(boolean compoundType) {
        checkSealed();
        this.compoundType = compoundType;
    }

    @Override
    public boolean isMixin() {
        return mixin;
    }

    public void setMixin(boolean mixin) {
        checkSealed();
        this.mixin = mixin;
    }

    @Override
    public boolean isTemplateType() {
        return templateType;
    }

    public void setTemplateType(boolean templateType) {
        checkSealed();
        this.templateType = templateType;
    }

    @Override
    public boolean isCascadeValidate() {
        return cascadeValidate;
    }

    public void setCascadeValidate(boolean cascadeValidate) {
        this.cascadeValidate = cascadeValidate;
    }

    @Override
    public Map<String, ContentTypeField> getFields() {
        return fields;
    }

    public int hashCode() {
        return isSealed() ? getName().hashCode() : super.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ContentTypeImpl && this.isSealed() && ((Sealable)obj).isSealed()) {
            return this.getName().equals(((ContentTypeImpl)obj).getName());
        }
        return false;
    }

    public boolean contains(ContentTypeImpl other) {
        for (String s : other.superTypes) {
            if (!isContentType(s)) {
                return false;
            }
        }
        for (String s : other.aggregatedTypes) {
            if (!isContentType(s)) {
                return false;
            }
        }
        return true;
    }

    public boolean merge(ContentTypeImpl other, boolean superType) {
        if (!ent.merge(other.getEffectiveNodeType(), superType) && contains(other)) {
            return false;
        }

        aggregate = true;
        name = null;
        prefix = null;

        ContentTypeFieldImpl ctf;
        for (Map.Entry<String, ContentTypeField> entry : other.getFields().entrySet()) {
            if (!isContentType(entry.getValue().getDefiningType())) {
                ctf = (ContentTypeFieldImpl)fields.get(entry.getKey());
                if (ctf != null) {
                    // duplicate field name
                    if (ctf.isMultiple() != entry.getValue().isMultiple() ||
                            ctf.isPropertyField() != entry.getValue().isPropertyField() ||
                            ctf.getFieldType().equals(entry.getValue().getFieldType())) {
                        log.error("Conflicting ContentType field named {} encountered while merging ContentType {} with {}. Incoming field ignored."
                                , new String[]{ctf.getName(), getName(), entry.getValue().getName()});
                    }
                    else {
                        log.warn("Duplicate ContentType field named {} encountered while merging ContentType {} with {}. Incoming field ignored."
                               , new String[]{ctf.getName(), getName(), entry.getValue().getName()});
                    }
                }
                else {
                    fields.put(entry.getKey(), new ContentTypeFieldImpl((ContentTypeFieldImpl)entry.getValue()));
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

        if (!other.isCompoundType()) {
            this.compoundType = false;
        }
        if (!other.isDerivedType()) {
            this.derivedType = false;
        }

        if (other.isCascadeValidate()) {
            this.cascadeValidate = true;
        }

        return true;
    }

    public void resolveFields(ContentTypesCache ctCache) {
        checkSealed();
        Set<String> ignoredFields = new HashSet<String>();
        mergeInheritedFields(ctCache);
        resolvePropertiesToFields(ctCache, ignoredFields);
        resolveChildrenToFields(ctCache, ignoredFields);
        resolveFieldsToResidualItems(ctCache);
    }

    private void mergeInheritedFields(ContentTypesCache ctCache) {
        for (String s : superTypes) {
            ContentTypeImpl ct = ctCache.getActCache().get(s);
            for (Map.Entry<String, ContentTypeField> entry : ct.fields.entrySet()) {
                if (!fields.containsKey(entry.getKey())) {
                    fields.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void resolvePropertiesToFields(ContentTypesCache ctCache, Set<String> ignoredFields) {
        for (Map.Entry<String,List<EffectiveNodeTypeProperty>> entry : ent.getProperties().entrySet()) {
            if (!"*".equals(entry.getKey())) {
                ContentTypeFieldImpl cft = (ContentTypeFieldImpl)fields.get(entry.getKey());
                if (cft == null) {
                    // create new derived field
                    cft = new ContentTypeFieldImpl(entry.getValue().get(0));
                    fields.put(entry.getKey(), cft);
                    if (ent.getProperties().get(entry.getKey()).size() > 1) {
                        log.warn("Effective NodeType {} defines multiple properties named {} without corresponding field in ContentType {}. "
                                + "A derived field is created for only the first property definition with type {}."
                                , new String[]{ent.getName(), entry.getKey(), getName(), cft.getFieldType()});
                    }
                }
                else if (cft.isSealed()) {
                    // skip already processed inherited fields
                    continue;
                }
                else if (!cft.isPropertyField()) {
                    if (ent.getChildren().containsKey(entry.getKey())) {
                        log.warn("Effective NodeType {} defines both a property and a child node named {} with a (possibly) matching Child Node field in ContentType {}. "
                                + "Corresponding property will be hidden."
                                , new String[]{ent.getName(), getName(), entry.getKey()});
                    }
                    else {
                        log.error("Effective NodeType {} defines a property named {} with a conflicting named Child Node field in ContentType {}. "
                                + "The Corresponding property will be hidden and the field removed."
                                , new String[]{ent.getName(), entry.getKey(), getName()});
                        fields.remove(entry.getKey());
                        ignoredFields.add(entry.getKey());
                    }
                }
                else {
                    EffectiveNodeTypeProperty matchingProperty = null;
                    boolean mismatch = false;
                    for (EffectiveNodeTypeProperty p : entry.getValue()) {
                        if (matchingProperty == null && p.isMultiple() == cft.isMultiple() && p.getType().equals(cft.getItemType())) {
                            matchingProperty = p;
                        }
                        else {
                            mismatch = true;
                        }
                    }
                    if (matchingProperty == null) {
                        if (mismatch) {
                            log.error("Effective NodeType {} defines multiple properties named {} but not of required type {} or multiplicity for its corresponding field in ContentType {}. "
                                    + "The properties will be hidden and the field removed."
                                    , new String[]{ent.getName(), entry.getKey(), cft.getFieldType(), getName()});
                        }
                        else {
                            log.error("Effective NodeType {} defines a property named {} but not of required type {} or multiplicity for its corresponding field in ContentType {}. "
                                    + "The property will be hidden and the field removed."
                                    , new String[]{ent.getName(), entry.getKey(), cft.getFieldType(), getName()});
                        }
                        fields.remove(entry.getKey());
                    }
                    else {
                        if (mismatch) {
                            log.warn("Effective NodeType {} defines multiple properties named {} for its corresponding field of type {} in ContentType {}. "
                                    + "Other properties will be hidden."
                                    , new String[]{ent.getName(), entry.getKey(), cft.getFieldType(), getName()});
                        }
                        if (matchingProperty.isAutoCreated() && !cft.isAutoCreated()) {
                            log.warn("Effective NodeType {} property named {} is autoCreated while its corresponding field in ContentType {} is not. "
                                    + "Field is corrected to be autoCreated."
                                    , new String[]{ent.getName(), entry.getKey(), getName()});
                            cft.setAutoCreated(true);
                        }
                        if (matchingProperty.isMandatory() && !cft.isMandatory()) {
                            log.warn("Effective NodeType {} property named {} is mandatory while its corresponding field in ContentType {} is not. "
                                    + "Field is corrected to be mandatory."
                                    , new String[]{ent.getName(), entry.getKey(), getName()});
                            cft.setMandatory(true);
                        }
                        if (matchingProperty.isProtected() && !cft.isProtected()) {
                            log.warn("Effective NodeType {} property named {} is protected while its corresponding field in ContentType {} is not. "
                                    + "Field is corrected to be protected."
                                    , new String[]{ent.getName(), entry.getKey(), getName()});
                            cft.setProtected(true);
                        }
                        cft.setEffectiveNodeTypeItem(matchingProperty);
                    }
                }
            }
        }
    }

    private void resolveChildrenToFields(ContentTypesCache ctCache, Set<String> ignoredFields) {
        for (Map.Entry<String,List<EffectiveNodeTypeChild>> entry : ent.getChildren().entrySet()) {
            if (!"*".equals(entry.getKey())) {
                ContentTypeFieldImpl cft = (ContentTypeFieldImpl)fields.get(entry.getKey());
                if (cft == null) {
                    if (!ignoredFields.contains(entry.getKey())) {
                        // create derived field
                        cft = new ContentTypeFieldImpl(entry.getValue().get(0));
                        fields.put(entry.getKey(), cft);
                        if (ent.getChildren().get(entry.getKey()).size() > 1) {
                            log.warn("Effective NodeType {} defines multiple child nodes named {} without corresponding field in ContentType {}. "
                                    + "A derived field is created for only the child node definition with type {}."
                                    , new String[]{ent.getName(), entry.getKey(), getName(), cft.getFieldType()});
                        }
                    }
                }
                else if (cft.isSealed()) {
                    // skip already processed inherited fields
                    continue;
                }
                else if (cft.isPropertyField()) {
                    if (cft.isDerivedField()) {
                        log.error("Effective NodeType {} defines both a property and a child node named {} without a corresponding field in ContentType {}. "
                                + "The Corresponding child node will be hidden."
                                , new String[]{ent.getName(), entry.getKey(), getName()});
                    }
                    else {
                        log.error("Effective NodeType {} defines a child node named {} with a conflicting named property field in ContentType {}. "
                                + "The Corresponding property will be hidden and the field removed."
                                , new String[]{ent.getName(), entry.getKey(), getName()});
                        fields.remove(entry.getKey());
                    }
                }
                else {
                    // first check predefined ContentTypes cache: it might contain an aggregated (with optional mixins) ContentType
                    ContentTypeImpl ct = ctCache.getType(cft.getItemType());
                    if (ct == null) {
                        // not an aggregated ContentType: get it from the aggregated ContentType cache (which also contains every non-aggregated type)
                        ct = ctCache.getActCache().get(cft.getItemType());
                    }
                    if (ct == null) {
                        log.error("Effective NodeType {} defines a child node named {} with corresponding field in ContentType {} which has unresolved type {}. "
                                + "The corresponding child node will be hidden and the field removed.",
                                new String[]{ent.getName(), entry.getKey(), getName(), cft.getItemType()});
                        fields.remove(entry.getKey());
                    }
                    else {
                        EffectiveNodeTypeChild matchingChild = null;
                        boolean mismatch = false;
                        for (EffectiveNodeTypeChild c : entry.getValue()) {
                            if (matchingChild == null && (!cft.isMultiple() || c.isMultiple())) {
                                boolean match = true;
                                for (String requiredType : c.getRequiredPrimaryTypes()) {
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
                                log.error("Effective NodeType {} defines multiple child nodes named {} but not with matching type {} or multiplicity for its corresponding field in ContentType {}. "
                                        + "The child nodes will be hidden and the field removed."
                                        , new String[]{ent.getName(), entry.getKey(), cft.getItemType(), getName()});
                            }
                            else {
                                log.error("Effective NodeType {} defines a child node named {} but not with matching type {} or multiplicity for its corresponding field in ContentType {}. "
                                        + "The child node will be hidden and the field removed."
                                        , new String[]{ent.getName(), entry.getKey(), cft.getItemType(), getName()});
                            }
                            fields.remove(entry.getKey());
                        }
                        else {
                            if (mismatch) {
                                log.warn("Effective NodeType {} defines multiple child nodes named {} for its corresponding field of type {} in ContentType {}. "
                                        + "Other child nodes will be hidden."
                                        , new String[]{ent.getName(), entry.getKey(), cft.getItemType(), getName()});
                            }
                            if (matchingChild.isAutoCreated() && !cft.isAutoCreated()) {
                                log.warn("Effective NodeType {} child node named {} is autoCreated while its corresponding field in ContentType {} is not. "
                                        + "Field is corrected to be autoCreated."
                                        , new String[]{ent.getName(), entry.getKey(), getName()});
                                cft.setAutoCreated(true);
                            }
                            if (matchingChild.isMandatory() && !cft.isMandatory()) {
                                log.warn("Effective NodeType {} child node named {} is mandatory while its corresponding field in ContentType {} is not. "
                                        + "Field is corrected to be mandatory."
                                        , new String[]{ent.getName(), entry.getKey(), getName()});
                                cft.setMandatory(true);
                            }
                            if (matchingChild.isProtected() && !cft.isProtected()) {
                                log.warn("Effective NodeType {} child node named {} is protected while its corresponding field in ContentType {} is not. "
                                        + "Field is corrected to be protected."
                                        , new String[]{ent.getName(), entry.getKey(), getName()});
                                cft.setProtected(true);
                            }
                            cft.setEffectiveNodeTypeItem(matchingChild);
                        }
                    }
                }
            }
        }
    }

    private void resolveFieldsToResidualItems(ContentTypesCache ctCache) {
        for (Iterator<String> fieldNameIterator = fields.keySet().iterator(); fieldNameIterator.hasNext(); ) {
            ContentTypeFieldImpl cft = (ContentTypeFieldImpl)fields.get(fieldNameIterator.next());
            if (cft.isSealed()) {
                // skip already processed inherited fields
                continue;
            }
            if (cft.getEffectiveNodeTypeItem() == null) {
                if (cft.isPropertyField()) {
                    List<EffectiveNodeTypeProperty> properties = ent.getProperties().get("*");
                    if (properties != null) {
                        for (EffectiveNodeTypeProperty p : properties) {
                            if (p.getType().equals(cft.getFieldType()) && p.isMultiple() == cft.isMultiple()) {
                                cft.setEffectiveNodeTypeItem(p);
                                if (p.isAutoCreated() && !cft.isAutoCreated()) {
                                    log.warn("Matching residual Effective NodeType {} property is autoCreated while its corresponding field named {} in ContentType {} is not. "
                                            + "Field is corrected to be autoCreated."
                                            , new String[]{ent.getName(), cft.getName(), getName()});
                                    cft.setAutoCreated(true);
                                }
                                if (p.isMandatory() && !cft.isMandatory()) {
                                    log.warn("Matching residual Effective NodeType {} property is mandatory while its corresponding field named {} in ContentType {} is not. "
                                            + "Field is corrected to be mandatory."
                                            , new String[]{ent.getName(), cft.getName(), getName()});
                                    cft.setMandatory(true);
                                }
                                if (p.isProtected() && !cft.isProtected()) {
                                    log.warn("Matching residual Effective NodeType {} property is protected while its corresponding field named {} in ContentType {} is not. "
                                            + "Field is corrected to be protected."
                                            , new String[]{ent.getName(), cft.getName(), getName()});
                                    cft.setProtected(true);
                                }
                                break;
                            }
                        }
                    }
                    if (cft.getEffectiveNodeTypeItem() == null) {
                        log.error("ContentType {} defines property field named {} without matching named or residual property in its Effective NodeType {}. "
                                + "Field is removed."
                                , new String[]{getName(), cft.getName(), ent.getName()});
                        fieldNameIterator.remove();
                    }
                }
                else {
                    // first check predefined ContentTypes cache: it might contain an aggregated (with optional mixins) ContentType
                    ContentTypeImpl ct = ctCache.getType(cft.getItemType());
                    if (ct == null) {
                        // not an aggregated ContentType: get it from the aggregated ContentType cache (which also contains every non-aggregated type)
                        ct = ctCache.getActCache().get(cft.getItemType());
                    }
                    if (ct == null) {
                        log.error("ContentType {} defines node child field named {} with unresolved type {}. "
                                + "Field is removed.",
                                new String[]{getName(), cft.getName(), cft.getItemType()});
                        fieldNameIterator.remove();
                    }
                    else {
                        List<EffectiveNodeTypeChild> children = ent.getChildren().get("*");
                        if (children != null) {
                            for (EffectiveNodeTypeChild c : children) {
                                if (!cft.isMultiple() || c.isMultiple()) {
                                    boolean match = true;
                                    for (String requiredType : c.getRequiredPrimaryTypes()) {
                                        if (!ct.isContentType(requiredType)) {
                                            match = false;
                                            break;
                                        }
                                    }
                                    if (match) {
                                        if (c.isAutoCreated() && !cft.isAutoCreated()) {
                                            log.warn("Matching residual Effective NodeType {} child node is autoCreated while its corresponding field named {} in ContentType {} is not. "
                                                    + "Field is corrected to be autoCreated."
                                                    , new String[]{ent.getName(), cft.getName(), getName()});
                                            cft.setAutoCreated(true);
                                        }
                                        if (c.isMandatory() && !cft.isMandatory()) {
                                            log.warn("Matching residual Effective NodeType {} child node is mandatory while its corresponding field named {} in ContentType {} is not. "
                                                    + "Field is corrected to be autoCreated."
                                                    , new String[]{ent.getName(), cft.getName(), getName()});
                                            cft.setMandatory(true);
                                        }
                                        if (c.isProtected() && !cft.isProtected()) {
                                            log.warn("Matching residual Effective NodeType {} child node is protected while its corresponding field named {} in ContentType {} is not. "
                                                    + "Field is corrected to be autoCreated."
                                                    , new String[]{ent.getName(), cft.getName(), getName()});
                                            cft.setProtected(true);
                                        }
                                        cft.setEffectiveNodeTypeItem(c);
                                        break;
                                    }
                                }
                            }
                        }
                        if (cft.getEffectiveNodeTypeItem() == null) {
                            log.error("ContentType {} defines node child field named {} without matching named or residual child node in its Effective NodeType {}. "
                                    + "Field is removed.",
                                    new String[]{getName(), cft.getName(), ent.getName()});
                            fieldNameIterator.remove();
                        }
                    }
                }
            }
        }
    }
}
