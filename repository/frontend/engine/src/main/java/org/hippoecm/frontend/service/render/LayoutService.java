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
package org.hippoecm.frontend.service.render;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayoutService extends RenderService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(LayoutService.class);

    public static final String EXTENSIONS = "wicket.extensions";
    public static final String VARIATION = "wicket.variation";

    private String variation;

    public LayoutService(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String[] extensions = config.getStringArray(EXTENSIONS);
        if (extensions != null) {
            for (String extension : extensions) {
                addExtensionPoint(extension);
            }
        } else {
            log.error("No extensions defined");
        }

        variation = config.getString(VARIATION);
    }

    @Override
    public String getVariation() {
        if (variation != null) {
            return variation;
        }
        return super.getVariation();
    }

}
