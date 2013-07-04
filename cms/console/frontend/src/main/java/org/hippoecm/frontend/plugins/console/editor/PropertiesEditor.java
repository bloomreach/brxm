/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.console.editor;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesEditor extends DataView {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertiesEditor.class);

    public PropertiesEditor(String id, IDataProvider model) {
        super(id, model);
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    @Override
    protected void populateItem(Item item) {
        JcrPropertyModel model = (JcrPropertyModel) item.getModel();
        try {
            item.add(deleteLink("delete", model));

            JcrName propName = new JcrName(model.getProperty().getName());
            item.add(new Label("name", propName.getName()));

            item.add(new Label("type", PropertyType.nameFromValue(model.getProperty().getType())));
            item.add(new PropertyValueEditor("values", model));

            if (model.getProperty().getDefinition().isMultiple() && !model.getProperty().getDefinition().isProtected()) {
                item.add(addLink("add", model));
            } else {
                item.add(new Label("add", ""));
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    // privates

    private Component deleteLink(String id, final JcrPropertyModel model) throws RepositoryException {
        Component result = null;
        if (model.getProperty().getDefinition().isProtected()) {
            result = new Label(id, "(protected)");

        } else {
            result = new AjaxLink(id, model) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(final ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("class", "property-value-remove");
                }

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        Property prop = model.getProperty();
                        prop.remove();
                    } catch (RepositoryException e) {
                        log.error(e.getMessage());
                    }
                    NodeEditor editor = findParent(NodeEditor.class);
                    target.add(editor);
                }
            };
        }
        return result;
    }

    private AjaxLink addLink(String id, final JcrPropertyModel model) {
        return new AjaxLink(id, model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Property prop = model.getProperty();
                Value[] newValues;
                try {
                    Value[] oldValues = prop.getValues();
                    newValues = new Value[oldValues.length + 1];
                    for (int i = 0; i < oldValues.length; i++) {
                        newValues[i] = oldValues[i];
                    }
                    newValues[newValues.length - 1] = createDefaultValue(prop.getType());
                    prop.setValue(newValues);
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                    return;
                }

                NodeEditor editor = findParent(NodeEditor.class);
                target.add(editor);
            }
        };
    }

    private Value createDefaultValue(int valueType) throws RepositoryException {
        final ValueFactory valueFactory = UserSession.get().getJcrSession().getValueFactory();
        switch (valueType) {
            case PropertyType.BOOLEAN : return valueFactory.createValue(false);
            case PropertyType.DATE : return valueFactory.createValue(Calendar.getInstance());
            case PropertyType.DECIMAL : return valueFactory.createValue(new BigDecimal(0d));
            case PropertyType.DOUBLE : return valueFactory.createValue(0d);
            case PropertyType.LONG : return valueFactory.createValue(0l);
            case PropertyType.NAME : return valueFactory.createValue("jcr:name", PropertyType.NAME);
            case PropertyType.PATH : return valueFactory.createValue("/", PropertyType.PATH);
            case PropertyType.URI : return valueFactory.createValue("http://www.onehippo.org/", PropertyType.URI);
            case PropertyType.REFERENCE : return valueFactory.createValue("cafebabe-cafe-babe-cafe-babecafebabe", PropertyType.REFERENCE);
            case PropertyType.WEAKREFERENCE : return valueFactory.createValue("cafebabe-cafe-babe-cafe-babecafebabe", PropertyType.REFERENCE);
            default : return valueFactory.createValue("");
        }
    }
    
}
