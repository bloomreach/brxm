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

package org.onehippo.cms7.essentials.plugin.sdk.config;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.sdk.api.model.ProjectSettings;
import org.onehippo.cms7.essentials.sdk.api.model.Module;

import com.google.common.base.Strings;

@XmlRootElement(name = "project")
public class ProjectSettingsBean implements ProjectSettings {

    private String projectNamespace;

    private String selectedBeansPackage;
    private String selectedProjectPackage;
    private String selectedComponentsPackage;
    private String selectedRestPackage;

    private String templateLanguage;
    private boolean useSamples;
    private boolean enterprise;
    private boolean confirmParams;
    private boolean extraTemplates;

    private String siteModule;
    private String siteComponentsSubModule;
    private String siteWebappSubModule;
    private String cmsModule;
    private String cmsDependenciesModule;
    private String repositoryDataModule;
    private String applicationSubModule;
    private String developmentSubModule;
    private String webfilesSubModule;
    private String beansFolder;

    private boolean setupDone;
    private Set<String> pluginRepositories = new HashSet<>();

    @Override
    public String getBeansFolder() {
        return beansFolder;
    }

    public void setBeansFolder(final String beansFolder) {
        this.beansFolder = beansFolder;
    }

    @Override
    public String getSiteModule() {
        if (Strings.isNullOrEmpty(siteModule)) {
            return Module.SITE.getName();
        }
        return siteModule;
    }

    public void setSiteModule(final String siteModule) {
        this.siteModule = siteModule;
    }

    @Override
    public String getSiteComponentsSubModule() {
        return Strings.isNullOrEmpty(siteComponentsSubModule) ?
                Module.SITE_COMPONENTS.getName() : siteComponentsSubModule;
    }

    public void setSiteComponentsSubModule(final String siteComponentsSubModule) {
        this.siteComponentsSubModule = siteComponentsSubModule;
    }

    @Override
    public String getSiteWebappSubModule() {
        return Strings.isNullOrEmpty(siteWebappSubModule) ?
                Module.SITE_WEBAPP.getName() : siteWebappSubModule;
    }

    public void setSiteWebappSubModule(final String siteWebappSubModule) {
        this.siteWebappSubModule = siteWebappSubModule;
    }

    @Override
    public String getCmsModule() {
        if (Strings.isNullOrEmpty(cmsModule)) {
            return Module.CMS.getName();
        }
        return cmsModule;
    }

    public void setCmsModule(final String cmsModule) {
        this.cmsModule = cmsModule;
    }

    @Override
    public String getCmsDependenciesModule() {
        if (Strings.isNullOrEmpty(cmsDependenciesModule)) {
            return Module.CMS_DEPENDENCIES.getName();
        }
        return cmsDependenciesModule;
    }

    public void setCmsDependenciesModule(final String cmsDependenciesModule) {
        this.cmsDependenciesModule = cmsDependenciesModule;
    }

    @Override
    public String getRepositoryDataModule() {
        if (Strings.isNullOrEmpty(repositoryDataModule)) {
            return Module.REPOSITORY_DATA.getName();
        }
        return repositoryDataModule;
    }

    public void setRepositoryDataModule(final String repositoryDataFolder) {
        repositoryDataModule = repositoryDataFolder;
    }

    @Override
    public String getApplicationSubModule() {
        if (Strings.isNullOrEmpty(applicationSubModule)) {
            return Module.REPOSITORY_DATA_APPLICATION.getName();
        }
        return applicationSubModule;
    }

    public void setApplicationSubModule(final String applicationSubModule) {
        this.applicationSubModule = applicationSubModule;
    }

    @Override
    public String getDevelopmentSubModule() {
        if (Strings.isNullOrEmpty(developmentSubModule)) {
            return Module.REPOSITORY_DATA_DEVELOPMENT.getName();
        }
        return developmentSubModule;
    }

    public void setDevelopmentSubModule(final String developmentSubModule) {
        this.developmentSubModule = developmentSubModule;
    }

    @Override
    public String getWebfilesSubModule() {
        if (Strings.isNullOrEmpty(webfilesSubModule)) {
            return Module.REPOSITORY_DATA_WEB_FILES.getName();
        }
        return webfilesSubModule;
    }

    public void setWebfilesSubModule(final String webfilesSubModule) {
        this.webfilesSubModule = webfilesSubModule;
    }

    @Override
    public String getProjectNamespace() {
        return projectNamespace;
    }

    public void setProjectNamespace(final String projectNamespace) {
        this.projectNamespace = projectNamespace;
    }

    @Override
    public String getSelectedRestPackage() {
        return selectedRestPackage;
    }

    public void setSelectedRestPackage(final String selectedRestPackage) {
        this.selectedRestPackage = selectedRestPackage;
    }

    @Override
    public String getSelectedBeansPackage() {
        return selectedBeansPackage;
    }

    public void setSelectedBeansPackage(final String selectedBeansPackage) {
        this.selectedBeansPackage = selectedBeansPackage;
    }

    @Override
    public String getSelectedComponentsPackage() {
        return selectedComponentsPackage;
    }

    public void setSelectedComponentsPackage(final String selectedComponentsPackage) {
        this.selectedComponentsPackage = selectedComponentsPackage;
    }

    @Override
    public String getSelectedProjectPackage() {
        return this.selectedProjectPackage;
    }

    public void setSelectedProjectPackage(final String selectedProjectPackage) {
        this.selectedProjectPackage = selectedProjectPackage;
    }

    @Override
    public String getTemplateLanguage() {
        return templateLanguage;
    }

    public void setTemplateLanguage(final String templateLanguage) {
        this.templateLanguage = templateLanguage;
    }

    @Override
    public boolean isUseSamples() {
        return useSamples;
    }

    public void setUseSamples(final boolean useSamples) {
        this.useSamples = useSamples;
    }

    @Override
    public boolean isEnterprise() {
        return enterprise;
    }

    public void setEnterprise(final boolean enterprise) {
        this.enterprise = enterprise;
    }

    @Override
    public boolean isConfirmParams() {
        return confirmParams;
    }

    public void setConfirmParams(final boolean confirmParams) {
        this.confirmParams = confirmParams;
    }

    @Override
    public boolean isExtraTemplates() {
        return extraTemplates;
    }

    public void setExtraTemplates(final boolean extraTemplates) {
        this.extraTemplates = extraTemplates;
    }

    public boolean getSetupDone() {
        return setupDone;
    }

    public void setSetupDone(final boolean setupDone) {
        this.setupDone = setupDone;
    }

    public Set<String> getPluginRepositories() {
        return pluginRepositories;
    }

    public void setPluginRepositories(final Set<String> pluginRepositories) {
        this.pluginRepositories = pluginRepositories;
    }
}

