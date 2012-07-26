/*
 *  Copyright 2009 Hippo.
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
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.CancelEventIfNoAjaxDecorator;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.Link;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;
import org.hippoecm.frontend.behaviors.IContextMenu;

abstract class MenuLink extends Link {

    private static final long serialVersionUID = 1L;

    public MenuLink(final String id) {
        super(id);

        if(isEnabled()) {
            add(new AjaxEventBehavior("onclick") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    IContextMenu parent = findParent(IContextMenu.class);
                    if (parent != null) {
                        parent.collapse(target);
                    }
                    onClick();
                }

                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    return new CancelEventIfNoAjaxDecorator(MenuLink.this.getAjaxCallDecorator());
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
            add(new EventStoppingBehavior("onclick"));
        }
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

    protected IAjaxCallDecorator getAjaxCallDecorator() {
        return new EventStoppingDecorator(null);
    }

}
