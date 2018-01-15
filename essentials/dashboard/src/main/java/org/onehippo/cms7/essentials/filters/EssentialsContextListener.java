/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.essentials.filters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.onehippo.cms7.essentials.WebUtils;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads plugin descriptors from jar files and caches those once application is initialized
 */
public class EssentialsContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(EssentialsContextListener.class);
    private static final LoadingCache<String, PluginDescriptor> PLUGIN_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            // keep it for a while:
            .expireAfterAccess(365, TimeUnit.DAYS).build(new CacheLoader<String, PluginDescriptor>() {
                @Override
                public PluginDescriptor load(final String key) throws Exception {
                    // we load cache ourselves
                    return null;
                }
            });


    public static LoadingCache<String, PluginDescriptor> getPluginCache() {
        return PLUGIN_CACHE;
    }

    /**
     * Add plugin to plugin cache
     *
     * @param descriptor final PluginDescriptorRestful descriptor
     */
    public static void addToPluginToCache(final PluginDescriptor descriptor) {
        if (descriptor == null) {
            log.error("Plugin descriptor was null");
            return;
        }
        PLUGIN_CACHE.put(descriptor.getId(), descriptor);
    }


    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        PLUGIN_CACHE.invalidateAll();
        scanPluginDescriptors(servletContextEvent.getServletContext());
    }


    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        PLUGIN_CACHE.invalidateAll();
    }

    private void scanPluginDescriptors(final ServletContext context) {

        final Set<String> libraryDirectory = context.getResourcePaths(EssentialConst.WEB_INF_LIB);
        if (libraryDirectory != null) {
            for (final String path : libraryDirectory) {
                if (path.endsWith(EssentialConst.JAR_EXT)) {
                    try {
                        final String realPath = context.getRealPath(path);
                        if (realPath != null) {
                            log.debug("Real path is: {}", realPath);
                            processUrl(new File(realPath));
                        } else {
                            // unpacked wars don't work well with essentials:
                            throw new UnsupportedOperationException("War needs to be unpacked");
                        }
                    } catch (IOException e) {
                        log.error("Error processing url", e);
                    }
                }
            }
        }
    }

    private void processUrl(final File file) throws IOException {
        log.debug("Processing jar file: {}", file);
        try (final JarFile jar = new JarFile(file)) {
            final ZipEntry entry = jar.getEntry("plugin-descriptor.json");
            if (entry != null) {
                final InputStream inputStream = jar.getInputStream(entry);
                final String content = GlobalUtils.readStreamAsText(inputStream);
                if (Strings.isNullOrEmpty(content)) {
                    log.warn("Plugin descriptor was empty for: {}", file);
                    return;
                }
                final PluginDescriptor descriptor = WebUtils.fromJson(content, PluginDescriptor.class);
                if (descriptor != null) {
                    final String id = descriptor.getId();
                    log.debug("Cache plugin for id: {}", id);
                    PLUGIN_CACHE.put(id, descriptor);
                } else {
                    log.error("=======================================");
                    log.error("Invalid plugin descriptor: {}", content);
                    log.error("=======================================");
                }
            } else {
                log.debug("No plugin-descriptor.json in file: {}", file);
            }
        }
    }
}
