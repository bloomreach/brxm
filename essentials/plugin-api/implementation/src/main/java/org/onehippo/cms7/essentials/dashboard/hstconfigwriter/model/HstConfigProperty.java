/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model;

/**
 * @version "$Id: HstConfigProperty.java 171565 2013-07-24 14:38:15Z mmilicevic $"
 */
public class HstConfigProperty {


    private final String name;
    private final String value;

    public HstConfigProperty(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HstConfigProperty{");
        sb.append("name='").append(name).append('\'');
        sb.append(", value=").append(value);
        sb.append('}');
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
