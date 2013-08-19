package org.hippoecm.frontend.plugins.richtext.view;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.plugins.standards.diff.HtmlDiffModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders the difference between two rich text fields, including images and clickable links that open the referring
 * document. Elements that have been added to or removed from the base version of the model are marked with green and
 * red, respectively.
 */
public class RichTextDiffPanel extends AbstractRichTextDiffPanel {

    private static final Logger log = LoggerFactory.getLogger(RichTextDiffPanel.class);

    public RichTextDiffPanel(final String id,
                             final JcrPropertyValueModel<String> baseModel,
                             final JcrPropertyValueModel<String> currentModel) {
        super(id);

        final IModel<String> diffModel = createDiffModel(baseModel, currentModel);
        addView(diffModel);
    }

    private IModel<String> createDiffModel(final JcrPropertyValueModel<String> baseModel,
                                           final JcrPropertyValueModel<String> currentModel) {

        return new HtmlDiffModel(new StripScriptModel(baseModel), new StripScriptModel(currentModel));
    }

}
