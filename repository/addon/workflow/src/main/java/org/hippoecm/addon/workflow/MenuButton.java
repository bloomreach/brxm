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

import org.hippoecm.frontend.plugin.ContextMenu;
import org.hippoecm.frontend.plugin.ContextMenuManager;

class MenuButton extends Panel implements ContextMenu {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Panel content;
    private AbstractLink link;
    private boolean pinned;
    private int activeCount = 0;
    private String name;
    
    MenuButton(String id, String name, final MenuHierarchy menu) {
        super(id);
        this.name = name;
        setOutputMarkupId(true);
        add(content = new MenuList("item", null, menu));
        content.setOutputMarkupId(true);
        content.setVisible(false);
        pinned = false;
        add(link = new DualAjaxLink("link") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                pinned = !pinned;
                content.setVisible(pinned);
                target.addComponent(content);
                target.addComponent(MenuButton.this);
                ContextMenuManager manager = (ContextMenuManager)findParent(ContextMenuManager.class);
                if (manager != null) {
                    activeCount = 2;
                    manager.collapse(MenuButton.this, target);
                    manager.addContextMenu(MenuButton.this);
                } else {
                    if (content.isVisible()) {
                        activeCount = 1;
                        final MenuBar bar = (MenuBar)findParent(MenuBar.class);
                        bar.collapse(MenuButton.this, target);
                    }
                }
            }
            @Override
            public void onRightClick(AjaxRequestTarget target) {
                pinned = !pinned;
                content.setVisible(pinned);
                target.addComponent(content);
                target.addComponent(MenuButton.this);
                if (content.isVisible()) {
                    activeCount = 1;
                    final MenuBar bar = (MenuBar)findParent(MenuBar.class);
                    bar.collapse(MenuButton.this, target);
                    ((ContextMenuManager)findParent(ContextMenuManager.class)).addContextMenu(MenuButton.this);
                }
            }
        });
        link.add(new Label("label",new StringResourceModel(name,MenuButton.this,null,name)));
    }

    public void collapse(AjaxRequestTarget target) {
        if(activeCount > 0) {
            --activeCount;
            return;
        }
        pinned = false;
        content.setVisible(pinned);
        target.addComponent(content);
        target.addComponent(MenuButton.this);
    }
}
