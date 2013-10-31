package org.onehippo.cms7.essentials.dashboard.config;

import java.util.List;

/**
 * @version "$Id: ConfigDocument.java 171585 2013-07-24 16:57:57Z mmilicevic $"
 */
public interface ConfigDocument {

    String getName();

    void addProperty(ConfigProperty property);

    List<ConfigProperty > getProperties();


    String getValue(String name);

    List<String> getValues(String name);
}
