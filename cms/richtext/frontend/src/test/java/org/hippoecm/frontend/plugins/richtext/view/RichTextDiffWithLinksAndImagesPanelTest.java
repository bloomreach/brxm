/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.view;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.mock.MockHomePage;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketModel;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketNodeFactory;
import org.hippoecm.frontend.plugins.richtext.model.RichTextModelFactory;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorConfig;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorFactory;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorImpl;
import org.onehippo.cms7.services.htmlprocessor.richtext.URLEncoder;
import org.onehippo.cms7.services.htmlprocessor.richtext.model.RichTextProcessorModel;
import org.onehippo.cms7.services.htmlprocessor.serialize.HtmlSerializer;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link RichTextDiffWithLinksAndImagesPanel}.
 */
public class RichTextDiffWithLinksAndImagesPanelTest {

    private MockNode root;
    private RichTextModelFactory modelFactory;

    @Before
    public void setUp() throws RepositoryException, NoSuchMethodException {
        final HippoTester tester = new HippoTester();
        tester.startPage(MockHomePage.class);

        final javax.jcr.Session session = UserSession.get().getJcrSession();
        root = (MockNode) session.getRootNode();

        final HtmlProcessorConfig processorConfig = new HtmlProcessorConfig();
        processorConfig.setCharset("UTF-8");
        processorConfig.setSerializer(HtmlSerializer.SIMPLE);
        processorConfig.setOmitComments(false);
        processorConfig.setFilter(false);

        final HtmlProcessorFactory processorFactory = () -> new HtmlProcessorImpl(processorConfig);
        modelFactory = new RichTextModelFactory("default") {
            @Override
            public IModel<String> create(final IModel<String> valueModel, final IModel<Node> nodeModel) {
                return new RichTextModel(new RichTextProcessorModel(WicketModel.of(valueModel),
                                                                    WicketModel.of(nodeModel),
                                                                    processorFactory,
                                                                    WicketNodeFactory.INSTANCE,
                                                                    URLEncoder.OPAQUE));
            }
        };
    }

    @Test
    public void sameTextShowsNoChanges() throws RepositoryException {
        Node base = addHtmlNode(root, "base", "Some text");
        Node current = addHtmlNode(root, "current", "Some text");
        String diff = createDiff(base, current);
        assertEquals(htmlEncode("<html>Some text</html>\n"), diff);
    }

    @Test
    public void sameCharacterDifferentEncodingShowsNoChanges() throws RepositoryException {
        Node base = addHtmlNode(root, "base", "e &#101; ' ' &apos; &apos; &#39; &#39;");
        Node current = addHtmlNode(root, "current", "&#101; e &apos; &#39; ' &#39; ' &apos;");
        String diff = createDiff(base, current);
        assertEquals(htmlEncode("<html>e e &#039; &#039; &#039; &#039; &#039; &#039;</html>\n"), diff);
    }

    @Test
    public void differentTextShowsChanges() throws RepositoryException {
        Node base = addHtmlNode(root, "base", "aap");
        Node current = addHtmlNode(root, "current", "noot");
        String diff = createDiff(base, current);
        assertEquals(htmlEncode("<html>\n"
                + "<span class=\"diff-html-removed\" id=\"removed-null-0\" previous=\"first-null\" changeId=\"removed-null-0\" next=\"added-null-0\">aap</span>"
                + "<span class=\"diff-html-added\" id=\"added-null-0\" previous=\"removed-null-0\" changeId=\"added-null-0\" next=\"last-null\">noot</span>\n"
                + "</html>\n"), diff);
    }

    @Test
    public void addedImageShowsChanges() throws RepositoryException {
        MockNode imageHandle = addImage(root, "image.jpg");

        Node base = addHtmlNode(root, "base", "text");

        Node current = addHtmlNode(root, "current", "text <img src=\"image.jpg/{_document}/hippogallery:thumbnail\"/>");
        addFacet(current, imageHandle);

        String diff = createDiff(base, current);
        assertEquals(htmlEncode("<html>text "
                + "<span class=\"diff-html-added\" id=\"added-null-0\" previous=\"first-null\" changeId=\"added-null-0\" next=\"last-null\">"
                + "<img src=\"binaries/image.jpg/image.jpg/hippogallery:thumbnail\" data-uuid=\""+ imageHandle.getIdentifier() +"\" data-type=\"hippogallery:thumbnail\" changeType=\"diff-added-image\">"
                + "</span>\n"
                + "</html>\n"), diff);
    }

    @Test
    public void removedImageShowsChanges() throws RepositoryException {
        MockNode imageHandle = addImage(root, "image.jpg");

        Node base = addHtmlNode(root, "base", "text <img src=\"image.jpg/{_document}/hippogallery:thumbnail\"/>");
        addFacet(base, imageHandle);

        Node current = addHtmlNode(root, "current", "text");

        String diff = createDiff(base, current);

        assertEquals(htmlEncode("<html>text"
                + "<span class=\"diff-html-removed\" previous=\"first-null\" changeId=\"removed-null-0\" next=\"last-null\"> </span>"
                + "<span class=\"diff-html-removed\" id=\"removed-null-0\" previous=\"first-null\" changeId=\"removed-null-0\" next=\"last-null\">"
                + "<img src=\"binaries/image.jpg/image.jpg/hippogallery:thumbnail\" data-uuid=\""+ imageHandle.getIdentifier() +"\" data-type=\"hippogallery:thumbnail\" changeType=\"diff-removed-image\">"
                + "</span>\n"
                + "</html>\n"), diff);
    }

    private MockNode addImage(final MockNode node, final String name) throws RepositoryException {
        MockNode imageHandle = node.addNode(name, "hippo:handle");
        MockNode imageSet = imageHandle.addNode(name, "hippogallery:imageset");
        imageSet.setPrimaryItemName("hippogallery:thumbnail");
        imageSet.addNode("hippogallery:thumbnail", "hippo:resource");
        return imageHandle;
    }

    private void addFacet(final Node current, final MockNode imageHandle) throws RepositoryException {
        Node facet = current.addNode("image.jpg", HippoNodeType.NT_FACETSELECT);
        facet.setProperty(HippoNodeType.HIPPO_DOCBASE, imageHandle.getIdentifier());
    }

    private String createDiff(Node base, Node current) {
        final IModel<Node> baseModel = createNodeModel(base);
        final IModel<Node> currentModel = createNodeModel(current);
        final RichTextDiffWithLinksAndImagesPanel panel = new RichTextDiffWithLinksAndImagesPanel("id",
                                                                                                  baseModel,
                                                                                                  currentModel,
                                                                                                  null,
                                                                                                  null,
                                                                                                  modelFactory);

        return panel.get(RichTextDiffWithLinksAndImagesPanel.WICKET_ID_VIEW).getDefaultModelObjectAsString();
    }

    private static Node addHtmlNode(Node node, String name, String content) throws RepositoryException {
        Node htmlNode = node.addNode(name, HippoStdNodeType.NT_HTML);
        htmlNode.setProperty(HippoStdNodeType.HIPPOSTD_CONTENT, content);
        return htmlNode;
    }

    private static IModel<Node> createNodeModel(Node node) {
        IModel<Node> model = createMock(IModel.class);
        expect(model.getObject()).andReturn(node).anyTimes();
        replay(model);
        return model;
    }

    private static String htmlEncode(final String text) {
        return text.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
    }

}
