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
package org.hippoecm.frontend.editor.layout;

import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayoutProviderPlugin extends Plugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LayoutProviderPlugin.class);

    public LayoutProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        ILayoutProvider layoutProvider = new LayoutProvider(new LoadableDetachableModel<ClassLoader>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected ClassLoader load() {
                return UserSession.get().getClassLoader();
            }

        });
        context.registerService(layoutProvider, ILayoutProvider.class.getName());
    }

}
