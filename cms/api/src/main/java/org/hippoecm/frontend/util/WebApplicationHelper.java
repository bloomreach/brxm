/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.xml.bind.DatatypeConverter;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.hippoecm.frontend.PluginApplication;

import static org.slf4j.LoggerFactory.getLogger;


public class WebApplicationHelper {

    public static final String HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME = "hal";
    public static final String REMEMBERME_COOKIE_BASE_NAME = "rememberme";
    public static final String APPLICATION_HASH = calculateApplicationHash();

    private WebApplicationHelper() {
    }

    public static String getFullyQualifiedCookieName(String cookieBaseName) {
        validateNotBlank(cookieBaseName);

        return getApplicationName() + "." + cookieBaseName;
    }

    public static void clearCookie(String cookieName) {
        validateNotBlank(cookieName);

        Cookie cookie = ((WebRequest) RequestCycle.get().getRequest()).getCookie(cookieName);

        if (cookie != null) {
            cookie.setMaxAge(0);
            cookie.setValue("");
            ((WebResponse) RequestCycle.get().getResponse()).addCookie(cookie);
        }
    }

    public static ServletWebRequest retrieveWebRequest() {
        return (ServletWebRequest) RequestCycle.get().getRequest();
    }

    public static WebResponse retrieveWebResponse() {
        return (WebResponse) RequestCycle.get().getResponse();
    }

    public static String getApplicationName() {
        return PluginApplication.get().getPluginApplicationName();
    }

    public static boolean isDevelopmentMode() {
        return Application.get().getConfigurationType().equals(RuntimeConfigurationType.DEVELOPMENT);
    }

    public static boolean isPartOfPage(final Component component) {
        return component.findParent(Page.class) != null;
    }

    public static UrlResourceReference createUniqueUrlResourceReference(Url url) {
        url.addQueryParameter("antiCache", APPLICATION_HASH);
        return new UrlResourceReference(url);
    }

    protected static void validateNotBlank(String value) {
        if (value == null || "".equals(value)) {
            throw new IllegalArgumentException("Null or '' are not allowed values!");
        }
    }

    /**
     * Unique hash based on the last modified dates of all the jars on the classpath
     */
    private static String calculateApplicationHash() {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final List<File> jarFiles = getAllWebAppJarFiles();
            for (File jarFile : jarFiles) {
                md5.update(String.valueOf(jarFile.lastModified()).getBytes());
            }
            String s = DatatypeConverter.printBase64Binary(md5.digest());
            // make url friendly
            return s.replace('+', '.').replace('/', '-').replace('=', '_');
        } catch (NoSuchAlgorithmException | IOException e) {
            final String msg = "Failed to calculate application hash";
            getLogger(WebApplicationHelper.class).error(msg, e);
        }
        return String.valueOf(System.currentTimeMillis());
    }

    private static List<File> getAllWebAppJarFiles() throws IOException {
        List<File> jarFiles = new ArrayList<>();
        Enumeration<URL> manifests = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
        while (manifests.hasMoreElements()) {
            final URL manifest = manifests.nextElement();
            if (manifest.getFile().contains("WEB-INF/lib")) {
                try {
                    final File baseJarFile = getBaseJarFileFromURL(manifest);
                    if (baseJarFile != null) {
                        jarFiles.add(baseJarFile);
                    } else {
                        final String msg = String.format("Manifest %s does not seem to be inside a jar", manifest.getFile());
                        getLogger(WebApplicationHelper.class).debug(msg);
                    }
                } catch (URISyntaxException e) {
                    final String msg = String.format("Failed to get jar file from manifest %s", manifest.getFile());
                    getLogger(WebApplicationHelper.class).error(msg, e);
                }
            }
        }
        Collections.sort(jarFiles, new Comparator<File>() {
            @Override
            public int compare(final File o1, final File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return jarFiles;
    }

    private static File getBaseJarFileFromURL(final URL url) throws URISyntaxException {
        String file = url.getFile();
        int offset = file.indexOf(".jar!");

        if (offset == -1) {
            return null;
        }

        file = file.substring(0, offset + 4);

        if (!file.startsWith("file:")) {
            if (file.startsWith("/")) {
                file = "file://" + file;
            } else {
                file = "file:///" + file;
            }
        }

        return new File(URI.create(file));
    }

}
