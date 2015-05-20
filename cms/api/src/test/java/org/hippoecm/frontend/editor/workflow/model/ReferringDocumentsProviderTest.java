/*
 * Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow.model;

import java.util.List;

import javax.jcr.Node;

import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ReferringDocumentsProviderTest extends RepositoryTestCase {

    @Test(expected = IllegalArgumentException.class)
    public void testReferringDocumentsStatementDepthAtLeast1() {
         ReferringDocumentsProvider.createReferrersStatement(false, "cafebabe-cafe-babe-cafe-babecafebabe", 0);
    }

    @Test
    public void testReferringDocumentsStatement() {
        final String referrersStatement1 = ReferringDocumentsProvider.createReferrersStatement(false, "cafebabe-cafe-babe-cafe-babecafebabe", 1);
        assertEquals("//element(*,hippo:handle)[*/hippo:availability='live' and (*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe')] order by @jcr:name ascending",
                referrersStatement1);
        final String referrersStatement2 = ReferringDocumentsProvider.createReferrersStatement(false, "cafebabe-cafe-babe-cafe-babecafebabe", 2);
        assertEquals("//element(*,hippo:handle)[*/hippo:availability='live' and (*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe' or */*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe')] order by @jcr:name ascending",
                referrersStatement2);

        final String referrersStatement3 = ReferringDocumentsProvider.createReferrersStatement(false, "cafebabe-cafe-babe-cafe-babecafebabe", 3);
        assertEquals("//element(*,hippo:handle)[*/hippo:availability='live' and (*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe' or */*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe' or */*/*/@hippo:docbase='cafebabe-cafe-babe-cafe-babecafebabe')] order by @jcr:name ascending",
                referrersStatement3);
    }

    @Test
    public void testBasicReferringDocument() throws Exception {
        String[] content = new String[]{
                "/test", "nt:unstructured",
                    "/test/content", "hippostd:folder",
                        "/test/content/target", "hippo:handle",
                            "jcr:mixinTypes", "mix:referenceable",
                            "/test/content/target/target", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "/test/content/target/target/description", "hippostd:html",
                                    "hippostd:content", "text",
                        "/test/content/linkInPreview", "hippo:handle",
                            "jcr:mixinTypes", "mix:referenceable",
                            "/test/content/linkInPreview/linkInPreview", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "hippo:availability", "preview",
                                "/test/content/linkInPreview/linkInPreview/description", "hippostd:html",
                                    "hippostd:content", "text",
                                    "/test/content/linkInPreview/linkInPreview/description/some-link", "hippo:facetselect",
                                        "hippo:facets", null,
                                        "hippo:modes", null,
                                        "hippo:values", null,
                                        "hippo:docbase", "/test/content/target"
        };

        build(content, session);
        session.save();

        Node node = session.getRootNode().getNode("test/content/target");
        String[] nodePaths = getReferringNodePaths(node, true);
        assertArrayEquals(new String[] { "/test/content/linkInPreview" }, nodePaths);

        nodePaths = getReferringNodePaths(node, false);
        assertArrayEquals(new String[] {}, nodePaths);
    }

    @Test
    @Ignore
    public void testUnpublishedVariantWithUpdatedPreview() throws Exception {
        /* This state occurs when a user de-publishes a document, and then updates all links from 'oldTarget' to
         * 'newTarget'. See also CMS7-9221.
         */
        String[] content = new String[]{
                "/test", "nt:unstructured",
                    "/test/content", "hippostd:folder",
                        "/test/content/oldTarget", "hippo:handle",
                            "jcr:mixinTypes", "mix:referenceable",
                            "/test/content/oldTarget/oldTarget", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "/test/content/oldTarget/oldTarget/description", "hippostd:html",
                                    "hippostd:content", "text",
                        "/test/content/newTarget", "hippo:handle",
                            "jcr:mixinTypes", "mix:referenceable",
                            "/test/content/newTarget/newTarget", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "/test/content/newTarget/newTarget/description", "hippostd:html",
                                    "hippostd:content", "text",
                        "/test/content/unpublishedDoc", "hippo:handle",
                            "jcr:mixinTypes", "mix:referenceable",
                            "/test/content/unpublishedDoc/unpublishedDoc", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "hippo:availability", "preview",
                                "/test/content/unpublishedDoc/unpublishedDoc/description", "hippostd:html",
                                    "hippostd:content", "text",
                                    "/test/content/unpublishedDoc/unpublishedDoc/description/some-link", "hippo:facetselect",
                                        "hippo:facets", null,
                                        "hippo:modes", null,
                                        "hippo:values", null,
                                        "hippo:docbase", "/test/content/newTarget",
                            "/test/content/unpublishedDoc/unpublishedDoc", "hippo:document",
                                "jcr:mixinTypes", "mix:referenceable,hippostd:relaxed",
                                "hippo:availability", null,
                                "/test/content/unpublishedDoc/unpublishedDoc[2]/description", "hippostd:html",
                                    "hippostd:content", "text",
                                    "/test/content/unpublishedDoc/unpublishedDoc[2]/description/some-link", "hippo:facetselect",
                                        "hippo:facets", null,
                                        "hippo:modes", null,
                                        "hippo:values", null,
                                        "hippo:docbase", "/test/content/oldTarget"
        };

        build(content, session);
        session.save();

        Node node = session.getRootNode().getNode("test/content/newTarget");
        String[] nodePaths = getReferringNodePaths(node, true);
        assertArrayEquals(new String[] {"/test/content/unpublishedDoc"}, nodePaths);

        node = session.getRootNode().getNode("test/content/oldTarget");
        nodePaths = getReferringNodePaths(node, true);
        assertArrayEquals(new String[] {}, nodePaths);
    }

    private String[] getReferringNodePaths(Node node, final boolean retrieveUnpublished) throws Exception {
        List<Node> referringNodes = ReferringDocumentsProvider.getReferrersSortedByName(node, retrieveUnpublished, 100);

        String[] nodePaths = new String[referringNodes.size()];
        for (int i = 0; i < referringNodes.size(); i++) {
            nodePaths[i] = referringNodes.get(i).getPath();
        }

        return nodePaths;
    }
}
