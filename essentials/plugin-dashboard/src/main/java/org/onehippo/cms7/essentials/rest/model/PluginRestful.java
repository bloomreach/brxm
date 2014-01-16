package org.onehippo.cms7.essentials.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "items")
public class PluginRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private VendorRestful vendor;
    private DependencyRestful dependency;
    private String title;
    private String introduction;
    private String pluginLink;
    private boolean installed;

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(final boolean installed) {
        this.installed = installed;
    }

    public String getPluginLink() {
        return pluginLink;
    }

    public void setPluginLink(final String pluginLink) {
        this.pluginLink = pluginLink;
    }

    public VendorRestful getVendor() {
        return vendor;
    }

    public void setVendor(final VendorRestful vendor) {
        this.vendor = vendor;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(final String introduction) {
        this.introduction = introduction;
    }

    public DependencyRestful getDependency() {
        return dependency;
    }

    public void setDependency(final DependencyRestful dependency) {
        this.dependency = dependency;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginRestful{");
        sb.append("vendor=").append(vendor);
        sb.append(", title='").append(title).append('\'');
        sb.append(", introduction='").append(introduction).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
