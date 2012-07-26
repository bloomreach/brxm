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
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.ChildNodeProvider;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeFieldPlugin extends AbstractFieldPlugin<Node, JcrNodeModel> {

    final static Logger log = LoggerFactory.getLogger(NodeFieldPlugin.class);

    private static final long serialVersionUID = 1L;

    public NodeFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IFieldDescriptor field = getFieldHelper().getField();
        // use caption for backwards compatibility; i18n should use field name
        add(new Label("name", getCaptionModel()));

        add(createNrItemsLabel());

        Label required = new Label("required", "*");
        if (field != null && !field.getValidators().contains("required")) {
            required.setVisible(false);
        }
        add(required);

        add(createAddLink());
    }

    @Override
    protected AbstractProvider<Node, JcrNodeModel> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type,
            IModel<Node> nodeModel) {
        try {
            JcrNodeModel prototype = (JcrNodeModel) getTemplateEngine().getPrototype(type);
            return new ChildNodeProvider(descriptor, prototype, new JcrItemModel<Node>(nodeModel.getObject()));
        } catch (TemplateEngineException ex) {
            log.warn("Could not find prototype", ex);
            return null;
        }
    }

    @Override
    public void onModelChanged() {
        redraw();
    }

    @Override
    protected void onBeforeRender() {
        replace(createAddLink());
        super.onBeforeRender();
    }

    @Override
    public void onEvent(Iterator events) {
        IFieldDescriptor field = getFieldHelper().getField();

        // filter events
        if (field == null) {
            return;
        }
        if (field.getPath().equals("*")) {
            modelChanged();
            return;
        }

        while (events.hasNext()) {
            JcrEvent jcrEvent = (JcrEvent) events.next();
            Event event = jcrEvent.getEvent();
            try {
                switch (event.getType()) {
                case 0:
                    modelChanged();
                    return;
                case Event.NODE_ADDED:
                case Event.NODE_MOVED:
                case Event.NODE_REMOVED:
                    String path = event.getPath();
                    String name = path.substring(path.lastIndexOf('/') + 1);
                    if (name.indexOf('[') > 0) {
                        name = name.substring(0, name.indexOf('['));
                    }
                    if (name.equals(field.getPath())) {
                        modelChanged();
                        return;
                    }
                }
            } catch (RepositoryException ex) {
                log.error("Error filtering event", ex);
            }
        }
    }

    @Override
    protected void populateViewItem(Item<IRenderService> item) {
        Fragment fragment = new TransparentFragment("fragment", "view-fragment", this);
        item.add(fragment);
    }

    @Override
    protected void populateEditItem(Item item, final JcrNodeModel model) {
        Fragment fragment = new TransparentFragment("fragment", "edit-fragment", this);

        final int index = item.getIndex();

        WebMarkupContainer controls = new WebMarkupContainer("controls");
        controls.setVisible(canRemoveItem() || canReorderItems());
        fragment.add(controls);

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
        controls.add(remove);

        MarkupContainer upLink = new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onMoveItemUp(model, target);
            }
        };
        if (!canReorderItems()) {
            upLink.setVisible(false);
        }
        if (index == 0) {
            upLink.setEnabled(false);
        }
        controls.add(upLink);

        MarkupContainer downLink = new AjaxLink("down") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IFieldDescriptor field = getFieldHelper().getField();
                String name = field.getPath();
                JcrNodeModel parent = model.getParentModel();
                if (parent != null) {
                    JcrNodeModel nextModel = new JcrNodeModel(parent.getItemModel().getPath() + "/" + name + "["
                            + (index + 2) + "]");
                    onMoveItemUp(nextModel, target);
                }
            }
        };
        if (!canReorderItems()) {
            downLink.setVisible(false);
        }
        boolean isLast = (index == provider.size() - 1);
        downLink.setEnabled(!isLast);
        controls.add(downLink);

        item.add(fragment);
    }

    @Override
    protected void populateCompareItem(Item<IRenderService> item) {
        Fragment fragment = new TransparentFragment("fragment", "view-fragment", this);
        item.add(fragment);
    }

    protected Component createAddLink() {
        if (canAddItem()) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    target.focusComponent(this);
                    NodeFieldPlugin.this.onAddItem(target);
                }
            };
        } else {
            return new Label("add").setVisible(false);
        }
    }
}
