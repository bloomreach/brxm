package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class AutoCalcDeviceService extends DefaultDeviceService {

    private static Logger log = LoggerFactory.getLogger(AutoCalcDeviceService.class);

    /**
     * Construct a new Plugin.
     *
     * @param context the plugin context
     * @param config  the plugin config
     */
    public AutoCalcDeviceService(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    public StyleableDevice createStyleable(final IPluginContext context, final IPluginConfig config) {
        final int type = config.getInt("type", 1);
        switch (type) {
            case 2:
                return new StyleableTemplateDeviceModel(config);
            case 3:
                return new StyleableAutoCalculatingDeviceModel(config);
        }
        return new SimpleStylableDeviceModel(config);
    }


}
