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

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.hippoecm.repository.security.domain.FacetRule;

/**
 * Extension for the {@link HippoAccessManager} to impose additional facet rules.
 * Comes with it's own read cache to
 */
public class AuthorizationExtension {

    private final Map<String, Collection<FacetRule>> extendedFacetRules;
    private final WeakHashMap<NodeId, Boolean> readAccessCache = new WeakHashMap<NodeId, Boolean>();

    public AuthorizationExtension(Map<String, Collection<FacetRule>> extendedFacetRules) throws RepositoryException {
        this.extendedFacetRules = extendedFacetRules;
    }

    public Map<String, Collection<FacetRule>> getExtendedFacetRules() {
        return extendedFacetRules;
    }

    public Boolean getAccessFromCache(final NodeId id) {
        return readAccessCache.get(id);
    }

    public void addAccessToCache(final NodeId id, final boolean value) {
        readAccessCache.put(id, value);
    }

    public void removeAccessFromCache(final NodeId id) {
        readAccessCache.remove(id);
    }
}
