/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.Home;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemInfoDataProvider implements IDataProvider<Map.Entry<String, String>> {

    private static final Logger log = LoggerFactory.getLogger(SystemInfoDataProvider.class);

    private static final double MB = (double) 1024 * 1024;
    public static final String UNKNOWN = "unknown";

    public class SystemInfoDataEntry implements Map.Entry<String, String>, Serializable {

        String key;

        public SystemInfoDataEntry(String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return info.get(key);
        }

        @Override
        public String setValue(String value) {
            throw new UnsupportedOperationException("SystemInfo is read only");
        }

    }

    private final Map<String, String> info = new LinkedHashMap<>();

    public SystemInfoDataProvider() {
        refresh();
    }

    @Override
    public Iterator<Entry<String, String>> iterator(long first, long count) {
        return info.entrySet().iterator();
    }

    @Override
    public IModel<Map.Entry<String, String>> model(Map.Entry<String, String> object) {
        final Map.Entry<String, String> entry = new SystemInfoDataEntry((object).getKey());
        return () -> entry;
    }

    public void refresh() {
        Runtime runtime = Runtime.getRuntime();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        info.clear();
        info.put("Project version", getProjectVersion());
        info.put("Bloomreach release version", getReleaseVersion());
        info.put("Memory maximum", nf.format(((double) runtime.maxMemory()) / MB) + " MB");
        info.put("Java vendor", System.getProperty("java.vendor"));
        info.put("Java version", System.getProperty("java.version"));
        info.put("Java VM", System.getProperty("java.vm.name"));
        info.put("OS architecture", System.getProperty("os.arch"));
        info.put("OS name", System.getProperty("os.name"));
        info.put("OS version", System.getProperty("os.version"));
        info.put("Processors", "# " + runtime.availableProcessors());
    }

    @Override
    public long size() {
        return info.size();
    }

    @Override
    public void detach() {
    }

    public String getReleaseVersion() {
        try {
            final Manifest manifest = getWebAppManifest();
            if (manifest != null) {
                String releaseVersion = manifest.getMainAttributes().getValue("BloomReach-Release-Version");
                if (releaseVersion != null) {
                    return releaseVersion;
                }
                // for backwards compatibility
                releaseVersion = manifest.getMainAttributes().getValue("Hippo-Release-Version");
                if (releaseVersion != null) {
                    return releaseVersion;
                }
            }
            return getCMSVersion();
        } catch (IOException iOException) {
            log.debug("Error occurred getting the cms release version from the webapp-manifest.", iOException);
        }
        return UNKNOWN;
    }

    public String getProjectVersion() {
        try {
            final Manifest manifest = getWebAppManifest();
            if (manifest != null) {
                return buildVersionString(manifest);
            }
        } catch (IOException iOException) {
            log.debug("Error occurred getting the project version from the webapp-manifest.", iOException);
        }
        return System.getProperty("project.version", UNKNOWN);
    }

    public String getCMSVersion() {
        final String implVersion = Home.class.getPackage().getImplementationVersion();
        if (implVersion != null) {
            return implVersion;
        }
        return UNKNOWN;
    }

    private Manifest getWebAppManifest() throws IOException {
        final ServletContext servletContext = WebApplication.get().getServletContext();
        final InputStream manifestInputStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF");
        if (manifestInputStream != null) {
            return new Manifest(manifestInputStream);
        }

        final File manifestFile = new File(servletContext.getRealPath("/"), "META-INF/MANIFEST.MF");
        if (manifestFile.exists()) {
            return new Manifest(new FileInputStream(manifestFile));
        }
        return null;
    }

    private String buildVersionString(final Manifest manifest) {
        StringBuilder versionString = new StringBuilder();

        final Attributes attributes = manifest.getMainAttributes();
        final String projectVersion = attributes.getValue("Project-Version");
        if (projectVersion != null) {
            versionString.append(projectVersion);
        }
        final String projectBuild = attributes.getValue("Project-Build");
        if (projectBuild != null && !"-1".equals(projectBuild)) {
            if (versionString.length() > 0) {
                versionString.append(", build: ");
            }
            versionString.append(projectBuild);
        }
        return versionString.toString();
    }

}
