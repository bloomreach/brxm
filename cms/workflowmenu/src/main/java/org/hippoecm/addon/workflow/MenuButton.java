/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.behaviors.IContextMenuManager;

class MenuButton extends Panel implements IContextMenu {

    private static final long serialVersionUID = 1L;

    private MenuList content;

    MenuButton(String id, String name, final MenuHierarchy menu) {
        this(id, name, menu, null);
    }

    public MenuButton(final String item, final String key, final List<MenuDescription> menuDescriptions, Form form) {
        this(item, key, new MenuHierarchy(form), menuDescriptions);
    }

    MenuButton(String id, String name, final MenuHierarchy menu, final List<MenuDescription> descriptions) {
        super(id);
        setOutputMarkupId(true);
        add(content = new MenuList("item", menu));
        content.setOutputMarkupId(true);
        content.setVisible(false);

        AbstractLink link;
        add(link = new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(new EventStoppingDecorator());
            }

            void updateContent() {
                if (descriptions != null) {
                    menu.clear();
                    MenuVisitor visitor = new MenuVisitor(menu, "list");
                    for (MenuDescription description : descriptions) {
                        MarkupContainer descriptionContent = description.getContent();
                        if (descriptionContent != null) {
                            descriptionContent.visitChildren(Panel.class, visitor);
                        }
                    }
                    menu.flatten();
                    content.update();
                }
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                content.setVisible(!content.isVisible());
                target.add(MenuButton.this);
                if (content.isVisible()) {
                    updateContent();
                    IContextMenuManager manager = getContextMenuManager();
                    manager.showContextMenu(MenuButton.this);
                }
            }

        });

        Component label = null;
        if (descriptions != null) {
            label = descriptions.get(0).getLabel();
        }
        if (label == null) {
            link.add(new Label("label", new StringResourceModel(name, MenuButton.this, null, name)));
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

    public void collapse(AjaxRequestTarget target) {
        if (content.isVisible()) {
            content.setVisible(false);
            target.add(MenuButton.this);
        }
    }

}
