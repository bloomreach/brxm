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
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.plugins.richtext.model.BrowsableModel;
import org.hippoecm.frontend.plugins.standards.diff.HtmlDiffModel;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for showing the differences between two rich text fields.
 */
public abstract class AbstractRichTextDiffPanel extends AbstractRichTextViewPanel {

    private static final CssResourceReference DIFF_CSS = new CssResourceReference(HtmlDiffModel.class, "diff.css");

    public AbstractRichTextDiffPanel(final String id) {
        super(id);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(DIFF_CSS));
    }

}
