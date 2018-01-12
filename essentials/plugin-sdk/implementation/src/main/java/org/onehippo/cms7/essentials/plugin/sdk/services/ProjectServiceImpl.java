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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.service.model.Module;
import org.onehippo.cms7.essentials.plugin.sdk.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.service.SettingsService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.ProjectUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final static String MODULE_NAME_APPLICATION = "application";
    private final static String MODULE_NAME_CMS = "cms";
    private final static String MODULE_NAME_DEVELOPMENT = "development";
    private final static String MODULE_NAME_REPOSITORY_DATA = "repository-data";
    private final static String MODULE_NAME_SITE = "site";
    private final static String MODULE_NAME_WEB_FILES = "webfiles";
    private final static Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

    @Inject private SettingsService settingsService;

    @Override
    public Path getBasePathForModule(final Module module) {
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
    public Path getPomPathForModule(final Module module) {
        return getBasePathForModule(module).resolve("pom.xml");
    }

    @Override
    public Path getJavaRootPathForModule(final Module module) {
        return getBasePathForModule(module).resolve("src").resolve("main").resolve("java");
    }

    @Override
    public Path getResourcesRootPathForModule(final Module module) {
        return getBasePathForModule(module).resolve("src").resolve("main").resolve("resources");
    }

    @Override
    public Path getWebApplicationRootPathForModule(final Module module) {
        return getBasePathForModule(module).resolve("src").resolve("main").resolve("webapp");
    }

    @Override
    public Path getWebInfPathForModule(final Module module) {
        return getWebApplicationRootPathForModule(module).resolve("WEB-INF");
    }

    @Override
    public Path getBeansRootPath() {
        final String explicitBeansFolder = settingsService.getSettings().getBeansFolder();

        if (StringUtils.isNotBlank(explicitBeansFolder)) {
            final Path projectPath = getBasePathForModule(Module.PROJECT);
            return projectPath.resolve(explicitBeansFolder);
        }

        return getJavaRootPathForModule(Module.SITE);
    }

    @Override
    public Path getBeansPackagePath() {
        final String beansPackage = settingsService.getSettings().getSelectedBeansPackage();
        return makePackagePath(getBeansRootPath(), beansPackage);
    }

    @Override
    public Path getRestPackagePath() {
        final String restPackage = settingsService.getSettings().getSelectedRestPackage();
        return makePackagePath(getJavaRootPathForModule(Module.SITE), restPackage);
    }

    @Override
    public Path getComponentsPackagePath() {
        final String componentsPackage = settingsService.getSettings().getSelectedComponentsPackage();
        return makePackagePath(getJavaRootPathForModule(Module.SITE), componentsPackage);
    }

    @Override
    public Path getContextXmlPath() {
        return getConfDirPath().resolve("context.xml");
    }

    @Override
    public Path getAssemblyFolderPath() {
        return getBasePathForModule(Module.PROJECT).resolve("src").resolve("main").resolve("assembly");
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
        return getBasePathForModule(Module.PROJECT).resolve("conf");
    }

    @Override
    public List<File> getLog4j2Files() {
        final FilenameFilter log4j2Filter = (dir, name) -> name.matches("log4j2.*\\.xml");
        final File[] log4j2Files = getConfDirPath().toFile().listFiles(log4j2Filter);

        return log4j2Files != null ? Arrays.asList(log4j2Files) : Collections.emptyList();
    }

    @Override
    public boolean copyResource(final String resourcePath, final String targetLocation, final PluginContext context,
                                final boolean canOverwrite, final boolean isBinary) {
        final String destinationPath = TemplateUtils.replaceTemplateData(targetLocation, context.getPlaceholderData());
        final File destination = new File(destinationPath);
        if (!canOverwrite && destination.exists()) {
            log.info("File already exists {}", destinationPath);
            return false;
        }

        try {
            InputStream stream = getClass().getResourceAsStream(resourcePath);
            if (stream == null) {
                log.error("Failed to access resource '{}'.", resourcePath);
                return false;
            }
            // replace file placeholders if needed:
            if (isBinary) {
                FileUtils.copyInputStreamToFile(stream, destination);
            } else {
                final String content = GlobalUtils.readStreamAsText(stream);
                final String replacedData = TemplateUtils.replaceTemplateData(content, context.getPlaceholderData());
                FileUtils.copyInputStreamToFile(IOUtils.toInputStream(replacedData, StandardCharsets.UTF_8), destination);
            }
            log.info("Copied file from '{}' to '{}'.", resourcePath, destinationPath);
        } catch (IOException e) {
            log.error("Failed to copy file from '{}' to '{}'.", resourcePath, destinationPath, e);
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteFile(final String targetLocation, final PluginContext context) {
        final String destinationFile = TemplateUtils.replaceTemplateData(targetLocation, context.getPlaceholderData());
        final Path path = new File(destinationFile).toPath();
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Failed to deleting file '{}'.", destinationFile, e);
            return false;
        }
        return true;
    }
}
