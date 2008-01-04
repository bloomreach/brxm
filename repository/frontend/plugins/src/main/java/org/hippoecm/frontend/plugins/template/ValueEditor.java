/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.template;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueEditor extends DataView {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueEditor.class);

    protected FieldDescriptor descriptor;
    protected TemplateEngine engine;

    public ValueEditor(String id, JcrPropertyModel dataProvider, FieldDescriptor descriptor, TemplateEngine engine) {
        super(id, dataProvider);
        this.descriptor = descriptor;
        this.engine = engine;
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    // Implement DataView
    @Override
    protected void populateItem(Item item) {
        final JcrPropertyValueModel valueModel = (JcrPropertyValueModel) item.getModel();
        try {
            item.add(engine.createWidget("value", descriptor, valueModel));
        } catch (RepositoryException e) {
            item.add(new Label("value", e.getMessage()));
            log.error(e.getMessage());
        }

        //Remove value link
        if (descriptor.isMultiple()) {
            item.add(new AjaxLink("remove", valueModel) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    ValueEditor.this.onRemove(target, (JcrPropertyValueModel) getModel());
                }
            });
        } else {
            item.add(new Label("remove", ""));
        }
    }

    protected void onRemove(AjaxRequestTarget target, JcrPropertyValueModel model) {
        try {
            Property prop = ((JcrPropertyModel) ValueEditor.this.getModel()).getProperty();
            Value[] values = prop.getValues();
            values = (Value[]) ArrayUtils.remove(values, model.getIndex());
            prop.setValue(values);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }
}
