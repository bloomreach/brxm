/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.core.state.ItemState;
import org.hippoecm.repository.security.domain.Domain;
import org.hippoecm.repository.security.domain.QFacetRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.PropertyType.TYPENAME_REFERENCE;
import static org.apache.jackrabbit.JcrConstants.JCR_PATH;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_TYPE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_FACET;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAIN;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAINFOLDER;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAINRULE;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETRULE;

public class QFacetRuleStateManager {

    private final static Logger log = LoggerFactory.getLogger(QFacetRuleStateManager.class);

    private volatile long updateCounter = 0;

    private final ConcurrentHashMap<String, String> facetRuleUUIDToJcrPath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> jcrPathToJcrUUID = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> jcrUUIDToJcrPath = new ConcurrentHashMap<>();

    public long getUpdateCounter() {
        return updateCounter;
    }

    public String getReferenceUUID(final String facetRuleUUID) {
        final String jcrPath = facetRuleUUIDToJcrPath.get(facetRuleUUID);
        return jcrPath != null ? jcrPathToJcrUUID.get(jcrPath) : null;
    }

    void processNewPath(final String newPath, final ItemState created) {
        if (jcrPathToJcrUUID.containsKey(newPath)) {
            // update the uuid
            log.info("Previously missing path reference '{}' has been created.", newPath);
            jcrPathToJcrUUID.put(newPath, created.getId().toString());
            jcrUUIDToJcrPath.put(created.getId().toString(), newPath);
            updateCounter++;
        }
    }

    void processDestroyedId(final String destroyedId) {
        if (facetRuleUUIDToJcrPath.remove(destroyedId) != null) {
            // FacetRule is destroyed
            // We cannot remove (clean) jcrPathToJcrUUID because there may be multiple facetRules referencing
            // the same path. This may keep now 'unused' reference<->uuid entries, but this should be neglectable.
            updateCounter++;
        } else {
            final String path = jcrUUIDToJcrPath.remove(destroyedId);
            if (path != null) {
                // path reference destroyed
                jcrPathToJcrUUID.put(path, StringUtils.EMPTY);
                updateCounter++;
            }
        }
    }

    void processDomainFolder(final Node domainFolderNode) throws RepositoryException {
        if (Domain.isValidDomainFolderLocation(domainFolderNode)) {
            for (NodeIterator domainNodes = domainFolderNode.getNodes(); domainNodes.hasNext();) {
                final Node domainNode = domainNodes.nextNode();
                if (domainNode.isNodeType(NT_DOMAIN)) {
                    for (NodeIterator facetRuleNodes = domainFolderNode.getNodes(); facetRuleNodes.hasNext();) {
                        final Node facetRuleNode = facetRuleNodes.nextNode();
                        if (facetRuleNode.isNodeType(NT_FACETRULE)) {
                            processFacetRuleJcrPath(facetRuleNode);
                        }
                    }
                }
            }
        } else if (log.isWarnEnabled()) {
            try {
                log.warn("Skipping domain folder in not-supported location: {}", domainFolderNode.getPath());
            } catch (RepositoryException e) {
                log.warn("Skipping domain folder in not-supported location");
                log.info("Exception while fetching path for domain folder. Most likely the result of a node move which " +
                        "has not been processed by other (session) listeners. Ignore this exception");
            }
        }
    }

    void processFacetRule(final Node facetRuleNode) throws RepositoryException {
        if (facetRuleNode.getDepth() > 4 &&
                facetRuleNode.getParent().isNodeType(NT_DOMAINRULE) &&
                facetRuleNode.getParent().getParent().isNodeType(NT_DOMAIN) &&
                facetRuleNode.getParent().getParent().getParent().isNodeType(NT_DOMAINFOLDER) &&
                Domain.isValidDomainFolderLocation(facetRuleNode.getParent().getParent())) {
            processFacetRuleJcrPath(facetRuleNode);
        } else if (log.isWarnEnabled()) {
            try {
                log.warn("Skipping facet rule in not-supported location: {}", facetRuleNode.getPath());
            } catch (RepositoryException e) {
                log.warn("Skipping facet rule in not-supported location");
                log.info("Exception while fetching path for facet rule. Most likely the result of a node move which " +
                        "has not been processed by other (session) listeners. Ignore this exception");
            }
        }
    }

    private void processFacetRuleJcrPath(final Node facetRuleNode) throws RepositoryException {
        final String facet = facetRuleNode.getProperty(HIPPO_FACET).getString();
        if (TYPENAME_REFERENCE.equals(facetRuleNode.getProperty(HIPPOSYS_TYPE).getString())
                && (JCR_PATH.equalsIgnoreCase(facet) || JCR_UUID.equalsIgnoreCase(facet))) {

            final String path = QFacetRule.getFullyQualifiedPath(facetRuleNode);

            facetRuleUUIDToJcrPath.put(facetRuleNode.getIdentifier(), path);

            if (facetRuleNode.getSession().nodeExists(path)) {
                final Node referencee = facetRuleNode.getSession().getNode(path);
                final String identifier = referencee.getIdentifier();
                jcrPathToJcrUUID.put(path, identifier);
                jcrUUIDToJcrPath.put(identifier, path);
            } else {
                jcrPathToJcrUUID.put(path, StringUtils.EMPTY);
            }
            updateCounter++;
        }
    }
}
