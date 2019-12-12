/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.usagestatistics.events;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Strategy to find the id of the handle of a node.</p>
 *
 * <p>The strategy returns an {@link Optional} with the identifier of the handle if:
 * <ul>
 *     <li>The node is the handle</li>
 *     <li>The node is a descendant of a handle</li>
 * </ul>
 * In all other cases a {@link Optional#empty()} is returned.
 * </p>
 */
public class HandleIdentifierStrategy implements IdentifierStrategy {

    public static final Logger log = LoggerFactory.getLogger(HandleIdentifierStrategy.class);
    public static final String REP_ROOT = "rep:root";

    /**
     * @param descendant Descendant of handle
     * @return {@link Optional} containing the identifier of the handle or {@link Optional#empty()}
     * @throws RepositoryException {@link RepositoryException} if an identifier or parent cannot be obtained
     */
    public Optional<String> getIdentifier(Node descendant) throws RepositoryException {
        if (descendant != null) {
            Node node = descendant.getSession().getNode(descendant.getPath());
            while (isLeaf(node)) {
                if (isHandle(node)) {
                    log.debug("Return handle { path: {} } as ascendant of descendant { path: {} }",
                            node.getPath(), descendant);
                    return Optional.of(node.getIdentifier());
                }
                node = node.getParent();
            }
            log.warn("Node { path: {} } is not a handle or descendant of a handle, " +
                    "please provide a path to a handle or document", descendant.getPath());
        }
        return Optional.empty();
    }

    private boolean isLeaf(final Node handle) throws RepositoryException {
        return !handle.isNodeType(REP_ROOT);
    }

    private boolean isHandle(final Node handle) throws RepositoryException {
        return handle.isNodeType(HippoNodeType.NT_HANDLE);
    }

}
