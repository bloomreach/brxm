/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.model;

import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.model.Restful;
import org.onehippo.cms7.services.contenttype.ContentType;

import com.google.common.base.Splitter;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "document")
public class DocumentRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private String fullName;
    private String name;
    private String prefix;
    private boolean mixin;
    private boolean compoundType;
    private Set<String> superTypes;
    private Set<String> fieldLocations;

    public DocumentRestful(final ContentType contentType) {
        this.fullName = extractFullName(contentType.getName());
        this.prefix = contentType.getPrefix();
        this.mixin = contentType.isMixin();
        this.compoundType = contentType.isCompoundType();
        this.superTypes = contentType.getSuperTypes();
        this.name = extractName(fullName);
    }

    private String extractFullName(final CharSequence name) {
        final Iterable<String> split = Splitter.on(",").split(name);
        return split.iterator().next();
    }

    public Set<String> getFieldLocations() {
        return fieldLocations;
    }

    public void setFieldLocations(final Set<String> fieldLocations) {
        this.fieldLocations = fieldLocations;
    }

    private String extractName(final String name) {
        final int idx = name.indexOf(':');
        if (idx != -1) {
            return name.substring((idx + 1), name.length());
        }
        return name;
    }


    public DocumentRestful() {
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

    public boolean isCompoundType() {
        return compoundType;
    }

    public void setCompoundType(final boolean compoundType) {
        this.compoundType = compoundType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Set<String> getSuperTypes() {
        return superTypes;
    }

    public void setSuperTypes(final Set<String> superTypes) {
        this.superTypes = superTypes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DocumentRestful{");
        sb.append("fullName='").append(fullName).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", prefix='").append(prefix).append('\'');
        sb.append(", mixin=").append(mixin);
        sb.append(", compoundType=").append(compoundType);
        sb.append(", superTypes=").append(superTypes);
        sb.append('}');
        return sb.toString();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }
}
