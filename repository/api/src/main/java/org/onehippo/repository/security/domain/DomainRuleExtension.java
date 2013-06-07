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

import java.util.Collection;

public final class DomainRuleExtension {

    private final String domainName;
    private final String domainRuleName;
    private final Collection<FacetRule> facetRules;

    public DomainRuleExtension(final String domainName, final String domainRuleName, final Collection<FacetRule> facetRules) {
        this.domainName = domainName;
        this.domainRuleName = domainRuleName;
        this.facetRules = facetRules;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDomainRuleName() {
        return domainRuleName;
    }

    public Collection<FacetRule> getFacetRules() {
        return facetRules;
    }

}
