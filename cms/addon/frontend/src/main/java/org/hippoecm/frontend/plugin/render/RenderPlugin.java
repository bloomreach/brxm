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
package org.hippoecm.frontend.plugin.render;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.service.render.RenderService;

public class RenderPlugin extends RenderService implements Plugin {
    private static final long serialVersionUID = 1L;

    public static final String MODEL_ID = "wicket.model";
    public static final String WICKET_ID = "wicket.id";
    public static final String PARENT_ID = "wicket.parent";

    public void start(PluginContext context) {
        init(context, context.getProperty(Plugin.SERVICE_ID), context.getProperty(PARENT_ID), context
                .getProperty(WICKET_ID), context.getProperty(MODEL_ID));
    }

    public void stop() {
        destroy();
    }

}
