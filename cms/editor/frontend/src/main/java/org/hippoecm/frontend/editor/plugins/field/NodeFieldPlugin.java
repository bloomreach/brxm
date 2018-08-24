/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

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
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.editor.EditorForm;
import org.hippoecm.frontend.editor.editor.EditorPlugin;
import org.hippoecm.frontend.editor.plugins.fieldhint.FieldHint;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.ChildNodeProvider;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeFieldPlugin extends AbstractFieldPlugin<Node, JcrNodeModel> {

    final static Logger log = LoggerFactory.getLogger(NodeFieldPlugin.class);

    public NodeFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // use caption for backwards compatibility; i18n should use field name
        add(new Label("name", helper.getCaptionModel(this)));

        add(createNrItemsLabel());

        final Label required = new Label("required", "*");
        add(required);

        add(new FieldHint("hint-panel", helper.getHintModel(this)));
        add(createAddLink());

        final IFieldDescriptor field = getFieldHelper().getField();
        if (field != null) {
            if (!field.getValidators().contains("required")) {
                required.setVisible(false);
            }

            final String name = cssClassName(field.getTypeDescriptor().getName());
            add(CssClass.append("hippo-node-field-name-" + name));

            final String type = cssClassName(field.getTypeDescriptor().getType());
            add(CssClass.append("hippo-node-field-type-" + type));

            if (field.isMultiple()) {
                add(CssClass.append("hippo-node-field-multiple"));
            }

            if (field.isMandatory()) {
                add(CssClass.append("hippo-node-field-mandatory"));
            }

            if (field.isProtected()) {
                add(CssClass.append("hippo-node-field-protected"));
            }

            final List<String> superTypes = field.getTypeDescriptor().getSuperTypes();
            if (superTypes.contains("hippo:compound")) {
                add(CssClass.append("hippo-editor-compound-field"));
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
    protected AbstractProvider<Node, JcrNodeModel> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type,
            IModel<Node> nodeModel) {
        try {
            JcrNodeModel prototype = (JcrNodeModel) getTemplateEngine().getPrototype(type);
            return new ChildNodeProvider(descriptor, prototype, new JcrItemModel<>(nodeModel.getObject()));
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
    protected void populateViewItem(Item<IRenderService> item, final JcrNodeModel model) {
        item.add(new FieldContainer("fieldContainer", item));
    }

    @Override
    protected void populateEditItem(Item<IRenderService> item, final JcrNodeModel model) {
        item.add(new EditableNodeFieldContainer("fieldContainer", item, model, this));
    }

    @Override
    protected void populateCompareItem(Item<IRenderService> item, final JcrNodeModel newModel, final JcrNodeModel oldModel) {
        populateViewItem(item, newModel);
    }

    protected Component createAddLink() {
        if (canAddItem()) {
            final AjaxLink link = new AjaxLink("add") {
                @Override
                public void onClick(AjaxRequestTarget target) {
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

    protected AbstractProvider getProvider() {
        return provider;
    }

    /**
     * If validation has already been done, trigger it again. This is useful when items in the form
     * have moved to a different location or have been removed. After redrawing a possible error message
     * is shown at the correct field.
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
