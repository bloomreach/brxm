/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import org.onehippo.cms7.essentials.dashboard.config.Asset;

/**
 * @version "$Id: Plugin.java 174057 2013-08-16 13:45:41Z dvandiepen $"
 */
@XmlTransient
public interface Plugin extends Serializable {


    void addScreenShot(Screenshot screenShot);

    void addAsset(Asset asset);

    String getDescription();

    void setDescription(String description);

    List<Asset> getAssets();

    Asset getAsset(final String id);

    void setAssets(List<Asset> assets);

    String getVendorLink();

    void setVendorLink(String vendorLink);

    String getVendor();

    void setVendor(String vendor);

    String getDocumentationLink();

    void setDocumentationLink(String documentationLink);

    String getIssuesLink();

    void setIssuesLink(String issuesLink);

    List<Screenshot> getScreenshots();

    void setScreenshots(List<Screenshot> screenshots);

    String getName();

    void setName(String name);

    String getType();

    void setType(String type);

    String getIcon();

    void setIcon(String icon);

    String getPluginClass();

    void setPluginClass(String pluginClass);
}
