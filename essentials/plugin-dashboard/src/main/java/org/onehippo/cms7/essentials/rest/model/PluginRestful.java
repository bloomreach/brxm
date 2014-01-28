package org.onehippo.cms7.essentials.rest.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "items")
public class PluginRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private VendorRestful vendor;
    private List<DependencyRestful> dependencies;
    private String title;
    private String name;
    private String introduction;
    private String pluginLink;
    private String pluginClass;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    private boolean installed;
    private boolean needsInstallation;

    public boolean isNeedsInstallation() {
        return needsInstallation;
    }

    public void setNeedsInstallation(final boolean needsInstallation) {
        this.needsInstallation = needsInstallation;
    }

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


    public List<DependencyRestful> getDependencies() {
        if(dependencies ==null){
            return new ArrayList<>();
        }
        return dependencies;
    }

    public void setDependencies(final List<DependencyRestful> dependencies) {
        this.dependencies = dependencies;
    }

    public void addDependency(final DependencyRestful dependency){
        if(dependencies ==null){
            dependencies = new ArrayList<>();
        }
        dependencies.add(dependency);

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


    public String getPluginClass() {
        return pluginClass;
    }

    public void setPluginClass(final String pluginClass) {
        this.pluginClass = pluginClass;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginRestful{");
        sb.append("vendor=").append(vendor);
        sb.append(", dependencies=").append(dependencies);
        sb.append(", title='").append(title).append('\'');
        sb.append(", introduction='").append(introduction).append('\'');
        sb.append(", pluginLink='").append(pluginLink).append('\'');
        sb.append(", pluginClass='").append(pluginClass).append('\'');
        sb.append(", installed=").append(installed);
        sb.append('}');
        return sb.toString();
    }
}
