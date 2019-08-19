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
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.core.state.ItemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.PropertyType.TYPENAME_REFERENCE;
import static org.apache.jackrabbit.JcrConstants.JCR_PATH;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_TYPE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_VALUE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_FACET;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETRULE;

public class QFacetRuleStateManager {

    private final static Logger log = LoggerFactory.getLogger(QFacetRuleStateManager.class);

    private final FacetRuleJcrPathVisitor facetRuleJcrPathVisitor = new FacetRuleJcrPathVisitor();

    private volatile long updateCounter = 0;

    private final ConcurrentHashMap<String, String> facetRuleUUIDToJcrPath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> jcrPathToJcrUUID = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> jcrUUIDToJcrPath = new ConcurrentHashMap<>();

    public long getUpdateCounter() {
        return updateCounter;
    }

    public String getReferenceUUID(final String facetRuleUUID) {
        final String jcrPath = facetRuleUUIDToJcrPath.get(facetRuleUUID);
        return jcrPathToJcrUUID.get(jcrPath);
    }

    void visit(final Node node)  throws RepositoryException {
        facetRuleJcrPathVisitor.visit(node);
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
            final String path = jcrUUIDToJcrPath.get(destroyedId);
            if (path != null) {
                // path reference destroyed
                jcrPathToJcrUUID.put(path, StringUtils.EMPTY);
                jcrUUIDToJcrPath.remove(destroyedId);
                updateCounter++;
            }
        }
    }

    final private class FacetRuleJcrPathVisitor extends TraversingItemVisitor.Default {

        @Override
        protected void entering(final Node node, final int level) throws RepositoryException {

            if (node.isNodeType(NT_FACETRULE)) {
                final String facet = node.getProperty(HIPPO_FACET).getString();
                if (TYPENAME_REFERENCE.equals(node.getProperty(HIPPOSYS_TYPE).getString())
                        && (JCR_PATH.equals(facet) || JCR_UUID.equals(facet))) {
                    final String path = node.getProperty(HIPPOSYS_VALUE).getString();

                    facetRuleUUIDToJcrPath.put(node.getIdentifier(), path);

                    if (node.getSession().nodeExists(path)) {
                        final Node referencee = node.getSession().getNode(path);
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
    }
}
