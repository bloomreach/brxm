package org.hippocms.repository.webapp.node;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableMultiLineLabel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.Model;
import org.hippocms.repository.webapp.model.JcrPropertyModel;

public class PropertiesEditor extends DataView {
    private static final long serialVersionUID = 1L;

    public PropertiesEditor(String id, IDataProvider dataProvider) {
        super(id, dataProvider);
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    protected void populateItem(Item item) {
        JcrPropertyModel model = (JcrPropertyModel) item.getModel();
        try {
            item.add(deleteLink("delete", model));
            item.add(propertyNameRenderer("name", model));
            item.add(propertyValueRenderer("value", model));
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Component deleteLink(String id, final JcrPropertyModel model) throws RepositoryException {
        Component result = null;
        if (model.getProperty().getDefinition().isProtected()) {
            result = new Label(id, "");
            
        } else {
            result = new AjaxLink(id, model) {
                private static final long serialVersionUID = 1L;

                public void onClick(AjaxRequestTarget target) {
                    try {
                        Property prop = model.getProperty();
                        Node node = prop.getParent();
                        prop.remove();
                        node.save();
                    } catch (RepositoryException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    NodeEditor form = (NodeEditor) findParent(NodeEditor.class);
                    form.save();
                    target.addComponent(form);
                }
            };
        }
        return result;
    }

    private Component propertyNameRenderer(String id, JcrPropertyModel model) throws RepositoryException {
        Property prop = model.getProperty();
        return new Label(id, new Model(prop.getName()));
    }

    private Component propertyValueRenderer(String id, JcrPropertyModel model) throws RepositoryException {
        Property prop = model.getProperty();

        Component result = null;
        if (prop.getDefinition().isProtected()) {
            result = new Label(id, model);
        } else {
            if (prop.getValue().getString().contains("\n")) {
                result = new AjaxEditableMultiLineLabel(id, model) {
                    private static final long serialVersionUID = 1L;

                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);
                        NodeEditor form = (NodeEditor) findParent(NodeEditor.class);
                        form.save();
                    }
                };
            } else {
                result = new AjaxEditableLabel(id, model) {
                    private static final long serialVersionUID = 1L;

                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);
                        NodeEditor form = (NodeEditor) findParent(NodeEditor.class);
                        form.save();
                    }
                };
            }
        }
        return result;
    }

}
