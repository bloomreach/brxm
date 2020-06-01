/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.BreadCrumbLink;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.request.cycle.RequestCycle;

public abstract class AjaxBreadCrumbLink extends BreadCrumbLink {

    public AjaxBreadCrumbLink(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);

        add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                onClick(target);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                // add the onclick handler only if link is enabled
                if (isEnabledInHierarchy()) {
                    super.onComponentTag(tag);
                }
            }
        });
    }

    public final void onClick() {
        onClick(RequestCycle.get().find(AjaxRequestTarget.class));
    }

    /**
     * Listener method invoked on the ajax request generated when the user clicks the link
     *
     * @param target Request target associated with current ajax request.
     */
    public void onClick(final AjaxRequestTarget target) {
        super.onClick();
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
