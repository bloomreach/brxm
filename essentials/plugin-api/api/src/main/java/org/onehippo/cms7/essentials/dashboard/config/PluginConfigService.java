package org.onehippo.cms7.essentials.dashboard.config;

/**
 * @version "$Id: PluginConfigService.java 171587 2013-07-24 17:22:33Z mmilicevic $"
 */
public interface PluginConfigService {

    /**
     * Stores given configuration
     *
     * @param document ConfigDocument instance
     */
    void write(ConfigDocument document);

    ConfigDocument read(String pluginClass);

    /**
     * Reads config document
     *
     * @return null object if nothing found
     */
    ConfigDocument read();
}
