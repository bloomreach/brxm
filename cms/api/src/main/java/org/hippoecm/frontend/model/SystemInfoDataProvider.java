/*
 *  Copyright 2008 Hippo.
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

import javax.jcr.Repository;
import javax.servlet.ServletContext;

import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.util.RepoUtils;
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

    public Iterator<Entry<String, String>> iterator(int first, int count) {
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

    public int size() {
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
            Manifest manifest = RepoUtils.getManifest(Home.class);
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
}
