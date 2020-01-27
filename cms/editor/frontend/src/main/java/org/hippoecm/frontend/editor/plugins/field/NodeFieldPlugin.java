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
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.editor.EditorForm;
import org.hippoecm.frontend.editor.editor.EditorPlugin;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.ChildNodeProvider;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ViolationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeFieldPlugin extends AbstractFieldPlugin<Node, JcrNodeModel> {

    private static final Logger log = LoggerFactory.getLogger(NodeFieldPlugin.class);

    private final FlagList collapsedItems = new FlagList();

    public NodeFieldPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final IModel<String> caption = helper.getCaptionModel(this);
        final IModel<String> hint = helper.getHintModel(this);
        final FieldTitle fieldTitle = new FieldTitle("field-title", caption, hint, helper.isRequired());
        fieldTitle.setVisible(!helper.isCompoundField());
        add(fieldTitle);

        add(createNrItemsLabel());
        add(createAddLink());

        final IFieldDescriptor field = getFieldHelper().getField();
        if (field != null) {
            final String name = cssClassName(field.getTypeDescriptor().getName());
            add(ClassAttribute.append("hippo-node-field-name-" + name));

            final String type = cssClassName(field.getTypeDescriptor().getType());
            add(ClassAttribute.append("hippo-node-field-type-" + type));

            if (field.isMultiple()) {
                add(ClassAttribute.append("hippo-node-field-multiple"));
            }

            if (field.isMandatory()) {
                add(ClassAttribute.append("hippo-node-field-mandatory"));
            }

            if (field.isProtected()) {
                add(ClassAttribute.append("hippo-node-field-protected"));
            }

            if (helper.isCompoundField()) {
                add(ClassAttribute.append("hippo-editor-compound-field"));
            }
        }
    }

    private String cssClassName(final String name) {
        if (StringUtils.isEmpty(name)) {
            return StringUtils.EMPTY;
        }
        return StringUtils.replace(name, ":", "-").toLowerCase();
    }

    @Override
    protected AbstractProvider<Node, JcrNodeModel> newProvider(final IFieldDescriptor descriptor, final ITypeDescriptor type,
                                                               final IModel<Node> nodeModel) {
        try {
            final JcrNodeModel prototype = (JcrNodeModel) getTemplateEngine().getPrototype(type);
            return new ChildNodeProvider(descriptor, prototype, new JcrItemModel<>(nodeModel.getObject()));
        } catch (final TemplateEngineException ex) {
            log.warn("Could not find prototype", ex);
            return null;
        }
    }

    @Override
    public void render(final PluginRequestTarget target) {
        if (isActive() && IEditor.Mode.EDIT == mode && target != null) {
            final FieldPluginHelper fieldHelper = getFieldHelper();
            final IFieldDescriptor field = fieldHelper.getField();
            final IModel<IValidationResult> validationModel = fieldHelper.getValidationModel();
            final String selector = String.format("$('#%s > .hippo-editor-field > .hippo-editor-field-subfield')",
                    getMarkupId());

            final String script = ViolationUtils.getViolationPerCompoundScript(selector, field, validationModel);
            if (script != null) {
                target.appendJavaScript(script);
            }
        }
        super.render(target);
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
    public void onEvent(final Iterator events) {
        final IFieldDescriptor field = getFieldHelper().getField();

        // filter events
        if (field == null) {
            return;
        }
        if (field.getPath().equals("*")) {
            modelChanged();
            return;
        }

        while (events.hasNext()) {
            final JcrEvent jcrEvent = (JcrEvent) events.next();
            final Event event = jcrEvent.getEvent();
            try {
                switch (event.getType()) {
                    case 0:
                        modelChanged();
                        return;
                    case Event.NODE_ADDED:
                    case Event.NODE_MOVED:
                    case Event.NODE_REMOVED:
                        final String path = event.getPath();
                        String name = path.substring(path.lastIndexOf('/') + 1);
                        if (name.indexOf('[') > 0) {
                            name = name.substring(0, name.indexOf('['));
                        }
                        if (name.equals(field.getPath())) {
                            modelChanged();
                            return;
                        }
                }
            } catch (final RepositoryException ex) {
                log.error("Error filtering event", ex);
            }
        }
    }

    @Override
    protected void populateViewItem(final Item<IRenderService> item, final JcrNodeModel model) {
        if (helper.isCompoundField()) {
            item.add(new CollapsibleFieldContainer("fieldContainer", item, this));
        } else {
            item.add(new FieldContainer("fieldContainer", item));
        }
    }

    @Override
    protected void populateEditItem(final Item<IRenderService> item, final JcrNodeModel model) {
        if (helper.isCompoundField()) {
            final boolean isCollapsed = collapsedItems.get(item.getIndex());
            item.add(new EditableCollapsibleFieldContainer("fieldContainer", item, model, this, isCollapsed) {
                @Override
                protected void onCollapse(final boolean isCollapsed) {
                    collapsedItems.set(item.getIndex(), isCollapsed);
                }
            });
        } else {
            item.add(new EditableNodeFieldContainer("fieldContainer", item, model, this));
        }

    }

    @Override
    protected void populateCompareItem(final Item<IRenderService> item, final JcrNodeModel newModel, final JcrNodeModel oldModel) {
        populateViewItem(item, newModel);
    }

    protected Component createAddLink() {
        if (canAddItem()) {
            final AjaxLink<Void> link = new AjaxLink<Void>("add") {
                @Override
                public void onClick(final AjaxRequestTarget target) {
                    target.focusComponent(this);
                    NodeFieldPlugin.this.onAddItem(target);
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

    protected void moveCollapsedItemToTop(final int index) {
        collapsedItems.moveTo(index, 0);
    }

    protected void moveCollapsedItemUp(final int index) {
        collapsedItems.moveUp(index);
    }

    protected void moveCollapsedItemDown(final int index) {
        collapsedItems.moveDown(index);
    }

    protected void moveCollapsedItemToBottom(final int index) {
        collapsedItems.moveTo(index, provider.size());
    }

    protected void removeCollapsedItem(final int index) {
        collapsedItems.remove(index);
    }

    @Override
    public void onMoveItemUp(final JcrNodeModel model, final AjaxRequestTarget target) {
        super.onMoveItemUp(model, target);
        validateModelObjects();
    }

    @Override
    public void onRemoveItem(final JcrNodeModel childModel, final AjaxRequestTarget target) {
        super.onRemoveItem(childModel, target);
        validateModelObjects();
    }

    @Override
    public void onMoveItemToTop(final JcrNodeModel model) {
        super.onMoveItemToTop(model);
        validateModelObjects();
    }

    @Override
    public void onMoveItemToBottom(final JcrNodeModel model) {
        super.onMoveItemToBottom(model);
        validateModelObjects();
    }

    protected AbstractProvider<Node, JcrNodeModel> getProvider() {
        return provider;
    }

    /**
     * If validation has already been done, trigger it again. This is useful when items in the form have moved to a
     * different location or have been removed. After redrawing a possible error message is shown at the correct field.
     */
    private void validateModelObjects() {
        final EditorPlugin editorPlugin = findParent(EditorPlugin.class);
        if (editorPlugin != null && editorPlugin.getForm() instanceof EditorForm) {
            final EditorForm editorForm = (EditorForm) editorPlugin.getForm();
            if (editorForm.hasErrorMessage()) {
                editorForm.onValidateModelObjects();
            }
        }
    }
}
