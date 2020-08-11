/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.security.domain;

import javax.jcr.PropertyType;

public final class FacetRule {

    private final String facet;
    private final String value;
    private final boolean equal;
    private final boolean optional;
    private final int type;

    public FacetRule(final String facet, final String value, final boolean equal, final boolean optional, final int type) {
        this.facet = facet;
        this.value = value;
        this.equal = equal;
        this.optional = optional;
        this.type = type;
    }

    /**
     * Get the string representation of the facet
     * @return the facet
     */
    public String getFacet() {
        return facet;
    }

    /**
     * The value of the facet rule to match
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Check for equality or inequality
     * @return true if to rule has to check for equality
     */
    public boolean isEqual() {
        return equal;
    }

    /**
     * When the facet is optional, it does not need to be present on a node for the rule to match.
     * If it <strong>is</strong> present on the node, it's value must conform to the #isEqual and #getValue.
     * <p>
     * When the facet is not optional, the rule only matches when the facet is available on the node.
     *
     * @return true if the facet is optional
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Get the PropertyType of the facet
     * @return the type
     */
    public int getType() {
        return type;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FacetRule");
        sb.append("(").append(PropertyType.nameFromValue(type)).append(")");
        sb.append("[");
        sb.append(facet);
        if (equal) {
            sb.append(" == ");
        } else {
            sb.append(" != ");
        }
        sb.append(value).append("]");
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FacetRule)) {
            return false;
        }
        FacetRule other = (FacetRule) obj;
        return facet.equals(other.getFacet()) && value.equals(other.getValue()) && (equal == other.isEqual());
    }

    @Override
    public int hashCode() {
        int result = facet.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (equal ? 1 : 0);
        result = 31 * result + (optional ? 1 : 0);
        result = 31 * result + type;
        return result;
    }
}
