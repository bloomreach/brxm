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

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.processor.html.visit.TagVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface HtmlProcessorFactory extends Serializable {

    Logger log = LoggerFactory.getLogger(HtmlProcessorFactory.class);

    String RICHTEXT_PROCESSOR_SERVICE = "richtext";
    String FORMATTED_HTML_PROCESSOR_SERVICE = "formatted";
    String DEFAULT_HTML_PROCESSOR_SERVICE = "no-filter";

    String DEPRECATED_FORMATTED_HTMLCLEANER_ID = "org.hippoecm.frontend.plugins.richtext.DefaultHtmlCleanerService";
    String DEPRECATED_RICHTEXT_HTMLCLEANER_ID = "org.hippoecm.frontend.plugins.richtext.IHtmlCleanerService";

    HtmlProcessor NOOP = new HtmlProcessor() {
        @Override
        public String read(final String html, final List<TagVisitor> ignore) throws IOException {
            return html;
        }

        @Override
        public String write(final String html, final List<TagVisitor> ignore) throws IOException {
            return html;
        }
    };

    HtmlProcessor getProcessor();

    static HtmlProcessorFactory of(final String id) {
        return () -> {
            final String processorId = parseProcessorId(id);
            final HtmlProcessorService service = HippoServiceRegistry.getService(HtmlProcessorService.class);
            if (service == null) {
                log.warn("Could not load HtmlProcessorService, returning NOOP HtmlProcessor");
                return NOOP;
            }

            final HtmlProcessor processor = service.getHtmlProcessor(processorId);
            if (processor == null) {
                log.warn("Could not load HtmlProcessor with id '{}', returning NOOP HtmlProcessor", processorId);
                return NOOP;
            }
            return processor;
        };
    }

    static String parseProcessorId(final String id) {
        if (StringUtils.isBlank(id)) {
            log.info("CKEditor plugin does not have a server-side HTML processor configured, using default");
            return DEFAULT_HTML_PROCESSOR_SERVICE;
        }

        if (id.equals(DEPRECATED_RICHTEXT_HTMLCLEANER_ID)) {
            log.warn("HtmlProcessor id '{}' has been replaced by '{}', please update the configuration.",
                     DEPRECATED_RICHTEXT_HTMLCLEANER_ID, RICHTEXT_PROCESSOR_SERVICE);
            return RICHTEXT_PROCESSOR_SERVICE;
        } else if (id.equals(DEPRECATED_FORMATTED_HTMLCLEANER_ID)) {
            log.warn("HtmlProcessor id '{}' has been replaced by '{}', please update the configuration.",
                     DEPRECATED_FORMATTED_HTMLCLEANER_ID, FORMATTED_HTML_PROCESSOR_SERVICE);
            return FORMATTED_HTML_PROCESSOR_SERVICE;
        }
        return id;

    }
}
