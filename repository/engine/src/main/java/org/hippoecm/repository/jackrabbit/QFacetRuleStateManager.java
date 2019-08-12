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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.core.state.ItemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_TYPE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_VALUE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_FACET;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETRULE;

public class QFacetRuleStateManager {

    private final static Logger log = LoggerFactory.getLogger(QFacetRuleStateManager.class);

    private Session systemSession;
    private FacetRuleJcrPathVisitor facetRuleJcrPathVisitor = new FacetRuleJcrPathVisitor();

    private volatile long updateCounter = 0;

    private ConcurrentHashMap<String, String> facetRuleUuidsToRefPathMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> jcrPathToUUIDReferences = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> jcrUuidToPathReferences = new ConcurrentHashMap<>();

    public long getUpdateCounter() {
        return updateCounter;
    }

    public String getUUID(final String pathReference) {
        return jcrPathToUUIDReferences.get(pathReference);
    }

    void setSystemSession(final Session systemSession) {
        this.systemSession = systemSession;
    }

    void visit(final String absPath) throws RepositoryException {
        if (!systemSession.nodeExists(absPath)) {
            return;
        }
        final Node domains = systemSession.getNode(absPath);
        facetRuleJcrPathVisitor.visit(domains);
    }

    void visit(final Node node)  throws RepositoryException {
        facetRuleJcrPathVisitor.visit(node);
    }

    void processNewPath(final String newPath, final ItemState created) {
        if (jcrPathToUUIDReferences.containsKey(newPath)) {
            // update the uuid
            log.info("Previously missing path reference '{}' has been created.", newPath);
            updateCounter++;
            jcrPathToUUIDReferences.put(newPath, created.getId().toString());
            jcrUuidToPathReferences.put(created.getId().toString(), newPath);
        }
    }

    void processDestroyedId(final String destroyedId) {
        if (facetRuleUuidsToRefPathMap.containsKey(destroyedId)) {
            updateCounter++;
            // a facet rule has been removed
            final String jcrPath = facetRuleUuidsToRefPathMap.remove(destroyedId);
            // now remove from jcrUuidToPathReferences and possibly from jcrPathToUUIDReferences
            final String removedReferenceUuid = jcrPathToUUIDReferences.remove(jcrPath);
            if (removedReferenceUuid != null) {
                jcrUuidToPathReferences.remove(removedReferenceUuid);
            }
            return;
        }

        final String path = jcrUuidToPathReferences.get(destroyedId);
        if (path != null) {
            jcrPathToUUIDReferences.put(path, StringUtils.EMPTY);
            jcrUuidToPathReferences.remove(destroyedId);
            updateCounter++;
        }
    }

    final private class FacetRuleJcrPathVisitor extends TraversingItemVisitor {

        @Override
        protected void entering(final Property property, final int level) throws RepositoryException {
        }

        @Override
        protected void entering(final Node node, final int level) throws RepositoryException {

            if (node.isNodeType(NT_FACETRULE)) {
                final String facet = node.getProperty(HIPPO_FACET).getString();
                if ("Reference".equals(node.getProperty(HIPPOSYS_TYPE).getString())
                        && ("jcr:path".equals(facet) || "jcr:uuid".equals(facet))) {
                    final String path = node.getProperty(HIPPOSYS_VALUE).getString();

                    facetRuleUuidsToRefPathMap.put(node.getIdentifier(), path);

                    if (node.getSession().nodeExists(path)) {
                        final Node referencee = node.getSession().getNode(path);
                        final String identifier = referencee.getIdentifier();
                        jcrPathToUUIDReferences.put(path, identifier);
                        jcrUuidToPathReferences.put(identifier, path);
                    } else {
                        jcrPathToUUIDReferences.put(path, StringUtils.EMPTY);
                    }
                }
            }
        }

        @Override
        protected void leaving(final Property property, final int level) throws RepositoryException {
        }

        @Override
        protected void leaving(final Node node, final int level) throws RepositoryException {
        }
    }
}
