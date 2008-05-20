/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.plugin.render;

import org.hippoecm.frontend.sa.core.IPlugin;
import org.hippoecm.frontend.sa.core.IPluginContext;
import org.hippoecm.frontend.service.render.RenderService;

public class RenderPlugin extends RenderService implements IPlugin {
    private static final long serialVersionUID = 1L;

    public void start(IPluginContext context) {
        init(context, context.getProperties());
    }

    public void stop() {
        destroy();
    }

}
