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
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.IAjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;

public abstract class DualAjaxLink extends AbstractLink implements IAjaxLink {

    private static final long serialVersionUID = 1L;

    public DualAjaxLink(final String id) {
        this(id, null);
    }

    public DualAjaxLink(final String id, final IModel model) {
        super(id, model);

        add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            protected void onEvent(AjaxRequestTarget target) {
                onClick(target);
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(new EventStoppingDecorator());
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                if (isEnabledInHierarchy()) {
                    super.onComponentTag(tag);
                }
            }
        });

        add(new AjaxEventBehavior("oncontextmenu") {
            private static final long serialVersionUID = 1L;

            protected void onEvent(AjaxRequestTarget target) {
                onRightClick(target);
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(new EventStoppingDecorator());
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                if (isEnabledInHierarchy()) {
                    super.onComponentTag(tag);
                }
            }
        });
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (isEnabledInHierarchy()) {
            if (tag.getName().equalsIgnoreCase("a") || tag.getName().equalsIgnoreCase("link") || tag.getName().equalsIgnoreCase("area")) {
                tag.put("href", "#");
            }
        } else {
            disableLink(tag);
        }

    }

    public abstract void onClick(final AjaxRequestTarget target);

    public void onRightClick(final AjaxRequestTarget target) {
        onClick(target);
    }
}
