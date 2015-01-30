/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext;

import org.apache.wicket.model.IModel;
import org.htmlcleaner.TagNodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextModel implements IModel<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RichTextModel.class);

    private IModel<String> valueModel;
    private IHtmlCleanerService cleaner;
    private UuidConverterBuilder converterBuilder;

    public RichTextModel(IModel<String> valueModel, IHtmlCleanerService cleaner) {
        this.valueModel = valueModel;
        this.cleaner = cleaner;
    }

    public RichTextModel(IModel<String> valueModel, IHtmlCleanerService cleaner, UuidConverterBuilder converterBuilder) {
        this.valueModel = valueModel;
        this.cleaner = cleaner;
        this.converterBuilder = converterBuilder;
    }

    public String getObject() {
        try {
            TagNodeVisitor converter = null;
            if (converterBuilder != null) {
                converter = converterBuilder.createRetrievalConverter();
            }
            return cleaner.clean(valueModel.getObject(), false,
                    null, converter);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Value not retrieved because html cleaning failed", e);
            } else {
                log.warn("Value not retrieved because html cleaning failed : {}", e.toString());
            }
            return "";
        }
    }

    public void setObject(String value) {
        if (log.isDebugEnabled()) {
            log.debug("Cleaning value {}", value);
        }
        try {
            TagNodeVisitor converter = null;
            if (converterBuilder != null) {
                converter = converterBuilder.createStorageConverter();
            }
            valueModel.setObject(cleaner.clean(value, true,
                    converter, null));
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Value not set because html cleaning failed", e);
            } else {
                log.warn("Value not set because html cleaning failed : {}", e.toString());
            }
        }
    }

    public void detach() {
        valueModel.detach();
        if (converterBuilder != null) {
            converterBuilder.detach();
        }
    }

}
