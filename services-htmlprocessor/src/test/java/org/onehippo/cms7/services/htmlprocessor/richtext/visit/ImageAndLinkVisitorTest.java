/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.richtext.visit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.Tag;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.TestUtil;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.JcrNodeFactory;
import org.onehippo.cms7.services.htmlprocessor.util.FacetUtil;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ImageAndLinkVisitorTest {

    private MockNode document;
    private Model<Node> documentModel;
    private MockNode documents;
    private MockNode images;

    @Before
    public void setUp() throws Exception {
        final MockNode root = MockNode.root();
        document = root.addNode("document", "hippo:document");

        final JcrNodeFactory factory = JcrNodeFactory.of(root);
        documentModel = factory.getNodeModelByNode(document);

        documents = root.addNode("documents", "nt:folder");
        images = root.addNode("images", "nt:folder");
    }

    @Test
    public void readNonExistingLinkAndImage() throws Exception {
        final ImageAndLinkVisitor visitor = new ImageAndLinkVisitor(documentModel, src -> src);

        final Tag link = TestUtil.createTag("a");
        link.addAttribute("href", "non-existing-link");

        final Tag image = TestUtil.createTag("img");
        image.addAttribute("src", "non-existing-image");

        read(visitor, link, image);
        assertEquals(0, document.getNodes().getSize());

        assertEquals("non-existing-link", link.getAttribute("href"));
        assertNull(link.getAttribute("data-uuid"));

        assertEquals("non-existing-image", image.getAttribute("src"));
        assertNull(image.getAttribute("data-uuid"));
    }

    @Test
    public void writeNonExistingLinkAndImage() throws Exception {
        final ImageAndLinkVisitor visitor = new ImageAndLinkVisitor(documentModel, src -> src);

        final Tag link = TestUtil.createTag("a");
        link.addAttribute("href", "non-existing-link");

        final Tag image = TestUtil.createTag("img");
        image.addAttribute("src", "non-existing-image");

        write(visitor, link, image);
        assertEquals(0, document.getNodes().getSize());

        assertEquals("non-existing-link", link.getAttribute("href"));
        assertNull(link.getAttribute("data-uuid"));

        assertEquals("non-existing-image", image.getAttribute("src"));
        assertNull(image.getAttribute("data-uuid"));
    }

    @Test
    public void writeNewImageAndLinkWithSameName() throws Exception {
        final Node doc1 = documents.addNode("node1", "nt:unstructured");
        final Node img1 = images.addNode("node1", "nt:unstructured");
        final ImageAndLinkVisitor visitor = new ImageAndLinkVisitor(documentModel, src -> src);

        final Tag link = TestUtil.createTag("a");
        link.addAttribute("href", "new-link");
        link.addAttribute("data-uuid", doc1.getIdentifier());

        final Tag image = TestUtil.createTag("img");
        image.addAttribute("src", "new-image");
        image.addAttribute("data-uuid", img1.getIdentifier());

        write(visitor, link, image);

        assertEquals(2, document.getNodes().getSize());
        assertFacetTagAfterWrite("node1", doc1, link, "href");
        assertFacetTagAfterWrite("node1_1", img1, image, "src");
    }

    @Test
    public void writeNewImageWithSameNameAsExistingLink() throws Exception {
        final Node doc1 = documents.addNode("node1", "nt:unstructured");
        final Node img1 = images.addNode("node1", "nt:unstructured");
        addChildFacetNode("node1", doc1.getIdentifier());

        final ImageAndLinkVisitor visitor = new ImageAndLinkVisitor(documentModel, src -> src);

        final Tag link = TestUtil.createTag("a");
        link.addAttribute("href", "existing-link");
        link.addAttribute("data-uuid", doc1.getIdentifier());

        final Tag image = TestUtil.createTag("img");
        image.addAttribute("src", "new-img");
        image.addAttribute("data-uuid", img1.getIdentifier());

        write(visitor, link, image);

        assertEquals(2, document.getNodes().getSize());
        assertFacetTagAfterWrite("node1", doc1, link, "href");
        assertFacetTagAfterWrite("node1_1", img1, image, "src");
    }

    @Test
    public void readWriteTest() throws Exception {
        final Node doc1 = documents.addNode("node1", "nt:unstructured");
        final Node img1 = images.addNode("node1", "nt:unstructured");
        addChildFacetNode("node1", doc1.getIdentifier());

        final ImageAndLinkVisitor visitor = new ImageAndLinkVisitor(documentModel, src -> src);

        final Tag link = TestUtil.createTag("a");
        link.addAttribute("href", "existing-link");
        link.addAttribute("data-uuid", doc1.getIdentifier());

        final Tag image = TestUtil.createTag("img");
        image.addAttribute("src", "new-img");
        image.addAttribute("data-uuid", img1.getIdentifier());

        write(visitor, link, image);
        read(visitor, link, image);

        assertEquals(2, document.getNodes().getSize());
        assertEquals("http://", link.getAttribute("href"));
        assertEquals(FacetUtil.getChildDocBaseOrNull(document, "node1"), link.getAttribute("data-uuid"));
        assertEquals("node1_1", image.getAttribute("src"));
        assertEquals(FacetUtil.getChildDocBaseOrNull(document, "node1_1"), image.getAttribute("data-uuid"));

        final Tag link2 = TestUtil.createTag("a");
        link2.addAttribute("href", "new-link");
        link2.addAttribute("data-uuid", doc1.getIdentifier());

        write(visitor, link, link2, image);
        assertEquals(3, document.getNodes().getSize());
        assertFacetTagAfterWrite("node1", doc1, link, "href");
        assertFacetTagAfterWrite("node1_1", img1, image, "src");
        assertFacetTagAfterWrite("node1_2", doc1, link2, "href");

        read(visitor, link, link2, image);
        assertEquals(3, document.getNodes().getSize());
        assertEquals("http://", link.getAttribute("href"));
        assertEquals(FacetUtil.getChildDocBaseOrNull(document, "node1"), link.getAttribute("data-uuid"));
        assertEquals("node1_1", image.getAttribute("src"));
        assertEquals(FacetUtil.getChildDocBaseOrNull(document, "node1_1"), image.getAttribute("data-uuid"));
        assertEquals("http://", link2.getAttribute("href"));
        assertEquals(FacetUtil.getChildDocBaseOrNull(document, "node1_2"), link2.getAttribute("data-uuid"));
    }

    private void read(final ImageAndLinkVisitor visitor, final Tag... tags) throws RepositoryException {
        visitor.before();
        for (final Tag tag : tags) {
            visitor.onRead(null, tag);
        }
        visitor.after();
    }

    private void write(final ImageAndLinkVisitor visitor, final Tag... tags) throws RepositoryException {
        visitor.before();
        for (final Tag tag : tags) {
            visitor.onWrite(null, tag);
        }
        visitor.after();
    }

    private void assertFacetTagAfterWrite(final String name, final Node node, final Tag tag, final String attr) throws RepositoryException {
        assertEquals(name, tag.getAttribute(attr));
        assertNull(tag.getAttribute("data-uuid"));
        assertEquals(node.getIdentifier(), FacetUtil.getChildDocBaseOrNull(document, name));
    }

    private void addChildFacetNode(final String name, final String uuid) throws RepositoryException {
        TestUtil.addChildFacetNode(document, name, uuid);
    }

}
