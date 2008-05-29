package org.hippoecm.frontend.plugins.cms.management;
 
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.template.editor.EditorPlugin;

public class ManagementPlugin extends EditorPlugin {
    private static final long serialVersionUID = 1L;

    public ManagementPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
    }
}
