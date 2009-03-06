package org.hippoecm.addon.workflow;

import org.hippoecm.addon.workflow.*;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

public class MenuList extends Panel {
    private static final long serialVersionUID = 1L;

    public MenuList(String id, Menu menu) {
        super(id);
        MenuContainer container = new MenuContainer("container");
        Workflow wf = new StdWorkflow(container, "wf", menu.getName());
        wf.setOutputMarkupId(true);
        wf.setVisible(false);
        add(wf);

        Component fragment = wf.get("text");
        if (fragment instanceof Workflow.WorkflowFragment) {
            ((Workflow.WorkflowFragment)fragment).substantiate();
            add(fragment);
        } else if (fragment instanceof Fragment) {
            add(fragment);
        } else {
            // wf.setVisible(true);
        }

        fragment = wf.get("icon");
        if (fragment instanceof Workflow.WorkflowFragment) {
            ((Workflow.WorkflowFragment)fragment).substantiate();
            add(fragment);
        } else if (fragment instanceof Fragment) {
            add(fragment);
        }
        add(new DataView("list", new ListDataProvider(menu)) {
            public void populateItem(final Item item) {
                final Menu menu = (Menu)item.getModelObject();
                item.add(new MenuItem("item", menu));
            }
        });
    }
}
