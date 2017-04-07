/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.processor.html;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlProcessorServiceConfig implements Serializable {

    public static final Logger log = LoggerFactory.getLogger(HtmlProcessorServiceConfig.class);

    private Map<String, HtmlProcessorConfig> configs;
    private Map<String, HtmlProcessor> processors;

    void reconfigure(final Node config) {
        configs = new HashMap<>();
        processors = new HashMap<>();

        try {
            final NodeIterator iterator = config.getNodes();
            while (iterator.hasNext()) {
                final Node child = iterator.nextNode();
                HtmlProcessorConfig processorConfig = new HtmlProcessorConfig();
                processorConfig.reconfigure(child);
                configs.put(child.getName(), processorConfig);
            }
        } catch (RepositoryException e) {
            log.error("Failed to create Html processor config");
        }
    }

    /**
     * Returns instance of HtmlProcessor or null if the configuration cannot be found
     * @param id The HTML processor id
     * @return Instance of HTML processor
     */
    HtmlProcessor getProcessor(final String id) {
        if (!processors.containsKey(id)) {
            if (configs.containsKey(id)) {
                processors.put(id, new HtmlProcessorImpl(configs.get(id)));
            }
        }
        return processors.get(id);
    }
}
