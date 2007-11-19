package org.hippoecm.frontend.plugins.admin.login;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class PasswordLabel extends AjaxEditableLabel {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private String label;

    public PasswordLabel(String id) {
        super(id);
        label = defaultNullLabel();
    }

    public PasswordLabel(String id, IModel model) {
        super(id, model);
        label = defaultNullLabel();
    }

    @Override
    protected FormComponent newEditor(MarkupContainer parent, String componentId, IModel model) {
        TextField editor = new PasswordTextField(componentId, model);
        editor.setOutputMarkupId(true);
        editor.setVisible(false);
        editor.add(this . new EditorAjaxBehavior());
        return editor;
    }
    
    @Override
    protected Component newLabel(MarkupContainer parent, String componentId, IModel model)
    {
        return super.newLabel(parent, componentId, new PropertyModel(this, "label"));
    }

    @Override
    protected void onSubmit(AjaxRequestTarget target)
    {
        this.label = "***";
        super.onSubmit(target);
    }

    @Override
    protected void onError(AjaxRequestTarget target)
    {
        this.label = defaultNullLabel();
        super.onError(target);
    }
}
