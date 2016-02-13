/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.mock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;

/**
 * Mock implementation of {@link ItemDefinition} that only implements {@link #getName}.
 */
public class MockItemDefinition implements ItemDefinition {

    // mandatory item names (from alphabetically ordered list) specified in the section 3.7, JCR 2.0 specification
    private static final Set<String> JCR2_SPEC_MANDATORY_ITEM_NAMES =
            new HashSet<>(Arrays.asList(
                    "jcr:autoCreated",
                    "jcr:availableQueryOperators",
                    "jcr:baseVersion",
                    "jcr:childVersionHistory",
                    "jcr:content",
                    "jcr:created",
                    "jcr:data",
                    "jcr:frozenPrimaryType",
                    "jcr:hasOrderableChildNodes",
                    "jcr:isAbstract",
                    "jcr:isCheckedOut",
                    "jcr:isFullTextSearchable",
                    "jcr:isMixin",
                    "jcr:isQueryOrderable",
                    "jcr:isQueryable",
                    "jcr:mandatory",
                    "jcr:multiple",
                    "jcr:nodeTypeName",
                    "jcr:onParentVersion",
                    "jcr:predecessors",
                    "jcr:primaryType",
                    "jcr:protected",
                    "jcr:requiredPrimaryTypes",
                    "jcr:requiredType",
                    "jcr:rootVersion",
                    "jcr:sameNameSiblings",
                    "jcr:uuid",
                    "jcr:versionHistory",
                    "jcr:versionableUuid"
                    ));

    // autocreated item names (from alphabetically ordered list) specified in the section 3.7, JCR 2.0 specification
    private static final Set<String> JCR2_SPEC_AUTOCREATED_ITEM_NAMES =
            new HashSet<>(Arrays.asList(
                    "jcr:childVersionHistory",
                    "jcr:created",
                    "jcr:createdBy",
                    "jcr:etag",
                    "jcr:frozenPrimaryType",
                    "jcr:isCheckedOut",
                    "jcr:lastModified",
                    "jcr:lastModifiedBy",
                    "jcr:primaryType",
                    "jcr:rootVersion",
                    "jcr:uuid",
                    "jcr:versionableUuid"
                    ));

    // protected item names (from alphabetically ordered list) specified in the section 3.7, JCR 2.0 specification
    private static final Set<String> JCR2_SPEC_PROTECTED_ITEM_NAMES =
            new HashSet<>(Arrays.asList(
                    "jcr:activity",
                    "jcr:autoCreated",
                    "jcr:availableQueryOperators",
                    "jcr:baseVersion",
                    "jcr:childNodeDefinition",
                    "jcr:childVersionHistory",
                    "jcr:configuration",
                    "jcr:copiedFrom",
                    "jcr:created",
                    "jcr:defaultPrimaryType",
                    "jcr:defaultValues",
                    "jcr:etag",
                    "jcr:frozenMixinTypes",
                    "jcr:frozenNode",
                    "jcr:frozenPrimaryType",
                    "jcr:frozenUuid",
                    "jcr:hasOrderableChildNodes",
                    "jcr:isAbstract",
                    "jcr:isCheckedOut",
                    "jcr:isFullTextSearchable",
                    "jcr:isMixin",
                    "jcr:isQueryOrderable",
                    "jcr:isQueryable",
                    "jcr:mandatory",
                    "jcr:mergeFailed",
                    "jcr:mixinTypes",
                    "jcr:multiple",
                    "jcr:name",
                    "jcr:nodeTypeName",
                    "jcr:onParentVersion",
                    "jcr:predecessors",
                    "jcr:primaryItemName",
                    "jcr:primaryType",
                    "jcr:propertyDefinition",
                    "jcr:protected",
                    "jcr:requiredPrimaryTypes",
                    "jcr:requiredType",
                    "jcr:rootVersion",
                    "jcr:sameNameSiblings",
                    "jcr:successors",
                    "jcr:supertypes",
                    "jcr:uuid",
                    "jcr:valueConstraints",
                    "jcr:versionHistory",
                    "jcr:versionLabels",
                    "jcr:versionableUuid"
                    ));

    private final String name;

    private Boolean mandatory;
    private Boolean autoCreated;
    private Boolean isProtected;

    public MockItemDefinition(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMandatory() {
        if (mandatory != null) {
            return mandatory.booleanValue();
        }

        if (JCR2_SPEC_MANDATORY_ITEM_NAMES.contains(getName())) {
            return true;
        }

        return false;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    @Override
    public boolean isAutoCreated() {
        if (autoCreated != null) {
            return autoCreated.booleanValue();
        }

        if (JCR2_SPEC_AUTOCREATED_ITEM_NAMES.contains(getName())) {
            return true;
        }

        return false;
    }

    public void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    @Override
    public boolean isProtected() {
        if (isProtected != null) {
            return isProtected.booleanValue();
        }

        if (JCR2_SPEC_PROTECTED_ITEM_NAMES.contains(getName())) {
            return true;
        }

        return false;
    }

    public void setProtected(boolean isProtected) {
        this.isProtected = isProtected;
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public NodeType getDeclaringNodeType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getOnParentVersion() {
        throw new UnsupportedOperationException();
    }

}
