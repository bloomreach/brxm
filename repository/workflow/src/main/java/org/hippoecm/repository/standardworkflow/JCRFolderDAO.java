/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.repository.standardworkflow;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class transforms a hippostd:folder or hippostd:directory node
 * to a {@link Folder} instance and vice versa.
 * So far it only takes into account the mixins on the node.
 */
public final class JCRFolderDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(JCRFolderDAO.class);
    /**
     * Identifier of a node of type hippostd:folder or hippostd:directory.
     */
    private String folderNodeIdentifier;
    /**
     * Jcr root session.
     */
    private Session rootSession;

    /**
     * Create new FolderDAO.
     * @param session    {@link Session} root session
     * @param identifier Identifier of a hippostd:folder or hippostd:directory
     */
    public JCRFolderDAO(final Session session, final String identifier) {
        if (identifier == null) {
            throw new NullPointerException("Identifier should not be null");
        }
        if (session == null) {
            throw new NullPointerException("Session should not be null");
        }
        LOGGER.debug("Create JCRFolderDAO with session : { userID: {}} for node : { identifier : {}}"
                , session.getUserID(), identifier);
        this.folderNodeIdentifier = identifier;
        this.rootSession = session;
    }

    /**
     * Constructs a {@link Folder} POJO based on the backing node.
     *
     * @return {@link Folder} instance with the mixin names of the backing folder node
     * @throws RepositoryException if a mixin could not be added
     */
    public Folder get() throws RepositoryException {
        final Folder folder = new FolderImpl();
        getMixinNames().forEach(mixin -> folder.addMixin(mixin));
        LOGGER.debug("folder: {}", folder);
        return folder;
    }

    /**
     * Synchronize the mixins on the backing node of type hippostd:folder or hippostd:directory
     * with the folder. Inherited mixins are not taken into account.
     *
     * @param folder {@link Folder} instance whose mixins will be mapped to the backing node. Folder should
     *               not be {@code null}.
     * @return {@link Folder} instance
     * @throws RepositoryException
     */
    public Folder update(final Folder folder) throws RepositoryException {
        if (folder == null) {
            throw new NullPointerException("folder should not be null");
        }
        LOGGER.debug("Update node : { identifier: {} } from folder : {}", folderNodeIdentifier, folder);
        updateMixins(folder);
        rootSession.save();
        return folder;
    }

    private void updateMixins(final Folder folder) throws RepositoryException {
        Set<String> mixinsOnJcrNode = getMixinNames();
        Set<String> mixinsOnFolderPOJO = folder.getMixins();

        final Node folderNode = getNodeByIdentifier();
        for (final String mixin : difference(mixinsOnFolderPOJO, mixinsOnJcrNode)) {
            folderNode.addMixin(mixin);
        }
        for (final String mixin : difference(mixinsOnJcrNode, mixinsOnFolderPOJO)) {
            folderNode.removeMixin(mixin);
        }
    }

    private Set<String> getMixinNames() throws RepositoryException {
        return Stream.of(getNodeByIdentifier().getMixinNodeTypes())
                .map(NodeType::getName)
                .collect(Collectors.toSet());
    }

    private Set<String> difference(final Set<String> a, final Set<String> b) {
        Set<String> result = new HashSet<>(a);
        result.removeAll(b);
        return result;
    }

    private Node getNodeByIdentifier() throws RepositoryException {
        return rootSession.getNodeByIdentifier(folderNodeIdentifier);
    }
}
