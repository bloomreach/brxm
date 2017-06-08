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
package org.onehippo.cms7.services.htmlprocessor.service;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.services.htmlprocessor.util.FacetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetService {

    private static final Logger log = LoggerFactory.getLogger(FacetService.class);

    private final Node node;
    private Map<String, String> unmarkedFacets = Collections.emptyMap();

    public FacetService(final Node node) {
        this.node = node;
        try {
            unmarkedFacets = FacetUtil.getFacets(node);
        } catch (final RepositoryException e) {
            log.error("Error loading facets from node", e);
            unmarkedFacets = Collections.emptyMap();
        }
    }

    public String getFacetId(final String name) {
        return unmarkedFacets.get(name);
    }

    public void markVisited(final String name) {
        unmarkedFacets.remove(name);
    }

    public void removeUnmarkedFacets() {
        // unmarked facets are no longer referenced from markup and can be removed
        if (!unmarkedFacets.isEmpty()) {
            unmarkedFacets.forEach((name, uuid) -> FacetUtil.removeFacet(node, name));
            unmarkedFacets.clear();
        }
    }

    public String findOrCreateFacet(final String uuid) {
        final String name = getFacetName(uuid);
        if (name != null) {
            return name;
        }

        try {
            return FacetUtil.createFacet(node, uuid);
        } catch (final RepositoryException e) {
            log.error("Failed to create facet for node with id {}", uuid, e);
        }
        return null;
    }

    String getFacetName(final String uuid) {
        return unmarkedFacets.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), uuid))
                .map(Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
