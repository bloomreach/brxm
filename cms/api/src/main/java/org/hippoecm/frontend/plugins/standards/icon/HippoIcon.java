/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.icon;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.plugins.standards.image.InlineSvg;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;

public class HippoIcon extends Panel {

    public HippoIcon(final String id, final Icon icon) {
        super(id);

        setRenderBodyOnly(true);

        final WebMarkupContainer container = new WebMarkupContainer("container") {
            @Override
            protected void onComponentTag(final ComponentTag tag) {
                final Response response = RequestCycle.get().getResponse();
                response.write(icon.getInlineSvg());
                super.onComponentTag(tag);
            }
        };
        container.setRenderBodyOnly(true);
        add(container);
    }

    public HippoIcon(final String id, final ResourceReference reference) {
        this(id, reference, null, null);
    }

    public HippoIcon(final String id, final ResourceReference reference, IconSize size) {
        this(id, reference, size.getSize(), size.getSize());
    }

    public HippoIcon(final String id, final ResourceReference reference, final Integer width, final Integer height) {
        super(id);
        
        setRenderBodyOnly(true);

        Fragment fragment;
        if (reference.getExtension().equalsIgnoreCase("svg")) {
            fragment = new Fragment("container", "svgFragment", this);
            fragment.add(new InlineSvg("svg", reference));
        } else {
            fragment = new Fragment ("container", "imageFragment", this);
            Image image = new CachingImage("image", reference);
            fragment.add(image);
            
            if (width != null) {
                image.add(AttributeModifier.replace("width", width));
            }
            if (height != null) {
                image.add(AttributeModifier.replace("height", height));
            }
        }

        fragment.setRenderBodyOnly(true);
        add(fragment);
    }
}
