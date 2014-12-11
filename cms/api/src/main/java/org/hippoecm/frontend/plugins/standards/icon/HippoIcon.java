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
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.plugins.standards.image.InlineSvg;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;

public class HippoIcon extends Panel {

    /**
     * Renders a hippo icon via a reference to the icon sprite.
     * @param id the Wicket id of the icon
     * @param icon the icon to render
     * @return the icon component
     */
    public static HippoIcon fromSprite(final String id, final Icon icon) {
        return new HippoIcon(id, icon, false);
    }

    /**
     * Renders a hippo icon as an inline SVG. This makes it possible to, for example,
     * style the individual shapes in the SVG via CSS.
     * @param id the Wicket id of the icon
     * @param icon the icon to render
     * @return the icon component
     */
    public static HippoIcon inline(final String id, final Icon icon) {
        return new HippoIcon(id, icon, true);
    }

    private HippoIcon(final String id, final Icon icon, final boolean inline) {
        super(id);

        setRenderBodyOnly(true);

        final WebMarkupContainer container = new WebMarkupContainer("container") {
            @Override
            protected void onComponentTag(final ComponentTag tag) {
                final Response response = RequestCycle.get().getResponse();
                if (inline) {
                    response.write(icon.getInlineSvg());
                } else {
                    response.write(icon.getSpriteReference());
                }
                super.onComponentTag(tag);
            }
        };
        container.setRenderBodyOnly(true);
        add(container);
    }

    /**
     * Renders an icon stored in a resource. When the icon's file extension is '.svg',
     * the icon is rendered as an inline SVG image.
     * @param id the Wicket id of the icon
     * @param reference the resource to render
     * @return the icon component
     */
    public static HippoIcon fromResource(final String id, final ResourceReference reference) {
        return fromResource(id, reference, -1, -1);
    }

    /**
     * Renders an icon stored in a resource, including 'width' and 'height' atrtibutes.
     * When the icon's file extension is '.svg', the icon is rendered as an inline SVG image.
     * @param id the Wicket id of the icon
     * @param reference the resource to render
     * @param size the size to use as width and height value, in pixels
     * @return the icon component
     */
    public static HippoIcon fromResource(final String id, final ResourceReference reference, final IconSize size) {
        return fromResource(id, reference, size.getSize(), size.getSize());
    }

    /**
     * Renders an icon stored in a resource, including 'width' and 'height' attributes.
     * When the icon's file extension is '.svg', the icon is rendered as an inline SVG image.
     * @param id the Wicket id of the icon
     * @param reference the resource to render
     * @param width the width of the icon in pixels
     * @param height the height of the icon in pixels
     * @return the icon component
     */
    public static HippoIcon fromResource(final String id, final ResourceReference reference, final int width, final int height) {
        return new HippoIcon(id, reference, width, height);
    }

    private HippoIcon(final String id, final ResourceReference reference, final int width, final int height) {
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
            
            if (width >= 0) {
                image.add(AttributeModifier.replace("width", width));
            }
            if (height >= 0) {
                image.add(AttributeModifier.replace("height", height));
            }
        }

        fragment.setRenderBodyOnly(true);
        add(fragment);
    }

    /*
     * Prevent instantiation via parent constructor.
     */
    private HippoIcon(final String id) {
        super(id);
    }

    /*
     * Prevent instantiation via parent constructor.
     */
    private HippoIcon(final String id, final IModel<?> model) {
        super(id, model);
    }

}
