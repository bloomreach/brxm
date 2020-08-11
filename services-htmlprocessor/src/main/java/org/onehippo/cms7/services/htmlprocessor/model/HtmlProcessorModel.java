/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.model;

import java.util.List;

import org.onehippo.cms7.services.htmlprocessor.HtmlProcessor;
import org.onehippo.cms7.services.htmlprocessor.TagVisitor;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlProcessorModel implements Model<String> {

    public static final Logger log = LoggerFactory.getLogger(HtmlProcessorModel.class);

    private final Model<String> valueModel;
    private final HtmlProcessorFactory factory;

    public HtmlProcessorModel(final Model<String> valueModel, final HtmlProcessorFactory factory) {
        this.valueModel = valueModel;
        this.factory = factory;
    }

    @Override
    public String get() {
        try {
            final HtmlProcessor processor = factory.getProcessor();
            return processor.read(valueModel.get(), getVisitors());
        } catch (final Exception e) {
            if (log.isInfoEnabled()) {
                log.warn("Value not retrieved because html processing failed", e);
            } else {
                log.warn("Value not retrieved because html processing failed : {}", e.toString());
            }
            return "";
        }
    }

    @Override
    public void set(final String value) {
        if (log.isDebugEnabled()) {
            log.debug("Processing value {}", value);
        }
        try {
            final HtmlProcessor processor = factory.getProcessor();
            final String processedValue = processor.write(value, getVisitors());
            valueModel.set(processedValue);
        } catch (final Exception e) {
            if (log.isInfoEnabled()) {
                log.warn("Value not set because html processing failed", e);
            } else {
                log.warn("Value not set because html processing failed : {}", e.toString());
            }
        }
    }

    public List<TagVisitor> getVisitors() {
        return null;
    }
}
