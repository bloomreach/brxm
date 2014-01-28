/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class JarResourceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    public static final Pattern PLUGIN_CLASS_PATTERN = Pattern.compile("(?:^[/]*)([a-z,A-z.]+)");
    private static Logger log = LoggerFactory.getLogger(JarResourceServlet.class);


    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final String servletPath = req.getServletPath();
        final String pathInfo = coalesce(req.getPathInfo(), "");
        final String path = servletPath + pathInfo;
        log.info("@Processing static plugin path {}", path);
        try {

            final String pluginClassName = getPluginClassName(path);
            if(Strings.isNullOrEmpty(pluginClassName)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            final URL url = getServletContext().getResource(path);
            if (url == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            final ZipEntry zipEntry = ((JarURLConnection) url.openConnection()).getJarEntry();
            if (zipEntry != null && !zipEntry.isDirectory()) {
                new StaticFile(zipEntry.getTime(), getServletContext().getMimeType(path), zipEntry.getSize(), url).respondGet(resp);
            }
        } catch (MalformedURLException e) {
            log.error("Invalid url", e);

        }

    }


    @SafeVarargs
    public static <T> T coalesce(T... ts) {
        for (T t : ts) {
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    public String getPluginClassName(final CharSequence input) {
        final Matcher matcher = JarResourceServlet.PLUGIN_CLASS_PATTERN.matcher(input);

        final boolean matches = matcher.matches();
        if (matches) {
            return matcher.group(1);
        }

        return null;
    }

    private static class StaticFile {
        private final long lastModified;
        private final String mimeType;
        private final int contentLength;
        private final URL url;

        @SuppressWarnings("NumericCastThatLosesPrecision")
        public StaticFile(long lastModified, String mimeType, long contentLength, URL url) {
            this.lastModified = lastModified;
            this.mimeType = mimeType;
            this.contentLength = (int) contentLength;
            this.url = url;
        }

        public long getLastModified() {
            return lastModified;
        }


        protected void setHeaders(HttpServletResponse resp) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(mimeType);
            resp.setContentLength(contentLength);
        }

        public void respondGet(HttpServletResponse resp) throws IOException {
            setHeaders(resp);
            final OutputStream os = resp.getOutputStream();
            IOUtils.copy(url.openStream(), os);

        }

        public void respondHead(HttpServletResponse resp) {
            setHeaders(resp);
        }
    }
}
