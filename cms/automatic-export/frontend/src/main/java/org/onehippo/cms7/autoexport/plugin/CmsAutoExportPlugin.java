/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport.plugin;

import org.apache.commons.lang3.Validate;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.INestedBrowserContextService;

/**
 * The CMS version of the AutoExportPlugin does not reveal itself when auto-export is not available.
 */
public class CmsAutoExportPlugin extends AutoExportPlugin {
    public CmsAutoExportPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected boolean isLinkVisible() {
        return isExportAvailable();
    }
    
    @Override
    public boolean isVisible() {
        INestedBrowserContextService nestedBrowserContextService = getPluginContext()
                .getService(INestedBrowserContextService.class.getName(),INestedBrowserContextService.class);
        final String message = String.format("%s should not be null, make sure it's registered on the %s"
                , INestedBrowserContextService.class.getName(), IPluginContext.class.getName());
        Validate.notNull(nestedBrowserContextService, message);
   		return isLinkVisible() && !nestedBrowserContextService.hidePerspectiveMenu();
   	}
}
