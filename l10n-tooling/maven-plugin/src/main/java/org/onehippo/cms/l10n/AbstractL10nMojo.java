package org.onehippo.cms.l10n;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractL10nMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", readonly = true)
    private String baseDir;

    @Parameter
    private String locales;

    @Parameter
    private String locale;
    
    @Parameter(defaultValue = "Default")
    private String format;
    
    @Component
    protected MavenProject project;
    
    protected final String getModuleName() throws IOException {
        return getBaseDir().getName();
    }
    
    protected final File getBaseDir() throws IOException {
        return new File(baseDir).getCanonicalFile();
    }

    protected final Collection<String> getLocales() throws MojoExecutionException {
        if (StringUtils.isEmpty(locales)) {
            throw new MojoExecutionException("No locales specified");
        }
        final List<String> locales = Arrays.asList(StringUtils.split(this.locales, " ,\t\f\r\n"));
        for (String locale : locales) {
            try {
                LocaleUtils.toLocale(locale);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException("Unrecognized locale: " + locale);
            }
        }
        return locales;
    }

    protected final String getLocale() throws MojoExecutionException {
        if (StringUtils.isEmpty(locale)) {
            throw new MojoExecutionException("No locale specified");
        }
        try {
            LocaleUtils.toLocale(locale);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Unrecognized locale: " + locale);
        }
        return locale;
    }

    protected final String getCSVFormat() throws MojoExecutionException {
        if (StringUtils.isEmpty(format)) {
            throw new MojoExecutionException("No format specified");
        }
        try {
            CSVFormat.valueOf(format);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Unrecognized format: " + format);
        }
        return format;
    }
    
    protected final ClassLoader getResourcesClassLoader() throws MalformedURLException {
        return new URLClassLoader(getHippoArtifactFiles());
    }
    
    private URL[] getHippoArtifactFiles() throws MalformedURLException {
        final Collection<URL> artifactFiles = new ArrayList<>();
        for (Artifact artifact : project.getDependencyArtifacts()) {
            if (isHippoArtifact(artifact)) {
                artifactFiles.add(artifact.getFile().toURI().toURL());
            }
        }
        return artifactFiles.toArray(new URL[artifactFiles.size()]);
    }

    private boolean isHippoArtifact(final Artifact artifact) {
        final String groupId = artifact.getGroupId();
        return groupId != null && (groupId.startsWith("org.onehippo.cms") || groupId.startsWith("com.onehippo.cms"));
    }

}
