/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.generated.jaxb.WebXml;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.utils.common.PackageVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public final class ProjectUtils {


    public static final String ENT_GROUP_ID = "com.onehippo.cms7";
    public static final String ENT_ARTIFACT_ID = "hippo-cms7-enterprise-release";
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
        final File bootstrapFolder = getFolder(context.getProjectSettings().getBootstrapModule());
        final String webfilesPath = bootstrapFolder.getAbsolutePath() + File.separator + "webfiles" + File.separator
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
     * Returns Configuration root folder e.g. {@code /home/foo/myproject/bootstrap/configuration}
     *
     * @return Configuration project folder
     */
    public static File getBootstrapConfigFolder(final PluginContext context) {
        return getFolder(context.getProjectSettings().getBootstrapModule()+File.separator + "configuration");

    }

    /**
     * Returns Content root folder e.g. {@code /home/foo/myproject/bootstrap/content}
     *
     * @return Content project folder
     */
    public static File getBootstrapContentFolder(final PluginContext context) {
        return getFolder(context.getProjectSettings().getBootstrapModule() + File.separator + "content");
    }

    /**
     * Returns Webfiles root folder e.g. {@code /home/foo/myproject/bootstrap/webfiles}
     *
     * @return Webfiles project folder
     */
    public static File getBootstrapWebfilesFolder(final PluginContext context) {
        return getFolder(context.getProjectSettings().getBootstrapModule() + File.separator + "webfiles");
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
     * Returns Bootstrap root folder e.g. {@code /home/foo/myproject/bootstrap}
     *
     * @return Bootstrap project folder
     */
    public static File getBootstrapFolder(final PluginContext context) {
        return getFolder(context.getProjectSettings().getBootstrapModule());
    }

    public static Model getPomModel(final PluginContext context, final TargetPom targetPom) {
        final String pomPath = getPomPath(context, targetPom);
        if (Strings.isNullOrEmpty(pomPath)) {
            throw new IllegalStateException("pom.xml could not be found for:" + targetPom);
        }
        return getPomModel(pomPath);

    }

    /**
     * Read web.xml file content
     *
     * @param path path of XMl file
     * @return
     */
    public static WebXml readWebXmlFile(final String path) {
        try {
            final JAXBContext context = JAXBContext.newInstance(WebXml.class);
            final Marshaller m = context.createMarshaller();
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            return (WebXml) unmarshaller.unmarshal(new File(path));
        } catch (JAXBException e) {
            log.error("Error reading web.xml:" + path, e);
        }
        return null;
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
            case BOOTSTRAP:
                return getPomForDir(ProjectUtils.getBootstrapFolder(context));
            case BOOTSTRAP_CONFIG:
                return getPomForDir(ProjectUtils.getBootstrapConfigFolder(context));
            case BOOTSTRAP_CONTENT:
                return getPomForDir(ProjectUtils.getBootstrapContentFolder(context));
            case ESSENTIALS:
                return getPomForDir(ProjectUtils.getEssentialsFolderName(context));
            case BOOTSTRAP_WEB_FILES:
                return getPomForDir(ProjectUtils.getBootstrapWebfilesFolder(context));
        }
        return null;

    }

    public static String getWebXmlPath(final PluginContext context, final TargetPom targetPom) {
        if (targetPom == null
                || targetPom == TargetPom.INVALID
                || targetPom == TargetPom.BOOTSTRAP
                || targetPom == TargetPom.BOOTSTRAP_CONFIG
                || targetPom == TargetPom.BOOTSTRAP_CONTENT) {
            return null;
        }
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
