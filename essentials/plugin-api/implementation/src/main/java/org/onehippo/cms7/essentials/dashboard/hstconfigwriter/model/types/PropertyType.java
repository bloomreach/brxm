/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types;

/**
 * * @version "$Id: PropertyType.java 171483 2013-07-24 09:26:52Z mmilicevic $"
 */
public enum PropertyType {

    REFERENCE_SITEMAP_ITEM("hst:referencesitemapitem"),
    CONFIGURATION_ID("hst:componentconfigurationid"),
    RELATIVE_CONTENT_PATH("hst:relativecontentpath"),
    COMPONENT_CLASS_NAME("hst:componentclassname"),
    TEMPLATE("hst:template"),
    RENDER_PATH("hst:renderpath"),
    REFERENCE_COMPONENT("hst:referencecomponent");
    private final String name;

    PropertyType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
