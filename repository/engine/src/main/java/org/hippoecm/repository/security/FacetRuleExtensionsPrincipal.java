/*
 *  Copyright 2013-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hippoecm.repository.security.domain.DomainRule;
import org.hippoecm.repository.security.domain.FacetAuthDomain;
import org.hippoecm.repository.security.domain.QFacetRule;

public final class FacetRuleExtensionsPrincipal implements Principal {

    /**
     * The name representing this principal, effectively the class name
     */
    private final String name = FacetRuleExtensionsPrincipal.class.getName();

    private final Map<String, Collection<QFacetRule>> facetRuleExtensions;

    public FacetRuleExtensionsPrincipal(final Map<String, Collection<QFacetRule>> facetRuleExtensions) {
        this.facetRuleExtensions = facetRuleExtensions;
    }

    @Override
    public String getName() {
        return name;
    }


    public Map<String, Collection<QFacetRule>> getExpandedFacetRules(Set<FacetAuthDomain> fads) {
        Map<String, Collection<QFacetRule>> expandedFacetRules = new HashMap<>();

        for (Map.Entry<String, Collection<QFacetRule>> entry : facetRuleExtensions.entrySet()) {
            final String domainPath = entry.getKey();
            if (domainPath.startsWith("*/") || domainPath.endsWith("/*")) {
                continue;
            }
            if (!expandedFacetRules.containsKey(domainPath)) {
                expandedFacetRules.put(domainPath, new ArrayList<>());
            }
            expandedFacetRules.get(domainPath).addAll(entry.getValue());
        }

        for (Map.Entry<String, Collection<QFacetRule>> entry : facetRuleExtensions.entrySet()) {
            final String domainPath = entry.getKey();
            if (!domainPath.startsWith("*/") && !domainPath.endsWith("/*")) {
                continue;
            }
            final String[] parts = domainPath.split("/");
            if (parts.length != 2) {
                continue;
            }

            final String domainName = parts[0];
            final boolean matchDomain = !parts[0].equals("*");
            final String domainRuleName = parts[1];
            final boolean matchDomainRule = !parts[1].equals("*");

            for (FacetAuthDomain fad : fads) {
                if (matchDomain && !fad.getDomainName().equals(domainName)) {
                    continue;
                }
                for (DomainRule domainRule : fad.getRules()) {
                    if (matchDomainRule && !domainRule.getName().equals(domainRuleName)) {
                        continue;
                    }
                    String expandedPath = fad.getDomainName() + "/" + domainRule.getName();
                    if (!expandedFacetRules.containsKey(expandedPath)) {
                        expandedFacetRules.put(expandedPath, new ArrayList<>());
                    }
                    expandedFacetRules.get(expandedPath).addAll(entry.getValue());
                }
            }
        }
        return expandedFacetRules;
    }

    /**
     * Enforce that only a single instance of FacetRuleExtensionsPrincipal can be contained within a set
     * @param obj
     * @return true when compared with any other FacetRuleExtensionsPrincipal instance, else false
     */
    final public boolean equals(Object obj) {
        return this == obj || obj instanceof FacetRuleExtensionsPrincipal;
    }

    public int hashCode() {
        return this.getClass().hashCode();
    }
}
