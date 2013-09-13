/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.configwriterwriter.model;

import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.HstConfigProperty;
import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types.ConfigType;
import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types.PropertyType;

/**
 * @version "$Id: ConfigComponent.java 171565 2013-07-24 14:38:15Z mmilicevic $"
 */
public class ConfigComponent extends org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.ConfigNode {

    public ConfigComponent(final String name) {
        super(ConfigType.COMPONENT, name);
    }

    public void setTemplate(final String value) {
        addProperty(new HstConfigProperty(PropertyType.TEMPLATE.getName(), value));
    }

    public void setComponentClassName(final String value) {
        addProperty(new HstConfigProperty(PropertyType.COMPONENT_CLASS_NAME.getName(), value));
    }
}
