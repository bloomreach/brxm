package org.onehippo.cms7.channelmanager.templatecomposer;

import org.hippoecm.frontend.extjs.ExtWidget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Base class for a variant adder.
 */
public class VariantAdder extends ExtWidget {

    public VariantAdder(IPluginContext context, IPluginConfig config) {
        super("Hippo.ChannelManager.TemplateComposer.VariantAdder", context);
    }

}
