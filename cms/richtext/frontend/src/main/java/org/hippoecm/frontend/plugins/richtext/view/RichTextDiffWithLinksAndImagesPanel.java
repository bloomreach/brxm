/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugins.richtext.IHtmlCleanerService;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.plugins.richtext.UuidConverterBuilder;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.RichTextImageURLProvider;
import org.hippoecm.frontend.plugins.standards.diff.DiffService;
import org.hippoecm.frontend.plugins.standards.diff.HtmlDiffModel;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a compare version of a rich text field, including images and clickable links that open the referring
 * document. Elements that have been added to or removed from the base version of the model are marked with green and
 * red, respectively.
 */
public class RichTextDiffWithLinksAndImagesPanel extends AbstractRichTextDiffPanel {

    private static final CssResourceReference DIFF_CSS = new CssResourceReference(HtmlDiffModel.class, "diff.css");

    private static final Logger log = LoggerFactory.getLogger(RichTextDiffWithLinksAndImagesPanel.class);

    public RichTextDiffWithLinksAndImagesPanel(final String id,
                                               final IModel<Node> baseNodeModel,
                                               final IModel<Node> currentNodeModel,
                                               final IBrowseService<IModel<Node>> browser,
                                               final DiffService diffService,
                                               final IHtmlCleanerService cleaner) {
        super(id);

        final PreviewLinksBehavior previewLinksBehavior = new PreviewLinksBehavior(browser);
        add(previewLinksBehavior);

        final IModel<String> viewModel = createDiffModel(baseNodeModel, currentNodeModel, diffService, cleaner);
        addView(viewModel);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(DIFF_CSS));
    }

    private static IModel<String> createDiffModel(final IModel<Node> baseNodeModel,
                                                  final IModel<Node> currentNodeModel,
                                                  final DiffService diffService,
                                                  final IHtmlCleanerService cleaner) {

        final JcrPropertyValueModel<String> baseModel = getContentModelOrNull(baseNodeModel);
        final IRichTextLinkFactory baseLinkFactory = new JcrRichTextLinkFactory(baseNodeModel);
        final IRichTextImageFactory baseImageFactory = new JcrRichTextImageFactory(baseNodeModel);

        final JcrPropertyValueModel<String> currentModel = getContentModelOrNull(currentNodeModel);
        final IRichTextLinkFactory currentLinkFactory = new JcrRichTextLinkFactory(currentNodeModel);
        final IRichTextImageFactory currentImageFactory = new JcrRichTextImageFactory(currentNodeModel);

        final IImageURLProvider baseDecorator = new RichTextImageURLProvider(baseImageFactory, baseLinkFactory, baseNodeModel);
        final IImageURLProvider currentDecorator = new RichTextImageURLProvider(currentImageFactory, currentLinkFactory, currentNodeModel);

        final UuidConverterBuilder baseConverterBuilder = new UuidConverterBuilder(baseNodeModel, baseLinkFactory, baseDecorator);
        final UuidConverterBuilder currentConverterBuilder = new UuidConverterBuilder(currentNodeModel, currentLinkFactory, currentDecorator);

        final IModel<String> decoratedBase = new RichTextModel(baseModel, cleaner, baseConverterBuilder);
        final IModel<String> decoratedCurrent = new RichTextModel(currentModel, cleaner, currentConverterBuilder);

        final StripScriptModel scriptlessBase = new StripScriptModel(decoratedBase);
        final StripScriptModel scriptlessCurrent = new StripScriptModel(decoratedCurrent);

        return new HtmlDiffModel(scriptlessBase, scriptlessCurrent, diffService);
    }

    private static JcrPropertyValueModel<String> getContentModelOrNull(final IModel<Node> nodeModel) {
        final Node node = nodeModel.getObject();
        try {
            if (node == null) {
                return null;
            }
            final Property prop = node.getProperty(HippoStdNodeType.HIPPOSTD_CONTENT);
            return new JcrPropertyValueModel<>(new JcrPropertyModel(prop));
        } catch (final RepositoryException e) {
            log.error("Cannot read HTML content from '" + JcrUtils.getNodePathQuietly(node) + "'", e);
        }
        return null;
    }

}
