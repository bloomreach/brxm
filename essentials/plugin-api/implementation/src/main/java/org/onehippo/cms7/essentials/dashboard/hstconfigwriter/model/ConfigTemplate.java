/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model;


import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types.ConfigType;
import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types.PropertyType;

/**
 * @version "$Id: ConfigTemplate.java 171565 2013-07-24 14:38:15Z mmilicevic $"
 */
public class ConfigTemplate extends ConfigNode {


    public ConfigTemplate(final String name) {
        super(ConfigType.TEMPLATE, name);
    }

    public void setRenderPath(final String value) {
        addProperty(new HstConfigProperty(PropertyType.RENDER_PATH.getName(), value));
    }


}
