/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class FilePluginService implements PluginConfigService {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
    private static Logger log = LoggerFactory.getLogger(FilePluginService.class);
    public static final String SETTINGS_EXTENSION = ".xml";


    private final PluginContext context;

    public FilePluginService(final PluginContext context) {
        this.context = context;
    }


    @Override
    public boolean write(final Document document) {
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(document.getClass());
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            final String path = getFilePath(document);
            log.info("Writing settings of: {}", path);
            final Writer writer = new FileWriter(new File(path));
            marshaller.marshal(document, writer);
            return true;
        } catch (JAXBException e) {
            log.error("Error writing settings", e);
        } catch (IOException e) {
            log.error("Error writing file", e);
        }


        return false;
    }


    @Override
    public boolean write(final Document document, final String pluginId) {
        if (!Strings.isNullOrEmpty(pluginId)) {
            document.setName(pluginId);
        }
        return write(document);
    }

    @Override
    public <T extends Document> T read(final String pluginClass, final Class<T> clazz) {
        return read(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Document> T read(final Class<T> clazz) {
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(clazz.getClass());
            final Marshaller m = jaxbContext.createMarshaller();
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            final String path = getFilePath(GlobalUtils.newInstance(clazz));
            log.info("Reading settings of: {}", path);
            final String setting = GlobalUtils.readStreamAsText(new FileInputStream(path));
            return (T) unmarshaller.unmarshal(new StringReader(setting));
        } catch (JAXBException e) {
            log.error("Error reading settings", e);
        } catch (IOException e) {
            log.error("Error reading file", e);
        }

        return null;


    }

    @Override
    public void close() throws Exception {
        // do nothing
    }

    //############################################
    // UTILS
    //############################################

    private String getFilePath(final Document document) {
        final File essentialsDirectory = context.getEssentialsDirectory();
        final String fileName;
        if (Strings.isNullOrEmpty(document.getName())) {
            fileName = fileNameForClass(document);
        } else {
            fileName = document.getName() + SETTINGS_EXTENSION;
        }
        return essentialsDirectory.getAbsolutePath() + File.separator + fileName;
    }


    private String fileNameForClass(final Document document) {
        final String name = document.getClass().getName();
        return DOT_PATTERN.matcher(name).replaceAll("_") + SETTINGS_EXTENSION;
    }


}
