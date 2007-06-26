package org.hippocms.repository.webapp;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableMultiLineLabel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class PropertiesPanel extends Panel implements NodeEditor {

    private static final long serialVersionUID = 1L;

    private List properties = new ArrayList();

    public List getProperties() {
        return properties;
    }

    public PropertiesPanel(String id, final Form form) {
        super(id);

        ListView view = new ListView("properties", new PropertyModel(this, "properties")) {
            private static final long serialVersionUID = 1L;

            protected void populateItem(final ListItem item) {
                NameValuePair prop = (NameValuePair) item.getModelObject();
                item.add(new Label("name", prop.getName()));
                item.add(new AjaxEditableMultiLineLabel("value", new Model(prop.getValue())));
            }
        };
        add(view);

        add(new Button("save"));
    }

    public void renderNode(Node jcrNode) {
        getProperties().clear();
        try {
            for (PropertyIterator iter = jcrNode.getProperties(); iter.hasNext();) {
                Property prop = iter.nextProperty();
                if (prop.getDefinition().isMultiple()) {
                    Value[] values = prop.getValues();
                    for (int i = 0; i < values.length; i++) {
                        getProperties().add(new NameValuePair(prop.getName(), values[i].getString()));
                    }
                } else {
                    getProperties().add(new NameValuePair(prop.getName(), prop.getString()));
                }
            }
        } catch (ValueFormatException e) {
            getProperties().add(new NameValuePair(e.getClass().getName(), e.getMessage()));
        } catch (IllegalStateException e) {
            getProperties().add(new NameValuePair(e.getClass().getName(), e.getMessage()));
        } catch (RepositoryException e) {
            getProperties().add(new NameValuePair(e.getClass().getName(), e.getMessage()));
        }
    }

    private class NameValuePair implements IClusterable {

        private static final long serialVersionUID = 1L;

        private String name;
        private String value;

        NameValuePair(String name, String value) {
            this.name = name;
            this.value = value;
        }

        String getName() {
            return name;
        }

        String getValue() {
            return value;
        }
    }
}
