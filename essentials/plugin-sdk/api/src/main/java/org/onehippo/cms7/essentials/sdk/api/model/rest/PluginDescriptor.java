/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.sdk.api.model.rest;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PluginDescriptor implements Serializable {

    public static final String TYPE_TOOL = "tool";
    public static final String TYPE_FEATURE = "feature";

    private List<String> restClasses;

    private Vendor vendor;
    private String name;
    private String introduction;
    private String description;
    private List<String> imageUrls;
    private String id;
    private boolean hasConfiguration;
    private boolean rebuildAfterInstallation = true;
    private boolean installWithParameters = true; // for plugins with no setup parameters, the setup phase can always be triggered automatically
    private boolean hasMessages;
    private String packageFile;
    private String type;
    @JsonIgnore private InstallState state;
    private String icon;

    private Calendar dateInstalled;
    private String documentationLink;

    private Map<String, Set<String>> categories;
    private List<Dependency> pluginDependencies;
    private String dependencySummary; // human-readable summary of inter-plugin dependencies

    public Map<String, Set<String>> getCategories() {
        return categories;
    }

    public void setCategories(final Map<String, Set<String>> categories) {
        this.categories = categories;
    }

    public Calendar getDateInstalled() {
        return dateInstalled;
    }

    public void setDateInstalled(final Calendar dateInstalled) {
        this.dateInstalled = dateInstalled;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getInstallState() {
        return state != null ? state.toString() : null;
    }

    public void setInstallState(final String installState) {
        this.state = InstallState.fromString(installState);
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getPackageFile() {
        return packageFile;
    }

    public void setPackageFile(final String packageFile) {
        this.packageFile = packageFile;
    }

    public void setHasConfiguration(final boolean hasConfiguration) {
        this.hasConfiguration = hasConfiguration;
    }

    public boolean getHasConfiguration() {
        return hasConfiguration;
    }

    public boolean isRebuildAfterInstallation() {
        return rebuildAfterInstallation;
    }

    public void setRebuildAfterInstallation(final boolean rebuildAfterInstallation) {
        this.rebuildAfterInstallation = rebuildAfterInstallation;
    }
    
    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(final Vendor vendor) {
        this.vendor = vendor;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDocumentationLink() {
        return documentationLink;
    }

    public void setDocumentationLink(final String documentationLink) {
        this.documentationLink = documentationLink;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(final String introduction) {
        this.introduction = introduction;
    }

    public List<String> getRestClasses() {
        return restClasses;
    }

    public void setRestClasses(final List<String> restClasses) {
        this.restClasses = restClasses;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getIcon() {
        if (Strings.isNullOrEmpty(icon)) {
            return "/essentials/images/icons/missing-icon.png";
        }
        return icon;
    }

    public void setIcon(final String icon) {
        this.icon = icon;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginRestful{");
        sb.append("restClasses=").append(restClasses);
        sb.append(", vendor=").append(vendor);
        sb.append(", name='").append(name).append('\'');
        sb.append(", introduction='").append(introduction).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", dateInstalled=").append(dateInstalled);
        sb.append('}');
        return sb.toString();
    }

    public boolean isInstallWithParameters() {
        return installWithParameters;
    }

    public void setInstallWithParameters(final boolean installWithParameters) {
        this.installWithParameters = installWithParameters;
    }

    public List<Dependency> getPluginDependencies() {
        return pluginDependencies;
    }

    public void setPluginDependencies(final List<Dependency> pluginDependencies) {
        this.pluginDependencies = pluginDependencies;
    }

    public InstallState getState() {
        return state;
    }

    public void setState(final InstallState state) {
        this.state = state;
    }

    public String getDependencySummary() {
        return dependencySummary;
    }

    public void setDependencySummary(final String dependencySummary) {
        this.dependencySummary = dependencySummary;
    }

    public boolean getHasMessages() {
        return hasMessages;
    }

    public void setHasMessages(final boolean hasMessages) {
        this.hasMessages = hasMessages;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Vendor implements Serializable {
        private String name;
        private String url;
        private String logo;

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

        public String getLogo() {
            return logo;
        }

        public void setLogo(final String logo) {
            this.logo = logo;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Dependency implements Serializable{
        private String pluginId;
        @JsonIgnore private InstallState minStateForInstalling = InstallState.INSTALLING;

        public String getPluginId() {
            return pluginId;
        }

        public void setPluginId(final String pluginId) {
            this.pluginId = pluginId;
        }

        public InstallState getMinStateForInstalling() {
            return minStateForInstalling;
        }

        public String getMinInstallStateForInstalling() {
            return minStateForInstalling != null ? minStateForInstalling.toString() : null;
        }

        public void setMinInstallStateForInstalling(final String minInstallStateForInstalling) {
            this.minStateForInstalling = InstallState.fromString(minInstallStateForInstalling);
        }
    }
}