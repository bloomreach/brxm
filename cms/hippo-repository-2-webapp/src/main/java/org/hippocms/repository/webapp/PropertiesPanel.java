package org.hippocms.repository.webapp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableMultiLineLabel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;

public class PropertiesPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private PropertiesForm form;

    public PropertiesPanel(String id, Session jcrSession) {
        super(id);
        form = new PropertiesForm("form", jcrSession);
        form.setOutputMarkupId(true);
        add(form);
    }

    public NodeEditor getNodeEditor() {
        return (NodeEditor) form;
    }

    private class PropertiesForm extends Form implements NodeEditor {
        private static final long serialVersionUID = 1L;

        private PropertiesView view;
        
        //TODO: don't use transient fields, find another way of keeping the session
        private transient Session jcrSession;

        public PropertiesForm(String id, Session jcrSession) {
            super(id, new CompoundPropertyModel(new PropertiesModel()));
            this.jcrSession = jcrSession;

            view = new PropertiesView("properties");
            add(view);

            Button saveButton = new Button("save");
            add(saveButton);
        }

        public void setNode(Node jcrNode) {
            setModel(new CompoundPropertyModel(new PropertiesModel(jcrNode)));
        }

        protected void onSubmit() {
            PropertiesModel model = (PropertiesModel) getModelObject();

            try {
                Item item = jcrSession.getItem(model.getPath());
                Node jcrNode = (Node) item;

                Iterator it = model.getProperties().iterator();
                while (it.hasNext()) {
                    NameValuePair nvp = (NameValuePair) it.next();

                    if (jcrNode.hasProperty(nvp.getName())) {
                        Property prop = jcrNode.getProperty(nvp.getName());
                        if (prop.getDefinition().isMultiple()) {
                            //Value[] values = prop.getValues();
                            //...
                        } else {
                            if (!prop.getDefinition().isProtected()) {
                                prop.setValue(nvp.getValue());
                            }
                        }
                    }
                }
                jcrNode.save();

            } catch (ValueFormatException e) {
                e.printStackTrace();
            } catch (VersionException e) {
                e.printStackTrace();
            } catch (LockException e) {
                e.printStackTrace();
            } catch (ConstraintViolationException e) {
                e.printStackTrace();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
    }

    private class PropertiesView extends ListView {
        private static final long serialVersionUID = 1L;

        public PropertiesView(String id) {
            super(id);
            setReuseItems(true);
        }

        protected void populateItem(ListItem item) {
            item.add(new Label("name", new PropertyModel(item.getModel(), "name")));
            item.add(new AjaxEditableMultiLineLabel("value", new PropertyModel(item.getModel(), "value")));
        }

    }

    private class PropertiesModel implements IClusterable {
        private static final long serialVersionUID = 1L;

        private String path;
        private List properties;

        public PropertiesModel() {
            this.setPath("");
            this.properties = new ArrayList();
        }

        public PropertiesModel(Node jcrNode) {
            try {
                //FIXME: temporary hack to workaround double slash bug
                this.path = jcrNode.getPath().substring(1);
                this.properties = new ArrayList();

                for (PropertyIterator iter = jcrNode.getProperties(); iter.hasNext();) {
                    Property prop = iter.nextProperty();
                    if (prop.getDefinition().isMultiple()) {
                        Value[] values = prop.getValues();
                        for (int i = 0; i < values.length; i++) {
                            properties.add(new NameValuePair(prop.getName(), values[i].getString()));
                        }
                    } else {
                        properties.add(new NameValuePair(prop.getName(), prop.getString()));
                    }
                }
            } catch (ValueFormatException e) {
                properties.add(new NameValuePair(e.getClass().getName(), e.getMessage()));
            } catch (IllegalStateException e) {
                properties.add(new NameValuePair(e.getClass().getName(), e.getMessage()));
            } catch (RepositoryException e) {
                properties.add(new NameValuePair(e.getClass().getName(), e.getMessage()));
            }
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public List getProperties() {
            return properties;
        }

        public void setProperties(List properties) {
            this.properties = properties;
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
