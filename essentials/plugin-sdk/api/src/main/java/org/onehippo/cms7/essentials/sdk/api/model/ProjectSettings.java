/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.model;

/**
 * ProjectSettings exposes Essentials' project settings to Essentials plugins. The typical way to programmatically
 * obtain the current project settings is to @Inject the SettingsService into the plugin code and call #get on it.
 *
 * The project settings can partially be adjusted in the dashboard's #introduction page, or completely by editing
 * the file src/main/resources/project-settings.xml in your project's Essentials module.
 */
public interface ProjectSettings {

    /**
     * The name of the project root-level directory representing the 'site' module, defaults to 'site'.
     */
    String getSiteModule();

    /**
     * The name of the sub-directory of the site components module, representing the components module, defaults to 'components'.
     */
    String getSiteComponentsSubModule();

    /**
     * The name of the sub-directory of the site webapp module, representing the web application module, defaults to 'webapp'.
     */
    String getSiteWebappSubModule();

    /**
     * The name of the project root-level directory representing the 'cms' module, defaults to 'cms'.
     */
    String getCmsModule();

    /**
     * The name of the project root-level directory representing the 'cms-dependencies' module, defaults to 'cms-dependencies'.
     */
    String getCmsDependenciesModule();

    /**
     * The name of the project root-level directory representing the repository data module, defaults to 'repository-data'.
     */
    String getRepositoryDataModule();

    /**
     * The name of the sub-directory of the repository data module, representing the application module, defaults to 'application'.
     */
    String getApplicationSubModule();

    /**
     * The name of the sub-directory of the repository data module, representing the development module, defaults to 'development'.
     */
    String getDevelopmentSubModule();

    /**
     * The name of the sub-directory of the repository data module, representing the web files module, defaults to 'webfiles'.
     */
    String getWebfilesSubModule();

    /**
     * The primary JCR namespace used by the project, for example 'myhippoproject'.
     */
    String getProjectNamespace();

    /**
     * The nested Java package name used primarily throughout the project, for example 'com.myhippoproject'.
     */
    String getSelectedProjectPackage();

    /**
     * The nested Java package name containing the project's REST resources, typically below the 'project package'.
     */
    String getSelectedRestPackage();

    /**
     * The nested Java package name containing the project's HST bean classes, typically below the 'project package'.
     */
    String getSelectedBeansPackage();

    /**
     * The nested Java package name containing the project's HST component classes, typically below the 'project package'.
     */
    String getSelectedComponentsPackage();

    /**
     * The templating language used primarily by the project. Supported are 'jsp' and 'freemarker'.
     */
    String getTemplateLanguage();

    /**
     * Flag indicating that, unless specified otherwise, when installing a plugin, samples also should be installed
     * (if available in the plugin).
     */
    boolean isUseSamples();

    /**
     * Flag indicating if plugins pertaining to enterprise features should be displayed in the dashboard application.
     *
     * Note that in order to *use* enterprise features successfully, you need access to BloomReach's enterprise maven
     * repository.
     */
    boolean isEnterprise();

    /**
     * Flag indicating that for every plugin which has installation parameters, the dashboard user should be prompted
     * to set or confirm the parmeters before installation.
     */
    boolean isConfirmParams();

    /**
     * Flag indicating that, unless specified otherwise, when installing a plugin, extra templates should be installed
     * (if available in the plugin).
     */
    boolean isExtraTemplates();

    /**
     * Optional alternative directory path relative to the project's root directory to point to the Java sources
     * root of the module where your HST beans live, i.e. relative to which the 'selectedBeansPackage' is applicable.
     *
     * When specified, make sure to use the appropriate directory separator character used for your operating system.
     * Development environments with different separators (i.e. Unix vs. Windows) is currently not supported for this
     * parameter.
     *
     * Example/default: 'site/components/src/main/java'
     */
    String getBeansFolder();
}
