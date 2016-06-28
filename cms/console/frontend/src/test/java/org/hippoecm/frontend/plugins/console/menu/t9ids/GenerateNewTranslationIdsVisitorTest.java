/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.t9ids;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockNodeFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class GenerateNewTranslationIdsVisitorTest {

    private MockNode root;

    @Before
    public void setUp() throws Exception {
        root = MockNodeFactory.fromXml("/org/hippoecm/frontend/plugins/console/menu/t9ids/GenerateNewTranslationIdsVisitorTest.xml");
        root.accept(new GenerateNewTranslationIdsVisitor());
    }

    @Test
    public void skipNonTranslatedNodes() throws Exception {
        assertNull(getT9Id("folder-no-t9"));
        assertNull(getT9Id("folder/document-no-t9"));
        assertNull(getT9Id("folder/document-no-t9/document-no-t9"));
        assertNull(getT9Id("folder/document"));
    }

    @Test
    public void skipVirtualNodes() throws Exception {
        assertThat(getT9Id("virtual-folder"), is("virtual-translation-id"));
    }

    @Test
    public void newT9IdForNonDocuments() throws Exception {
        // verify that t9id has changed
        final String folderT9Id = getT9Id("folder");
        assertThat(folderT9Id, is(not("test-folder-t9id")));

        final String nestedFolderT9Id = getT9Id("folder/nest-folder");
        assertThat(nestedFolderT9Id, is(not("test-folder-nested-folder-t9id")));

        // verify it is not the same for different nodes
        assertThat(nestedFolderT9Id, is(not(folderT9Id)));
    }

    @Test
    public void sharedT9IdForDocuments() throws Exception {
        final String doc1T9Id = getT9Id("folder/document/document");
        final String doc2T9Id = getT9Id("folder/document/document[2]");

        // verify that t9id has changed
        assertThat(doc1T9Id, is(not("document-t9id")));
        assertThat(doc2T9Id, is(not("different-document-t9id")));

        // verify that t9id is shared between document variants
        assertThat(doc1T9Id, is(doc2T9Id));
    }

    private String getT9Id(final String relPath) throws RepositoryException {
        if (root.hasNode(relPath)) {
            final Node node = root.getNode(relPath);
            if (node.hasProperty(HippoTranslationNodeType.ID)) {
                return node.getProperty(HippoTranslationNodeType.ID).getString();
            }
        }
        return null;
    }
}
