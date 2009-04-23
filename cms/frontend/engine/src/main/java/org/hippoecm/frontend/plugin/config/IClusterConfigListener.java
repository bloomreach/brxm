/**
 * 
 */
package org.hippoecm.frontend.plugin.config;

import java.util.EventListener;

import org.apache.wicket.IClusterable;

public interface IClusterConfigListener extends EventListener, IClusterable {

    void onPluginAdded(IPluginConfig config);

    void onPluginChanged(IPluginConfig config);

    void onPluginRemoved(IPluginConfig config);
}