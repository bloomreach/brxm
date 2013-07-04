/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.jcr.Repository;
import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemInfoDataProvider implements IDataProvider {

    private static final Logger log = LoggerFactory.getLogger(SystemInfoDataProvider.class);

    private static final long serialVersionUID = 1L;

    private final static double MB = 1024 * 1024;

    public class SystemInfoDataEntry implements Map.Entry<String, String>, Serializable {
        private static final long serialVersionUID = 1L;

        String key;
        
        public SystemInfoDataEntry(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return info.get(key);
        }

        public String setValue(String value) {
            throw new UnsupportedOperationException("SystemInfo is read only");
        }
        
    }
    
    Map<String, String> info = new LinkedHashMap<String, String>();

    public SystemInfoDataProvider() {
        refresh();
    }

    public Iterator<Entry<String, String>> iterator(long first, long count) {
        return info.entrySet().iterator();
    }

    public IModel model(Object object) {
        final Map.Entry<String, String> entry = new SystemInfoDataEntry(((Map.Entry<String, String>) object).getKey());
        return new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                return entry;
            }
        };
    }

    public void refresh() {
        Runtime runtime = Runtime.getRuntime();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        info.clear();
        info.put("Memory maximum", nf.format(((double) runtime.maxMemory()) / MB) + " MB");
        info.put("Memory taken", nf.format(((double) runtime.totalMemory()) / MB) + " MB");
        info.put("Memory free", nf.format(((double) runtime.freeMemory()) / MB) + " MB");
        info.put("Memory in use", nf.format(((double) (runtime.totalMemory() - runtime.freeMemory())) / MB) + " MB");
        info.put("Memory total free", nf.format(((double) 
                (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory())) / MB) + " MB");
        info.put("Hippo Release Version", getReleaseVersion());
        info.put("Hippo CMS version", getCMSVersion());
        info.put("Project Version", getProjectVersion());
        info.put("Repository vendor", getRepositoryVendor());
        info.put("Repository version", getRepositoryVersion());
        info.put("Java vendor", System.getProperty("java.vendor"));
        info.put("Java version", System.getProperty("java.version"));
        info.put("Java VM", System.getProperty("java.vm.name"));
        info.put("OS architecture", System.getProperty("os.arch"));
        info.put("OS name", System.getProperty("os.name"));
        info.put("OS version", System.getProperty("os.version"));
        info.put("Processors", "# " + runtime.availableProcessors());
    }

    public long size() {
        return info.size();
    }

    public void detach() {
    }

    public String getReleaseVersion() {
        try {
            final Manifest manifest = getWebAppManifest();
            if (manifest != null) {
                return manifest.getMainAttributes().getValue("Hippo-Release-Version");
            }
        } catch(IOException iOException) {
            log.debug("Error occurred getting the hippo cms release version from the webapp-manifest.", iOException);
        }
        return "unknown";
    }

    public String getProjectVersion() {
        try {
            final Manifest manifest = getWebAppManifest();
            if (manifest != null) {
                return buildVersionString(manifest, "Project-Version", "Project-Build");
            }
        } catch(IOException iOException) {
            log.debug("Error occurred getting the project version from the webapp-manifest.", iOException);
        }
        return "unknown";
    }

    public String getCMSVersion() {
        try {
            Manifest manifest = getManifest(Home.class);
            if (manifest == null) {
                manifest = getWebAppManifest();
            }
            if (manifest != null) {
                return buildVersionString(manifest, "Implementation-Version", "Implementation-Build");
            }
        } catch (IOException iOException) {
            log.debug("Error occurred getting the cms version from the manifest.", iOException);
        }

        return "unknown";
    }

    private Manifest getWebAppManifest() throws IOException {
        final ServletContext servletContext = WebApplication.get().getServletContext();
        final InputStream manifestInputStream = servletContext.getResourceAsStream("META-INF/MANIFEST.MF");
        if (manifestInputStream != null) {
            return new Manifest(manifestInputStream);
        }

        final File manifestFile = new File(servletContext.getRealPath("/"), "META-INF/MANIFEST.MF");
        if (manifestFile.exists()) {
            return new Manifest(new FileInputStream(manifestFile));
        }
        return null;
    }

    private String buildVersionString(final Manifest manifest, final String versionAttribute, final String buildAttribute) {
        StringBuilder versionString = new StringBuilder();

        final Attributes attributes = manifest.getMainAttributes();
        final String projectVersion = attributes.getValue(versionAttribute);
        if (projectVersion != null) {
            versionString.append(projectVersion);
        }
        final String projectBuild = attributes.getValue(buildAttribute);
        if (projectBuild != null && !"-1".equals(projectBuild)) {
            if (versionString.length() > 0) {
                versionString.append(", build: ");
            }
            versionString.append(projectBuild);
        }
        return versionString.toString();
    }

    public String getRepositoryVersion() {
        Repository repository = UserSession.get().getJcrSession().getRepository();
        if (repository != null) {
            return repository.getDescriptor(Repository.REP_VERSION_DESC);
        } else {
            return "unknown";
        }
    }
    
    public String getRepositoryVendor() {
        Repository repository = UserSession.get().getJcrSession().getRepository();
        if (repository != null) {
            return repository.getDescriptor(Repository.REP_NAME_DESC);
        } else {
            return "unknown";
        }
    }


    /**
     * @param clazz the class object for which to obtain a reference to the manifest
     * @return the URL of the manifest found, or {@code null} if it could not be obtained
     */
    private static URL getManifestURL(Class clazz) {
        try {
            final StringBuilder sb = new StringBuilder();
            final String[] classElements = clazz.getName().split("\\.");
            for (int i=0; i<classElements.length-1; i++) {
                sb.append("../");
            }
            sb.append("META-INF/MANIFEST.MF");
            final URL classResource = clazz.getResource(classElements[classElements.length-1]+".class");
            if (classResource != null) {
                return new URL(classResource, new String(sb));
            }
        } catch (MalformedURLException ignore) {
        }
        return null;
    }

    /**
     *
     * @param clazz  the class object for which to obtain the manifest
     * @return  the manifest object, or {@code null} if it could not be obtained
     * @throws IOException  if something went wrong while reading the manifest
     */
    private static Manifest getManifest(Class clazz) throws IOException {
        final URL url = getManifestURL(clazz);
        if (url != null) {
            final InputStream is = url.openStream();
            try {
                return new Manifest(is);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return null;
    }
}
