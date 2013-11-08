/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextModel implements IModel<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RichTextModel.class);

    private IModel<String> valueModel;
    private IHtmlCleanerService cleaner;

    public RichTextModel(IModel<String> valueModel) {
        this.valueModel = valueModel;
    }

    public String getObject() {
        return valueModel.getObject();
    }

    public void setObject(String value) {
        if (log.isDebugEnabled()) {
            log.debug("Cleaning value {}", value);
        }
        try {
            final String cleanedValue = clean(value);
            valueModel.setObject(cleanedValue);
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.error("Value not set because html cleaning failed", e);   
            } else {
                log.info("Value not set because html cleaning failed.");
            }
        }
    }

    public void detach() {
        valueModel.detach();
    }

    public void setCleaner(IHtmlCleanerService cleaner) {
        this.cleaner = cleaner;
    }

    public IHtmlCleanerService getCleaner() {
        return cleaner;
    }

    private String clean(final String value) throws Exception {
        if (cleaner != null) {
            return cleaner.clean(value);

        }
        return value;
    }

}
