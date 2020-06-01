/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;

/**
 * Renders an icon from the SVG sprite in a list cell.
 */
public class SpriteIconRenderer implements IListCellRenderer {

    private final Icon icon;
    private final IconSize size;

    public SpriteIconRenderer(final Icon icon, final IconSize size) {
        this.icon = icon;
        this.size = size;
    }

    @Override
    public Component getRenderer(final String id, final IModel model) {
        return HippoIcon.fromSprite(id, icon, size);
    }

    @Override
    public IObservable getObservable(final IModel model) {
        return null;
    }
}
