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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Strings;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.utils.common.PackageVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectUtils {

    public static final String ENT_GROUP_ID = "com.onehippo.cms7";
    public static final String ENT_RELEASE_ID = "hippo-cms7-enterprise-release";
    public static final String ENT_REPO_ID = "hippo-maven2-enterprise";
    public static final String ENT_REPO_NAME = "Hippo Enterprise Maven 2";
    public static final String ENT_REPO_URL = "https://maven.onehippo.com/maven2-enterprise";
    public static final String ADDON_EDITION_INDICATOR_ARTIFACT_ID = "hippo-addon-edition-indicator";
    public static final String FOLDER_ASSEMBLY = "src/main/assembly";
    public static final String FOLDER_CONF = "conf";
    public static final String CONTEXT_XML = "context.xml";

    private static Logger log = LoggerFactory.getLogger(ProjectUtils.class);

    private ProjectUtils() {
    }

    public static List<String> getSitePackages(final PluginContext context) {

        final File folder = getSiteJavaFolder(context);
        // traverse folder and add packages:
        final Path path = Paths.get(folder.getAbsolutePath());
        try {
            final PackageVisitor visitor = new PackageVisitor();
            Files.walkFileTree(path, visitor);
            final Collection<String> packages = visitor.getPackages();
            final List<String> packageList = new ArrayList<>(packages);
            Collections.sort(packageList);
            return packageList;
        } catch (IOException e) {
            log.error("Error walking tree", e);
        }

        return Collections.emptyList();
    }


    public static File getSiteJavaFolder(final PluginContext context) {
        final File siteDirectory = context.getSiteDirectory();
        return getJavaFolder(siteDirectory);
    }

    public static File getSiteImagesFolder(final PluginContext context) {
        final File site = getSite(context);
        final String absolutePath = site.getAbsolutePath() + File.separator + "src" + File.separator + "main" + File.separator + "webapp" + File.separator + "images";
        return new File(absolutePath);
    }

    public static File getWebfilesResources(final PluginContext context) {
        final File repositoryDataFolder = getFolder(context.getProjectSettings().getRepositoryDataModule());
        final String webfilesPath = repositoryDataFolder.getAbsolutePath() + File.separator + "webfiles" + File.separator
                + "src" + File.separator + "main" + File.separator + "resources";
        return new File(webfilesPath);
    }

    public static File getWebfiles(final PluginContext context) {
        final File webfilesResources = getWebfilesResources(context);
        // TODO make site part configurable ??
        final String webfilesPath = webfilesResources.getAbsolutePath() + File.separator + "site";
        return new File(webfilesPath);
    }
    /**
     * Returns SITE root folder e.g. {@code /home/foo/myproject/site}
     *
     * @return site project folder
     */
    public static File getSite(final PluginContext context) {
        return getFolder(context.getProjectSettings().getSiteModule());
    }

    public static String getBaseProjectDirectory() {
        if (System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY) != null && !System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY).isEmpty()) {
            return System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY);
        }
        throw new IllegalStateException("System property 'project.basedir' was null or empty. Please start your application with -D=project.basedir=/project/path");
    }

    public static String getEssentialsModuleName() {
        if (System.getProperty(EssentialConst.ESSENTIALS_BASEDIR_PROPERTY) != null && !System.getProperty(EssentialConst.ESSENTIALS_BASEDIR_PROPERTY).isEmpty()) {
            return System.getProperty(EssentialConst.ESSENTIALS_BASEDIR_PROPERTY);
        }
        return "essentials";
    }

    public static File getSiteJspFolder(final PluginContext context) {
        final File site = getSite(context);
        final String absolutePath = site.getAbsolutePath() + File.separator + "src" + File.separator + "main" + File.separator + "webapp"
                + File.separator + "WEB-INF"
                + File.separator + "jsp";
        return new File(absolutePath);
    }

    /**
     * Returns CMS root folder e.g. {@code /home/foo/myproject/cms}
     *
     * @return CMS project folder
     */
    public static File getCms(final PluginContext context) {
        return getFolder(context.getProjectSettings().getCmsModule());

    }

    /**
     * Returns Config root folder e.g. {@code /home/foo/myproject/repository-data/config}
     *
     * @return Configuration project folder
     */
    public static File getRepositoryDataApplicationFolder(final PluginContext context) {
        return getRepositoryDataSubFolder(context, context.getProjectSettings().getApplicationSubModule());
    }

    /**
     * Returns Content root folder e.g. {@code /home/foo/myproject/repository-data/content}
     *
     * @return Content project folder
     */
    public static File getRepositoryDataDevelopmentFolder(final PluginContext context) {
        return getRepositoryDataSubFolder(context, context.getProjectSettings().getDevelopmentSubModule());
    }

    /**
     * Returns Webfiles root folder e.g. {@code /home/foo/myproject/repository-data/webfiles}
     *
     * @return Webfiles project folder
     */
    public static File getRepositoryDataWebfilesFolder(final PluginContext context) {
        return getRepositoryDataSubFolder(context, context.getProjectSettings().getWebfilesSubModule());
    }

    private static File getRepositoryDataSubFolder(final PluginContext context, final String subModule) {
        return getFolder(context.getProjectSettings().getRepositoryDataModule() + File.separator + subModule);
    }

    /**
     * Returns Essentials root folder e.g. {@code /home/foo/myproject/essentials}
     *
     * @return Essentials project folder
     */
    public static File getEssentialsFolderName(final PluginContext context) {
        return new File(getBaseProjectDirectory() +File.separator + getEssentialsModuleName());
    }


    public static File getEssentialsResourcesFolder() {
        final File root = getProjectRootFolder();
        final String absolutePath = root.getAbsolutePath() + File.separator+ getEssentialsModuleName() + "src" + File.separator + "main" + File.separator + "resources";
        return new File(absolutePath);
    }

    public static File getProjectRootFolder() {
        return new File(getBaseProjectDirectory());
    }

    /**
     * Returns repository-data root folder e.g. {@code /home/foo/myproject/repository-data}
     *
     * @return repository-data project folder
     */
    public static File getRepositoryDataFolder(final PluginContext context) {
        return getFolder(context.getProjectSettings().getRepositoryDataModule());
    }

    public static Model getPomModel(final PluginContext context, final TargetPom targetPom) {
        final String pomPath = getPomPath(context, targetPom);
        if (Strings.isNullOrEmpty(pomPath)) {
            throw new IllegalStateException("pom.xml could not be found for:" + targetPom);
        }
        return getPomModel(pomPath);

    }

    /**
     * Return full pom.xml file path for given dependency targetPom
     *
     * @param targetPom targetPom of dependency
     * @return null if targetPom is invalid
     */
    public static String getPomPath(final PluginContext context, final TargetPom targetPom) {
        if (targetPom == null || targetPom == TargetPom.INVALID) {
            return null;
        }
        switch (targetPom) {
            case SITE:
                return getPomForDir(ProjectUtils.getSite(context));
            case CMS:
                return getPomForDir(ProjectUtils.getCms(context));
            case PROJECT:
                return getPomForDir(ProjectUtils.getProjectRootFolder());
            case REPOSITORY_DATA:
                return getPomForDir(ProjectUtils.getRepositoryDataFolder(context));
            case REPOSITORY_DATA_APPLICATION:
                return getPomForDir(ProjectUtils.getRepositoryDataApplicationFolder(context));
            case REPOSITORY_DATA_DEVELOPMENT:
                return getPomForDir(ProjectUtils.getRepositoryDataDevelopmentFolder(context));
            case REPOSITORY_DATA_WEB_FILES:
                return getPomForDir(ProjectUtils.getRepositoryDataWebfilesFolder(context));
            case ESSENTIALS:
                return getPomForDir(ProjectUtils.getEssentialsFolderName(context));
        }
        return null;

    }

    public static String getWebXmlPath(final PluginContext context, final TargetPom targetPom) {
        switch (targetPom) {
            case SITE:
                return getWebXmlForDir(ProjectUtils.getSite(context));
            case CMS:
                return getWebXmlForDir(ProjectUtils.getCms(context));
            case ESSENTIALS:
                return getWebXmlForDir(ProjectUtils.getEssentialsFolderName(context));
        }
        return null;

    }

    public static List<File> getLog4j2Files() {
        try {
            FilenameFilter log4j2Filter = (dir, name) -> name.matches("log4j2.*\\.xml");
            return Arrays.asList(getConfFolder().listFiles(log4j2Filter));
        } catch(Exception e) {
            log.error("No log4j2 configuration files found in {}", getConfFolder(), e);
            return new ArrayList<>();
        }
    }

    public static List<File> getAssemblyFiles() {
        try {
            FilenameFilter assemblyFilter = (dir, name) -> name.matches(".*\\.xml");
            return Arrays.asList(getAssemblyFolder().listFiles(assemblyFilter));
        } catch(Exception e) {
            log.error("No assembly files found in {}", getAssemblyFolder().getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }

    public static File getAssemblyFile(String filename) {
        return new File(getAssemblyFolder(), filename);
    }

    public static File getContextXml() {
        return new File(getConfFolder(), CONTEXT_XML);
    }

    private static File getConfFolder() {
        return new File(getProjectRootFolder(), FOLDER_CONF);
    }

    private static File getAssemblyFolder() {
        return new File(getProjectRootFolder(), FOLDER_ASSEMBLY);
    }

    private static File getJavaFolder(final File baseFolder) {
        final String absolutePath = baseFolder.getAbsolutePath();
        final String javaFolder = absolutePath
                + File.separatorChar
                + "src" + File.separatorChar
                + "main" + File.separatorChar
                + "java" + File.separatorChar;
        return new File(javaFolder);
    }

    private static File getFolder(final String name) {
        final String baseDir = GlobalUtils.decodeUrl(ProjectUtils.getBaseProjectDirectory());
        if (Strings.isNullOrEmpty(baseDir)) {
            return null;
        }
        final File baseFile = new File(baseDir);
        if (!baseFile.exists() || !baseFile.isDirectory()) {
            log.warn("Base project folder does not exist or invalid type: {}", baseFile);
            return null;
        }
        final File folder = new File(baseDir + File.separatorChar + name);
        if (!folder.exists()) {
            log.warn("Folder does not exist: {}", folder);
             return null;
        }
        if (folder.isDirectory()) {
            return folder;
        }else{
            log.warn("Expected to get folder but got file: {}", folder);
        }
        return null;
    }

    private static String getPomForDir(final File folder) {
        if (folder != null) {
            return folder.getPath() + File.separatorChar + EssentialConst.POM_XML;
        }
        return null;
    }

    private static Model getPomModel(String path) {

        try (Reader fileReader = new FileReader(path)) {
            final MavenXpp3Reader reader = new MavenXpp3Reader();
            return reader.read(fileReader);
        } catch (XmlPullParserException | IOException e) {
            log.error("Error parsing pom", e);
        }
        return null;

    }

    private static String getWebXmlForDir(final File folder) {

        if (folder != null) {
            return folder.getPath() + File.separatorChar + EssentialConst.PATH_REL_WEB_INF + File.separatorChar + "web.xml";
        }
        return null;

    }


}
