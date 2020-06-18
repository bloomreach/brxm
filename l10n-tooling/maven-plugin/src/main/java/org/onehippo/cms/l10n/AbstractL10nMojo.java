/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
    
    @Parameter(defaultValue = "excel")
    private String format;
    
    @Parameter
    private String[] excludes;

    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    protected enum FileFormat {
        Excel, CSV
    }

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

    protected final FileFormat getFileFormat() throws MojoExecutionException {
        if (StringUtils.isEmpty(format)) {
            throw new MojoExecutionException("No format specified");
        }
        if (format.equals("excel")) {
            return FileFormat.Excel;
        }
        if (format.equals("csv") || format.startsWith("csv,")) {
            return FileFormat.CSV;
        }
        throw new MojoExecutionException("Unrecognized format: " + format);
    }

    protected final String getCSVFormat() throws MojoExecutionException {
        if (!format.contains(",")) {
            return "Default";
        }
        final String csvFormat = StringUtils.substringAfter(format, ",");
        if (StringUtils.isEmpty(csvFormat)) {
            throw new MojoExecutionException("No CSV format specified");
        }
        try {
            CSVFormat.valueOf(csvFormat);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Unrecognized CSV format: " + csvFormat);
        }
        return csvFormat;
    }
    
    protected final ClassLoader getResourcesClassLoader() throws MalformedURLException {
        return new URLClassLoader(getHippoArtifactFiles());
    }
    
    protected String[] getExcludes() {
        if (excludes == null) {
            return new String[] {};
        }
        return excludes;
    }
    
    private URL[] getHippoArtifactFiles() throws MalformedURLException {
        final Collection<URL> artifactFiles = new ArrayList<>();
        for (Artifact artifact : project.getDependencyArtifacts()) {
            if (ArtifactInfo.isBloomreachArtifactGroupId(artifact.getGroupId())) {
                artifactFiles.add(artifact.getFile().toURI().toURL());
            }
        }
        return artifactFiles.toArray(new URL[artifactFiles.size()]);
    }


}
