package org.hippoecm.repository.frontend.plugin;

import org.hippoecm.repository.frontend.model.JcrNodeModel;

/**
 * A ContextPlugin is a Plugin that can be conditionally be replaced by another Plugin.
 */
public abstract class ContextPlugin extends Plugin {

    public ContextPlugin(String id, JcrNodeModel model) {
        super(id, model);
    }
    
    /**
     * @return The class name of the new plugin that will replace this one, 
     *          if null this plugin doesn't need to be replaced.    
     */
    public abstract String newPluginClass(JcrNodeModel model);

}
