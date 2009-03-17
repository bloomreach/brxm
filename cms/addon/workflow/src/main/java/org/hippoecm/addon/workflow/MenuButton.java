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

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.AppendingStringBuffer;

import org.hippoecm.frontend.plugin.ContextMenu;
import org.hippoecm.frontend.plugin.ContextMenuManager;

class MenuButton extends Panel implements ContextMenu {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Panel content;
    private AjaxLink link;
    private boolean pinned;
    private boolean active=false;
    private String name;
    
    MenuButton(String id, String name, final MenuHierarchy menu) {
        super(id);
        this.name = name;
        setOutputMarkupId(true);
        add(content = new MenuList("item", null, menu));
        content.setOutputMarkupId(true);
        content.setVisible(false);
        pinned = false;
        add(link = new AjaxLink("link") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    pinned = !pinned;
                    content.setVisible(pinned);
                    target.addComponent(content);
                    target.addComponent(MenuButton.this);
                    if(content.isVisible()) {
                        active = true;
                        final MenuBar bar = (MenuBar) findParent(MenuBar.class);
                        bar.collapse(MenuButton.this, target);
                        ((ContextMenuManager) findParent(ContextMenuManager.class)).addContextMenu(MenuButton.this);
                    }
                }
            });
            link.add(new AjaxEventBehavior("oncontextmenu") {
                public void onEvent(AjaxRequestTarget target) {
                    pinned = !pinned;
                    content.setVisible(pinned);
                    target.addComponent(content);
                    target.addComponent(MenuButton.this);
                    if(content.isVisible()) {
                        active = true;
                        final MenuBar bar = (MenuBar) findParent(MenuBar.class);
                        bar.collapse(MenuButton.this, target);
                        ((ContextMenuManager) findParent(ContextMenuManager.class)).addContextMenu(MenuButton.this);
                    }
                }
                @Override
                protected CharSequence getEventHandler() {
                    return new AppendingStringBuffer(super.getEventHandler()).append("; return false;");
                }
            });
        link.add(new Label("label",new StringResourceModel(name,MenuButton.this,null,name)));
    }

    public void collapse(AjaxRequestTarget target) {
        if(active) {
            active = false;
            return;
        }
        pinned = false;
        content.setVisible(pinned);
        target.addComponent(content);
        target.addComponent(MenuButton.this);
    }
}
