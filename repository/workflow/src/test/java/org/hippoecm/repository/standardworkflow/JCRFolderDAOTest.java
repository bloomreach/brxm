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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Folder;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.*;

public class JCRFolderDAOTest extends RepositoryTestCase {


    private Node folderNode;
    private JCRFolderDAO dao;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Node root = session.getRootNode().addNode("test","nt:unstructured");
        folderNode = root.addNode("folderNode","nt:unstructured");
        dao = new JCRFolderDAO(session, folderNode.getIdentifier());
    }

    @Test
    public void get() throws RepositoryException {
        folderNode.addMixin("mix:referenceable");
        final Folder folder = dao.get();
        assertEquals(Stream.of("mix:referenceable").collect(Collectors.toSet()), folder.getMixins());
    }

    @Test
    public void update_add_mixin() throws RepositoryException {
        final Folder folder = dao.get();
        folder.addMixin("mix:referenceable");
        dao.update(folder);
        assertTrue(folderNode.isNodeType("mix:referenceable"));
    }

    @Test
    public void update_remove_mixin() throws RepositoryException {
        folderNode.addMixin("mix:referenceable");
        final Folder folder = dao.get();
        folder.removeMixin("mix:referenceable");
        dao.update(folder);
        assertFalse(folderNode.isNodeType("mix:referenceable"));
    }
}
