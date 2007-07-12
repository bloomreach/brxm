package org.hippocms.repository.webapp.editor;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableMultiLineLabel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.hippocms.repository.webapp.model.JcrPropertyModel;
import org.hippocms.repository.webapp.model.JcrValueModel;

public class PropertyValueEditor extends DataView {
    private static final long serialVersionUID = 1L;

    private boolean isProtected;

    public PropertyValueEditor(String id, JcrPropertyModel dataProvider) {
        super(id, dataProvider);
        try {
            isProtected = dataProvider.getProperty().getDefinition().isProtected();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    protected void populateItem(Item item) {
        String id = "value";
        JcrValueModel valueModel = (JcrValueModel) item.getModel();
        if (isProtected) {
            Label label = new Label(id, valueModel);
            item.add(label);
        } else {
            if (valueModel.getObject().toString().contains("\n")) {
                AjaxEditableMultiLineLabel editor = new AjaxEditableMultiLineLabel(id, valueModel);
                editor.setCols(80);
                editor.setRows(25);
                item.add(editor);
            } else {
                AjaxEditableLabel editor = new AjaxEditableLabel(id, valueModel);
                item.add(editor);
            }
        }
    }
}
