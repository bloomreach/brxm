package org.hippocms.repository.webapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableMultiLineLabel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class PropertiesPanel extends Panel implements NodeEditor {

    private static final long serialVersionUID = 1L;

    private List model;
    private ListView view;

    public PropertiesPanel(String id) {
        super(id);

        model = new ArrayList();
        view = new ListView("properties", model) {
            private static final long serialVersionUID = 1L;

            protected void populateItem(final ListItem item) {
                NameValuePair prop = (NameValuePair) item.getModelObject();
                item.add(new Label("name", prop.getName()));
                item.add(new AjaxEditableMultiLineLabel("value", new Model(prop.getValue())));
            }
        };
        add(view);
    }

    public void renderNode(Node jcrNode) {
        model.clear();
        try {
            for (PropertyIterator iter = jcrNode.getProperties(); iter.hasNext();) {
                Property prop = iter.nextProperty();
                if (prop.getDefinition().isMultiple()) {
                    Value[] values = prop.getValues();
                    for (int i = 0; i < values.length; i++) {
                        NameValuePair entry = new NameValuePair(prop.getName(), values[i].getString());
                        model.add(entry);
                    }
                } else {
                    NameValuePair entry = new NameValuePair(prop.getName(), prop.getString());
                    model.add(entry);
                }
            }
        } catch (ValueFormatException e) {
        } catch (IllegalStateException e) {
        } catch (RepositoryException e) {
        }
    }

    private class NameValuePair implements Serializable {

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


