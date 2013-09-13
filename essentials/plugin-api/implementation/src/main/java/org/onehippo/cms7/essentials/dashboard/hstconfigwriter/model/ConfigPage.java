/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model;


import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types.ConfigType;
import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types.PropertyType;

/**
 * @version "$Id: ConfigPage.java 171565 2013-07-24 14:38:15Z mmilicevic $"
 */
public class ConfigPage extends ConfigNode {

    public ConfigPage(final String name) {
        super(ConfigType.PAGE, name);
    }

    public void setReferenceComponent(final String value) {
        addProperty(new HstConfigProperty(PropertyType.REFERENCE_COMPONENT.getName(), value));
    }
}
