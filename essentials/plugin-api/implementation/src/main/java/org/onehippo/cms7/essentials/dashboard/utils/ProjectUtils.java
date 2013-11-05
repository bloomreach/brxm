/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.onehippo.cms7.essentials.dashboard.DependencyType;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.common.PackageVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id: ProjectUtils.java 164013 2013-05-11 14:05:39Z mmilicevic $"
 */
public class ProjectUtils {

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

    public static File getSiteImagesFolder(){
        final File site = getSite();
        final String absolutePath = site.getAbsolutePath() + File.separator + "src"+ File.separator + "main"+ File.separator + "webapp"+ File.separator + "images";
        return new File(absolutePath);
    }


    public static File getSiteJspFolder() {
        final File site = getSite();
        final String absolutePath = site.getAbsolutePath() + File.separator + "src" + File.separator + "main" + File.separator + "webapp"
                + File.separator + "WEB-INF"
                + File.separator + "jsp";
        return new File(absolutePath);
    }


    /**
     * Returns SITE root folder e.g. {@code /home/foo/myproject/site}
     * @return  site project folder
     */
    public static File getSite() {
        return getFolder("site");
    }

    /**
     * Returns CMS root folder e.g. {@code /home/foo/myproject/cms}
     *
     * @return CMS project folder
     */
    public static File getCms() {
        return getFolder("cms");
    }

    private static File getFolder(final String name) {
        final String baseDir = getBaseProjectDirectory();
        if (Strings.isNullOrEmpty(baseDir)) {
            return null;
        }
        final File baseFile = new File(baseDir);
        if (!baseFile.exists() || !baseFile.isDirectory()) {
            return null;
        }
        File site = new File(baseDir + File.separatorChar + name);
        if (site.isDirectory()) {
            return site;
        }
        return null;
    }

    public static String getBaseProjectDirectory() {
        if (System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY) != null && !System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY).isEmpty()) {
            return System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY);
        } else {
            return null;
        }
    }

    private static Model getPomModel(String path) {
        Reader fileReader = null;
        Model model = null;
        try {
            final MavenXpp3Reader reader = new MavenXpp3Reader();
            fileReader = new FileReader(path + File.separatorChar + "pom.xml");
            model = reader.read(fileReader);
        } catch (XmlPullParserException | IOException e) {
            log.error("Error parsing pom", e);
        } finally {
            IOUtils.closeQuietly(fileReader);
        }
        return model;
    }

    public static Model getSitePomModel() {
        if (ProjectUtils.getSite() != null) {
            return getPomModel(ProjectUtils.getSite().getPath());
        } else {
            return null;
        }
    }

    public static Model getCMSPomModel() {
        if (ProjectUtils.getCms() != null) {
            return getPomModel(ProjectUtils.getCms().getPath());
        } else {
            return null;
        }
    }

    public static boolean isInstalled(final DependencyType type, final Dependency dependency) {
        final Model model = type == DependencyType.SITE ? getSitePomModel() : getCMSPomModel();
        final List<Dependency> dependencies = model.getDependencies();
        for (Dependency dep : dependencies) {
            if (dep.getArtifactId().equals(dependency.getArtifactId()) && dep.getGroupId().equals(dependency.getGroupId())) {
                return true;
            }

        }
        return false;
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

}
