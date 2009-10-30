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
package org.hippoecm.frontend.plugins.cms.admin.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepositoryFactory;

public class SystemInfoDataProvider implements IDataProvider {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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
        info.put("Hippo CMS version", getCMSVersion());
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

    private String getCMSVersion() {
        StringBuffer sb = new StringBuffer();
        ServletContext servletContext = ((WebApplication) Application.get()).getServletContext();
        try {
            InputStream istream = servletContext.getResourceAsStream("META-INF/MANIFEST.MF");
            if (istream == null) {
                File manifestFile = new File(servletContext.getRealPath("/"), "META-INF/MANIFEST.MF");
                if (manifestFile.exists()) {
                    istream = new FileInputStream(manifestFile);
                }
            }
            if (istream == null) {
                try {
                    istream = HippoRepositoryFactory.getManifest(getClass()).openStream();
                } catch (FileNotFoundException ex) {
                } catch (IOException ex) {
                }
            }
            if (istream != null) {
                Manifest manifest = new Manifest(istream);
                Attributes atts = manifest.getMainAttributes();
                if (atts.getValue("Implementation-Version") != null) {
                    sb.append(atts.getValue("Implementation-Version"));
                }
                if (atts.getValue("Implementation-Build") != null) {
                    sb.append(" build ");
                    sb.append(atts.getValue("Implementation-Build"));
                }
            }
        } catch (IOException ex) {
            // deliberate ignore
        }
        if (sb.length() == 0) {
            return "unknown";
        } else {
            return sb.toString();
        }
    }

    private String getRepositoryVersion() {
        Repository repository = ((UserSession) Session.get()).getJcrSession().getRepository();
        if (repository != null) {
            return repository.getDescriptor(Repository.REP_VERSION_DESC);
        } else {
            return "unknown";
        }
    }
    
    private String getRepositoryVendor() {
        Repository repository = ((UserSession) Session.get()).getJcrSession().getRepository();
        if (repository != null) {
            return repository.getDescriptor(Repository.REP_NAME_DESC);
        } else {
            return "unknown";
        }
    }
}
