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

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;

import com.google.common.base.Strings;

@XmlRootElement(name = "project")
public class ProjectSettingsBean extends BaseDocument implements ProjectSettings {

    public static final String DEFAULT_NAME = "project-settings";

    private String projectNamespace;

    private String selectedBeansPackage;
    private String selectedProjectPackage;
    private String selectedComponentsPackage;
    private String selectedRestPackage;
    private Boolean setupDone;

    private String templateLanguage;
    private boolean useSamples;
    private boolean enterprise;
    private boolean confirmParams;
    private boolean extraTemplates;

    private String siteModule;
    private String cmsModule;
    private String repositoryDataModule;
    private String applicationSubModule;
    private String developmentSubModule;
    private String webfilesSubModule;
    private String beansFolder;


    @Override
    public String getBeansFolder() {
        return beansFolder;
    }

    @Override
    public void setBeansFolder(final String beansFolder) {
        this.beansFolder = beansFolder;
    }

    @Override
    public String getSiteModule() {
        if (Strings.isNullOrEmpty(siteModule)) {
            return TargetPom.SITE.getName();
        }
        return siteModule;
    }

    @Override
    public void setSiteModule(final String siteModule) {
        this.siteModule = siteModule;
    }

    @Override
    public String getCmsModule() {
        if (Strings.isNullOrEmpty(cmsModule)) {
            return TargetPom.CMS.getName();
        }
        return cmsModule;
    }

    @Override
    public void setCmsModule(final String cmsModule) {
        this.cmsModule = cmsModule;
    }

    @Override
    public String getRepositoryDataModule() {
        if (Strings.isNullOrEmpty(repositoryDataModule)) {
            return TargetPom.REPOSITORY_DATA.getName();
        }
        return repositoryDataModule;
    }

    @Override
    public void setRepositoryDataModule(final String repositoryDataFolder) {
        repositoryDataModule = repositoryDataFolder;
    }

    @Override
    public String getApplicationSubModule() {
        if (Strings.isNullOrEmpty(applicationSubModule)) {
            return TargetPom.REPOSITORY_DATA_APPLICATION.getName();
        }
        return applicationSubModule;
    }

    @Override
    public void setApplicationSubModule(final String applicationSubModule) {
        this.applicationSubModule = applicationSubModule;
    }

    @Override
    public String getDevelopmentSubModule() {
        if (Strings.isNullOrEmpty(developmentSubModule)) {
            return TargetPom.REPOSITORY_DATA_DEVELOPMENT.getName();
        }
        return developmentSubModule;
    }

    @Override
    public void setDevelopmentSubModule(final String developmentSubModule) {
        this.developmentSubModule = developmentSubModule;
    }

    @Override
    public String getWebfilesSubModule() {
        if (Strings.isNullOrEmpty(webfilesSubModule)) {
            return TargetPom.REPOSITORY_DATA_WEB_FILES.getName();
        }
        return webfilesSubModule;
    }

    @Override
    public void setWebfilesSubModule(final String webfilesSubModule) {
        this.webfilesSubModule = webfilesSubModule;
    }

    private Set<String> pluginRepositories = new HashSet<>();


    public ProjectSettingsBean() {
        super(DEFAULT_NAME);
    }

    public ProjectSettingsBean(final String name) {
        super(name);
    }

    @Override
    public Boolean getSetupDone() {
        return setupDone == null ? false : setupDone;
    }

    @Override
    public void setSetupDone(final Boolean setupDone) {
        if (setupDone == null) {
            this.setupDone = false;
        } else {
            this.setupDone = setupDone;
        }
    }


    @Override
    public String getProjectNamespace() {
        return projectNamespace;
    }

    @Override
    public void setProjectNamespace(final String projectNamespace) {
        this.projectNamespace = projectNamespace;
    }

    @Override
    public String getSelectedRestPackage() {
        return selectedRestPackage;
    }

    @Override
    public void setSelectedRestPackage(final String selectedRestPackage) {
        this.selectedRestPackage = selectedRestPackage;
    }

    @Override
    public String getSelectedBeansPackage() {
        return selectedBeansPackage;
    }

    @Override
    public void setSelectedBeansPackage(final String selectedBeansPackage) {
        this.selectedBeansPackage = selectedBeansPackage;
    }

    @Override
    public String getSelectedComponentsPackage() {
        return selectedComponentsPackage;
    }

    @Override
    public void setSelectedComponentsPackage(final String selectedComponentsPackage) {
        this.selectedComponentsPackage = selectedComponentsPackage;
    }

    @Override
    public String getSelectedProjectPackage() {
        return this.selectedProjectPackage;
    }

    @Override
    public void setSelectedProjectPackage(final String selectedProjectPackage) {
        this.selectedProjectPackage = selectedProjectPackage;
    }

    @Override
    public String getTemplateLanguage() {
        return templateLanguage;
    }

    @Override
    public void setTemplateLanguage(final String templateLanguage) {
        this.templateLanguage = templateLanguage;
    }

    @Override
    public boolean isUseSamples() {
        return useSamples;
    }

    @Override
    public void setUseSamples(final boolean useSamples) {
        this.useSamples = useSamples;
    }

    @Override
    public boolean isEnterprise() {
        return enterprise;
    }

    @Override
    public void setEnterprise(final boolean enterprise) {
        this.enterprise = enterprise;
    }

    @Override
    public boolean isConfirmParams() {
        return confirmParams;
    }

    @Override
    public void setConfirmParams(final boolean confirmParams) {
        this.confirmParams = confirmParams;
    }

    @Override
    public Set<String> getPluginRepositories() {
        return pluginRepositories;
    }

    @Override
    public void setPluginRepositories(final Set<String> pluginRepositories) {
        this.pluginRepositories = pluginRepositories;

    }

    public void addPluginRepository(final String path) {
        pluginRepositories.add(path);
    }

    @Override
    public boolean isExtraTemplates() {
        return extraTemplates;
    }

    @Override
    public void setExtraTemplates(final boolean extraTemplates) {
        this.extraTemplates = extraTemplates;
    }
}

