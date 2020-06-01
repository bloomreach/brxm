/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

    private static final long serialVersionUID = 1L;

    public MenuLink(final String id) {
        super(id);

        if(isEnabled()) {
            Form form = getForm();
            if (form != null) {
                add(new AjaxFormSubmitBehavior(form, "onclick") {

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target) {
                        IContextMenu parent = findParent(IContextMenu.class);
                        if (parent != null) {
                            parent.collapse(target);
                        } else {
                            IContextMenuManager manager = findParent(IContextMenuManager.class);
                            if (manager != null) {
                                manager.collapseAllContextMenus();
                            }
                        }
                        onClick();
                    }

                    @Override
                    protected void onError(final AjaxRequestTarget target) {
                    }

                    @Override
                    protected CharSequence getPreconditionScript() {
                        return "return true;";
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        // add the onclick handler only if link is enabled
                        if (isLinkEnabled()) {
                            super.onComponentTag(tag);
                        }
                    }
                });
            } else {
                add(new AjaxEventBehavior("onclick") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        IContextMenu parent = findParent(IContextMenu.class);
                        if (parent != null) {
                            parent.collapse(target);
                        } else {
                            IContextMenuManager manager = findParent(IContextMenuManager.class);
                            if (manager != null) {
                                manager.collapseAllContextMenus();
                            }
                        }
                        onClick();
                    }

                    @Override
                    protected CharSequence getPreconditionScript() {
                        return "return true;";
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        // add the onclick handler only if link is enabled
                        if (isLinkEnabled()) {
                            super.onComponentTag(tag);
                        }
                    }
                });
            }
        } else {
            add(new EventStoppingBehavior("onclick"));
        }
    }

    protected Form getForm() {
        return null;
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (isLinkEnabled()) {
            // disable any href attr in markup
            if (tag.getName().equalsIgnoreCase("a") || tag.getName().equalsIgnoreCase("link")
                    || tag.getName().equalsIgnoreCase("area")) {
                tag.put("href", "#");
            }
        } else {
            disableLink(tag);
        }
    }

}
