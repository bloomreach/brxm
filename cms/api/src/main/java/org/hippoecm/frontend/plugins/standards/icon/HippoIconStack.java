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

    private RepeatingView icons;

    public HippoIconStack(final String id) {
        this(id, IconSize.M);
    }

    public HippoIconStack(final String id, final IconSize size) {
        super(id);

        setRenderBodyOnly(true);

        final WebMarkupContainer stack = new WebMarkupContainer("stack");
        addCssClasses(stack, size);
        add(stack);

        icons = new RepeatingView("icons");
        stack.add(icons);
    }

    private static void addCssClasses(final WebMarkupContainer stack, final IconSize size) {
        stack.add(CssClass.append("hi hi-stack hi-" + size.name().toLowerCase()));
    }

    public HippoIcon addFromSprite(final Icon icon) {
        final HippoIcon spriteIcon = HippoIcon.fromSprite(icons.newChildId(), icon);
        icons.add(spriteIcon);
        return spriteIcon;
    }

    public HippoIcon addInline(final CmsIcon icon) {
        final HippoIcon inlineIcon = HippoIcon.inline(icons.newChildId(), icon);
        icons.add(inlineIcon);
        return inlineIcon;
    }

    public HippoIcon addFromResource(final ResourceReference reference) {
        final HippoIcon resourceIcon = HippoIcon.fromResource(icons.newChildId(), reference);
        icons.add(resourceIcon);
        return resourceIcon;
    }

    public HippoIcon addCopyOf(final HippoIcon icon) {
        HippoIcon copy = HippoIcon.copy(icon, icons.newChildId());
        icons.add(copy);
        return copy;
    }

    public HippoIcon replaceFromSprite(final HippoIcon oldIcon, final Icon newIcon) {
        HippoIcon newCopy = HippoIcon.fromSprite(oldIcon.getId(), newIcon);
        icons.replace(newCopy);
        return newCopy;
    }

    public HippoIcon replaceInline(final HippoIcon oldIcon, final CmsIcon newIcon) {
        HippoIcon newCopy = HippoIcon.inline(oldIcon.getId(), newIcon);
        icons.replace(newCopy);
        return newCopy;
    }

    public HippoIcon replaceFromResource(final HippoIcon oldIcon, final ResourceReference newReference) {
        HippoIcon newCopy = HippoIcon.fromResource(oldIcon.getId(), newReference);
        icons.replace(newCopy);
        return newCopy;
    }

    public HippoIcon replaceCopyOf(final HippoIcon oldIcon, final HippoIcon newIcon) {
        HippoIcon newCopy = HippoIcon.copy(newIcon, oldIcon.getId());
        icons.replace(newCopy);
        return newCopy;
    }

}
