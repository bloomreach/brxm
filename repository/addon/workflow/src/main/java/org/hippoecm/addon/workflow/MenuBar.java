package org.hippoecm.addon.workflow;

import java.util.LinkedList;
import java.util.List;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

public class MenuBar extends Panel {
    List<MenuButton> buttons;
    public MenuBar(String id, List<Menu> list) {
        super(id);
        buttons = new LinkedList<MenuButton>();
        add(new DataView("list", new ListDataProvider(list)) {
            public void populateItem(final Item item) {
                final Menu name = (Menu)item.getModelObject();
                MenuButton button = new MenuButton("item", name);
                buttons.add(button);
                item.add(button);
            }
        });
    }
    public void collapse(MenuButton current, AjaxRequestTarget target) {
        for(MenuButton button : buttons) {
            if(button != current) {
                button.collapse(target);
            }
        }
    }
}
