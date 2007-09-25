package org.hippoecm.frontend.plugin;

import java.lang.reflect.Constructor;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class WorkflowPluginFactory {

    private WorkflowManager manager;
    private WorkflowDescriptor workflowDescriptor;

    public WorkflowPluginFactory(WorkflowManager manager, WorkflowDescriptor descriptor) {
        this.manager = manager;
        this.workflowDescriptor = descriptor;
    }

    public Plugin getWorkflowPlugin(String id, JcrNodeModel model) {
        Plugin plugin;
        try {
            Class clazz = Class.forName(workflowDescriptor.getRendererName());
            Class[] formalArgs = new Class[] { String.class, JcrNodeModel.class, WorkflowManager.class, WorkflowDescriptor.class };
            Constructor constructor = clazz.getConstructor(formalArgs);
            Object[] actualArgs = new Object[] { id, model, manager, workflowDescriptor };
            plugin = (Plugin) constructor.newInstance(actualArgs);
        } catch (Exception e) {
            String message = "Failed to instantiate workflow plugin '" + workflowDescriptor.getRendererName()
                    + "' for id '" + id + "'.";
            plugin = new ErrorPlugin(id, e, message);
        }
        return plugin;
    }
}
