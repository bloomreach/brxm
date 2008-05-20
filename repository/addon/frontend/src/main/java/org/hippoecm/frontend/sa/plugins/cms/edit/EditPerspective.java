/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.sa.plugins.cms.edit;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.sa.core.IPluginConfig;
import org.hippoecm.frontend.sa.core.IPluginContext;
import org.hippoecm.frontend.sa.plugin.perspective.Perspective;
import org.hippoecm.frontend.service.IViewService;

/**
 * Panel representing the content panel for the first tab.
 */
public class EditPerspective extends Perspective implements IViewService {
    private static final long serialVersionUID = 1L;

    @Override
    public void init(IPluginContext context, IPluginConfig properties) {
        super.init(context, properties);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    public void view(IModel model) {
        // TODO Auto-generated method stub

    }
}
