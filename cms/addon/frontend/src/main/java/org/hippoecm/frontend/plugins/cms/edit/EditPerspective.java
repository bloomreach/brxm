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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.Map;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.composite.Perspective;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.service.IEditService;

/**
 * Panel representing the content panel for the first tab.
 */
public class EditPerspective extends Perspective implements IEditService {
    private static final long serialVersionUID = 1L;

    @Override
    public void init(PluginContext context, String serviceId, Map<String, ParameterValue> properties) {
        super.init(context, serviceId, properties);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    public void edit(IModel model) {
        // TODO Auto-generated method stub

    }
}
