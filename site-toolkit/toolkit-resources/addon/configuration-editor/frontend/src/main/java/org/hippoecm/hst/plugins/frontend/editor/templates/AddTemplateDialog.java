package org.hippoecm.hst.plugins.frontend.editor.templates;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dialogs.AddNodeDialog;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.Template;
import org.hippoecm.hst.plugins.frontend.editor.validators.NodeUniqueValidator;

public class AddTemplateDialog extends AddNodeDialog<Template> {
    private static final long serialVersionUID = 1L;

    public AddTemplateDialog(EditorDAO<Template> dao, RenderPlugin plugin, JcrNodeModel parent) {
        super(dao, plugin, parent);

        FormComponent textField = new RequiredTextField("name");
        textField.setOutputMarkupId(true);
        textField.add(new NodeUniqueValidator<Template>(new BeanProvider<Template>() {
            private static final long serialVersionUID = 1L;

            public Template getBean() {
                return bean;
            }

        }));
        add(textField);
        setFocus(textField);
    }
}
