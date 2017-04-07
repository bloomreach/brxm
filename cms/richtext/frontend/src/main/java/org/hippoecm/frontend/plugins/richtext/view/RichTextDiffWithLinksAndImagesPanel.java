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
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.plugins.richtext.processor.WicketModel;
import org.hippoecm.frontend.plugins.richtext.model.BrowsableModel;
import org.hippoecm.frontend.plugins.richtext.model.RichTextModelFactory;
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
                                               final IBrowseService browser,
                                               final DiffService diffService,
                                               final RichTextModelFactory modelFactory) {
        super(id);

        final PreviewLinksBehavior previewLinksBehavior = new PreviewLinksBehavior(currentNodeModel, browser, false);
        add(previewLinksBehavior);

        final IModel<String> viewModel = createDiffModel(baseNodeModel, currentNodeModel,
                                                         previewLinksBehavior, diffService, modelFactory);
        addView(viewModel);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(DIFF_CSS));
    }

    private static IModel<String> createDiffModel(final IModel<Node> baseNodeModel,
                                                  final IModel<Node> currentNodeModel,
                                                  final PreviewLinksBehavior previewLinksBehavior,
                                                  final DiffService diffService,
                                                  final RichTextModelFactory modelFactory) {

        final JcrPropertyValueModel<String> baseModel = getContentModelOrNull(baseNodeModel);
        final JcrPropertyValueModel<String> currentModel = getContentModelOrNull(currentNodeModel);

        final IModel<String> baseBrowsableModel = new BrowsableModel(baseModel, previewLinksBehavior);
        final BrowsableModel currentBrowsableModel = new BrowsableModel(currentModel, previewLinksBehavior);

        final IModel<String> baseRichTextModel = modelFactory.create(WicketModel.of(baseBrowsableModel),
                                                                     WicketModel.of(baseNodeModel));
        final IModel<String> currentRichTextModel = modelFactory.create(WicketModel.of(currentBrowsableModel),
                                                                        WicketModel.of(currentNodeModel));

        final StripScriptModel scriptlessBase = new StripScriptModel(baseRichTextModel);
        final StripScriptModel scriptlessCurrent = new StripScriptModel(currentRichTextModel);

        return new HtmlDiffModel(scriptlessBase, scriptlessCurrent, diffService);
    }

    private static JcrPropertyValueModel getContentModelOrNull(IModel<Node> nodeModel) {
        Node node = nodeModel.getObject();
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
