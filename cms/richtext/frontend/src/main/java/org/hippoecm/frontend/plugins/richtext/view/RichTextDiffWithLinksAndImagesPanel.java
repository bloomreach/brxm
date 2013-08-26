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
package org.hippoecm.frontend.plugins.richtext.view;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.RichTextLink;
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.model.BrowsableModel;
import org.hippoecm.frontend.plugins.richtext.model.PrefixingModel;
import org.hippoecm.frontend.plugins.standards.diff.HtmlDiffModel;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a compare version of a rich text field, including images and clickable links that open the referring document.
 * Elements that have been added to or removed from the base version of the model are marked with green and red,
 * respectively.
 */
public class RichTextDiffWithLinksAndImagesPanel extends AbstractRichTextDiffPanel {

    private static final CssResourceReference DIFF_CSS = new CssResourceReference(HtmlDiffModel.class, "diff.css");

    private static final Logger log = LoggerFactory.getLogger(RichTextDiffWithLinksAndImagesPanel.class);

    public RichTextDiffWithLinksAndImagesPanel(final String id,
                                               final JcrNodeModel baseNodeModel,
                                               final JcrNodeModel currentNodeModel,
                                               final IBrowseService browser) {
        super(id);

        final PreviewLinksBehavior previewLinksBehavior = new PreviewLinksBehavior(currentNodeModel, browser);
        add(previewLinksBehavior);

        final IModel<String> viewModel = createDiffModel(baseNodeModel, currentNodeModel, previewLinksBehavior);
        addView(viewModel);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(DIFF_CSS));
    }

    private IModel<String> createDiffModel(final JcrNodeModel baseNodeModel,
                                           final JcrNodeModel currentNodeModel,
                                           final PreviewLinksBehavior previewLinksBehavior) {

        final JcrPropertyValueModel<String> baseModel = getContentModelOrNull(baseNodeModel);
        final IRichTextLinkFactory baseLinkFactory = new JcrRichTextLinkFactory(baseNodeModel);
        final IRichTextImageFactory baseImageFactory = new JcrRichTextImageFactory(baseNodeModel);

        final JcrPropertyValueModel<String> currentModel = getContentModelOrNull(currentNodeModel);
        final IRichTextLinkFactory currentLinkFactory = new JcrRichTextLinkFactory(currentNodeModel);
        final IRichTextImageFactory currentImageFactory = new JcrRichTextImageFactory(currentNodeModel);

        // links that are in both: set to current
        // otherwise: set to respective prefix

        final IImageURLProvider baseDecorator = new RichTextImageURLProvider(baseImageFactory, baseLinkFactory);
        final IImageURLProvider currentDecorator = new RichTextImageURLProvider(currentImageFactory, currentLinkFactory);

        final IModel<String> decoratedBase = new PrefixingModel(baseModel, new IImageURLProvider() {
            private static final long serialVersionUID = 1L;

            public String getURL(String link) throws RichTextException {
                String facetName = link;
                if (link.indexOf('/') > 0) {
                    facetName = link.substring(0, link.indexOf('/'));
                }
                if (baseLinkFactory.getLinks().contains(facetName) && currentLinkFactory.getLinks().contains(facetName)) {
                    RichTextLink baseRtl = baseLinkFactory.loadLink(facetName);
                    RichTextLink currentRtl = currentLinkFactory.loadLink(facetName);
                    if (currentRtl.getTargetId().equals(baseRtl.getTargetId())) {
                        return currentDecorator.getURL(link);
                    }
                } else if (baseLinkFactory.getLinks().contains(facetName)) {
                    return baseDecorator.getURL(link);
                } else if (currentLinkFactory.getLinks().contains(facetName)) {
                    return currentDecorator.getURL(link);
                }
                return facetName;
            }

        }) {
            private static final long serialVersionUID = 1L;

            @Override
            public void detach() {
                baseLinkFactory.detach();
                currentLinkFactory.detach();
                super.detach();
            }
        };

        final IModel<String> decoratedCurrent = new PrefixingModel(currentModel, currentDecorator);
        final HtmlDiffModel diffModel = new HtmlDiffModel(new StripScriptModel(decoratedBase), new StripScriptModel(decoratedCurrent));

        return new BrowsableModel(diffModel, previewLinksBehavior);
    }

    private static JcrPropertyValueModel getContentModelOrNull(JcrNodeModel nodeModel) {
        Node node = nodeModel.getNode();
        try {
            if (node == null) {
                return null;
            }
            Property prop = node.getProperty(HippoStdNodeType.HIPPOSTD_CONTENT);
            return new JcrPropertyValueModel(new JcrPropertyModel(prop));
        } catch (RepositoryException e) {
            log.error("Cannot read HTML content from '" + JcrUtils.getNodePathQuietly(node) + "'", e);
        }
        return null;
    }

}
