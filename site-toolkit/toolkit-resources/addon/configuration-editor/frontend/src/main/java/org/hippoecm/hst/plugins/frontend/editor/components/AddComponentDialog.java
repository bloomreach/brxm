package org.hippoecm.hst.plugins.frontend.editor.components;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dialogs.AddNodeDialog;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component;
import org.hippoecm.hst.plugins.frontend.editor.validators.NodeUniqueValidator;

public class AddComponentDialog extends AddNodeDialog<Component> {
    private static final long serialVersionUID = 1L;

    public AddComponentDialog(EditorDAO<Component> dao, RenderPlugin plugin, JcrNodeModel parent) {
        super(dao, plugin, parent);

        FormComponent textField = new RequiredTextField("name");
        textField.setOutputMarkupId(true);
        textField.add(new NodeUniqueValidator<Component>(new BeanProvider<Component>() {
            private static final long serialVersionUID = 1L;

            public Component getBean() {
                return bean;
            }

        }));
        add(textField);
        setFocus(textField);
    }
}
