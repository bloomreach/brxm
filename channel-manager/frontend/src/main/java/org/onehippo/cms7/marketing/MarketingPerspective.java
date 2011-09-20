/**
 * Copyright 2010 Hippo
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

package org.onehippo.cms7.marketing;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.service.IconSize;
import org.onehippo.cms7.marketing.personas.PersonaManagerPanel;

public class MarketingPerspective extends Perspective {

    private final IModel<String> title;

    public MarketingPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        IPluginConfig wfConfig = config.getPluginConfig("layout.wireframe");
        if (wfConfig != null) {
            WireframeSettings wfSettings = new WireframeSettings(wfConfig);
            add(new WireframeBehavior(wfSettings));
        }

        title = new StringResourceModel("marketing-perspective-title", this, null);

        PersonaManagerPanel personaManagerPanel = new PersonaManagerPanel("persona-manager-panel");
        personaManagerPanel.setTitle(title);
        add(personaManagerPanel);
    }

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    public ResourceReference getIcon(IconSize type) {
        return new ResourceReference(MarketingPerspective.class, "marketing-perspective-" + type.getSize() + ".png");
    }

    @Override
    public IPluginContext getPluginContext() {
        return super.getPluginContext();
    }

}
