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
import java.util.Collection;
import java.util.Map;

import org.hippoecm.repository.security.domain.QFacetRule;

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
}
