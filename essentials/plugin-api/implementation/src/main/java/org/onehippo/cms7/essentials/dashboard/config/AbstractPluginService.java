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

import com.google.common.base.Strings;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;

/**
 * Created by tjeger on 6/4/14.
 */
public abstract class AbstractPluginService implements PluginConfigService {

    private static final String SETTINGS_EXTENSION = ".xml";
    private static Logger log = LoggerFactory.getLogger(AbstractPluginService.class);
    private final PluginContext context;

    public AbstractPluginService(final PluginContext context) {
        this.context = context;
    }

    public abstract boolean write(Document document);
    public abstract boolean write(Document document, String pluginId);
    public abstract <T extends Document> T read(final String pluginId, final Class<T> clazz);
    public abstract <T extends Document> T read(final Class<T> clazz);

    @Override
    public void close() throws Exception {
        // do nothing
    }

    /**
     * Determine name of a configuration file
     *
     * @param document   instance of a document represented by the configuration file
     * @param pluginName name of a plugin (may be null)
     * @return           name of the corresponding configuraion file.
     */
    protected String getFileName(final Document document, final String pluginName) {
        String fileName = pluginName;
        if (Strings.isNullOrEmpty(fileName)) {
            if (Strings.isNullOrEmpty(document.getName())) {
                fileName = GlobalUtils.validFileName(document.getClass().getName());
            } else {
                fileName = GlobalUtils.validFileName(document.getName());
            }
        }
        if(!fileName.endsWith(SETTINGS_EXTENSION)) {
            fileName = fileName + SETTINGS_EXTENSION;
        }
        return fileName;
    }

    protected String getFilePath(final Document document, final String pluginName) {
        return context.getEssentialsResourcePath() + File.separator + getFileName(document, pluginName);
    }

    /**
     * Marshal a document into a writer in order to serialize settings.
     *
     * @param writer
     * @param document
     * @return true upon success, false otherwise.
     */
    protected boolean marshalWriter(final Writer writer, final Document document) {
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(document.getClass());
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(document, writer);
            return true;
        } catch (JAXBException e) {
            log.error("Error writing settings", e);
        }
        return false;
    }

    /**
     * Unmarshal an input stream into a document/bean of type clazz.
     *
     * @param stream input stream to read from
     * @param clazz  destination class
     * @param <T>    return type
     * @return       document representing the parsed input stream
     */
    @SuppressWarnings("unchecked")
    protected <T extends Document> T unmarshalStream(final InputStream stream, final Class<T> clazz) {
        final String setting = GlobalUtils.readStreamAsText(stream);
        log.debug("setting {}", setting);

        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (T) unmarshaller.unmarshal(new StringReader(setting));
        } catch (JAXBException e) {
            log.error("Error reading settings", e);
        }
        return null;
    }
}