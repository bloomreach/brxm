package org.onehippo.cms7.essentials.dashboard;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
