package org.hippoecm.hst.plugins.frontend.editor.sitemenu;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dialogs.AddNodeDialog;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemenuItem;
import org.hippoecm.hst.plugins.frontend.editor.validators.NodeUniqueValidator;

public class AddSitemenuItemDialog extends AddNodeDialog<SitemenuItem> {

	public AddSitemenuItemDialog(EditorDAO<SitemenuItem> dao,
			RenderPlugin plugin, JcrNodeModel parent) {
		super(dao, plugin, parent);
		
        FormComponent textField = new RequiredTextField("name");
        textField.setOutputMarkupId(true);
        textField.add(new NodeUniqueValidator<SitemenuItem>(new BeanProvider<SitemenuItem>() {
            private static final long serialVersionUID = 1L;

            public SitemenuItem getBean() {
                return bean;
            }

        }));
        add(textField);
        setFocus(textField);

	}

}
