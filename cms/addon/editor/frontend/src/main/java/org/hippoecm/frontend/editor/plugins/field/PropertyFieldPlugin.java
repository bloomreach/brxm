/*
 *  Copyright 2008 Hippo.
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

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PropertyValueProvider;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyFieldPlugin extends AbstractFieldPlugin<Node, JcrPropertyValueModel> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertyFieldPlugin.class);

    private JcrNodeModel nodeModel;
    private JcrPropertyModel propertyModel;
    private int nrValues;
    private IObserver propertyObserver;

    public PropertyFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        nodeModel = (JcrNodeModel) getDefaultModel();

        // use caption for backwards compatibility; i18n should use field name
        IModel nameModel;
        if (config.getString("caption") != null) {
            nameModel = new Model(config.getString("caption"));
        } else {
            nameModel = new StringResourceModel(getFieldHelper().getField().getName(), this, null);
        }
        add(new Label("name", nameModel));

        Label required = new Label("required", "*");
        IFieldDescriptor field = getFieldHelper().getField();
        if (field != null) {
            subscribe();
            if (!field.getValidators().contains("required")) {
                required.setVisible(false);
            }
        }
        add(required);

        add(createAddLink());

        updateProvider();
    }

    protected void subscribe() {
        final IFieldDescriptor field = getFieldHelper().getField();
        if (!field.getPath().equals("*")) {
            JcrItemModel itemModel = new JcrItemModel(((JcrNodeModel) getDefaultModel()).getItemModel().getPath() + "/"
                    + field.getPath());
            propertyModel = new JcrPropertyModel(itemModel);
            nrValues = propertyModel.size();
            getPluginContext().registerService(propertyObserver = new IObserver<JcrPropertyModel>() {
                private static final long serialVersionUID = 1L;

                public JcrPropertyModel getObservable() {
                    return propertyModel;
                }

                public void onEvent(Iterator<? extends IEvent<JcrPropertyModel>> events) {
                    if (propertyModel.size() != nrValues || field.isOrdered()) { //Only redraw if number of properties has changed.
                        nrValues = propertyModel.size();
                        updateProvider();
                        redraw();
                    }
                }

            }, IObserver.class.getName());
        }
    }

    protected void unsubscribe() {
        IFieldDescriptor field = getFieldHelper().getField();
        if (!field.getPath().equals("*")) {
            getPluginContext().unregisterService(propertyObserver, IObserver.class.getName());
            propertyModel = null;
        }
    }

    @Override
    protected AbstractProvider<JcrPropertyValueModel> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type,
            IModel nodeModel) {
        if (!descriptor.getPath().equals("*")) {
            PropertyValueProvider provider = new PropertyValueProvider(descriptor, type, propertyModel.getItemModel());
            if (ITemplateEngine.EDIT_MODE.equals(mode) && !descriptor.isMultiple() && provider.size() == 0) {
                provider.addNew();
            }
            return provider;
        }
        return null;
    }

    @Override
    public void onModelChanged() {
        // filter out changes in the node model itself.
        // The property model observation takes care of that.
        if (!nodeModel.equals(getDefaultModel())) {
            IFieldDescriptor field = getFieldHelper().getField();
            if (field != null) {
                unsubscribe();
                subscribe();
            }
            replace(createAddLink());
            redraw();
        }
    }

    @Override
    protected void onDetach() {
        if (propertyModel != null) {
            propertyModel.detach();
        }
        super.onDetach();
    }

    @Override
    protected void onAddRenderService(Item item, IRenderService renderer) {
        super.onAddRenderService(item, renderer);

        final JcrPropertyValueModel model = getController().findItemRenderer(renderer).getModel();

        MarkupContainer remove = new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemoveItem(model, target);
            }
        };
        if (!canRemoveItem()) {
            remove.setVisible(false);
        }
        item.add(remove);

        MarkupContainer upLink = new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onMoveItemUp(model, target);
            }
        };
        boolean isFirst = (model.getIndex() == 0);
        if (!canReorderItems()) {
            upLink.setVisible(false);
        }
        upLink.setEnabled(!isFirst);
        item.add(upLink);

        MarkupContainer downLink = new AjaxLink("down") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                JcrPropertyValueModel nextModel = new JcrPropertyValueModel(model.getIndex() + 1, model
                        .getJcrPropertymodel());
                onMoveItemUp(nextModel, target);
            }
        };
        boolean isLast = (model.getIndex() == provider.size() - 1);
        if (!canReorderItems()) {
            downLink.setVisible(false);
        }
        downLink.setEnabled(!isLast);
        item.add(downLink);
    }

    // privates

    protected Component createAddLink() {
        IFieldDescriptor field = getFieldHelper().getField();
        if (ITemplateEngine.EDIT_MODE.equals(mode) && (field != null) && field.isMultiple()) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    PropertyFieldPlugin.this.onAddItem(target);
                }
            };
        } else {
            return new Label("add").setVisible(false);
        }
    }
}
