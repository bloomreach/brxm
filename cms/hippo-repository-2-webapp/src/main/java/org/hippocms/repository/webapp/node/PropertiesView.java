package org.hippocms.repository.webapp.node;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableMultiLineLabel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;

public class PropertiesView extends DataView {
    private static final long serialVersionUID = 1L;

    public PropertiesView(String id, PropertyDataProvider dataProvider) {
        super(id, dataProvider);
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    protected void populateItem(Item item) {
        Property prop = (Property) item.getModelObject();
        try {
            item.add(new Label("name", new Model(prop.getName())));
            if (prop.getDefinition().isProtected()) {
                item.add(new Label("value", new PropertyValueModel(prop)));
            } else {
                item.add(new AjaxEditableMultiLineLabel("value", new PropertyValueModel(prop)));
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
