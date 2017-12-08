/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Strings;

import org.onehippo.cms7.essentials.dashboard.rest.PluginModuleRestful;


public class PluginDescriptorRestful implements PluginDescriptor, Restful {

    private static final long serialVersionUID = 1L;

    private List<String> restClasses;

    @JsonDeserialize(as = VendorRestful.class)
    @JsonSerialize(as = VendorRestful.class)
    private Vendor vendor;
    private List<ModuleMavenDependency> dependencies;
    private List<ModuleMavenRepository> repositories;
    private String name;
    private String introduction;
    private String description;
    private List<String> imageUrls;
    private String id;
    private String packageClass;
    private boolean hasConfiguration = false;
    private String packageFile;
    private String type;
    private String installState;
    private String icon;

    private Calendar dateInstalled;
    private String documentationLink;
    private List<PluginModuleRestful.PrefixedLibrary> libraries = new ArrayList<>();

    private Map<String, Set<String>> categories;


    @Override
    public Map<String, Set<String>> getCategories() {
        return categories;
    }

    @Override
    public void setCategories(final Map<String, Set<String>> categories) {
        this.categories = categories;
    }


    public void addLibrary(final PluginModuleRestful.PrefixedLibrary library) {
        libraries.add(library);
    }

    public void addAllLibraries(final List<PluginModuleRestful.PrefixedLibrary> libraries) {
        if (this.libraries == null) {
            this.libraries = new ArrayList<>();

        }
        this.libraries.addAll(libraries);
    }

    public List<PluginModuleRestful.PrefixedLibrary> getLibraries() {
        return libraries;
    }

    public void setLibraries(final List<PluginModuleRestful.PrefixedLibrary> libraries) {
        this.libraries = libraries;
    }

    public PluginDescriptorRestful(final String name) {
        this.name = name;
    }

    public PluginDescriptorRestful() {

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
    public String getInstallState() {
        return installState;
    }

    @Override
    public void setInstallState(final String installState) {
        this.installState = installState;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public String getPackageFile() {
        return packageFile;
    }

    @Override
    public void setPackageFile(final String packageFile) {
        this.packageFile = packageFile;
    }

    @Override
    public String getPackageClass() {
        return packageClass;
    }

    @Override
    public void setPackageClass(final String packageClass) {
        this.packageClass = packageClass;
    }

    @Override
    public void setHasConfiguration(final boolean hasConfiguration) {
        this.hasConfiguration = hasConfiguration;
    }

    @Override
    public boolean getHasConfiguration() {
        return hasConfiguration;
    }
    
    @Override
    public Vendor getVendor() {
        return vendor;
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
    public List<ModuleMavenDependency> getDependencies() {
        if (dependencies == null) {
            return new ArrayList<>();
        }
        return dependencies;
    }

    @Override
    public void setDependencies(final List<ModuleMavenDependency> dependencies) {
        this.dependencies = dependencies;
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public List<String> getImageUrls() {
        return imageUrls;
    }

    @Override
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    @Override
    public List<ModuleMavenRepository> getRepositories() {
        if (repositories == null) {
            return new ArrayList<>();
        }
        return repositories;
    }

    @Override
    public void setRepositories(final List<ModuleMavenRepository> repositories) {
        this.repositories = repositories;
    }

    @Override
    public String getIcon() {
        if (Strings.isNullOrEmpty(icon)) {
            return "/essentials/images/icons/missing-icon.png";
        }
        return icon;
    }

    @Override
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
}