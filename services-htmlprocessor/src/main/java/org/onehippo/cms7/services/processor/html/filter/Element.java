/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.processor.html.filter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Element implements Serializable {

    private final String name;
    private final Collection<String> attributes;

    private Element(final String name) {
        this(name, Collections.emptyList());
    }

    private Element(final String name, final Collection<String> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public Collection<String> getAttributes() {
        return attributes;
    }

    public boolean hasAttribute(final String attributeName) {
        return attributes.contains(attributeName);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) {
            return false;
        }

        final Element other = (Element) obj;
        return new EqualsBuilder()
                .append(name, other.name)
                .append(attributes, other.attributes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(name)
                .append(attributes)
                .toHashCode();
    }

    public static Element create(final String name) {
        return new Element(name);
    }

    public static Element create(final String name, final String... attributes) {
        return new Element(name, Arrays.asList(attributes));
    }
}
