/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.xinha.htmlcleaner;

import nl.hippo.htmlcleaner.HtmlCleaner;
import nl.hippo.htmlcleaner.HtmlCleanerTemplate;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.IHtmlCleanerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlCleanerPlugin extends Plugin implements IHtmlCleanerService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(HtmlCleanerPlugin.class);

    private IPluginConfig htmlCleanerConfig;
    private transient HtmlCleanerTemplate htmlCleanerTemplate;

    public HtmlCleanerPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        htmlCleanerConfig = config.getPluginConfig("cleaner.config");
        if (htmlCleanerConfig != null) {
            try {
                context.registerService(this, IHtmlCleanerService.class.getName());
            } catch (Exception ex) {
                log.error("Exception whole creating HTMLCleaner template:", ex);
            }
        }
    }

    public String clean(final String value) throws Exception {
        if (htmlCleanerTemplate == null) {
            try {
                htmlCleanerTemplate = new JCRHtmlCleanerTemplateBuilder().buildTemplate(htmlCleanerConfig);
            } catch (Exception ex) {
                log.error("Exception whole creating HTMLCleaner template:", ex);
            }
        }

        if (htmlCleanerTemplate != null) {
            return new HtmlCleaner(htmlCleanerTemplate).cleanToString(value);
        } else {
            return value;
        }
    }

}
