/*
 *  Copyright 2013-2022 Bloomreach (https://bloomreach.com)
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import javax.jcr.PropertyType;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;

public final class DomainRuleExtension {

    public enum Type {
        RELAX,
        CONSTRAINT
    }

    private final String domainName;
    private final String domainRuleName;
    private final Collection<FacetRule> facetRules;
    private final Type type;

    public final static FacetRule HIPPO_AVAILABILITY_PREVIEW_RULE = new FacetRule(HIPPO_AVAILABILITY, "preview", true, true, PropertyType.STRING);


    public DomainRuleExtension(final String domainName,
                               final String domainRuleName,
                               final Collection<FacetRule> facetRules,
                               final Type type) {
        this.domainName = domainName;
        this.domainRuleName = domainRuleName;
        this.facetRules = new HashSet<FacetRule>(facetRules);
        this.type = type;
    }

    public DomainRuleExtension(final String domainName,
                               final String domainRuleName,
                               final Collection<FacetRule> facetRules) {
        this(domainName, domainRuleName, facetRules, Type.CONSTRAINT);
    }

    /**
     * To 'relax' the domain {@code domainName} this method can be used. There will be added a new domain rule below
     * {@code domainName} with {@code facetRules} as constraint.
     * @param domainName
     * @param facetRules
     * @return
     */
    public static DomainRuleExtension relaxDomain(final String domainName, final Collection<FacetRule> facetRules) {
        return new DomainRuleExtension(domainName, UUID.randomUUID().toString(), facetRules, Type.RELAX);
    }

    /**
     * To 'constraint' the domain rule {@code domainRuleName} for {@code domainName} this method can be used.
     * There will be added extra constraints (facetRules)  to the domain rule. If the {@code domainRuleName} does not exist
     * for the {@code domainName}, it will be skipped. Note that the {@code domainName} and/or {@code domainRuleName}
     * can be '*'
     * @param domainName
     * @param domainRuleName
     * @param facetRules
     * @return
     */
    public static DomainRuleExtension constraintDomainRule(final String domainName, final String domainRuleName, final Collection<FacetRule> facetRules) {
        return new DomainRuleExtension(domainName, domainRuleName, facetRules, Type.CONSTRAINT);
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDomainRuleName() {
        return domainRuleName;
    }

    public Collection<FacetRule> getFacetRules() {
        return Collections.unmodifiableCollection(facetRules);
    }

    @Override
    public int hashCode() {
        int result = domainName.hashCode();
        result = 31 * result + domainRuleName.hashCode();
        result = 31 * result + facetRules.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DomainRuleExtension)) {
            return false;
        }
        DomainRuleExtension other = (DomainRuleExtension) obj;
        return other.domainName.equals(domainName) && other.domainRuleName.equals(domainRuleName) && other.facetRules.equals(facetRules);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DomainRuleExtension");
        sb.append("(").append(domainName).append(':').append(domainRuleName).append(")");
        sb.append(facetRules);
        return sb.toString();
    }
}
