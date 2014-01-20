/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsPlugin;
import org.onehippo.cms7.essentials.dashboard.model.PluginNamespaceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: PluginScanner.java 160632 2013-04-12 09:43:49Z mmilicevic $"
 */
public class PluginScanner {

    private static Logger log = LoggerFactory.getLogger(PluginScanner.class);

    public static Marshaller createMarshaller() {
        try {
            final JAXBContext context = JAXBContext.newInstance(EssentialsPlugin.class);
            final Marshaller m = context.createMarshaller();
            m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new PluginNamespaceMapper());
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            return m;
        } catch (JAXBException e) {
            log.error("Error creating marshaller", e);
        }
        return null;
    }

    public static Unmarshaller createUnmarshaller() {

        try {
            final JAXBContext context = JAXBContext.newInstance(EssentialsPlugin.class);
            return context.createUnmarshaller();
        } catch (JAXBException e) {
            log.error("Error creating unmarshaller", e);
        }
        return null;
    }

    public List<Plugin> scan(final String path) {

        final File directory = new File(path);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("Path doesn't exists or is not a directory {}", path);
            return Collections.emptyList();
        }

        final Unmarshaller unmarshaller = createUnmarshaller();
        if (unmarshaller == null) {
            return Collections.emptyList();
        }


        final Collection<File> files = FileUtils.listFiles(directory, EssentialConst.JAR_FILTER, true);
        final List<Plugin> plugins = new ArrayList<>();
        for (File file : files) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(file);
                final JarEntry entry = jarFile.getJarEntry("META-INF/plugin.xml");
                if (entry == null) {
                    continue;
                }
                final InputStream input = jarFile.getInputStream(entry);
                final StringWriter writer = new StringWriter();
                IOUtils.copy(input, writer, Charset.forName("UTF-8"));
                final String xml = writer.toString();
                Plugin plugin = (Plugin) unmarshaller.unmarshal(new StringReader(xml));
                plugins.add(plugin);

            } catch (IOException e) {
                log.error(String.format("Error reading jar file: %s", file.getAbsolutePath()), e);
            } catch (JAXBException e) {
                log.error("Error parsing plugin descriptor:", e);
            } finally {
                IOUtils.closeQuietly(jarFile);
            }

        }
        return plugins;
    }
}
