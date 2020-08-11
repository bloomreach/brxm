/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.repository.standardworkflow;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toSet;
import static org.hippoecm.repository.util.JcrUtils.getMixinNodeTypes;
import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;
import static org.hippoecm.repository.util.JcrUtils.getPrimaryNodeType;
import static org.onehippo.repository.util.JcrConstants.JCR_FROZEN_NODE;

final class JCRFolderDAO {

    private static final Logger log = LoggerFactory.getLogger(JCRFolderDAO.class);

    private final Set<String> supportedPrimaryNodeTypes;

    /**
     * Constructs a Folder DAO that can be used to get and update folder nodes . When getting or updating a folder
     * for a node the node's primary type must be one from the supportedPrimaryNodeTypes.
     *
     * @param supportedPrimaryNodeTypes node types that are supported as folder node types
     */
    JCRFolderDAO(String... supportedPrimaryNodeTypes) {
        this.supportedPrimaryNodeTypes = Stream.of(supportedPrimaryNodeTypes).collect(toSet());
    }

    /**
     * Returns a {@link Folder} representing the given folder node.
     *
     * @param folderNode a folder node
     * @return folder representation of the node
     * @throws RepositoryException      if reading node properties fails
     * @throws IllegalArgumentException if the primary node type of the folder node is not supported
     */
    Folder get(final Node folderNode) throws RepositoryException {
        log.debug("get folder from node: { path: {} }", folderNode.getPath());

        validateHasSupportedPrimaryType(folderNode);

        return new FolderImpl(folderNode);
    }

    /**
     * Persists the properties of the @{link Folder} to the given folder node. The folder and node must have the same
     * identifier, otherwise an {@link IllegalArgumentException} will be thrown.
     *
     * @param folder     a folder
     * @param folderNode a node with the same identifier as the folder
     * @throws RepositoryException      if reading or writing node properties fails
     * @throws IllegalArgumentException if the primary node type of the folder node is not supported or if the node is frozen
     */
    void update(final Folder folder, final Node folderNode) throws RepositoryException {
        log.debug("update node : { path: {} } with {}", folderNode.getPath(), folder);

        validateHasSupportedPrimaryType(folderNode);
        validateNotFrozen(folderNode);
        validateEqualIdentifiers(folderNode, folder);

        final Set<String> mixinNames = folder.getMixins();

        for (final NodeType mixinNodeType : getMixinNodeTypes(folderNode)) {
            final String mixinName = mixinNodeType.getName();
            if (!mixinNames.contains(mixinName)) {
                log.debug("Removing mixin {}", mixinName);
                folderNode.removeMixin(mixinName);
            }
        }

        for (final String mixinName : mixinNames) {
            if (!folderNode.isNodeType(mixinName)) {
                log.debug("Adding mixin {}", mixinName);
                folderNode.addMixin(mixinName);
            }
        }
    }

    private void validateEqualIdentifiers(final Node folderNode, final Folder folder) throws RepositoryException {
        if (!folderNode.getIdentifier().equals(folder.getIdentifier())) {
            final String message = String.format(
                    "folderNode identifier '%s' not equal to folder identifier '%s'",
                    folderNode.getIdentifier(), folder.getIdentifier());
            throw new IllegalArgumentException(message);
        }
    }

    private void validateHasSupportedPrimaryType(final Node folderNode) throws RepositoryException {
        Objects.requireNonNull(folderNode, "Folder node must not be null");
        final Predicate<String> hasType = type -> {
            try {
                return folderNode.isNodeType(type);
            } catch (RepositoryException e) {
                log.error("Failed to test if node at path '{}; is of primary type {}", getNodePathQuietly(folderNode), type, e);
                return false;
            }
        };
        if (supportedPrimaryNodeTypes.stream().noneMatch(hasType)) {
            final String message = String.format(
                    "Node { path: %s } has an unsupported primary type: '%s'. Supported primary types are: %s",
                    folderNode.getPath(), getPrimaryNodeType(folderNode).getName(), supportedPrimaryNodeTypes);
            throw new IllegalArgumentException(message);
        }
    }

    private void validateNotFrozen(final Node folderNode) throws RepositoryException {
        if (folderNode.isNodeType(JCR_FROZEN_NODE)) {
            final String message = String.format(
                    "Node { path: %s } cannot be updated because it has mixin type '%s'.",
                    folderNode.getPath(), JCR_FROZEN_NODE);
            throw new IllegalArgumentException(message);
        }
    }

}
