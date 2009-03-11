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

public class MenuButton extends Panel implements ContextMenu {
    private static final long serialVersionUID = 1L;
    Panel content;
    AjaxLink link;
    boolean pinned;
    boolean active=false;
    public MenuButton(String id, final Menu menu) {
        super(id);
        setOutputMarkupId(true);
        add(content = new MenuList("item", menu));
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
                @Override protected CharSequence getEventHandler() {
                    return new AppendingStringBuffer(super.getEventHandler()).append("; return false;");
                }

            });
        link.add(new Label("label",new StringResourceModel(menu.getName(),MenuButton.this,null,menu.getName())));
    }

    public void collapse(AjaxRequestTarget target) {
        pinned = false;
        content.setVisible(pinned);
        target.addComponent(content);
        target.addComponent(MenuButton.this);
    }
}
