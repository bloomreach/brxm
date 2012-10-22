package org.onehippo.cms7.channelmanager.templatecomposer;

import org.hippoecm.frontend.extjs.ExtWidget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for a toolbar plugin. A toolbar plugin is an Ext widget that is rendered in the toolbar of the template
 * composer. That toolbar has two modes: 'view' (when viewing a channel), and 'edit' (when editing a channel).
 * The position of this toolbar plugin in the toolbar can be configured via the string properties 'position.view' and
 * 'position.edit' (one for each toolbar mode). These properties can have the following values:
 * <ul>
 *     <li>'first': position this toolbar plugin as the first item in the toolbar</li>
 *     <li>'last': position this toolbar plugin as the last item in the toolbar</li>
 *     <li>'hidden': do not show this toolbar plugin in the toolbar</li>
 *     <li>'before id-of-other-toolbar-item': position this toolbar plugin directly before the toolbar item with the given id</li>
 *     <li>'after id-of-other-toolbar-item': position this toolbar plugin directly after the toolbar item with the given id</li>
 * </ul>
 * The default value of both 'position.view' and 'position.edit' is 'last', so by default a toolbar plugin is added
 * as the last item in both 'view' and 'edit' mode.
 */
public class ToolbarPlugin extends ExtWidget {

    private static final String CONFIG_PLUGIN_CLASS = "plugin.class";
    private static final String CONFIG_POSITION_EDIT = "position.edit";
    private static final String CONFIG_POSITION_VIEW = "position.view";

    private static final String DEFAULT_POSITION = "last";

    static final String SERVICE_ID = ToolbarPlugin.class.getName();

    private static final Logger log = LoggerFactory.getLogger(ToolbarPlugin.class);
    private final String positionEdit;
    private final String positionView;

    public ToolbarPlugin(final IPluginContext context, final IPluginConfig config) {
        super(config.getString(CONFIG_PLUGIN_CLASS), context);

        positionEdit = config.getString(CONFIG_POSITION_EDIT, DEFAULT_POSITION);
        positionView = config.getString(CONFIG_POSITION_VIEW, DEFAULT_POSITION);

        log.info("Registering template composer toolbar plugin '{}' at view position '{}' and edit position '{}'",
                new Object[]{getXType(), positionView, positionEdit});
        context.registerService(this, SERVICE_ID);
    }

    public String getPositionView() {
        return positionView;
    }

    public String getPositionEdit() {
        return positionEdit;
    }

}
