/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model;


import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types.ConfigType;
import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types.PropertyType;

/**
 * @version "$Id: ConfigSitemap.java 171565 2013-07-24 14:38:15Z mmilicevic $"
 */
public class ConfigSitemap extends ConfigNode {

    public ConfigSitemap(final String name) {
        super(ConfigType.SITEMAP, name);
    }

    public void setConfigurationId(final String value) {
        addProperty(new HstConfigProperty(PropertyType.CONFIGURATION_ID.getName(), value));
    }


    public void setRelativeContentPath(final String value) {
        addProperty(new HstConfigProperty(PropertyType.RELATIVE_CONTENT_PATH.getName(), value));
    }
}
