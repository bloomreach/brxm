/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.console.behavior;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;

/**
 * Helper class for rendering the HCM origin path for a property as a title attribute on an icon, and for hiding
 * that icon if the origin is blank.
 */
public class OriginTitleBehavior extends Behavior {
    final IModel<String> model;

    public OriginTitleBehavior(final IModel<String> model) {
        this.model = model;
    }

    @Override
    public void onConfigure(final Component component) {
        component.setVisible(StringUtils.isNotBlank(model.getObject()));
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        tag.put("title", model.getObject());
    }
}