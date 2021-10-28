/*
 *  Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.filter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.apache.commons.lang.builder.ToStringBuilder;

public class Element implements Serializable {

    private final String name;
    private final Collection<String> attributes;

    private boolean omitJsProtocol = true;
    private boolean omitDataProtocol = true;
    private boolean secureTargetBlankLinks = true;

    public static Element create(final String name) {
        return new Element(name, Collections.emptyList());
    }

    public static Element create(final String name, final String... attributes) {
        return new Element(name, Arrays.asList(attributes));
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

    public Element setOmitJsProtocol(final boolean omitJsProtocol) {
        this.omitJsProtocol = omitJsProtocol;
        return this;
    }

    public boolean isOmitJsProtocol() {
        return omitJsProtocol;
    }

    public Element setOmitDataProtocol(final boolean omitDataProtocol) {
        this.omitDataProtocol = omitDataProtocol;
        return this;
    }

    public boolean isOmitDataProtocol() {
        return omitDataProtocol;
    }

    public Element setSecureTargetBlankLinks(final boolean secureTargetBlankLinks) {
        this.secureTargetBlankLinks = secureTargetBlankLinks;
        return this;
    }

    public boolean isSecureTargetBlankLinks() {
        return secureTargetBlankLinks;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Element element = (Element) o;
        return isOmitJsProtocol() == element.isOmitJsProtocol() &&
                isOmitDataProtocol() == element.isOmitDataProtocol() &&
                isSecureTargetBlankLinks() == element.isSecureTargetBlankLinks() &&
                Objects.equals(getName(), element.getName()) &&
                Objects.equals(getAttributes(), element.getAttributes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName(),
                getAttributes(),
                isOmitJsProtocol(),
                isOmitDataProtocol(),
                isSecureTargetBlankLinks()
        );
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("attributes", attributes)
                .append("omitJsProtocol", omitJsProtocol)
                .append("omitDataProtocol", omitDataProtocol)
                .append("secureTargetBlankLinks", secureTargetBlankLinks)
                .toString();
    }
}
