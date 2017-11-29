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

package org.onehippo.cms7.essentials.dashboard.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.inject.Singleton;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.onehippo.cms7.essentials.dashboard.service.LoggingService;
import org.onehippo.cms7.essentials.dashboard.utils.Dom4JUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Singleton
public class LoggingServiceImpl implements LoggingService {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingServiceImpl.class);

    @Override
    public boolean addLoggerToLog4jConfiguration(final File configuration, final String loggerName, final String level) {
        try {
            Document doc = new SAXReader().read(configuration);
            Element logger = loggerFor(doc, loggerName);
            if (logger == null) {
                logger = createLogger(doc, loggerName);
            }
            logger.addAttribute("level", level);
            writeLog4j2(doc, configuration);
            return true;
        } catch (DocumentException | IOException e) {
            LOG.error("Failed to add Logger to '{}'.", configuration.getAbsolutePath(), e);
        }
        return false;
    }

    private Element loggerFor(final Document doc, final String loggerName) {
        final String selector = String.format("/Configuration/*[name()='Loggers']/*[name()='Logger' and @name='%s']",
                loggerName);
        return (Element) doc.getRootElement().selectSingleNode(selector);
    }

    private Element createLogger(final Document doc, final String loggerName) {
        final String selector = "/Configuration/*[name()='Loggers']";
        Element loggers = (Element) doc.getRootElement().selectSingleNode(selector);
        if (loggers == null) {
            Element configuration = (Element) doc.getRootElement().selectSingleNode("/Configuration");
            if (configuration == null) {
                configuration = Dom4JUtils.addIndentedSameNameSibling(doc.getRootElement(), "Configuration", null);
            }
            loggers = Dom4JUtils.addIndentedSameNameSibling(configuration, "Loggers", null);
        }
        return Dom4JUtils.addIndentedSameNameSibling(loggers, "Logger", null)
                .addAttribute("name", loggerName);
    }

    private void writeLog4j2(Document doc, File target) throws IOException {
        FileWriter writer = new FileWriter(target);
        doc.write(writer);
        writer.close();
    }
}
