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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.skin.Icon;

/**
 * A stack of Hippo icons rendered on top of each other.
 */
public class HippoIconStack extends Panel {

    public enum Position {
        TOP_LEFT("top", "left"),
        TOP_CENTER("top", "hcenter"),
        TOP_RIGHT("top", "right"),
        CENTER_LEFT("vcenter", "left"),
        CENTER("vcenter", "hcenter"),
        CENTER_RIGHT("vcenter", "right"),
        BOTTOM_LEFT("bottom", "left"),
        BOTTOM_CENTER("bottom", "hcenter"),
        BOTTOM_RIGHT("bottom", "right");

        private final String vertical;
        private final String horizontal;

        Position(final String v, final String h) {
            vertical = v;
            horizontal = h;
        }
    }

    private final IconSize stackSize;
    private final RepeatingView icons;

    public HippoIconStack(final String id) {
        this(id, IconSize.M);
    }

    public HippoIconStack(final String id, final IconSize stackSize) {
        super(id);

        this.stackSize = stackSize;

        setRenderBodyOnly(true);

        final WebMarkupContainer stack = new WebMarkupContainer("stack");
        addCssClasses(stack, stackSize);
        add(stack);

        icons = new RepeatingView("icons");
        stack.add(icons);
    }

    private static void addCssClasses(final WebMarkupContainer stack, final IconSize size) {
        stack.add(CssClass.append("hi hi-stack hi-" + size.name().toLowerCase()));
    }

    public HippoIcon addFromSprite(final Icon icon) {
        return addFromSprite(icon, stackSize);
    }

    public HippoIcon addFromSprite(final Icon icon, final IconSize size) {
        return addFromSprite(icon, size, null);
    }

    public HippoIcon addFromSprite(final Icon icon, final IconSize size, final Position position) {
        validateSizeOfAddedIcon(size);

        final HippoIcon spriteIcon = HippoIcon.fromSprite(icons.newChildId(), icon, size);
        addIcon(spriteIcon, position);
        return spriteIcon;
    }

    private void validateSizeOfAddedIcon(final IconSize size) {
        if (size.getSize() > stackSize.getSize()) {
            throw new IllegalArgumentException("The size of the stacked icon (" + size + ")" +
                    " cannot be greater than the stack size itself (" + stackSize + ")");
        }
    }

    public HippoIcon addFromCms(final CmsIcon icon) {
        return addFromCms(icon, stackSize);
    }

    public HippoIcon addFromCms(final CmsIcon icon, final IconSize size) {
        return addFromCms(icon, size, null);
    }

    public HippoIcon addFromCms(final CmsIcon icon, final IconSize size, final Position position) {
        validateSizeOfAddedIcon(size);

        final HippoIcon inlineIcon = HippoIcon.inline(icons.newChildId(), icon);
        addIcon(inlineIcon, position);
        return inlineIcon;
    }

    public HippoIconStack addFromResource(final ResourceReference reference) {
        return addFromResource(reference, null);
    }

    public HippoIconStack addFromResource(final ResourceReference reference, final Position position) {
        final HippoIcon resourceIcon = HippoIcon.fromResource(icons.newChildId(), reference);
        addIcon(resourceIcon, position);
        return this;
    }

    public HippoIcon addCopyOf(final HippoIcon icon) {
        return addCopyOf(icon, null);
    }

    public HippoIcon addCopyOf(final HippoIcon icon, final Position position) {
        HippoIcon copy = HippoIcon.copy(icon, icons.newChildId());
        addIcon(copy, position);
        return copy;
    }

    public HippoIcon replaceFromSprite(final HippoIcon oldIcon, final Icon newIcon) {
        return replaceFromSprite(oldIcon, newIcon, null);
    }

    public HippoIcon replaceFromSprite(final HippoIcon oldIcon, final Icon newIcon, final Position position) {
        HippoIcon newCopy = HippoIcon.fromSprite(oldIcon.getId(), newIcon);
        addIcon(newCopy, position);
        return newCopy;
    }

    public HippoIcon replaceInline(final HippoIcon oldIcon, final CmsIcon newIcon) {
        return replaceInline(oldIcon, newIcon, null);
    }
    public HippoIcon replaceInline(final HippoIcon oldIcon, final CmsIcon newIcon, final Position position) {
        HippoIcon newCopy = HippoIcon.inline(oldIcon.getId(), newIcon);
        addIcon(newCopy, position);
        return newCopy;
    }

    public HippoIcon replaceFromResource(final HippoIcon oldIcon, final ResourceReference newReference) {
        return replaceFromResource(oldIcon, newReference, null);
    }

    public HippoIcon replaceFromResource(final HippoIcon oldIcon, final ResourceReference newReference,
                                         final Position position) {
        HippoIcon newCopy = HippoIcon.fromResource(oldIcon.getId(), newReference);
        addIcon(newCopy, position);
        return newCopy;
    }

    public HippoIcon replaceCopyOf(final HippoIcon oldIcon, final HippoIcon newIcon) {
        return replaceCopyOf(oldIcon, newIcon, null);
    }

    public HippoIcon replaceCopyOf(final HippoIcon oldIcon, final HippoIcon newIcon,
                                   final Position position) {
        HippoIcon newCopy = HippoIcon.copy(newIcon, oldIcon.getId());
        addIcon(newCopy, position);
        return newCopy;
    }

    private void addIcon(final HippoIcon icon, final Position position) {
        addPosition(icon, position);
        icons.addOrReplace(icon);
    }

    private void addPosition(final HippoIcon icon, final Position position) {
        if (position != null) {
            icon.addCssClass("hi-" + position.vertical);
            icon.addCssClass("hi-" + position.horizontal);
        }
    }
}
