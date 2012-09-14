package org.onehippo.cms7.channelmanager.templatecomposer;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.wicketstuff.js.ext.util.ExtClass;

/**
 * Base class for a component properties editor.
 */
@ExtClass("Hippo.ChannelManager.TemplateComposer.PlainVariantAdder")
public class PlainVariantAdder extends VariantAdder {

    public PlainVariantAdder(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

}
