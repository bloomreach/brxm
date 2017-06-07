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

import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.config.Document;

/**
 * ProjectSettings is used for storing global Essentials dashboard settings (user settings).
 *
 * @version "$Id$"
 */
public interface ProjectSettings extends Document {


    String getBeansFolder();

    void setBeansFolder(String beansFolder);

    String getSiteModule();

    void setSiteModule(String siteFolder);

    String getCmsModule();

    void setCmsModule(String cmsFolder);

    String getRepositoryDataModule();

    void setRepositoryDataModule(String repositoryDataFolder);

    String getWebfilesSubModule();

    void setWebfilesSubModule(String webfilesFolder);

    Boolean getSetupDone();

    void setSetupDone(Boolean setupDone);

    String getProjectNamespace();

    void setProjectNamespace(String projectNamespace);

    String getSelectedRestPackage();

    void setSelectedRestPackage(String selectedRestPackage);

    String getSelectedBeansPackage();

    void setSelectedBeansPackage(String selectedBeansPackage);

    String getSelectedComponentsPackage();

    void setSelectedComponentsPackage(String selectedComponentsPackage);

    String getSelectedProjectPackage();

    void setSelectedProjectPackage(String selectedProjectPackage);

    String getTemplateLanguage();

    void setTemplateLanguage(String templateLanguage);

    boolean isUseSamples();

    void setUseSamples(boolean useSamples);

    boolean isEnterprise();

    void setEnterprise(boolean enterprise);

    boolean isConfirmParams();

    void setConfirmParams(boolean confirmParams);

    Set<String> getPluginRepositories();

    void setPluginRepositories(Set<String> pluginRepositories);

    boolean isExtraTemplates();

    void setExtraTemplates(boolean extraTemplates);

}
