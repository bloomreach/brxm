/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PropertyValueProvider;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;

public class PropertyFieldPlugin extends AbstractFieldPlugin<Property, JcrPropertyValueModel> {

    private final JcrNodeModel nodeModel;
    private JcrPropertyModel propertyModel;
    private long nrValues;
    private IObserver propertyObserver;

    // flag to check if the value orders have been changed when the property is ordered and multiple
    private boolean hasChangedPropValueOrder;

    public PropertyFieldPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        hasChangedPropValueOrder = false;
        nodeModel = (JcrNodeModel) getDefaultModel();

        final IModel<String> captionModel = helper.getCaptionModel(this);
        final IModel<String> hintModel = helper.getHintModel(this);
        add(new FieldTitle("field-title", captionModel, hintModel, helper.isRequired()));
        add(createNrItemsLabel());
        add(createAddLink());

        final IFieldDescriptor field = getFieldHelper().getField();
        if (field != null) {
            subscribe(field);

            final String name = cssClassName(field.getTypeDescriptor().getName());
            add(ClassAttribute.append("hippo-property-field-name-" + name));

            final String type = cssClassName(field.getTypeDescriptor().getType());
            add(ClassAttribute.append("hippo-property-field-type-" + type));

            if (field.isMultiple()) {
                add(ClassAttribute.append("hippo-property-field-multiple"));
            }

            if (field.isMandatory()) {
                add(ClassAttribute.append("hippo-property-field-mandatory"));
            }

            if (field.isProtected()) {
                add(ClassAttribute.append("hippo-property-field-protected"));
            }
        }
    }

    private String cssClassName(final String name) {
        if (StringUtils.isEmpty(name)) {
            return StringUtils.EMPTY;
        }
        return StringUtils.replace(name, ":", "-").toLowerCase();

    }

    private JcrPropertyModel newPropertyModel(final JcrNodeModel model) {
        final IFieldDescriptor field = getFieldHelper().getField();
        if (field != null) {
            final String fieldAbsPath = model.getItemModel().getPath() + "/" + field.getPath();
            return new JcrPropertyModel(fieldAbsPath);
        } else {
            return new JcrPropertyModel((Property) null);
        }
    }

    @Override
    public void onRemoveItem(final JcrPropertyValueModel childModel, final AjaxRequestTarget target) {
        super.onRemoveItem(childModel, target);
        hasChangedPropValueOrder = true;
    }

    @Override
    public void onMoveItemUp(final JcrPropertyValueModel model, final AjaxRequestTarget target) {
        super.onMoveItemUp(model, target);
        hasChangedPropValueOrder = true;
    }

    protected void subscribe(final IFieldDescriptor field) {
        if (!field.getPath().equals("*")) {
            propertyModel = newPropertyModel((JcrNodeModel) getDefaultModel());
            nrValues = propertyModel.size();
            getPluginContext().registerService(propertyObserver = new IObserver<JcrPropertyModel>() {
                private static final long serialVersionUID = 1L;

                public JcrPropertyModel getObservable() {
                    return propertyModel;
                }

                public void onEvent(final Iterator<? extends IEvent<JcrPropertyModel>> events) {
                    //Only redraw if the number of properties or their order has changed.
                    if (propertyModel.size() != nrValues ||
                        (field.isOrdered() && hasChangedPropValueOrder)) {
                        nrValues = propertyModel.size();
                        resetValidation();
                        redraw();

                        // reset flag after redraw
                        hasChangedPropValueOrder = false;
                    }
                }

            }, IObserver.class.getName());
        }
    }

    protected void unsubscribe(final IFieldDescriptor field) {
        if (!field.getPath().equals("*")) {
            getPluginContext().unregisterService(propertyObserver, IObserver.class.getName());
            propertyModel = null;
        }
    }

    @Override
    protected AbstractProvider<Property, JcrPropertyValueModel> newProvider(final IFieldDescriptor descriptor, final ITypeDescriptor type,
                                                                            final IModel<Node> nodeModel) {
        if (!descriptor.getPath().equals("*")) {
            return new PropertyValueProvider(descriptor, type, newPropertyModel((JcrNodeModel) nodeModel).getItemModel());
        }
        return null;
    }

    @Override
    public void onModelChanged() {
        // filter out changes in the node model itself.
        // The property model observation takes care of that.
        if (!nodeModel.equals(getDefaultModel()) || (propertyModel != null && propertyModel.size() != nrValues)) {
            final IFieldDescriptor field = getFieldHelper().getField();
            if (field != null) {
                unsubscribe(field);
                subscribe(field);
            }
            redraw();
        }
    }

    @Override
    protected void onBeforeRender() {
        replace(createAddLink());
        super.onBeforeRender();
    }

    @Override
    protected void onDetach() {
        if (propertyModel != null) {
            propertyModel.detach();
        }
        super.onDetach();
    }

    @Override
    protected void populateViewItem(final Item<IRenderService> item, final JcrPropertyValueModel model) {
        item.add(new FieldContainer("fieldContainer", item));
    }

    @Override
    protected void populateEditItem(final Item<IRenderService> item, final JcrPropertyValueModel model) {
        item.add(new EditablePropertyFieldContainer("fieldContainer", item, model, this));
    }

    @Override
    protected void populateCompareItem(final Item<IRenderService> item, final JcrPropertyValueModel newModel, final JcrPropertyValueModel oldModel) {
        populateViewItem(item, newModel);
    }

    protected Component createAddLink() {
        if (canAddItem()) {
          final AjaxLink<Void> link = new AjaxLink<Void>("add") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
              target.focusComponent(this);
              PropertyFieldPlugin.this.onAddItem(target);
            }
          };

          final Label addLink = new Label("add-label", getString("add-label"));
          link.add(addLink);

          final HippoIcon addIcon = HippoIcon.fromSprite("add-icon", Icon.PLUS);
          link.add(addIcon);

          return link;
        } else {
            return new Label("add").setVisible(false);
        }
    }
}
