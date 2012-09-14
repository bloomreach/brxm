package org.onehippo.cms7.channelmanager.templatecomposer;

import org.hippoecm.frontend.extjs.ExtWidget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Base class for a component properties editor.
 */
public class PropertiesEditor extends ExtWidget {

    private static final String XTYPE = "Hippo.ChannelManager.TemplateComposer.PropertiesEditor";

    public PropertiesEditor(IPluginContext context, IPluginConfig config) {
        super(XTYPE, context);
    }

}
