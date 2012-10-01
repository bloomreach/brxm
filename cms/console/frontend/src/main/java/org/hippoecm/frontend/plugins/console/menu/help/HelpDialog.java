package org.hippoecm.frontend.plugins.console.menu.help;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugins.console.Shortcuts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Console help dialog
 *
 * @version "$Id$"
 */
public class HelpDialog extends AbstractDialog<Void> {

    private static final long serialVersionUID = 1L;


    public HelpDialog() {


        add(new Label("label", getTitle()));

        add(new PropertyListView<HelpDescription>("listview", Shortcuts.HELP_ITEMS) {
            private static final long serialVersionUID = 1L;

            protected void populateItem(ListItem<HelpDescription> item) {
                HelpDescription user = item.getModelObject();
                item.add(new Label("name", user.getName()));
                item.add(new Label("description", user.getDescription()));
                item.add(new Label("shortcut", user.getShortcutName()));
            }
        });
    }


    @Override
    public IModel<String> getTitle() {
        return new Model<String>("Shortcut descriptions");
    }

}
