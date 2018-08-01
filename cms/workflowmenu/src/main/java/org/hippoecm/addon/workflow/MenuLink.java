/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.addon.workflow;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.behaviors.IContextMenuManager;

abstract class MenuLink extends Link {

    public MenuLink(final String id) {
        super(id);

        if (isEnabled()) {
            final Form form = getForm();
            if (form != null) {
                add(new AjaxFormSubmitBehavior(form, "click") {

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target) {
                        collapseContextMenu(target);
                        onClick();
                    }

                    @Override
                    protected void onError(final AjaxRequestTarget target) {
                    }

                });
            } else {
                add(new AjaxEventBehavior("click") {

                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        collapseContextMenu(target);
                        onClick();
                    }
                });
            }
        } else {
            add(new EventStoppingBehavior("click"));
        }
    }

    private void collapseContextMenu(final AjaxRequestTarget target) {
        final IContextMenu parent = findParent(IContextMenu.class);
        if (parent != null) {
            parent.collapse(target);
        } else {
            final IContextMenuManager manager = findParent(IContextMenuManager.class);
            if (manager != null) {
                manager.collapseAllContextMenus();
            }
        }
    }

    protected Form getForm() {
        return null;
    }

    @Override
    protected void onComponentTag(final ComponentTag tag) {
        super.onComponentTag(tag);

        if (isEnabledInHierarchy()) {
            final String tagName = tag.getName();
            if (tagName.equalsIgnoreCase("a") ||
                tagName.equalsIgnoreCase("link") ||
                tagName.equalsIgnoreCase("area")) {

                // disable any href attr in markup
                tag.put("href", "javascript:;");
            }
        } else {
            disableLink(tag);
        }
    }
}
