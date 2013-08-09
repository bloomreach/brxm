package org.hippoecm.frontend.plugins.richtext.view;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.model.BrowsableModel;
import org.hippoecm.frontend.plugins.richtext.model.PrefixingModel;
import org.hippoecm.frontend.service.IBrowseService;

/**
 * Renders a preview version of a rich text field, including images and clickable links that open the referring document.
 */
public class RichTextPreviewPanel extends AbstractRichTextViewPanel {

    public RichTextPreviewPanel(final String id,
                                final JcrNodeModel nodeModel,
                                final IModel<String> htmlModel,
                                final IBrowseService browser) {
        super(id);

        final PreviewLinksBehavior previewLinksBehavior = new PreviewLinksBehavior(nodeModel, browser);
        add(previewLinksBehavior);

        final IModel<String> viewModel = createViewModel(nodeModel, htmlModel, previewLinksBehavior);
        addView(viewModel);
    }

    private IModel<String> createViewModel(JcrNodeModel nodeModel, IModel<String> htmlModel, PreviewLinksBehavior previewLinksBehavior) {
        final IRichTextImageFactory imageFactory = new JcrRichTextImageFactory(nodeModel);
        final IRichTextLinkFactory linkFactory = new JcrRichTextLinkFactory(nodeModel);
        final IImageURLProvider urlProvider = new RichTextImageURLProvider(imageFactory, linkFactory);

        final StripScriptModel stripScriptModel = new StripScriptModel(htmlModel);
        final PrefixingModel prefixingModel = new PrefixingModel(stripScriptModel, urlProvider);
        final BrowsableModel browsableModel = new BrowsableModel(prefixingModel, previewLinksBehavior);

        return browsableModel;
    }

}
