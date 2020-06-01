/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.treepickerrepresentation;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.DocumentTreePickerRepresentation.ExpandedNodeHierarchy;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.DocumentTreePickerRepresentation.ExpandedNodeHierarchy.createExpandedNodeHierarchy;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.DocumentTreePickerRepresentation.ExpandedNodeHierarchy.createSingleNodeHierarchy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExpandedNodeHierarchyTest {

    @Test
    public void singleNodeHierarchy_has_empty_list_for_children() throws RepositoryException {
        final MockNode rootContentNode = createMockContent();

        final ExpandedNodeHierarchy singleNodeHierarchy = createSingleNodeHierarchy(rootContentNode);
        assertEquals("/content/documents/myproject", singleNodeHierarchy.getNode().getPath());
        assertEquals(0, singleNodeHierarchy.getChildren().size());
    }

    @Test
    public void expandedNodeHierarchy_for_single_expansion_path_1_level_deep() throws RepositoryException {
        final MockNode rootContentNode = createMockContent();
        List<String> expansionPaths = new ArrayList<>();
        expansionPaths.add("/content/documents/myproject/folderA");
        final ExpandedNodeHierarchy hierarchy = createExpandedNodeHierarchy(rootContentNode.getSession(),
                "/content/documents/myproject", expansionPaths);

        assertEquals("/content/documents/myproject", hierarchy.getNode().getPath());
        assertEquals(1, hierarchy.getChildren().size());
        final ExpandedNodeHierarchy oneLevelDeep = hierarchy.getChildren().get("/content/documents/myproject/folderA");
        assertEquals("/content/documents/myproject/folderA", oneLevelDeep.getNode().getPath());
        assertEquals(0, oneLevelDeep.getChildren().size());
    }


    @Test
    public void expandedNodeHierarchy_for_single_expansion_path_2_folders_deep() throws RepositoryException {
        final MockNode rootContentNode = createMockContent();
        List<String> expansionPaths = new ArrayList<>();
        expansionPaths.add("/content/documents/myproject/folderA/folderAA");
        final ExpandedNodeHierarchy hierarchy = createExpandedNodeHierarchy(rootContentNode.getSession(),
                "/content/documents/myproject", expansionPaths);

        assertEquals("/content/documents/myproject", hierarchy.getNode().getPath());
        assertEquals(1, hierarchy.getChildren().size());
        final ExpandedNodeHierarchy oneLevelDeep = hierarchy.getChildren().get("/content/documents/myproject/folderA");
        assertEquals("/content/documents/myproject/folderA", oneLevelDeep.getNode().getPath());
        assertEquals(1, oneLevelDeep.getChildren().size());

        final ExpandedNodeHierarchy twoLevelDeep = oneLevelDeep.getChildren().get("/content/documents/myproject/folderA/folderAA");
        assertEquals("/content/documents/myproject/folderA/folderAA", twoLevelDeep.getNode().getPath());
        assertEquals(0, twoLevelDeep.getChildren().size());
    }

    @Test
    public void expandedNodeHierarchy_for_path_that_does_not_start_with_root_content_path_gets_ignored() throws RepositoryException {
        final MockNode rootContentNode = createMockContent();
        List<String> expansionPaths = new ArrayList<>();
        expansionPaths.add("/content/documents/foo/bar/lux");
        final ExpandedNodeHierarchy hierarchy = createExpandedNodeHierarchy(rootContentNode.getSession(),
                "/content/documents/myproject", expansionPaths);

        assertEquals("/content/documents/myproject", hierarchy.getNode().getPath());
        assertEquals(0, hierarchy.getChildren().size());
    }


    @Test
    public void expandedNodeHierarchy_for_path_that_points_to_non_existing_descendant_of_root_content_path() throws RepositoryException {
        final MockNode rootContentNode = createMockContent();
        List<String> expansionPaths = new ArrayList<>();
        expansionPaths.add("/content/documents/myproject/folderA/folderAA/non/existing/descendant");
        final ExpandedNodeHierarchy hierarchy = createExpandedNodeHierarchy(rootContentNode.getSession(),
                "/content/documents/myproject", expansionPaths);
        final ExpandedNodeHierarchy level1 = hierarchy.getChildren().get("/content/documents/myproject/folderA");
        assertNotNull(level1);
        final ExpandedNodeHierarchy level2 = level1.getChildren().get("/content/documents/myproject/folderA/folderAA");
        assertNotNull(level2);
        assertEquals("Below folderAA there are no nodes", 0, level2.getChildren().size());
    }


    @Test
    public void expandedNodeHierarchy_for_multiple_expansion_paths() throws RepositoryException {
        final MockNode rootContentNode = createMockContent();
        List<String> expansionPaths = new ArrayList<>();
        expansionPaths.add("/content/documents/myproject/folderA");
        expansionPaths.add("/content/documents/myproject/folderB");
        final ExpandedNodeHierarchy hierarchy = createExpandedNodeHierarchy(rootContentNode.getSession(),
                "/content/documents/myproject", expansionPaths);

        assertEquals("/content/documents/myproject", hierarchy.getNode().getPath());
        assertEquals(2, hierarchy.getChildren().size());

        final ExpandedNodeHierarchy oneLevelDeepA = hierarchy.getChildren().get("/content/documents/myproject/folderA");
        assertEquals("/content/documents/myproject/folderA", oneLevelDeepA.getNode().getPath());
        assertEquals(0, oneLevelDeepA.getChildren().size());

        final ExpandedNodeHierarchy oneLevelDeepB = hierarchy.getChildren().get("/content/documents/myproject/folderB");
        assertEquals("/content/documents/myproject/folderB", oneLevelDeepB.getNode().getPath());
        assertEquals(0, oneLevelDeepB.getChildren().size());
    }

    @Test
    public void expandedNodeHierarchy_for_multiple_expansion_paths_with_some_invalid() throws RepositoryException {
        final MockNode rootContentNode = createMockContent();
        List<String> expansionPaths = new ArrayList<>();
        expansionPaths.add("/content/documents/myproject/folderA");
        expansionPaths.add("/content/documents/myproject/folderB");
        expansionPaths.add("/content/documents/myproject/foo");
        expansionPaths.add("/content/documents/foo/bar");
        final ExpandedNodeHierarchy hierarchy = createExpandedNodeHierarchy(rootContentNode.getSession(),
                "/content/documents/myproject", expansionPaths);

        assertEquals("/content/documents/myproject", hierarchy.getNode().getPath());
        assertEquals(2, hierarchy.getChildren().size());

        final ExpandedNodeHierarchy oneLevelDeepA = hierarchy.getChildren().get("/content/documents/myproject/folderA");
        assertEquals("/content/documents/myproject/folderA", oneLevelDeepA.getNode().getPath());
        assertEquals(0, oneLevelDeepA.getChildren().size());

        final ExpandedNodeHierarchy oneLevelDeepB = hierarchy.getChildren().get("/content/documents/myproject/folderB");
        assertEquals("/content/documents/myproject/folderB", oneLevelDeepB.getNode().getPath());
        assertEquals(0, oneLevelDeepB.getChildren().size());
    }


    @Test
    public void expandedNodeHierarchy_for_multiple_partly_overlapping_expansion_paths_get_merged() throws RepositoryException {
        final MockNode rootContentNode = createMockContent();
        List<String> expansionPaths = new ArrayList<>();
        expansionPaths.add("/content/documents/myproject/folderA/folderAA");
        expansionPaths.add("/content/documents/myproject/folderA");
        expansionPaths.add("/content/documents/myproject/folderA/folderAB");
        final ExpandedNodeHierarchy hierarchy = createExpandedNodeHierarchy(rootContentNode.getSession(),
                "/content/documents/myproject", expansionPaths);

        final ExpandedNodeHierarchy folderA = hierarchy.getChildren().get("/content/documents/myproject/folderA");
        assertEquals("/content/documents/myproject/folderA", folderA.getNode().getPath());
        assertEquals(2, folderA.getChildren().size());

        assertTrue(folderA.getChildren().containsKey("/content/documents/myproject/folderA/folderAA"));
        assertTrue(folderA.getChildren().containsKey("/content/documents/myproject/folderA/folderAB"));

    }

    private MockNode createMockContent() throws RepositoryException {
        final MockNode root = MockNode.root();
        final MockNode rootContentNode = root.addNode("content", HippoStdNodeType.NT_FOLDER)
                .addNode("documents", HippoStdNodeType.NT_FOLDER)
                .addNode("myproject", HippoStdNodeType.NT_FOLDER);

        final MockNode folderA = rootContentNode.addNode("folderA", HippoStdNodeType.NT_FOLDER);
        folderA.addNode("docA", HippoNodeType.NT_DOCUMENT);
        folderA.addNode("docB", HippoNodeType.NT_DOCUMENT);

        final MockNode folderAA = folderA.addNode("folderAA", HippoStdNodeType.NT_FOLDER);
        folderAA.addNode("doc1", HippoNodeType.NT_DOCUMENT);
        folderAA.addNode("doc2", HippoNodeType.NT_DOCUMENT);

        final MockNode folderAB = folderA.addNode("folderAB", HippoStdNodeType.NT_FOLDER);
        folderAB.addNode("doc1", HippoNodeType.NT_DOCUMENT);
        folderAB.addNode("doc2", HippoNodeType.NT_DOCUMENT);

        rootContentNode.addNode("folderB", HippoStdNodeType.NT_FOLDER);
        return rootContentNode;
    }


}
