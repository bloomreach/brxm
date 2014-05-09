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

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class FilePluginService implements PluginConfigService {

    private static Logger log = LoggerFactory.getLogger(FilePluginService.class);

    private final PluginContext context;

    public FilePluginService(final PluginContext context) {
        this.context = context;
    }


    @Override
    public boolean write(final Document document) {
        final Document value = new ProjectSettingsBean();
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(ProjectSettingsBean.class);
            final Marshaller marshaller = jaxbContext.createMarshaller();
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            final StringWriter writer = new StringWriter();
            marshaller.marshal(value, writer);

            Document v = (Document) unmarshaller.unmarshal(new StringReader(writer.toString()));
        } catch (JAXBException e) {
            log.error("Error writing settings", e);
        }


        return false;
    }

    @Override
    public boolean write(final Document document, final String pluginId) {
        return false;
    }

    @Override
    public <T extends Document> T read(final String pluginClass, final Class<T> clazz) {
        return null;
    }

    @Override
    public <T extends Document> T read(final Class<T> clazz) {
        return null;
    }

    @Override
    public void close() throws Exception {
        // do nothing
    }
}
