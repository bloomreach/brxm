package org.hippoecm.hst.plugins.frontend.editor.sitemap;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dialogs.AddNodeDialog;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemapItem;
import org.hippoecm.hst.plugins.frontend.editor.validators.UniqueSitemapItemValidator;

public class AddSitemapItemDialog extends AddNodeDialog<SitemapItem> {
    private static final long serialVersionUID = 1L;

    public AddSitemapItemDialog(EditorDAO<SitemapItem> dao,
            RenderPlugin plugin, JcrNodeModel parent) {
        super(dao, plugin, parent);

        FormComponent textField = new RequiredTextField("matcher");
        textField.setOutputMarkupId(true);
        textField.add(new UniqueSitemapItemValidator(this, dao.getHstContext().sitemap));
        add(textField);
        setFocus(textField);

    }
}
