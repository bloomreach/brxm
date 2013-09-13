/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.Asset;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.Screenshot;

/**
 * @version "$Id: EssentialsPlugin.java 174057 2013-08-16 13:45:41Z dvandiepen $"
 */
@XmlRootElement(name = "plugin", namespace = EssentialsPlugin.HIPPO_PLUGIN_NAMESPACE_URL)
public class EssentialsPlugin implements Plugin {

    public static final String HIPPO_PLUGIN_NAMESPACE_URL = "http://www.onehippo.org/essentials";
    private static final long serialVersionUID = 1L;
    private String name;
    private String type;
    private String icon;
    private String pluginClass;
    private String vendor;
    private String vendorLink;
    private String description;
    private String documentationLink;
    private String issuesLink;
    private List<Screenshot> screenshots;
    private List<Asset> assets;

    @Override
    public void addScreenShot(final Screenshot screenShot) {
        if (screenshots == null) {
            screenshots = new ArrayList<>();
        }
        screenshots.add(screenShot);
    }

    @Override
    public void addAsset(final Asset asset) {
        if (assets == null) {
            assets = new ArrayList<>();
        }
        assets.add(asset);
    }

    @XmlElement(namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @XmlElement(namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    @Override
    public String getVendorLink() {
        return vendorLink;
    }

    @Override
    public void setVendorLink(final String vendorLink) {
        this.vendorLink = vendorLink;
    }

    @XmlElement(namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    @Override
    public String getVendor() {
        return vendor;
    }

    @Override
    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    @Override
    @XmlElement(name = "documentation-link", namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    public String getDocumentationLink() {
        return documentationLink;
    }

    @Override
    public void setDocumentationLink(final String documentationLink) {
        this.documentationLink = documentationLink;
    }

    @Override
    @XmlElement(name = "issues-link", namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    public String getIssuesLink() {
        return issuesLink;
    }

    @Override
    public void setIssuesLink(final String issuesLink) {
        this.issuesLink = issuesLink;
    }

    @Override
    @XmlElementWrapper(name = "screenshots", namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    @XmlElementRefs({@XmlElementRef(type = PluginScreenshot.class)})
    public List<Screenshot> getScreenshots() {
        return screenshots;
    }

    @Override
    public void setAssets(final List<Asset> assets) {
        this.assets = assets;
    }


    @Override
    @XmlElementWrapper(name = "assets", namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    @XmlElementRefs({@XmlElementRef(type = PluginAsset.class)})
    public List<Asset> getAssets() {
        return assets;
    }

    @Override
    public Asset getAsset(final String id) {
        if(id == null) {
            return null;
        }
        for(final Asset asset : getAssets()) {
            if(id.equals(asset.getId())) {
                return asset;
            }
        }
        return null;
    }

    @Override
    public void setScreenshots(final List<Screenshot> screenshots) {
        this.screenshots = screenshots;
    }


    @XmlElement(namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @XmlElement(namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(final String type) {
        this.type = type;
    }

    @XmlElement(namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(final String icon) {
        this.icon = icon;
    }

    @Override
    @XmlElement(name = "plugin-class", namespace = HIPPO_PLUGIN_NAMESPACE_URL)
    public String getPluginClass() {
        return pluginClass;
    }

    @Override
    public void setPluginClass(final String pluginClass) {
        this.pluginClass = pluginClass;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EssentialsPlugin{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", icon='").append(icon).append('\'');
        sb.append(", pluginClass='").append(pluginClass).append('\'');
        sb.append(", vendor='").append(vendor).append('\'');
        sb.append(", vendorLink='").append(vendorLink).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", documentationLink='").append(documentationLink).append('\'');
        sb.append(", issuesLink='").append(issuesLink).append('\'');
        sb.append(", screenshots=").append(screenshots);
        sb.append('}');
        return sb.toString();
    }
}
