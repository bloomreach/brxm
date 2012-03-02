package org.hippoecm.frontend.plugins.cms.admin.widgets;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;

public class AjaxLinkLabelListPanel extends Panel {

    public AjaxLinkLabelListPanel(final String id, final IModel<ArrayList<AjaxLinkLabelContainer>> linksListModel) {
        super(id);

        ListView listView = new ListView("listView", new ArrayList(linksListModel.getObject())) {
            @Override
            protected void populateItem(ListItem listItem) {
                AjaxLinkLabelContainer linkAjaxLabel = (AjaxLinkLabelContainer) listItem.getDefaultModelObject();
                listItem.add(linkAjaxLabel.getAjaxFallbackLink());
            }
        };
        add(listView);
    }
}
