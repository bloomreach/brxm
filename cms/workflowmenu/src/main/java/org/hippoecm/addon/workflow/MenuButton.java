/*
 *  Copyright 2009-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.l10n.ResourceBundleModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNodeType;

class MenuButton extends Panel implements IContextMenu {

    private final MenuList content;

    MenuButton(final String id, final String name, final MenuHierarchy menu, final IPluginConfig config) {
        this(id, name, menu, null, config);
    }

    public MenuButton(final String item, final String key, final List<MenuDescription> menuDescriptions, final Form form, final IPluginConfig config) {
        this(item, key, new MenuHierarchy(form, config), menuDescriptions, config);
    }

    MenuButton(final String id, final String name, final MenuHierarchy menu, final List<MenuDescription> descriptions, final IPluginConfig config) {
        super(id);
        setOutputMarkupId(true);
        add(content = new MenuList("item", menu, config));
        content.setOutputMarkupId(true);
        content.setVisible(false);

        final AbstractLink link;
        add(link = new AjaxLink<Void>("link") {

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(new EventStoppingDecorator());
            }

            void updateContent() {
                if (descriptions != null) {
                    menu.clear();
                    final MenuVisitor visitor = new MenuVisitor(menu, "list");
                    for (final MenuDescription description : descriptions) {
                        final MarkupContainer descriptionContent = description.getContent();
                        if (descriptionContent != null) {
                            descriptionContent.visitChildren(Panel.class, visitor);
                        }
                    }
                    menu.flatten();
                    content.update();
                }
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                content.setVisible(!content.isVisible());
                target.add(MenuButton.this);
                if (content.isVisible()) {
                    updateContent();
                    final IContextMenuManager manager = getContextMenuManager();
                    manager.showContextMenu(MenuButton.this);
                }
            }
        });

        link.add(ClassAttribute.append(() -> content.isVisible()
                ? "menu-item-active"
                : "menu-item-inactive"));

        Component label = null;
        if (descriptions != null) {
            label = descriptions.get(0).getLabel();
        }
        if (label == null) {
            final IModel<String> labelModel = new ResourceBundleModel
                    .Builder(HippoNodeType.WORKFLOWS_PATH, name)
                    .defaultValue(name)
                    .build();
            link.add(new Label("label", labelModel));
        } else {
            if (!"label".equals(label.getId())) {
                throw new WicketRuntimeException("Menu label component doesn't have correct id.  Should be 'label', but was '" + label.getId() + "'");
            }
            link.add(label);
        }
    }

    protected IContextMenuManager getContextMenuManager() {
        return findParent(IContextMenuManager.class);
    }

    public void collapse(final AjaxRequestTarget target) {
        if (content.isVisible()) {
            content.setVisible(false);
            if (target != null) {
                target.add(MenuButton.this);
            }
        }
    }

}
