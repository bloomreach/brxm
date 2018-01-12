/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.model;

import java.util.Set;

/**
 * Serializable representation of a project's content type.
 */
public class ContentType implements Restful {

    private String displayName; // "localized" (English) display name
    private String javaName;    // name of the Java bean class
    private String fullPath;    // filesystem path to the Java bean class
    private String fullName;    // full JCR type name
    private String name;        // non-prefixed JCR type name
    private String prefix;      // JCR type prefix
    private boolean mixin;      // flag if content type is a mixin
    private boolean draftMode;  // flag if content type is currently being edited (in CMS' document type editor)
    private boolean compoundType; // flag if content type is a compound
    private Set<String> superTypes; // list of JCR type names

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getJavaName() {
        return javaName;
    }

    public void setJavaName(final String javaName) {
        this.javaName = javaName;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(final String fullPath) {
        this.fullPath = fullPath;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public boolean isMixin() {
        return mixin;
    }

    public void setMixin(final boolean mixin) {
        this.mixin = mixin;
    }

    public boolean isDraftMode() {
        return draftMode;
    }

    public void setDraftMode(final boolean draftMode) {
        this.draftMode = draftMode;
    }

    public boolean isCompoundType() {
        return compoundType;
    }

    public void setCompoundType(final boolean compoundType) {
        this.compoundType = compoundType;
    }

    public Set<String> getSuperTypes() {
        return superTypes;
    }

    public void setSuperTypes(final Set<String> superTypes) {
        this.superTypes = superTypes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ContentType{");
        sb.append("fullName='").append(fullName).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", prefix='").append(prefix).append('\'');
        sb.append(", mixin=").append(mixin);
        sb.append(", compoundType=").append(compoundType);
        sb.append(", superTypes=").append(superTypes);
        sb.append('}');
        return sb.toString();
    }
}
