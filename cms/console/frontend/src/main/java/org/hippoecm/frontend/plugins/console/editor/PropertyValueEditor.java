/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.DateConverter;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.model.properties.StringConverter;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.widgets.AjaxUpdatingWidget;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PropertyValueEditor extends DataView {

    private static final Logger log = LoggerFactory.getLogger(PropertyValueEditor.class);

    private static final int TEXT_AREA_MAX_COLUMNS = 100;

    private final JcrPropertyModel propertyModel;
    private final DateConverter dateConverter = new ISO8601DateConverter();

    private boolean focusOnLastItem;

    PropertyValueEditor(final String id, final JcrPropertyModel dataProvider) {
        super(id, dataProvider);

        this.propertyModel = dataProvider;
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    @Override
    protected void populateItem(final Item item) {
        try {
            final JcrPropertyValueModel valueModel = (JcrPropertyValueModel) item.getModel();
            final Component valueEditor = createValueEditor(valueModel);

            item.add(valueEditor);

            final AjaxLink removeLink = new AjaxLink<Void>("remove") {
                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        final Property prop = propertyModel.getProperty();
                        Value[] values = prop.getValues();
                        values = (Value[]) ArrayUtils.remove(values, valueModel.getIndex());
                        prop.getParent().setProperty(prop.getName(), values, prop.getType());
                    } catch (final RepositoryException e) {
                        log.error(e.getMessage());
                    }

                    final NodeEditor editor = findParent(NodeEditor.class);
                    if (editor != null) {
                        target.add(editor);
                    }
                }
            };

            removeLink.add(TitleAttribute.set(getString("property.value.remove")));

            final PropertyDefinition definition = propertyModel.getProperty().getDefinition();
            removeLink.setVisible(definition.isMultiple() && !definition.isProtected());

            item.add(removeLink);

            if (focusOnLastItem && item.getIndex() == getItemCount() - 1) {
                focusOnLastItem = false;

                final Optional<AjaxRequestTarget> ajax = RequestCycle.get().find(AjaxRequestTarget.class);
                if (ajax.isPresent() && valueEditor instanceof AjaxUpdatingWidget) {
                    ajax.get().focusComponent(((AjaxUpdatingWidget) valueEditor).getFocusComponent());
                }
            }
        }
        catch (final RepositoryException e) {
            log.error(e.getMessage());
            item.add(new Label("value", e.getClass().getName() + ":" + e.getMessage()));
            item.add(new Label("remove", ""));
        }
    }

    void setFocusOnLastItem(final boolean focusOnLastItem) {
        this.focusOnLastItem = focusOnLastItem;
    }

    /**
     * Finds {@link EditorPlugin} containing this from ancestor components.
     */
    private EditorPlugin getEditorPlugin() {
        return findParent(EditorPlugin.class);
    }

    /**
     * Creates property value editing component.
     *
     * @throws RepositoryException
     */
    protected Component createValueEditor(final JcrPropertyValueModel valueModel) throws RepositoryException {
        final List<ValueEditorFactory> factoryList = getEditorPlugin().getPluginContext().getServices(ValueEditorFactory.SERVICE_ID, ValueEditorFactory.class);

        for (final ValueEditorFactory factory : factoryList) {
            if (factory.canEdit(valueModel)) {
                return factory.createEditor("value", valueModel);
            }
        }

        if (propertyModel.getProperty().getType() == PropertyType.BINARY) {
            return new BinaryEditor("value", propertyModel, getEditorPlugin().getPluginContext());
        }
        else if (propertyModel.getProperty().getDefinition().isProtected()) {
            return new Label("value", valueModel) {
                @Override
                public IConverter getConverter(final Class type) {
                    if (type.equals(Date.class)) {
                        return dateConverter;
                    }

                    return super.getConverter(type);
                }
            };
        }
        else if (propertyModel.getProperty().getType() == PropertyType.BOOLEAN) {
            return new BooleanFieldWidget("value", valueModel);
        }
        else {
            final StringConverter stringModel = new StringConverter(valueModel);
            final String asString = stringModel.getObject();
            final TextAreaWidget editor = new TextAreaWidget("value", stringModel);

            if (asString.contains("\n")) {
                final String[] lines = StringUtils.splitByWholeSeparator(asString, "\n");
                int rowCount = lines.length;
                int columnCount = 1;

                for (final String line : lines) {
                    final int length = line.length();

                    if (length > columnCount) {
                        if (length > TEXT_AREA_MAX_COLUMNS) {
                            columnCount = TEXT_AREA_MAX_COLUMNS;
                            rowCount += (length / TEXT_AREA_MAX_COLUMNS) + 1;
                        } else {
                            columnCount = length;
                        }
                    }
                }

                editor.setRows(String.valueOf(rowCount + 1));
            }
            else if (asString.length() > TEXT_AREA_MAX_COLUMNS) {
                editor.setRows(String.valueOf((asString.length() / 80)));
            } else {
                editor.setRows("1");
            }
            return editor;
        }
    }

    private static class ISO8601DateConverter extends DateConverter {
        private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ");
        @Override
        public DateFormat getDateFormat(final Locale locale) {
            return dateFormat;
        }
    }
}
