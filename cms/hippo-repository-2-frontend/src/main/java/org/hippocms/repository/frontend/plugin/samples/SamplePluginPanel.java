package org.hippocms.repository.frontend.plugin.samples;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippocms.repository.frontend.dialog.DialogWindow;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.plugin.DynamicDialogCreator;
import org.hippocms.repository.frontend.plugin.GenericComponentFactory;
import org.hippocms.repository.frontend.update.IUpdatable;

public class SamplePluginPanel extends Panel implements IUpdatable {
    private static final long serialVersionUID = 1L;

    private DynamicDialogCreator pluginDialogCreator;
    private Component pluginComponent;

    public SamplePluginPanel(String id, JcrNodeModel model) {
        super(id, model);

        // A dynamic pop-up dialog whose implementation (java+markup) depends the selected Node:
        // Select a Node, add a Property 'renderer' with as value the FQN of a subclass of AbstractDialog,
        // select the Node again and click on the link.
        DialogWindow dialogWindow = new DialogWindow("plugin-dialog", model);
        pluginDialogCreator = new DynamicDialogCreator(dialogWindow, model);
        dialogWindow.setPageCreator(pluginDialogCreator);
        add(dialogWindow);
        add(dialogWindow.dialogLink("plugin-dialog-link"));

        // A plugged in Component using the GenericComponentFactory, if the Component
        // implements IUdatable it will automatically synchronize with the tree.
        String classname = "org.hippocms.repository.frontend.plugin.samples.component.UpdatableLabel";
        pluginComponent = new GenericComponentFactory(classname).getComponent("plugin-component", model);
        add(pluginComponent);
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        if (model != null && target != null) {
            pluginDialogCreator.update(target, model);

            if (pluginComponent instanceof IUpdatable) {
                ((IUpdatable) pluginComponent).update(target, model);
            }
        }

    }

}
