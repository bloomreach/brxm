/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.service.IBrowseService;

/**
 * Renders a preview version of a rich text field, including images and clickable links that open the referring document.
 */
public class RichTextPreviewWithLinksAndImagesPanel extends AbstractRichTextViewPanel {

    public RichTextPreviewWithLinksAndImagesPanel(final String id,
                                                  final IModel<Node> nodeModel,
                                                  final IModel<String> htmlModel,
                                                  final IBrowseService<IModel<Node>> browser,
                                                  final String htmlProcessorId) {
        super(id);

        final PreviewLinksBehavior previewLinksBehavior = new PreviewLinksBehavior(browser);
        add(previewLinksBehavior);

        final IModel<String> viewModel = createViewModel(nodeModel, htmlModel, htmlProcessorId);
        addView(viewModel);
    }

    private IModel<String> createViewModel(final IModel<Node> nodeModel, final IModel<String> htmlModel,
                                           final String htmlProcessorId) {
        final StripScriptModel stripScriptModel = new StripScriptModel(htmlModel);

        return new RichTextModel(htmlProcessorId, stripScriptModel, nodeModel);
    }
}
