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
package org.onehippo.cms7.services.processor.richtext.visit;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.easymock.EasyMock;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.html.visit.Tag;
import org.onehippo.cms7.services.processor.richtext.TestUtil;
import org.onehippo.cms7.services.processor.richtext.jcr.JcrNodeFactory;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LinkVisitorTest {

    private MockNode root;
    private MockNode document;
    private Model<Node> documentModel;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();
        document = root.addNode("document", "hippo:document");

        final JcrNodeFactory factory = JcrNodeFactory.of(root);
        documentModel = factory.getNodeModelByNode(document);
    }

    @Test
    public void testVisitsOnlyAnchorElements() throws Exception {
        final LinkVisitor visitor = new LinkVisitor(documentModel);

        final Tag anchor = EasyMock.createMock(Tag.class);
        expect(anchor.getName()).andReturn("div").times(2);
        EasyMock.replay(anchor);

        visitor.onRead(null, anchor);
        visitor.onWrite(null, anchor);

        EasyMock.verify(anchor);
    }

    @Test
    public void anchorDoesNotChange() throws RepositoryException {
        assertNoChanges("<a name=\"foo\">anchor</a>");
    }

    @Test
    public void relativeLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"somepage.html\">relative link</a>");
    }

    @Test
    public void relativeLinkWithPathDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"../somepage.html\">relative link with path</a>");
    }

    @Test
    public void relativeLinkWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"2*3=6.html\">Link to file with illegal JCR characters in its name</a>");
    }

    @Test
    public void externalHttpLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"http://www.example.com\">external link</a>");
    }

    @Test
    public void externalLinkWithPortDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"http://www.example.com:8080\">external link with port</a>");
    }

    @Test
    public void externalHttpOnlyLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"http://\">strange external link</a>");
    }

    @Test
    public void linkWithEmptyHrefDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"\">link with empty href</a>");
    }

    @Test
    public void linkWithEmptyHrefAndUuidDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"\" data-uuid=\"\">link with empty href and uuid</a>");
    }

    @Test
    public void externalFtpLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"ftp://www.example.com\">external FTP link</a>");
    }

    @Test
    public void externalFtpOnlyLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"ftp://\">strange external FTP link</a>");
    }

    @Test
    public void emptyLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a>strange empty link</a>");
    }

    @Test
    public void getLinkChildNodeNameIsRewrittenToUuid() throws RepositoryException {
        addChildFacetNode("linked-node", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        final Tag link = createLink("linked-node");
        read(link);
        assertLink(link, "http://", "d1b804c0-cf19-451f-8c0f-184da74289e4");
    }

    @Test
    public void getLinkWithEscapedNameIsRewrittenToUuid() throws RepositoryException {
        final String name = "A name that needs 'encoding'";
        final String linkTargetName = NodeNameCodec.encode(name, true);
        assertFalse(name.equals(linkTargetName));

        addChildFacetNode(linkTargetName, "d1b804c0-cf19-451f-8c0f-184da74289e4");

        final Tag link = createLink(linkTargetName);
        read(link);
        assertLink(link, "http://", "d1b804c0-cf19-451f-8c0f-184da74289e4");
    }

    @Test
    public void setNewLinkUuidCreatesChildNodeAndReplacesUuid() throws RepositoryException {
        final Node linkTarget = root.addNode("linked-node", "nt:unstructured");
        final Tag link = createLink("http://", linkTarget.getIdentifier());
        write(link);

        assertTrue("Text with new link UUID should create a child facet node", document.hasNode("linked-node"));
        assertEquals("Text with new link UUID should create exactly one child facet node", 1, document.getNodes().getSize());

        final Node child = document.getNode("linked-node");
        assertEquals(linkTarget.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
        assertEquals("linked-node", link.getAttribute("href"));
    }

    @Test
    public void setExistingLinkUuidDoesNotCreateChildNode() throws RepositoryException {
        final Node linkTarget = root.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        final Tag link = createLink("http://", linkTarget.getIdentifier());
        write(link);

        assertEquals("Text with existing link UUID should reuse existing child facet node", 1, document.getNodes().getSize());

        final Node child = document.getNode("linked-node");
        assertEquals(linkTarget.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
        assertEquals("linked-node", link.getAttribute("href"));
    }

    @Test
    public void setExternalLinkWithUuidIgnoresUuid() throws RepositoryException {
        final Node document1 = root.addNode("document1", "nt:unstructured");
        final Tag link = createLink("http://www.example.com", document1.getIdentifier());

        write(link);
        assertEquals("No child facet nodes should have been created", 0, document.getNodes().getSize());
        assertEquals("http://www.example.com", link.getAttribute("href"));
    }

    @Test
    public void setTextWithoutAnyLinksRemovesAllChildNodes() throws RepositoryException {
        final Node linkTarget = root.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        write(TestUtil.createTag("div"));
        assertEquals("all child facet nodes should have been removed", 0, document.getNodes().getSize());
    }

    @Test
    public void setEmptyTextRemovesAllChildNodes() throws RepositoryException {
        final Node linkTarget = root.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        write(TestUtil.createTag(""));
        assertEquals("all child facet nodes should have been removed", 0, document.getNodes().getSize());
    }

    @Test
    public void setNullTextRemovesAllChildNodes() throws RepositoryException {
        final Node linkTarget = root.addNode("linked-node", HippoNodeType.NT_FACETSELECT);
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        write(TestUtil.createTag(null));
        assertEquals("all child facet nodes should have been removed", 0, document.getNodes().getSize());
    }

    @Test
    public void setTextWithLinksRemovesUnusedChildNodes() throws RepositoryException {
        final Node document1 = root.addNode("document1", "nt:unstructured");
        final Node document2 = root.addNode("document2", "nt:unstructured");

        addChildFacetNode("document1", document1.getIdentifier());
        addChildFacetNode("document2", document2.getIdentifier());

        final Tag link = createLink("http://", document1.getIdentifier());
        write(link);

        final NodeIterator children = document.getNodes();
        assertEquals("Document node should have only one facet child node", 1, document.getNodes().getSize());

        final Node child = children.nextNode();
        assertEquals(document1.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals(child.getName(), link.getAttribute("href"));
    }

    @Test
    public void setTextWithLinksRemovesUnusedChildNodesWithAdditionalSuffix() throws RepositoryException {
        final Node document1 = root.addNode("document1", "nt:unstructured");

        addChildFacetNode("document", document1.getIdentifier());
        addChildFacetNode("document_1", document1.getIdentifier());

        final Tag link = createLink("http://", document1.getIdentifier());
        write(link);

        final NodeIterator children = document.getNodes();
        assertEquals("Document node should have only one facet child node", 1, children.getSize());

        final Node child = children.nextNode();
        assertLink(link, child.getName());
        assertEquals(document1.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
    }

    @Test
    public void setEmptyTextRemovesPreviouslyCreatedChildNodes() throws RepositoryException {
        final Node linked = root.addNode("linked", "nt:unstructured");
        final Tag link = createLink("http://", linked.getIdentifier());
        write(link);

        assertEquals("Child facet node should have been added", 1, document.getNodes().getSize());

        final Tag empty = TestUtil.createTag("");
        write(empty);
        assertEquals("Child facet node should have been removed", 0, document.getNodes().getSize());
    }

    @Test
    public void setDocumentsWithTheSameName() throws RepositoryException {
        final Node doc1 = root.addNode("doc", "nt:unstructured");
        final Node doc2 = root.addNode("doc", "nt:unstructured");

        final Tag div = TestUtil.createTag("div");
        final Tag link1 = createLink("http://", doc1.getIdentifier());
        final Tag link2 = createLink("http://", doc2.getIdentifier());

        final LinkVisitor visitor = new LinkVisitor(documentModel);
        visitor.onWrite(div, link1);
        visitor.onWrite(div, link2);

        assertTrue("facetselect node doc exists", document.hasNode("doc"));
        assertTrue("facetselect node doc_1 exists", document.hasNode("doc_1"));

        assertLink(link1, "doc");
        assertLink(link2, "doc_1");
    }


    // Helper methods
    private Tag createLink(final String href, final String uuid) {
        final Tag link = TestUtil.createTag("a");
        link.addAttribute("href", href);
        link.addAttribute("data-uuid", uuid);
        return link;
    }

    private void assertLink(final Tag link, final String href) {
        assertEquals("a", link.getName());
        assertEquals(href, link.getAttribute("href"));
    }

    private void assertLink(final Tag link, final String href, final String uuid) {
        assertEquals("a", link.getName());
        assertEquals(href, link.getAttribute("href"));
        assertEquals(uuid, link.getAttribute("data-uuid"));
    }

    private void read(final Tag link) throws RepositoryException {
        final LinkVisitor visitor = new LinkVisitor(documentModel);
        visitor.onRead(null, link);
    }

    private void write(final Tag link) throws RepositoryException {
        final LinkVisitor visitor = new LinkVisitor(documentModel);
        visitor.onWrite(null, link);
    }

    private void assertNoChanges(final String href) throws RepositoryException {
        final LinkVisitor visitor = new LinkVisitor(documentModel);
        assertNoChangesReading(href, visitor);
        assertNoChangesWriting(href, visitor);
    }

    private void assertNoChangesReading(final String href, final LinkVisitor visitor) throws RepositoryException {
        final Tag image = createLink(href);

        final long childNodesBeforeRead = document.getNodes().getSize();
        visitor.onRead(null, image);
        assertEquals("Value of src attribute should not have changed during read", href, image.getAttribute("href"));
        assertEquals("Number of child facet nodes should not have changed during read",
                     childNodesBeforeRead, document.getNodes().getSize());
    }

    private void assertNoChangesWriting(final String href, final LinkVisitor visitor) throws RepositoryException {
        final Tag link = createLink(href);

        final long childNodesBeforeWrite = document.getNodes().getSize();
        visitor.onWrite(null, link);
        assertEquals("Value of href attribute should not have changed during write", href, link.getAttribute("href"));
        assertEquals("Number of child facet nodes should not have changed during write",
                     childNodesBeforeWrite, document.getNodes().getSize());
    }

    private Tag createLink(final String href) {
        final Tag link = TestUtil.createTag("a");
        link.addAttribute("href", href);
        return link;
    }

    private void addChildFacetNode(final String name, final String uuid) throws RepositoryException {
        TestUtil.addChildFacetNode(document, name, uuid);
    }
}
