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
package org.hippoecm.frontend.plugins.standards.icon;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugins.standards.image.CachedJcrImage;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.plugins.standards.image.InlineSvg;
import org.hippoecm.frontend.plugins.standards.image.JcrImage;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.skin.Icon;

public abstract class HippoIcon extends Panel {

    private HippoIcon(final String id, IModel<?> model) {
        super(id, model);
    }

    /**
     * Renders a hippo icon of size {@link IconSize#M} via a reference to the icon sprite.
     * @param id the Wicket id of the icon
     * @param icon the icon to render
     * @return the icon component
     */
    public static HippoIcon fromSprite(final String id, final Icon icon) {
        return fromSprite(id, icon, IconSize.M);
    }

    /**
     * Renders a hippo icon via a reference to the icon sprite.
     * @param id the Wicket id of the icon
     * @param icon the icon to render
     * @param size the size of the icon
     * @return the icon component
     */
    public static HippoIcon fromSprite(final String id, final Icon icon, final IconSize size) {
        return new SpriteIcon(id, icon, size);
    }

    /**
     * Renders a hippo icon of size {@link IconSize#M} via a reference to the icon sprite.
     * @param id the Wicket id of the icon
     * @param model the model containing the icon to render
     * @return the icon component
     */
    public static HippoIcon fromSprite(final String id, final IModel<Icon> model) {
        return fromSprite(id, model, IconSize.M);
    }

    /**
     * Renders a hippo icon via a reference to the icon sprite.
     * @param id the Wicket id of the icon
     * @param model the model containing the icon to render
     * @param size the size of the icon
     * @return the icon component
     */
    public static HippoIcon fromSprite(final String id, final IModel<Icon> model, final IconSize size) {
        return new SpriteIcon(id, model, size);
    }

    /**
     * Renders a hippo icon of size {@link IconSize#M} as an inline SVG. This makes it possible to,
     * for example, style the individual shapes in the SVG via CSS.
     * @param id the Wicket id of the icon
     * @param icon the icon to render
     * @return the icon component
     */
    public static HippoIcon inline(final String id, final CmsIcon icon) {
        return new InlineSvgIcon(id, icon, IconSize.M);
    }

    /**
     * Renders a hippo icon of size {@link IconSize#M} as an inline SVG. This makes it possible
     * to, for example, style the individual shapes in the SVG via CSS.
     * @param id the Wicket id of the icon
     * @param model the model containing the icon to render
     * @return the icon component
     */
    public static HippoIcon inline(final String id, final IModel<CmsIcon> model) {
        return new InlineSvgIcon(id, model, IconSize.M);
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
     * Renders an icon stored in a resource, including 'width' and 'height' attributes.
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
        return new ResourceIcon(id, reference, width, height);
    }

    /**
     * Renders an icon stored in a resource which is referenced by a Wicket model.
     * When the icon's file extension is '.svg', the icon is rendered as an inline SVG image.
     * @param id the Wicket id of the icon
     * @param model the model containing the resource to render
     * @return the icon component
     */
    public static HippoIcon fromResourceModel(final String id, final IModel<ResourceReference> model) {
        return new ResourceIcon(id, model, -1, -1);
    }

    /**
     * Renders and icon/image from a JcrResourceStream which is referenced by an {@link IModel}. Delegates rendering
     * to the {@link JcrImage} which simply outputs an {@code <img />} element.
     * @param id the Wicket id of the icon
     * @param model the model containing the icon stream
     * @param width the width of the icon in pixels
     * @param height the height of the icon in pixels
     * @return the icon component
     */
    public static HippoIcon fromStream(final String id, final IModel<JcrResourceStream> model, final int width, final int height) {
        return new StreamIcon(id, model, width, height);
    }

    /**
     * Renders and icon/image from a JcrResourceStream which is referenced by an {@link IModel}. Delegates rendering
     * to the {@link JcrImage} which simply outputs an {@code <img />} element.
     * @param id the Wicket id of the icon
     * @param model the model containing the icon stream
     * @return the icon component
     */
    public static HippoIcon fromStream(final String id, final IModel<JcrResourceStream> model) {
        return fromStream(id, model, -1, -1);
    }

    /**
     * Renders a copy of the given icon, with a different Wicket ID.
     * @param icon the icon to render
     * @param newId the new Wicket ID
     * @return a copy of the given icon
     */
    public static HippoIcon copy(final HippoIcon icon, final String newId) {
        if (icon instanceof SpriteIcon) {
            return new SpriteIcon(newId, (SpriteIcon)icon);
        } else if (icon instanceof InlineSvgIcon) {
            return new InlineSvgIcon(newId, (InlineSvgIcon)icon);
        } else if (icon instanceof ResourceIcon) {
            return new ResourceIcon(newId, (ResourceIcon)icon);
        }
        throw new IllegalStateException("Expected HippoIcon's class to be either SpriteIcon, InlineSvgIcon or ResourceIcon, but got " + icon.getClass());
    }

    /**
     * Adds a CSS class to the top-level element of the rendered icon.
     * @param cssClass the CSS class to add.
     */
    public abstract void addCssClass(final String cssClass);


