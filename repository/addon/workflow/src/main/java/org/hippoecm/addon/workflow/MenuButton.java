/*
 *  Copyright 2009 Hippo.
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.behaviors.IContextMenuManager;

class MenuButton extends Panel implements IContextMenu {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Panel content;
    private AbstractLink link;

    MenuButton(String id, String name, final MenuHierarchy menu) {
        super(id);
        setOutputMarkupId(true);
        add(content = new MenuList("item", null, menu));
        content.setOutputMarkupId(true);
        content.setVisible(false);
        add(link = new DualAjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                content.setVisible(!content.isVisible());
                target.addComponent(MenuButton.this);
                if (content.isVisible()) {
                    IContextMenuManager manager = getContextMenuManager();
                    manager.showContextMenu(MenuButton.this);
                }
            }

            @Override
            public void onRightClick(AjaxRequestTarget target) {
                content.setVisible(!content.isVisible());
                target.addComponent(MenuButton.this);
                if (content.isVisible()) {
                    final MenuBar bar = (MenuBar) findParent(MenuBar.class);
                    getContextMenuManager().showContextMenu(MenuButton.this);
                }
            }
        });
        link.add(new Label("label", new StringResourceModel(name, MenuButton.this, null, name)));
    }

    protected IContextMenuManager getContextMenuManager() {
        return (IContextMenuManager) findParent(IContextMenuManager.class);
    }

    public void collapse(AjaxRequestTarget target) {
        content.setVisible(false);
        target.addComponent(MenuButton.this);
    }

}
