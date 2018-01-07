/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.services;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.service.ProjectService;
import org.onehippo.cms7.essentials.dashboard.service.SettingsService;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.springframework.stereotype.Service;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final static String MODULE_NAME_APPLICATION = "application";
    private final static String MODULE_NAME_CMS = "cms";
    private final static String MODULE_NAME_DEVELOPMENT = "development";
    private final static String MODULE_NAME_REPOSITORY_DATA = "repository-data";
    private final static String MODULE_NAME_SITE = "site";
    private final static String MODULE_NAME_WEB_FILES = "webfiles";

    @Inject private SettingsService settingsService;

    @Override
    public Path getBasePathForModule(final TargetPom module) {
        final Path projectPath = Paths.get(ProjectUtils.getBaseProjectDirectory());

        switch (module) {
            case PROJECT:
                return projectPath;
            case ESSENTIALS:
                return projectPath.resolve(ProjectUtils.getEssentialsModuleName());
            case SITE:
                return projectPath.resolve(getSiteModuleName());
            case CMS:
                return projectPath.resolve(getCmsModuleName());
            case REPOSITORY_DATA:
                return projectPath.resolve(getRepositoryDataModuleName());
            case REPOSITORY_DATA_APPLICATION:
                return projectPath.resolve(getRepositoryDataModuleName()).resolve(getApplicationModuleName());
            case REPOSITORY_DATA_DEVELOPMENT:
                return projectPath.resolve(getRepositoryDataModuleName()).resolve(getDevelopmentModuleName());
            case REPOSITORY_DATA_WEB_FILES:
                return projectPath.resolve(getRepositoryDataModuleName()).resolve(getWebFilesModuleName());
        }

        throw new IllegalArgumentException("Invalid module to derive path: " + module);
    }

    @Override
    public Path getPomPathForModule(final TargetPom module) {
        return getBasePathForModule(module).resolve("pom.xml");
    }

    @Override
    public Path getJavaRootPathForModule(final TargetPom module) {
        return getBasePathForModule(module).resolve("src").resolve("main").resolve("java");
    }

    @Override
    public Path getResourcesRootPathForModule(final TargetPom module) {
        return getBasePathForModule(module).resolve("src").resolve("main").resolve("resources");
    }

    @Override
    public Path getWebApplicationRootPathForModule(final TargetPom module) {
        return getBasePathForModule(module).resolve("src").resolve("main").resolve("webapp");
    }

    @Override
    public Path getWebInfPathForModule(final TargetPom module) {
        return getWebApplicationRootPathForModule(module).resolve("WEB-INF");
    }

    @Override
    public Path getBeansRootPath() {
        final String explicitBeansFolder = settingsService.getSettings().getBeansFolder();

        if (StringUtils.isNotBlank(explicitBeansFolder)) {
            final Path projectPath = getBasePathForModule(TargetPom.PROJECT);
            return projectPath.resolve(explicitBeansFolder);
        }

        return getJavaRootPathForModule(TargetPom.SITE);
    }

    @Override
    public Path getBeansPackagePath() {
        final String beansPackage = settingsService.getSettings().getSelectedBeansPackage();
        return makePackagePath(getBeansRootPath(), beansPackage);
    }

    @Override
    public Path getRestPackagePath() {
        final String restPackage = settingsService.getSettings().getSelectedRestPackage();
        return makePackagePath(getJavaRootPathForModule(TargetPom.SITE), restPackage);
    }

    @Override
    public Path getComponentsPackagePath() {
        final String componentsPackage = settingsService.getSettings().getSelectedComponentsPackage();
        return makePackagePath(getJavaRootPathForModule(TargetPom.SITE), componentsPackage);
    }

    @Override
    public Path getContextXmlPath() {
        return getConfDirPath().resolve("context.xml");
    }

    @Override
    public Path getAssemblyFolderPath() {
        return getBasePathForModule(TargetPom.PROJECT).resolve("src").resolve("main").resolve("assembly");
    }

    private Path makePackagePath(final Path basePath, final String packagesName) {
        Path packagePath = basePath;
        for (String packageName : packagesName.split("\\.")) {
            packagePath = packagePath.resolve(packageName);
        }
        return packagePath;
    }

    private String getSiteModuleName() {
        final String siteModuleName = settingsService.getSettings().getSiteModule();
        return StringUtils.isNotBlank(siteModuleName) ? siteModuleName : MODULE_NAME_SITE;
    }

    private String getCmsModuleName() {
        final String cmsModuleName = settingsService.getSettings().getCmsModule();
        return StringUtils.isNotBlank(cmsModuleName) ? cmsModuleName : MODULE_NAME_CMS;
    }

    private String getRepositoryDataModuleName() {
        final String repositoryDataModuleName = settingsService.getSettings().getRepositoryDataModule();
        return StringUtils.isNotBlank(repositoryDataModuleName) ? repositoryDataModuleName : MODULE_NAME_REPOSITORY_DATA;
    }

    private String getApplicationModuleName() {
        final String applicationModuleName = settingsService.getSettings().getApplicationSubModule();
        return StringUtils.isNotBlank(applicationModuleName) ? applicationModuleName : MODULE_NAME_APPLICATION;
    }

    private String getDevelopmentModuleName() {
        final String developmentModuleName = settingsService.getSettings().getDevelopmentSubModule();
        return StringUtils.isNotBlank(developmentModuleName) ? developmentModuleName : MODULE_NAME_DEVELOPMENT;
    }

    private String getWebFilesModuleName() {
        final String webFilesModuleName = settingsService.getSettings().getWebfilesSubModule();
        return StringUtils.isNotBlank(webFilesModuleName) ? webFilesModuleName : MODULE_NAME_WEB_FILES;
    }

    private Path getConfDirPath() {
        return getBasePathForModule(TargetPom.PROJECT).resolve("conf");
    }

    @Override
    public List<File> getLog4j2Files() {
        final FilenameFilter log4j2Filter = (dir, name) -> name.matches("log4j2.*\\.xml");
        final File[] log4j2Files = getConfDirPath().toFile().listFiles(log4j2Filter);

        return log4j2Files != null ? Arrays.asList(log4j2Files) : Collections.emptyList();
    }
}
