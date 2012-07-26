/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend;

import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;

public class NoRepositoryAvailablePage extends PluginPage {

    public NoRepositoryAvailablePage() {
        super(new IApplicationFactory() {

            public IPluginConfigService getDefaultApplication() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public IPluginConfigService getApplication(String name) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });

        error(new ClassResourceModel("repository.not.available", NoRepositoryAvailablePage.class).getObject());
    }
}
