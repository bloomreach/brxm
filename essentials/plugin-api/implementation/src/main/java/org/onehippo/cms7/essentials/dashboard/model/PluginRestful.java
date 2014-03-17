package org.onehippo.cms7.essentials.dashboard.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "plugin")
public class PluginRestful implements Plugin, Restful {

    private static final long serialVersionUID = 1L;

    private List<String> restClasses;
    private Vendor vendor;
    private List<EssentialsDependency> dependencies;
    private String title;
    private String name;
    private String introduction;
    private String description;
    private String pluginLink;
    private String powerpackClass;
    private String type;
    private boolean installed;
    private boolean needsInstallation;
    private boolean enabled;
    private Calendar dateInstalled;
    private String documentationLink;


    public PluginRestful(final String name) {
        this.name = name;
    }

    public PluginRestful() {

    }

    public Calendar getDateInstalled() {
        return dateInstalled;
    }

    public void setDateInstalled(final Calendar dateInstalled) {
        this.dateInstalled = dateInstalled;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public boolean isNeedsInstallation() {
        return needsInstallation;
    }

    @Override
    public void setNeedsInstallation(final boolean needsInstallation) {
        this.needsInstallation = needsInstallation;
    }

    @Override
    public boolean isInstalled() {
        return installed;
    }

    @Override
    public void setInstalled(final boolean installed) {
        this.installed = installed;
    }

    @Override
    public String getPluginId() {
        return pluginLink;
    }

    @Override
    public void setPluginId(final String pluginId) {
        this.pluginLink = pluginId;
    }

    @Override
    @XmlElementRef(type = VendorRestful.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({@JsonSubTypes.Type(value = VendorRestful.class, name = "vendor")})
    public Vendor getVendor() {
        return vendor;
    }
    @Override
    public String getPowerpackClass() {
        return powerpackClass;
    }

    @Override
    public void setPowerpackClass(final String powerpackClass) {
        this.powerpackClass = powerpackClass;
    }

    @Override
    public void setVendor(final Vendor vendor) {
        this.vendor = vendor;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getDocumentationLink() {
        return documentationLink;
    }

    @Override
    public void setDocumentationLink(final String documentationLink) {
        this.documentationLink = documentationLink;
    }

    @Override
    public String getIssuesLink() {
        return null;
    }

    @Override
    public void setIssuesLink(final String issuesLink) {

    }


    @XmlElementRef(type = DependencyRestful.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({@JsonSubTypes.Type(value = DependencyRestful.class, name = "dependency")})
    @Override
    public List<EssentialsDependency> getDependencies() {
        if (dependencies == null) {
            return new ArrayList<>();
        }
        return dependencies;
    }

    @Override
    public void setDependencies(final List<EssentialsDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public void addDependency(final EssentialsDependency dependency) {
        if (dependencies == null) {
            dependencies = new ArrayList<>();
        }
        dependencies.add(dependency);

    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public String getIntroduction() {
        return introduction;
    }

    @Override
    public void setIntroduction(final String introduction) {
        this.introduction = introduction;
    }



    public void addRestCLass(final String restClass) {

        if (restClasses == null) {
            restClasses = new ArrayList<>();
        }
        restClasses.add(restClass);
    }


    @Override
    public List<String> getRestClasses() {
        return restClasses;
    }

    @Override
    public void setRestClasses(final List<String> restClasses) {
        this.restClasses = restClasses;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginRestful{");
        sb.append("restClasses=").append(restClasses);
        sb.append(", vendor=").append(vendor);
        sb.append(", dependencies=").append(dependencies);
        sb.append(", title='").append(title).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", introduction='").append(introduction).append('\'');
        sb.append(", pluginId='").append(pluginLink).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", installed=").append(installed);
        sb.append(", needsInstallation=").append(needsInstallation);
        sb.append(", dateInstalled=").append(dateInstalled);
        sb.append('}');
        return sb.toString();
    }


}