    private static abstract class BaseIcon<T> extends HippoIcon {

        private List<String> extraCssClasses;

        private BaseIcon(final String id, final IModel<T> model) {
            super(id, model);
            setRenderBodyOnly(true);
        }

        @Override
        public void addCssClass(final String cssClass) {
            if (extraCssClasses == null) {
                extraCssClasses = new LinkedList<>();
            }
            extraCssClasses.add(cssClass);
        }

        protected String getExtraCssClasses() {
            return extraCssClasses == null ? StringUtils.EMPTY : StringUtils.join(extraCssClasses, " ");
        }

        @SuppressWarnings("unchecked")
        T getIcon() {
            return (T) getDefaultModelObject();
        }
    }

    private static abstract class BaseSvgIcon<T> extends BaseIcon<T> {

        private final IconSize size;

        private BaseSvgIcon(final String id, final IModel<T> model, final IconSize size) {
            super(id, model);

            this.size = size;

            final WebMarkupContainer container = new WebMarkupContainer("iconContainer") {
                @Override
                protected void onComponentTag(final ComponentTag tag) {
                    final Response response = RequestCycle.get().getResponse();
                    final String svgMarkup = getSvgMarkup(size, getExtraCssClasses());
                    response.write(svgMarkup);
                    super.onComponentTag(tag);
                }
            };
            container.setRenderBodyOnly(true);
            add(container);
        }

        IconSize getSize() {
            return size;
        }

        abstract String getSvgMarkup(final IconSize size, final String optionalCssClasses);
    }

    private static class SpriteIcon extends BaseSvgIcon<Icon> {

        private SpriteIcon(final String id, final IModel<Icon> model, final IconSize size) {
            super(id, model, size);
        }

        private SpriteIcon(final String id, final Icon icon, final IconSize size) {
            this(id, Model.of(icon), size);
        }

        private SpriteIcon(final String newId, final SpriteIcon original) {
            this(newId, original.getIcon(), original.getSize());
        }

        @Override
        String getSvgMarkup(final IconSize size, final String optionalCssClasses) {
            return getIcon().getSpriteReference(size, optionalCssClasses);
        }
    }

    private static class InlineSvgIcon extends BaseSvgIcon<CmsIcon> {

        private InlineSvgIcon(final String id, final IModel<CmsIcon> model, final IconSize size) {
            super(id, model, size);
        }

        private InlineSvgIcon(final String id, final CmsIcon icon, final IconSize size) {
            this(id, Model.of(icon), size);
        }

        private InlineSvgIcon(final String newId, final InlineSvgIcon original) {
            this(newId, original.getIcon(), original.getSize());
        }

        @Override
        String getSvgMarkup(final IconSize size, final String optionalCssClasses) {
            return getIcon().getInlineSvg(size, optionalCssClasses);
        }
    }

    private static class ResourceIcon extends BaseIcon<ResourceReference> {

        private static final String WICKET_ID_CONTAINER = "container";
        private static final String WICKET_ID_IMAGE = "image";
        private static final String WICKET_ID_SVG = "svg";
        private static final String WICKET_FRAGMENT_IMAGE = "imageFragment";
        private static final String WICKET_FRAGMENT_SVG = "svgFragment";

        private int width;
        private int height;

        private ResourceIcon(final String id, final IModel<ResourceReference> model, final int width, final int height) {
            super(id, model);

            this.width = width;
            this.height = height;

            setRenderBodyOnly(true);
        }

        private ResourceIcon(final String id, final ResourceReference reference, final int width, final int height) {
            this(id, Model.of(reference), width, height);
        }

        private ResourceIcon(final String newId, final ResourceIcon original) {
            this(newId, original.getIcon(), original.width, original.height);
        }

        @Override
        protected void onBeforeRender() {
            Fragment fragment;

            final ResourceReference icon = getIcon();
            if (icon.getExtension().equalsIgnoreCase("svg")) {
                fragment = new Fragment(WICKET_ID_CONTAINER, WICKET_FRAGMENT_SVG, this);
                fragment.add(new InlineSvg(WICKET_ID_SVG, icon, getExtraCssClasses()));
            } else {
                fragment = new Fragment (WICKET_ID_CONTAINER, WICKET_FRAGMENT_IMAGE, this);
                final Image image = new CachingImage(WICKET_ID_IMAGE, icon);
                image.add(CssClass.append(getExtraCssClasses()));

                fragment.add(image);

                if (width >= 0) {
                    image.add(AttributeModifier.replace("width", width));
                }
                if (height >= 0) {
                    image.add(AttributeModifier.replace("height", height));
                }
            }

            fragment.setRenderBodyOnly(true);
            addOrReplace(fragment);

            super.onBeforeRender();
        }
    }

    private static class StreamIcon extends BaseIcon<JcrResourceStream> {

        private StreamIcon(final String id, final IModel<JcrResourceStream> model, final int width, final int height) {
            super(id, model);
            setRenderBodyOnly(true);

            final CachedJcrImage streamIcon = new CachedJcrImage("streamIcon", getIcon(), width, height);
            streamIcon.add(CssClass.append(getExtraCssClasses()));
            add(streamIcon);
        }
    }
}
