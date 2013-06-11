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
package org.hippoecm.repository.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hippoecm.repository.security.domain.DomainRule;
import org.hippoecm.repository.security.domain.QFacetRule;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;

public class AuthorizationFilterPrincipal implements Principal {

    private final Map<String, Collection<QFacetRule>> facetRules;

    public AuthorizationFilterPrincipal(final Map<String, Collection<QFacetRule>> facetRules) {
        this.facetRules = facetRules;
    }

    @Override
    public String getName() {
        return "authorization-filter";
    }

    public Map<String, Collection<QFacetRule>> getFacetRules() {
        return facetRules;
    }

    public Map<String, Collection<QFacetRule>> getExpandedFacetRules(Set<FacetAuthPrincipal> faps) {
        Map<String, Collection<QFacetRule>> expandedFacetRules = new HashMap<String, Collection<QFacetRule>>();

        final Map<String, Collection<QFacetRule>> facetRules = getFacetRules();
        for (Map.Entry<String, Collection<QFacetRule>> entry : facetRules.entrySet()) {
            final String domainPath = entry.getKey();
            if (domainPath.startsWith("*/") || domainPath.endsWith("/*")) {
                continue;
            }
            if (!expandedFacetRules.containsKey(domainPath)) {
                expandedFacetRules.put(domainPath, new ArrayList<QFacetRule>());
            }
            expandedFacetRules.get(domainPath).addAll(entry.getValue());
        }

        for (Map.Entry<String, Collection<QFacetRule>> entry : facetRules.entrySet()) {
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

            for (FacetAuthPrincipal fap : faps) {
                if (matchDomain && !fap.getName().equals(domainName)) {
                    continue;
                }
                for (DomainRule domainRule : fap.getRules()) {
                    if (matchDomainRule && !domainRule.getName().equals(domainRuleName)) {
                        continue;
                    }
                    String expandedPath = fap.getName() + "/" + domainRule.getName();
                    if (!expandedFacetRules.containsKey(expandedPath)) {
                        expandedFacetRules.put(expandedPath, new ArrayList<QFacetRule>());
                    }
                    expandedFacetRules.get(expandedPath).addAll(entry.getValue());
                }
            }
        }
        return expandedFacetRules;
    }

}
