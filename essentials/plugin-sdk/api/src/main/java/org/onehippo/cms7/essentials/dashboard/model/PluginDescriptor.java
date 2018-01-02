/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;

public class PluginDescriptor implements Restful {

    public static final String TYPE_TOOL = "tool";
    public static final String TYPE_FEATURE = "feature";

    private List<String> restClasses;

    private Vendor vendor;
    private List<MavenDependency.WithModule> dependencies;
    private List<MavenRepository.WithModule> repositories;
    private String name;
    private String introduction;
    private String description;
    private List<String> imageUrls;
    private String id;
    private String packageClass;
    private boolean hasConfiguration;
    private boolean noRebuildAfterSetup;
    private boolean setupParameters = true; // for plugins with no setup parameters, the setup phase can always be triggered automatically
    private String packageFile;
    private String type;
    private String installState;
    private String icon;

    private Calendar dateInstalled;
    private String documentationLink;

    private Map<String, Set<String>> categories;

    public PluginDescriptor(final String name) {
        this.name = name;
    }

    public PluginDescriptor() {

    }

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
        return installState;
    }

    public void setInstallState(final String installState) {
        this.installState = installState;
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

    public String getPackageClass() {
        return packageClass;
    }

    public void setPackageClass(final String packageClass) {
        this.packageClass = packageClass;
    }

    public void setHasConfiguration(final boolean hasConfiguration) {
        this.hasConfiguration = hasConfiguration;
    }

    public boolean getHasConfiguration() {
        return hasConfiguration;
    }

    public boolean isNoRebuildAfterSetup() {
        return noRebuildAfterSetup;
    }

    public void setNoRebuildAfterSetup(final boolean noRebuildAfterSetup) {
        this.noRebuildAfterSetup = noRebuildAfterSetup;
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

    public List<MavenDependency.WithModule> getDependencies() {
        if (dependencies == null) {
            return new ArrayList<>();
        }
        return dependencies;
    }

    public void setDependencies(final List<MavenDependency.WithModule> dependencies) {
        this.dependencies = dependencies;
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

    public List<MavenRepository.WithModule> getRepositories() {
        if (repositories == null) {
            return new ArrayList<>();
        }
        return repositories;
    }

    public void setRepositories(final List<MavenRepository.WithModule> repositories) {
        this.repositories = repositories;
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
        sb.append(", dependencies=").append(dependencies);
        sb.append(", name='").append(name).append('\'');
        sb.append(", introduction='").append(introduction).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", dateInstalled=").append(dateInstalled);
        sb.append('}');
        return sb.toString();
    }

    public boolean hasSetupParameters() {
        return setupParameters;
    }

    public void setSetupParameters(final boolean noSetupParameters) {
        this.setupParameters = noSetupParameters;
    }

    public static class Vendor implements Restful {
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
}