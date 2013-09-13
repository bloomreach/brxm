/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.shared.model;

/**
 * @version "$Id: Vendor.java 157405 2013-03-08 09:15:49Z mmilicevic $"
 */
public class Vendor {

    private String name;
    private String url;
    private String logo;

    public String getLogo() {
        return logo;
    }

    public void setLogo(final String logo) {
        this.logo = logo;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Vendor{");
        sb.append("name='").append(name).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append(", logo='").append(logo).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
