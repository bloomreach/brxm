package org.onehippo.cms7.essentials.dashboard;

import java.util.List;

/**
 * @version "$Id: ConfigProperty.java 171565 2013-07-24 14:38:15Z mmilicevic $"
 */
public interface ConfigProperty {

    String[] getPropertyValuesArray();

    String getStringValue();

    String getPropertyName();

    List<String> getPropertyValues();

    void addValue(String value);
}
