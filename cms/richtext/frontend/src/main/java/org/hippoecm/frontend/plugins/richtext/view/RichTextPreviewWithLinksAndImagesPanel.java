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

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.RichTextImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.model.BrowsableModel;
import org.hippoecm.frontend.plugins.richtext.model.RichTextImageMetaDataModel;
import org.hippoecm.frontend.service.IBrowseService;

/**
 * Renders a preview version of a rich text field, including images and clickable links that open the referring document.
 */
public class RichTextPreviewWithLinksAndImagesPanel extends AbstractRichTextViewPanel {

    public RichTextPreviewWithLinksAndImagesPanel(final String id,
                                                  final IModel<Node> nodeModel,
                                                  final IModel<String> htmlModel,
                                                  final IBrowseService browser) {
        super(id);

        final PreviewLinksBehavior previewLinksBehavior = new PreviewLinksBehavior(nodeModel, browser, true);
        add(previewLinksBehavior);

        final IModel<String> viewModel = createViewModel(nodeModel, htmlModel, previewLinksBehavior);
        addView(viewModel);
    }

    private IModel<String> createViewModel(final IModel<Node> nodeModel, final IModel<String> htmlModel, final PreviewLinksBehavior previewLinksBehavior) {
        final IRichTextImageFactory imageFactory = new JcrRichTextImageFactory(nodeModel);
        final IRichTextLinkFactory linkFactory = new JcrRichTextLinkFactory(nodeModel);
        final IImageURLProvider urlProvider = new RichTextImageURLProvider(imageFactory, linkFactory, nodeModel);

        final StripScriptModel stripScriptModel = new StripScriptModel(htmlModel);
        final RichTextImageMetaDataModel prefixingModel = new RichTextImageMetaDataModel(stripScriptModel, urlProvider);
        final BrowsableModel browsableModel = new BrowsableModel(prefixingModel, previewLinksBehavior);

        return browsableModel;
    }

}
