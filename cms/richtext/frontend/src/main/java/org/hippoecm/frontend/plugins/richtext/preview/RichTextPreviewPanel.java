package org.hippoecm.frontend.plugins.richtext.preview;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.preview.PreviewLinksBehavior;
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
public class RichTextPreviewPanel extends Panel {

    private static final String WICKET_ID_HTML = "html";
    private static final ResourceReference CSS = new CssResourceReference(RichTextPreviewPanel.class, "richtext-preview.css");

    public RichTextPreviewPanel(final String id,
                                final JcrNodeModel nodeModel,
                                final IModel<String> htmlModel,
                                final IBrowseService browser) {
        super(id);

        final PreviewLinksBehavior previewLinksBehavior = new PreviewLinksBehavior(nodeModel, browser);
        add(previewLinksBehavior);

        final IModel<String> viewModel = createViewModel(nodeModel, htmlModel, previewLinksBehavior);
        final HtmlContainer preview = new HtmlContainer(WICKET_ID_HTML, viewModel);
        add(preview);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CSS));
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

    private static class HtmlContainer extends WebMarkupContainer {

        private static final long serialVersionUID = 1L;

        public HtmlContainer(final String id, final IModel<String> viewModel) {
            super(id, viewModel);
        }

        @Override
        public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
            final String text = getDefaultModelObject().toString();
            if (text != null) {
                replaceComponentTagBody(markupStream, openTag, text);
            } else {
                super.onComponentTagBody(markupStream, openTag);
            }
        }
    }
}
