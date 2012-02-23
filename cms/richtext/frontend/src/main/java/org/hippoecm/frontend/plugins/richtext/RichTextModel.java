/*
 *  Copyright 2010 Hippo.
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

import java.util.Set;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextModel implements IModel<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RichTextModel.class);

    private IModel<String> valueModel;
    private IHtmlCleanerService cleaner;
    private IRichTextLinkFactory linkFactory;

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
            String cleanedValue = clean(value);
            if (cleanedValue != null) {
                removeLinks(cleanedValue);
            }
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
        if (linkFactory != null) {
            linkFactory.detach();
        }
    }

    public void setCleaner(IHtmlCleanerService cleaner) {
        this.cleaner = cleaner;
    }

    public IHtmlCleanerService getCleaner() {
        return cleaner;
    }

    public void setLinkFactory(IRichTextLinkFactory linkService) {
        this.linkFactory = linkService;
    }

    public IRichTextLinkFactory getLinkFactory() {
        return linkFactory;
    }

    private String clean(final String value) throws Exception {
        if (cleaner != null) {
            return cleaner.clean(value);

        }
        return value;
    }

    private void removeLinks(String text) {
        if (linkFactory != null) {
            Set<String> linkNames = RichTextProcessor.getInternalLinks(text);
            linkFactory.cleanup(linkNames);
        }
    }

}
