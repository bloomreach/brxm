package org.hippoecm.frontend.plugins.richtext.view;

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
import org.hippoecm.frontend.service.IBrowseService;

/**
 * Renders a view of a rich text field. Subclasses must call {@link #addView} once with the view model to render.
 */
abstract class AbstractRichTextViewPanel extends Panel {

    protected static final String WICKET_ID_VIEW = "view";

    private static final ResourceReference CSS = new CssResourceReference(AbstractRichTextViewPanel.class, "richtext.css");

    public AbstractRichTextViewPanel(final String id) {
        super(id);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
    }

    protected void addView(IModel<String> model) {
        final HtmlContainer view = new HtmlContainer(WICKET_ID_VIEW, model);
        this.add(view);
    }

    private static class HtmlContainer extends WebMarkupContainer {

        private static final long serialVersionUID = 1L;

        HtmlContainer(final String id, final IModel<String> viewModel) {
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
