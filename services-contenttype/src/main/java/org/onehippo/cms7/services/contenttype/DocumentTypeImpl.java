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
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentTypeImpl extends Sealable implements DocumentType {

    static final Logger log = LoggerFactory.getLogger(DocumentTypeImpl.class);

    private final long version;
    private boolean aggregate;
    private boolean derivedType;
    private EffectiveNodeTypeImpl ent;
    private String name;
    private String prefix;
    private Set<String> superTypes = new TreeSet<String>();
    private Set<String> aggregatedTypes = new TreeSet<String>();
    private boolean compound;
    private boolean mixin;
    private boolean template;
    private boolean cascadeValidate;
    private Map<String, DocumentTypeField> fields = new LinkedHashMap<String, DocumentTypeField>();

    public DocumentTypeImpl(String prefix, String name, long documentTypesVersion) {
        this.version = documentTypesVersion;
        this.aggregate = false;
        this.derivedType = false;
        this.prefix = prefix;
        this.name = prefix + ":" + name;
        aggregatedTypes.add(this.name);
    }

    public DocumentTypeImpl(EffectiveNodeTypeImpl ent, long documentTypesVersion) {
        this.version = documentTypesVersion;
        aggregate = false;
        this.derivedType = true;
        this.name = ent.getName();
        this.prefix = ent.getPrefix();
        this.ent = ent;
        compound = false;
        mixin = ent.isMixin();
        template = false;
        superTypes.addAll(ent.getSuperTypes());
        aggregatedTypes.addAll(ent.getAggregatedTypes());
        cascadeValidate = false;
    }

    public DocumentTypeImpl(DocumentTypeImpl other) {
        this.version = other.version;
        aggregate = other.aggregate;
        this.derivedType = other.derivedType;
        prefix = other.prefix;
        this.ent = new EffectiveNodeTypeImpl(other.ent); // clone
        name = other.name;
        compound = other.compound;
        mixin = other.mixin;
        template = false;
        superTypes.addAll(other.superTypes);
        aggregatedTypes.addAll(other.aggregatedTypes);
        cascadeValidate = other.cascadeValidate;
        for (String name : other.fields.keySet()) {
            fields.put(name, new DocumentTypeFieldImpl((DocumentTypeFieldImpl)other.fields.get(name)));
        }
    }

    public boolean contains(DocumentTypeImpl other) {
        for (String s : other.superTypes) {
            if (!isDocumentType(s)) {
                return false;
            }
        }
        for (String s : other.aggregatedTypes) {
            if (!isDocumentType(s)) {
                return false;
            }
        }
        return true;
    }

    public boolean merge(DocumentTypeImpl other, boolean superType) {
        if (!ent.merge(other.getEffectiveNodeType(), superType) && !contains(other)) {
            return false;
        }

        aggregate = true;
        name = null;
        prefix = null;

        DocumentTypeFieldImpl dtf;
        for (Map.Entry<String, DocumentTypeField> entry : other.getFields().entrySet()) {
            if (!isDocumentType(entry.getValue().getDefiningType())) {
                dtf = (DocumentTypeFieldImpl)fields.get(entry.getKey());
                if (dtf != null) {
                    // duplicate field name
                    if (dtf.isMultiple() != entry.getValue().isMultiple() ||
                            dtf.isPropertyField() != entry.getValue().isPropertyField() ||
                            dtf.getFieldType() != dtf.getFieldType()) {
                        log.error("Conflicting DocumentType field named {} encountered while merging DocumentType {} with {}. Incoming field ignored."
                                , new String[]{dtf.getName(), getName(), entry.getValue().getName()});
                    }
                    else {
                        log.warn("Duplicate DocumentType field named {} encountered while merging DocumentType {} with {}. Incoming field ignored."
                               , new String[]{dtf.getName(), getName(), entry.getValue().getName()});
                    }
                }
                else {
                    fields.put(entry.getKey(), new DocumentTypeFieldImpl((DocumentTypeFieldImpl)entry.getValue()));
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

        if (!other.isCompound()) {
            this.compound = false;
        }
        if (!other.isDerivedType()) {
            this.derivedType = false;
        }

        if (other.isCascadeValidate()) {
            this.cascadeValidate = true;
        }

        return true;
    }

    @Override
    protected void doSeal() {
        ent.seal();
        superTypes = Collections.unmodifiableSet(superTypes);
        aggregatedTypes = Collections.unmodifiableSet(aggregatedTypes);
        for (DocumentTypeField df : fields.values() ) {
            ((Sealable)df).seal();
        }
        fields = Collections.unmodifiableMap(fields);
    }

    public void resolveFields(AggregatedDocumentTypesCache adtCache) {
        checkSealed();
        Set<String> ignoredFields = new HashSet<String>();

        // map all non-residual EffectiveNodeType properties and children to a DocumentTypeField
        for (Map.Entry<String,List<EffectiveNodeTypeProperty>> entry : ent.getProperties().entrySet()) {
            if (!"*".equals(entry.getKey())) {
                DocumentTypeFieldImpl dft = (DocumentTypeFieldImpl)fields.get(entry.getKey());
                if (dft == null) {
                    // create new derived field
                    dft = new DocumentTypeFieldImpl(getName(), ent.getProperties().get(entry.getKey()).get(0));
                    fields.put(entry.getKey(), dft);
                    if (ent.getProperties().get(entry.getKey()).size() > 1) {
                        log.warn("Effective NodeType {} defines multiple properties named {} without corresponding field in Document Type {}. "
                                + "A derived field is created for only the first property definition with type {}."
                                , new String[]{ent.getName(), entry.getKey(), getName(), dft.getFieldType()});
                    }
                }
                else if (!dft.isPropertyField()) {
                    if (ent.getChildren().containsKey(entry.getKey())) {
                        log.warn("Effective NodeType {} defines both a property and a child node named {} with a (possibly) matching Child Node field in Document Type {}. "
                                + "Corresponding property will be hidden."
                                , new String[]{ent.getName(), getName(), entry.getKey()});
                    }
                    else {
                        log.error("Effective NodeType {} defines a property named {} with a conflicting named Child Node field in Document Type {}. "
                                + "The Corresponding property will be hidden and the field removed."
                                , new String[]{ent.getName(), entry.getKey(), getName()});
                        fields.remove(entry.getKey());
                        ignoredFields.add(entry.getKey());
                    }
                }
                else {
                    EffectiveNodeTypeProperty matchingProperty = null;
                    boolean unmatched = false;
                    for (EffectiveNodeTypeProperty p : entry.getValue()) {
                        if (matchingProperty == null && p.isMultiple() == dft.isMultiple() && p.getType().equals(dft.getItemType())) {
                            matchingProperty = p;
                        }
                        else {
                            unmatched = true;
                        }
                    }
                    if (matchingProperty == null) {
                        if (unmatched) {
                            log.error("Effective NodeType {} defines multiple properties named {} but not of required type {} or multiplicity for its corresponding field in Document Type {}. "
                                    + "The properties will be hidden and the field removed."
                                    , new String[]{ent.getName(), entry.getKey(), dft.getFieldType(), getName()});
                        }
                        else {
                            log.error("Effective NodeType {} defines a property named {} but not of required type {} or multiplicity for its corresponding field in Document Type {}. "
                                    + "The property will be hidden and the field removed."
                                    , new String[]{ent.getName(), entry.getKey(), dft.getFieldType(), getName()});
                        }
                        fields.remove(entry.getKey());
                    }
                    else {
                        if (unmatched) {
                            log.warn("Effective NodeType {} defines multiple properties named {} for its corresponding field of type {} in Document Type {}. "
                                    + "Other properties will be hidden."
                                    , new String[]{ent.getName(), entry.getKey(), dft.getFieldType(), getName()});
                        }
                        if (matchingProperty.isAutoCreated() && !dft.isAutoCreated()) {
                            log.warn("Effective NodeType {} property named {} is autoCreated while its corresponding field in Document Type {} is not. "
                                    + "Field is corrected to be autoCreated."
                                    , new String[]{ent.getName(), entry.getKey(), getName()});
                            dft.setAutoCreated(true);
                        }
                        if (matchingProperty.isMandatory() && !dft.isMandatory()) {
                            log.warn("Effective NodeType {} property named {} is mandatory while its corresponding field in Document Type {} is not. "
                                    + "Field is corrected to be mandatory."
                                    , new String[]{ent.getName(), entry.getKey(), getName()});
                            dft.setMandatory(true);
                        }
                        if (matchingProperty.isProtected() && !dft.isProtected()) {
                            log.warn("Effective NodeType {} property named {} is protected while its corresponding field in Document Type {} is not. "
                                    + "Field is corrected to be protected."
                                    , new String[]{ent.getName(), entry.getKey(), getName()});
                            dft.setProtected(true);
                        }
                        dft.setEffectiveNodeTypeItem(matchingProperty);
                    }
                }
            }
        }
        for (Map.Entry<String,List<EffectiveNodeTypeChild>> entry : ent.getChildren().entrySet()) {
            if (!"*".equals(entry.getKey())) {
                DocumentTypeFieldImpl dft = (DocumentTypeFieldImpl)fields.get(entry.getKey());
                if (dft == null) {
                    if (!ignoredFields.contains(entry.getKey())) {
                        // create derived field
                        dft = new DocumentTypeFieldImpl(getName(), entry.getValue().get(0));
                        fields.put(entry.getKey(), dft);
                        if (ent.getChildren().get(entry.getKey()).size() > 1) {
                            log.warn("Effective NodeType {} defines multiple child nodes named {} without corresponding field in Document Type {}. "
                                    + "A derived field is created for only the child node definition with type {}."
                                    , new String[]{ent.getName(), entry.getKey(), getName(), dft.getFieldType()});
                        }
                    }
                }
                else if (dft.isPropertyField()) {
                    if (dft.isDerivedField()) {
                        log.error("Effective NodeType {} defines both a property and a child node named {} without a corresponding field in Document Type {}. "
                                + "The Corresponding child node will be hidden."
                                , new String[]{ent.getName(), entry.getKey(), getName()});
                    }
                    else {
                        log.error("Effective NodeType {} defines a child node named {} with a conflicting named property field in Document Type {}. "
                                + "The Corresponding property will be hidden and the field removed."
                                , new String[]{ent.getName(), entry.getKey(), getName()});
                        fields.remove(entry.getKey());
                    }
                }
                else {
                    DocumentTypeImpl ct = adtCache.getDocumentTypesCache().getType(dft.getItemType());
                    if (ct == null) {
                        ct = adtCache.get(dft.getItemType());
                    }
                    if (ct == null) {
                        log.error("Effective NodeType {} defines a child node named {} with corresponding field in Document Type {} which has unresolved type {}. "
                                + "The corresponding child node will be hidden and the field removed.",
                                new String[]{ent.getName(), entry.getKey(), getName(), dft.getItemType()});
                        fields.remove(entry.getKey());
                    }
                    else {
                        EffectiveNodeTypeChild matchingChild = null;
                        boolean unmatched = false;
                        for (EffectiveNodeTypeChild c : entry.getValue()) {
                            if (matchingChild == null && (!dft.isMultiple() || c.isMultiple())) {
                                boolean match = true;
                                for (String requiredType : c.getRequiredPrimaryTypes()) {
                                    if (!ct.isDocumentType(requiredType)) {
                                        match = false;
                                        break;
                                    }
                                }
                                if (match) {
                                    matchingChild = c;
                                }
                                else {
                                    unmatched = true;
                                }
                            }
                            else {
                                unmatched = true;
                            }
                        }
                        if (matchingChild == null) {
                            if (unmatched) {
                                log.error("Effective NodeType {} defines multiple child nodes named {} but not with matching type {} or multiplicity for its corresponding field in Document Type {}. "
                                        + "The child nodes will be hidden and the field removed."
                                        , new String[]{ent.getName(), entry.getKey(), dft.getItemType(), getName()});
                            }
                            else {
                                log.error("Effective NodeType {} defines a child node named {} but not with matching type {} or multiplicity for its corresponding field in Document Type {}. "
                                        + "The child node will be hidden and the field removed."
                                        , new String[]{ent.getName(), entry.getKey(), dft.getItemType(), getName()});
                            }
                            fields.remove(entry.getKey());
                        }
                        else {
                            if (unmatched) {
                                log.warn("Effective NodeType {} defines multiple child nodes named {} for its corresponding field of type {} in Document Type {}. "
                                        + "Other child nodes will be hidden."
                                        , new String[]{ent.getName(), entry.getKey(), dft.getItemType(), getName()});
                            }
                            if (matchingChild.isAutoCreated() && !dft.isAutoCreated()) {
                                log.warn("Effective NodeType {} child node named {} is autoCreated while its corresponding field in Document Type {} is not. "
                                        + "Field is corrected to be autoCreated."
                                        , new String[]{ent.getName(), entry.getKey(), getName()});
                                dft.setAutoCreated(true);
                            }
                            if (matchingChild.isMandatory() && !dft.isMandatory()) {
                                log.warn("Effective NodeType {} child node named {} is mandatory while its corresponding field in Document Type {} is not. "
                                        + "Field is corrected to be mandatory."
                                        , new String[]{ent.getName(), entry.getKey(), getName()});
                                dft.setMandatory(true);
                            }
                            if (matchingChild.isProtected() && !dft.isProtected()) {
                                log.warn("Effective NodeType {} child node named {} is protected while its corresponding field in Document Type {} is not. "
                                        + "Field is corrected to be protected."
                                        , new String[]{ent.getName(), entry.getKey(), getName()});
                                dft.setProtected(true);
                            }
                            dft.setEffectiveNodeTypeItem(matchingChild);
                        }
                    }
                }
            }
        }

        // map remaining unmatched fields to underlying EffectiveNodeType
        for (Iterator<String> fieldNameIterator = fields.keySet().iterator(); fieldNameIterator.hasNext(); ) {
            DocumentTypeFieldImpl dft = (DocumentTypeFieldImpl)fields.get(fieldNameIterator.next());
            if (dft.getEffectiveNodeTypeItem() == null) {
                if (dft.isPropertyField()) {
                    List<EffectiveNodeTypeProperty> properties = ent.getProperties().get("*");
                    if (properties != null) {
                        for (EffectiveNodeTypeProperty p : properties) {
                            if (p.getType().equals(dft.getFieldType()) && p.isMultiple() == dft.isMultiple()) {
                                dft.setEffectiveNodeTypeItem(p);
                                if (p.isAutoCreated() && !dft.isAutoCreated()) {
                                    log.warn("Matching residual Effective NodeType {} property is autoCreated while its corresponding field named {} in Document Type {} is not. "
                                            + "Field is corrected to be autoCreated."
                                            , new String[]{ent.getName(), dft.getName(), getName()});
                                    dft.setAutoCreated(true);
                                }
                                if (p.isMandatory() && !dft.isMandatory()) {
                                    log.warn("Matching residual Effective NodeType {} property is mandatory while its corresponding field named {} in Document Type {} is not. "
                                            + "Field is corrected to be mandatory."
                                            , new String[]{ent.getName(), dft.getName(), getName()});
                                    dft.setMandatory(true);
                                }
                                if (p.isProtected() && !dft.isProtected()) {
                                    log.warn("Matching residual Effective NodeType {} property is protected while its corresponding field named {} in Document Type {} is not. "
                                            + "Field is corrected to be protected."
                                            , new String[]{ent.getName(), dft.getName(), getName()});
                                    dft.setProtected(true);
                                }
                                break;
                            }
                        }
                    }
                    if (dft.getEffectiveNodeTypeItem() == null) {
                        log.error("Document Type {} defines property field named {} without matching named or residual property in its Effective NodeType {}. "
                                + "Field is removed."
                                , new String[]{getName(), dft.getName(), ent.getName()});
                        fieldNameIterator.remove();
                    }
                }
                else {
                    DocumentTypeImpl ct = adtCache.getDocumentTypesCache().getType(dft.getItemType());
                    if (ct == null) {
                        ct = adtCache.get(dft.getItemType());
                    }
                    if (ct == null) {
                        log.error("Document Type {} defines node child field named {} with unresolved type {}. "
                                + "Field is removed.",
                                new String[]{getName(), dft.getName(), dft.getItemType()});
                        fieldNameIterator.remove();
                    }
                    else {
                        List<EffectiveNodeTypeChild> children = ent.getChildren().get("*");
                        if (children != null) {
                            for (EffectiveNodeTypeChild c : children) {
                                if (!dft.isMultiple() || c.isMultiple()) {
                                    boolean match = true;
                                    for (String requiredType : c.getRequiredPrimaryTypes()) {
                                        if (!ct.isDocumentType(requiredType)) {
                                            match = false;
                                            break;
                                        }
                                    }
                                    if (match) {
                                        if (c.isAutoCreated() && !dft.isAutoCreated()) {
                                            log.warn("Matching residual Effective NodeType {} child node is autoCreated while its corresponding field named {} in Document Type {} is not. "
                                                    + "Field is corrected to be autoCreated."
                                                    , new String[]{ent.getName(), dft.getName(), getName()});
                                            dft.setAutoCreated(true);
                                        }
                                        if (c.isMandatory() && !dft.isMandatory()) {
                                            log.warn("Matching residual Effective NodeType {} child node is mandatory while its corresponding field named {} in Document Type {} is not. "
                                                    + "Field is corrected to be autoCreated."
                                                    , new String[]{ent.getName(), dft.getName(), getName()});
                                            dft.setMandatory(true);
                                        }
                                        if (c.isProtected() && !dft.isProtected()) {
                                            log.warn("Matching residual Effective NodeType {} child node is protected while its corresponding field named {} in Document Type {} is not. "
                                                    + "Field is corrected to be autoCreated."
                                                    , new String[]{ent.getName(), dft.getName(), getName()});
                                            dft.setProtected(true);
                                        }
                                        dft.setEffectiveNodeTypeItem(c);
                                        break;
                                    }
                                }
                            }
                        }
                        if (dft.getEffectiveNodeTypeItem() == null) {
                            log.error("Document Type {} defines node child field named {} without matching named or residual child node in its Effective NodeType {}. "
                                    + "Field is removed.",
                                    new String[]{getName(), dft.getName(), ent.getName()});
                            fieldNameIterator.remove();
                        }
                    }
                }
            }
        }
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
    public Set<String> getSuperTypes() {
        return superTypes;
    }

    public Set<String> getAggregatedTypes() {
        return aggregatedTypes;
    }

    @Override
    public boolean isDocumentType(final String documentTypeName) {
        return aggregatedTypes.contains(documentTypeName) || superTypes.contains(documentTypeName);
    }

    @Override
    public boolean isCompound() {
        return compound;
    }

    public void setCompound(boolean compound) {
        checkSealed();
        this.compound = compound;
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
    public boolean isTemplate() {
        return template;
    }

    public void setTemplate(boolean template) {
        checkSealed();
        this.template = template;
    }

    @Override
    public boolean isCascadeValidate() {
        return cascadeValidate;
    }

    public void setCascadeValidate(boolean cascadeValidate) {
        this.cascadeValidate = cascadeValidate;
    }

    @Override
    public Map<String, DocumentTypeField> getFields() {
        return fields;
    }
}
