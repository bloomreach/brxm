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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.api.Folder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.HippoStdNodeType.NT_DIRECTORY;
import static org.hippoecm.repository.HippoStdNodeType.NT_FOLDER;
import static org.onehippo.repository.util.JcrConstants.MIX_REFERENCEABLE;
import static org.onehippo.repository.util.JcrConstants.NT_UNSTRUCTURED;

public class JCRFolderDAOTest extends RepositoryTestCase {

    private Node folderRoot;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        folderRoot = session.getRootNode().addNode("test", NT_UNSTRUCTURED);
    }

    @After
    public void cleanup() throws RepositoryException {
        folderRoot.remove();
        session.save();
    }

    @Test
    public void get() throws RepositoryException {

        final Node folderNode = folderRoot.addNode("folderNode", NT_FOLDER);
        folderNode.addMixin(MIX_REFERENCEABLE);
        session.save();

        final JCRFolderDAO dao = new JCRFolderDAO(NT_FOLDER);
        final Folder folder = dao.get(folderNode);
        Assertions.assertThat(folder.getMixins())
                .containsExactlyInAnyOrder(MIX_REFERENCEABLE);

    }

    @Test
    public void update_add_mixin() throws RepositoryException {

        final Node folderNode = folderRoot.addNode("folderNode", NT_FOLDER);
        session.save();

        final JCRFolderDAO dao = new JCRFolderDAO(NT_FOLDER);
        final Folder folder = dao.get(folderNode);
        folder.addMixin(MIX_REFERENCEABLE);
        dao.update(folder, folderNode);
        session.save();

        Assertions.assertThat(folderNode.isNodeType(MIX_REFERENCEABLE)).isTrue();
    }

    @Test
    public void update_remove_mixin() throws RepositoryException {

        final Node directoryNode = folderRoot.addNode("directoryNode", NT_DIRECTORY);
        directoryNode.addMixin(MIX_REFERENCEABLE);
        session.save();

        final JCRFolderDAO dao = new JCRFolderDAO(NT_DIRECTORY);
        final Folder folder = dao.get(directoryNode);
        folder.removeMixin(MIX_REFERENCEABLE);
        dao.update(folder, directoryNode);
        session.save();

        Assertions.assertThat(directoryNode.isNodeType(MIX_REFERENCEABLE)).isFalse();
    }

    @Test
    public void supported_primary_types() throws RepositoryException {

        final Node unstructuredNode = folderRoot.addNode("unstructuredNode", NT_UNSTRUCTURED);
        unstructuredNode.addMixin(MIX_REFERENCEABLE);
        final Node folderNode = folderRoot.addNode("folderNode", NT_FOLDER);
        final Node directoryNode = folderRoot.addNode("directoryNode", NT_DIRECTORY);
        session.save();

        final JCRFolderDAO dao = new JCRFolderDAO(NT_FOLDER, NT_DIRECTORY);
        Assertions.assertThat(dao.get(folderNode).getMixins()).isEmpty();
        Assertions.assertThat(dao.get(directoryNode).getMixins()).isEmpty();
        Assertions.assertThatThrownBy(() -> dao.get(unstructuredNode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(NT_FOLDER)
                .hasMessageContaining(NT_DIRECTORY);
        Assertions.assertThat(unstructuredNode.isNodeType(MIX_REFERENCEABLE))
                .isTrue();
    }

}
