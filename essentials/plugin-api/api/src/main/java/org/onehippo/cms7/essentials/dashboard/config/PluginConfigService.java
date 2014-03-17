package org.onehippo.cms7.essentials.dashboard.config;

/**
 * @version "$Id$"
 */
public interface PluginConfigService {

    /**
     * Stores given configuration
     *
     * @param document Document instance
     */
    boolean write(Document document);

    boolean write(Document document, String pluginId);

    <T extends Document> T read(String pluginClass, Class<T> clazz);

    /**
     * Reads config document
     *
     * @return null object if nothing found
     */
    <T extends Document> T read(Class<T> clazz);
}
