/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.model;

import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.easymock.EasyMock;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextImage;
import org.hippoecm.frontend.plugins.richtext.jcr.RichTextImageURLProvider;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link RichTextImageMetaDataModel}
 */
public class RichTextImageMetaDataModelTest {

    private IModel<String> delegate;
    private RichTextImageMetaDataModel model;

    @Before
    public void setUp() {
        delegate = new Model<>("");
        model = new RichTextImageMetaDataModel(delegate, new PrefixingImageUrlProvider("binaries"));
    }

    @Test
    public void getTextChangesSrcAndAddsFacetSelectAndType() {
        delegate.setObject("<img src=\"image.png/{_document}/hippogallery:original\"/>");
        assertEquals("<img src=\"binaries/image.png/{_document}/hippogallery:original\" data-facetselect=\"image.png/{_document}/hippogallery:original\" data-type=\"hippogallery:original\"/>", model.getObject());
    }

    @Test
    public void setTextRestoresSrcAndRemovesFacetSelectAndType() {
        model.setObject("<img src=\"binaries/image.png/{_document}/hippogallery:original\" data-facetselect=\"image.png/{_document}/hippogallery:original\" data-type=\"hippogallery:original\"/>");
        assertEquals("<img src=\"image.png/{_document}/hippogallery:original\"/>", delegate.getObject());
    }

    @Test
    public void getSrcWithoutVariantOmitsType() {
        delegate.setObject("<img src=\"image.png\"/>");
        assertEquals("<img src=\"binaries/image.png\" data-facetselect=\"image.png\"/>", model.getObject());
    }

    @Test
    public void setSrcWithoutVariantRemovesFacetSelect() {
        model.setObject("<img src=\"binaries/image.png\" data-facetselect=\"image.png\"/>");
        assertEquals("<img src=\"image.png\"/>", delegate.getObject());
    }

    @Test
    public void getAdditionalImgAttributesAreNotChanged() {
        delegate.setObject("<img src=\"image.png/{_document}/hippogallery:original\" align=\"right\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\"/>");
        assertEquals("<img src=\"binaries/image.png/{_document}/hippogallery:original\" data-facetselect=\"image.png/{_document}/hippogallery:original\" data-type=\"hippogallery:original\" align=\"right\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\"/>", model.getObject());
    }

    @Test
    public void setAdditionalImgAttributesAreNotChanged() {
        model.setObject("<img src=\"binaries/image.png/{_document}/hippogallery:original\" data-facetselect=\"image.png/{_document}/hippogallery:original\" data-type=\"hippogallery:original\" align=\"right\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\"/>");
        assertEquals("<img src=\"image.png/{_document}/hippogallery:original\" align=\"right\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\"/>", delegate.getObject());
    }

    @Test
    public void getExternalImageDoesNotChange() {
        delegate.setObject("<img src=\"http://www.example.com/image.png\" align=\"right\"/>");
        assertEquals("<img src=\"http://www.example.com/image.png\" align=\"right\"/>", model.getObject());
    }

    @Test
    public void setExternalImageDoesNotChange() {
        model.setObject("<img src=\"http://www.example.com/image.png\" align=\"right\"/>");
        assertEquals("<img src=\"http://www.example.com/image.png\" align=\"right\"/>", delegate.getObject());
    }

    @Test
    public void getExternalImageWithEndTagDoesNotChange() {
        delegate.setObject("<img src=\"http://www.example.com/image.png\"></img>");
        assertEquals("<img src=\"http://www.example.com/image.png\"></img>", model.getObject());
    }

    @Test
    public void setExternalImageWithEndTagDoesNotChange() {
        model.setObject("<img src=\"http://www.example.com/image.png\"></img>");
        assertEquals("<img src=\"http://www.example.com/image.png\"></img>", delegate.getObject());
    }

    @Test
    public void getRichTextImageHasCorrectUrl() throws RepositoryException, RichTextException {
        final RichTextImage image = new RichTextImage("/content/gallery/image.png/image.png", "image.png");
        image.setSelectedResourceDefinition("hippogallery:original");

        final IRichTextImageFactory mockImageFactory = EasyMock.createMock(IRichTextImageFactory.class);
        expect(mockImageFactory.loadImageItem(eq("image.png/{_document}/hippogallery:original"))).andReturn(image);

        final IRichTextLinkFactory mockLinkFactory = EasyMock.createMock(IRichTextLinkFactory.class);
        expect(mockLinkFactory.getLinkUuids()).andReturn(Collections.singleton("0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21"));

        final Node documentNode = MockNode.root();
        final Node imageFacetNode = documentNode.addNode("image.png", HippoNodeType.NT_FACETSELECT);
        imageFacetNode.setProperty(HippoNodeType.HIPPO_DOCBASE, "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");

        final IModel<Node> mockNodeModel = EasyMock.createMock(IModel.class);
        expect(mockNodeModel.getObject()).andReturn(documentNode).anyTimes();

        final RichTextImageURLProvider urlProvider = new RichTextImageURLProvider(mockImageFactory, mockLinkFactory, mockNodeModel);

        replay(mockImageFactory, mockLinkFactory, mockNodeModel);

        model = new RichTextImageMetaDataModel(delegate, urlProvider);
        delegate.setObject("<img src=\"image.png/{_document}/hippogallery:original\"/>");

        assertEquals("<img src=\"binaries/content/gallery/image.png/image.png/hippogallery:original\" data-facetselect=\"image.png/{_document}/hippogallery:original\" data-type=\"hippogallery:original\"/>", model.getObject());
    }

    private class PrefixingImageUrlProvider implements IImageURLProvider {

        private final String prefix;

        PrefixingImageUrlProvider(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String getURL(final String link) throws RichTextException {
            return prefix + "/" + link;
        }
    }

}
