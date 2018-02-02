/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.Element;
import org.onehippo.cms7.essentials.sdk.api.service.LoggingService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.Dom4JUtils;
import org.springframework.stereotype.Service;

@Service
public class LoggingServiceImpl implements LoggingService {

    @Override
    public boolean addLoggerToLog4jConfiguration(final File configuration, final String loggerName, final String level) {
        return Dom4JUtils.update(configuration, doc -> {
            Element logger = loggerFor(doc, loggerName);
            if (logger == null) {
                logger = createLogger(doc, loggerName);
            }
            logger.addAttribute("level", level);
        });
    }

    private Element loggerFor(final Document doc, final String loggerName) {
        final String selector = String.format("/Configuration/Loggers/Logger[@name='%s']",
                loggerName);
        return (Element) doc.getRootElement().selectSingleNode(selector);
    }

    private Element createLogger(final Document doc, final String loggerName) {
        Element loggers = (Element) doc.getRootElement().selectSingleNode("/Configuration/Loggers");
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
}
